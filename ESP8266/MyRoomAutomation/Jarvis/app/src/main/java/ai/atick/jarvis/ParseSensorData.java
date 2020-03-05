package ai.atick.jarvis;

import android.content.Context;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

class ParseSensorData {

    private static final int LENGTH = 6;

    private int[] temp = new int[LENGTH];
    private int[] hum = new int[LENGTH];
    private int[] light = new int[LENGTH];

    void execute() {
        FetchSensorData.get(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                JSONObject[] sensorData = new JSONObject[LENGTH];
                for(int i = 0; i < LENGTH; i++) {
                    try {
                        sensorData[i] = response.getJSONObject(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                for(int i = 0; i < LENGTH; i++) {
                    try {
                        temp[i] = Integer.parseInt(sensorData[i].getString("temp"));
                        hum[i] = Integer.parseInt(sensorData[i].getString("hum"));
                        light[i] = Integer.parseInt(sensorData[i].getString("light"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                new MainActivity().updateCharts(temp, hum, light);
            }
        });
    }
}
