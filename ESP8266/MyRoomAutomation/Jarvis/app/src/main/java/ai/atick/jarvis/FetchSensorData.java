package ai.atick.jarvis;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

class FetchSensorData {
    private static final String BASE_URL = "http://192.168.0.101:5000";

    private static AsyncHttpClient client = new AsyncHttpClient();

    static void get(AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(), null, responseHandler);
    }

    private static String getAbsoluteUrl() {
        return BASE_URL + "/api/sensors";
    }
}
