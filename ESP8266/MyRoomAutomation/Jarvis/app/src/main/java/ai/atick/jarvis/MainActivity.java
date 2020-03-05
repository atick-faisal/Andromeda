package ai.atick.jarvis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    LineChart lineChart;
    LineDataSet lineDataSet;
    LineData data;
    ArrayList<Entry> dataList = new ArrayList<>();

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            lineDataSet = new LineDataSet(dataList, "Temperature");
            lineDataSet.setDrawValues(false);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSet.setDrawFilled(true);
            //lineDataSet.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            //lineDataSet.setFillDrawable(getResources().getDrawable(R.drawable.line_gradient));
            lineDataSet.setDrawCircleHole(false);
            lineDataSet.setDrawCircles(false);
            lineDataSet.setLineWidth(3.0f);

            data = new LineData(lineDataSet);
            data.notifyDataChanged();
            lineChart.setData(data);
            lineChart.invalidate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lineChart = findViewById(R.id.line_chart);

        lineChart.animateY(1000);
        lineChart.getDescription().setText("");
        lineChart.getAxisLeft().setDrawLabels(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getXAxis().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        for(int i = 0; i < 6; i++) {
            dataList.add(new Entry((float) i, (float) 0));
        }

        lineDataSet = new LineDataSet(dataList, "Temperature");
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setColor(getResources().getColor(R.color.colorPrimaryDark));
        lineDataSet.setFillDrawable(getResources().getDrawable(R.drawable.line_gradient));
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(3.0f);

        data = new LineData(lineDataSet);
        data.notifyDataChanged();
        lineChart.setData(data);
        lineChart.invalidate();

        ParseSensorData parseSensorData = new ParseSensorData();
        parseSensorData.execute();
    }

    public void updateCharts(int[] temp, int[] hum, int[] light) {
        dataList.clear();
        for(int i = 0; i < 6; i++) {
            dataList.add(new Entry((float) i, (float) temp[i]));
        }
        handler.sendEmptyMessage(0);
    }
}
