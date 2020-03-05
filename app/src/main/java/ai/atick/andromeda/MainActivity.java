package ai.atick.andromeda;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

import static android.speech.SpeechRecognizer.RESULTS_RECOGNITION;

public class MainActivity extends AppCompatActivity implements RecognitionListener{

    MqttAndroidClient client;
    boolean connectionFlag = false;

    LineChart[] lineChart = new LineChart[3];
    TextView tempValue, humValue, lightValue, lightStatus, fanStatus, speechPrompt;
    LinearLayout fanController, lightController, dots;
    ImageView fabButton;

    String brokerURL = "tcp://192.168.0.101:1883";
    String receivedMessage = "";

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                String[] values = receivedMessage.split(",");
                int l = Integer.parseInt(values[0]);
                int f = Integer.parseInt(values[1]);
                if (l == 1) {
                    lightStatus.setText(R.string.__on__);
                    YoYo.with(Techniques.Flash).duration(700).playOn(lightStatus);
                } else {
                    lightStatus.setText(R.string.__off__);
                    YoYo.with(Techniques.Flash).duration(700).playOn(lightStatus);
                }
                if (f == 1) {
                    fanStatus.setText(R.string.__on__);
                    YoYo.with(Techniques.Flash).duration(700).playOn(fanStatus);
                } else {
                    fanStatus.setText(R.string.__off__);
                    YoYo.with(Techniques.Flash).duration(700).playOn(fanStatus);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        requestPermissions();
        /////////////////////////////////////////////////////
        lineChart[0] = findViewById(R.id.line_chart_0);
        lineChart[1] = findViewById(R.id.line_chart_1);
        lineChart[2] = findViewById(R.id.line_chart_2);
        /////////////////////////////////////////////////////
        tempValue = findViewById(R.id.temp_value);
        humValue = findViewById(R.id.hum_value);
        lightValue = findViewById(R.id.light_value);
        lightStatus = findViewById(R.id.light_status);
        fanStatus = findViewById(R.id.fan_status);
        fanController = findViewById(R.id.fan_controller);
        lightController = findViewById(R.id.light_controller);
        fabButton = findViewById(R.id.fab);
        speechPrompt = findViewById(R.id.speech_prompt);
        dots = findViewById(R.id.dots);
        /////////////////////////////////////////////////////

        /////////////////////////////////////////////////////
        fanController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("__some_topic__", "f");
            }
        });
        lightController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("__some_topic__", "l");
            }
        });
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (vibrator != null) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
                promptSpeechInput();
                speechPrompt.setVisibility(View.VISIBLE);
                YoYo.with(Techniques.BounceInUp).duration(700).playOn(speechPrompt);
                YoYo.with(Techniques.RollOut).duration(700).playOn(fabButton);
                //fabButton.setVisibility(View.GONE);
            }
        });
        /////////////////////////////////////////////////////
        connectToBroker(brokerURL);
        /////////////////////////////////////////////////////
        for (int i = 0; i < 3; i++) {
            lineChart[i].getDescription().setText("");
            lineChart[i].getAxisLeft().setDrawLabels(false);
            lineChart[i].getAxisLeft().setEnabled(false);
            lineChart[i].getAxisRight().setDrawLabels(false);
            lineChart[i].getAxisRight().setEnabled(false);
            lineChart[i].getXAxis().setDrawLabels(false);
            lineChart[i].getXAxis().setEnabled(false);
            lineChart[i].getLegend().setEnabled(false);
            lineChart[i].setTouchEnabled(false);
        }
        /////////////////////////////////////////////////////
        float[] temp = {0f, 0f};
        float[] hum = {0f, 0f};
        float[] light = {0f, 0f};
        new updateCharts().drawCharts(temp, hum, light);
        /////////////////////////////////////////////////////
        new updateCharts().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class updateCharts extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            String update = "";
            try {
                /////////////////////////////////////////////////////////////////////
                URL url = new URL("http://192.168.0.101:5000/api/sensors/");
                /////////////////////////////////////////////////////////////////////
                HttpURLConnection httpURLConnection = null;
                try {
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    update = stringBuilder.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return update;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                JSONArray dataArray = new JSONArray(s);
                JSONObject[] dataObject = new JSONObject[dataArray.length()];
                /////////////////////////////////////////////////
                float[] temp = new float[dataArray.length()];
                float[] hum = new float[dataArray.length()];
                float[] light = new float[dataArray.length()];
                /////////////////////////////////////////////////
                for (int i = 0; i < dataArray.length(); i++) {
                    dataObject[i] = dataArray.getJSONObject(i);
                }
                /////////////////////////////////////////////////
                for (int i = 0; i < dataArray.length(); i++) {
                    temp[i] = Float.parseFloat(dataObject[i].getString("temp"));
                    hum[i] = Float.parseFloat(dataObject[i].getString("hum"));
                    light[i] = Float.parseFloat(dataObject[i].getString("light"));
                }
                tempValue.setText(String.format(Locale.getDefault(), "%d C", (int) temp[5]));
                humValue.setText(String.format(Locale.getDefault(), "%d %%", (int) hum[5]));
                lightValue.setText(String.format(Locale.getDefault(), "%d LUX", (int) light[5]));
                YoYo.with(Techniques.DropOut).duration(1000).playOn(dots);
                drawCharts(temp, hum, light);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void drawCharts(float[] temp, float[] hum, float[] light) {
            //////////////////////////////////////////////////////////////
            ArrayList<Entry> temp_entries = new ArrayList<>();
            ArrayList<Entry> hum_entries = new ArrayList<>();
            ArrayList<Entry> light_entries = new ArrayList<>();
            /////////////////////////////////////////////////////////////////////
            for (int i = 0; i < temp.length; i++) {
                temp_entries.add(new Entry((float) i, temp[i]));
                hum_entries.add(new Entry((float) i, hum[i]));
                light_entries.add(new Entry((float) i, light[i]));
            }
            ////////////////////////////////////////////////////////////////////////
            LineDataSet[] dataSet = new LineDataSet[3];
            dataSet[0] = new LineDataSet(temp_entries, "Temperature");
            dataSet[1] = new LineDataSet(hum_entries, "Humidity");
            dataSet[2] = new LineDataSet(light_entries, "Light");
            ///////////////////////////////////////////////////////////////
            dataSet[0].setColor(ContextCompat.getColor(getApplicationContext(), R.color.purpleDark));
            dataSet[0].setFillDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.line_gradient_purple));
            dataSet[1].setColor(ContextCompat.getColor(getApplicationContext(), R.color.tealDark));
            dataSet[1].setFillDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.line_gradient_teal));
            dataSet[2].setColor(ContextCompat.getColor(getApplicationContext(), R.color.orangeDark));
            dataSet[2].setFillDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.line_gradient_orange));
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            LineData[] data = new LineData[3];
            for (int i = 0; i < 3; i++) {
                dataSet[i].setDrawValues(false);
                dataSet[i].setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet[i].setDrawFilled(true);
                dataSet[i].setDrawCircleHole(false);
                dataSet[i].setDrawCircles(false);
                dataSet[i].setLineWidth(3.0f);
                data[i] = new LineData(dataSet[i]);
                data[i].notifyDataChanged();
                lineChart[i].setData(data[i]);
                lineChart[i].animateY(1000);
                lineChart[i].invalidate();
            }
        }
    }

    public void connectToBroker(String URL) {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), URL, clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ////////////////////////////////////////////////////////////////////////////////
                    connectionFlag = true;
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    subscribeToTopic("__other_topic__");
                    sendMessage("__some_topic__", "s");
                    ////////////////////////////////////////////////////////////////////////////////
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getApplicationContext(), "Failed to Connect", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String topic, String payload) {
        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribeToTopic(String topic) {
        try {
            if (client.isConnected()) {
                client.subscribe(topic, 0);
                client.setCallback(new MqttCallback() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void connectionLost(Throwable cause) {
                        Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
                        connectionFlag = false;
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        receivedMessage = message.toString();
                        handler.sendEmptyMessage(0);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    public void disconnect() {
        if (connectionFlag) {
            try {
                IMqttToken disconnectToken = client.disconnect();
                disconnectToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                        connectionFlag = false;
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {
        speechPrompt.setVisibility(View.GONE);
        YoYo.with(Techniques.RollIn).duration(500).playOn(fabButton);
        //fabButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(RESULTS_RECOGNITION);
        if(matches != null) {
            String bestMatch = matches.get(0);
            Toast.makeText(getApplicationContext(), bestMatch, Toast.LENGTH_SHORT).show();
            if(bestMatch.contains("light")) {
                sendMessage("__some_topic__", "l");
            }
            if(bestMatch.contains("fan")) {
                sendMessage("__some_topic__", "f");
            }
        }
        speechPrompt.setVisibility(View.GONE);
        YoYo.with(Techniques.RollIn).duration(700).playOn(fabButton);
        //fabButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    private void promptSpeechInput() {
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        speechRecognizer.startListening(intent);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WAKE_LOCK}, 0);
            } else if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 0);
            } else if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
            } else if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.INTERNET}, 0);
            } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
            } else {
                Toast.makeText(getApplicationContext(), "Please allow permissions...", Toast.LENGTH_LONG).show();
                requestPermissions();
            }
        }
    }
}