package com.stevenmhernandez.csi_labelling_app.Experiments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.csi_labelling_app.Services.BaseDataCollectorService;
import com.stevenmhernandez.csi_labelling_app.Services.FileDataCollectorService;
import com.stevenmhernandez.esp32csiserial.CSIDataInterface;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;

import java.io.IOException;

public class PressAndHoldMainActivity extends AppCompatActivity implements CSIDataInterface {

    /**
     * Custom Parameters
     */
    String[] actions = new String[]{
            "IS LOS",
            "IS NOT LOS",
    };
    /**
     * END Custom Parameters
     */

    private ESP32CSISerial csiSerial = new ESP32CSISerial();

    private ConstraintLayout background;
    private TextView textView;
    private TextView textViewTimeSinceLast;
    private TextView frameRateTextView;
    private TextView repetitionsTextView;

    BaseDataCollectorService dataCollectorService = new FileDataCollectorService();

    int actionIndex = 0;
    int actionsRepetitions = 0;
    long previousTime = 0;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stand_walk_run);

        background = findViewById(R.id.background);
        textView = findViewById(R.id.textView);
        textViewTimeSinceLast = findViewById(R.id.textViewTimeSinceLast);
        frameRateTextView = findViewById(R.id.frameRateTextView);
        repetitionsTextView = findViewById(R.id.repetitionsTextView);

        PressAndHoldMainActivity activity = this;

        dataCollectorService.setup(this);

        dataCollectorService.handle("type,smartphone_id,timestamp,current_action\n");

        textView.setText("Press once the following state is occurring: " + actions[actionIndex]);

        background.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    activity.dataCollectorService.handle(updateCsiString(activity, actions[actionIndex]));
                    textView.setText("Release after complete");
                    textView.setTextColor(Color.WHITE);
                    background.setBackgroundColor(Color.BLACK);
                    previousTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    activity.dataCollectorService.handle(updateCsiString(activity, "no action"));
                    actionIndex = (actionIndex + 1) % actions.length;
                    textView.setText("Press once the following state is occurring: " + actions[actionIndex]);
                    textView.setTextColor(Color.BLACK);
                    background.setBackgroundColor(Color.WHITE);
                    if (actionIndex == 0) {
                        actionsRepetitions++;
                    }
                    repetitionsTextView.setText("Total Reps: " + Integer.toString(actionsRepetitions) + " + "  + Integer.toString(actionIndex) + "/" + Integer.toString(actions.length));
                    previousTime = System.currentTimeMillis();
                    break;

            }

            return true;
        });

        previousTime = System.currentTimeMillis();

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long millis = System.currentTimeMillis();
                                textViewTimeSinceLast.setText(Integer.toString((int) ((millis - previousTime) / 1000.0)) + " Seconds");
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();


        csiSerial.setup(this, "example_experiment_name");
        csiSerial.onCreate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        csiSerial.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        csiSerial.onPause(this);
    }

    public void shareOverBluetooth(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        try {
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, dataCollectorService.getFileUri());
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String updateCsiString(Activity activity, String currentAction) {
        String deviceId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        return String.format("CURRENT_ACTION,%s,%d,%s\n", deviceId, System.currentTimeMillis(), currentAction);
    }

    long csiCounter = 0;
    long csiPerSecondCounter = 0;
    long counterStart = System.currentTimeMillis();
    long csiPerSecondCounterFinal = 0;
    @Override
    public void addCsi(String csi_string) {
        if (counterStart + 1000 < System.currentTimeMillis()) {
            counterStart = System.currentTimeMillis();
            csiPerSecondCounterFinal = csiPerSecondCounter;
            csiPerSecondCounter = 0;
        }
        csiCounter++;
        csiPerSecondCounter++;
        frameRateTextView.setText(String.valueOf(csiCounter) + " | " + String.valueOf(csiPerSecondCounterFinal));
    }
}
