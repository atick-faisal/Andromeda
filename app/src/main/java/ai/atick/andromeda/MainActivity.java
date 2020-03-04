package ai.atick.andromeda;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    LineChart[] lineChart = new LineChart[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        /////////////////////////////////////////////////////
        lineChart[0] = findViewById(R.id.line_chart_0);
        lineChart[1] = findViewById(R.id.line_chart_1);
        lineChart[2] = findViewById(R.id.line_chart_2);
        /////////////////////////////////////////////////////
        for(int i = 0; i < 3; i++) {
            lineChart[i].getDescription().setText("");
            lineChart[i].getAxisLeft().setDrawLabels(false);
            lineChart[i].getAxisLeft().setEnabled(false);
            lineChart[i].getAxisRight().setDrawLabels(false);
            lineChart[i].getAxisRight().setEnabled(false);
            lineChart[i].getXAxis().setDrawLabels(false);
            lineChart[i].getXAxis().setEnabled(false);
            lineChart[i].getLegend().setEnabled(false);
            lineChart[i].setClickable(false);
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
                    while ((line = bufferedReader.readLine()) != null)  {
                        stringBuilder.append(line).append("\n");
                    }
                    update = stringBuilder.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(httpURLConnection != null) {
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
                for(int i = 0; i < dataArray.length(); i++) {
                    dataObject[i] = dataArray.getJSONObject(i);
                }
                /////////////////////////////////////////////////
                for(int i = 0; i < dataArray.length(); i++) {
                    temp[i] = Float.parseFloat(dataObject[i].getString("temp"));
                    hum[i] = Float.parseFloat(dataObject[i].getString("hum"));
                    light[i] = Float.parseFloat(dataObject[i].getString("light"));
                }
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
             for(int i = 0; i < temp.length; i++) {
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
             for(int i = 0; i < 3; i++) {
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
}