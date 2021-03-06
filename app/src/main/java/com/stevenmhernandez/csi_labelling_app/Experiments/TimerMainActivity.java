package com.stevenmhernandez.csi_labelling_app.Experiments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.csi_labelling_app.Services.BaseDataCollectorService;
import com.stevenmhernandez.csi_labelling_app.Services.FileDataCollectorService;
import com.stevenmhernandez.esp32csiserial.CSIDataInterface;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class TimerMainActivity extends AppCompatActivity implements CSIDataInterface {

    /**
     * END Custom Parameters
     */
    double timer_pause_seconds = 3.0;
    String[] actions = new String[]{
            "sit",
            "*transition*",
            "stand",
            "*transition*",
    };
    /**
     * END Custom Parameters
     */

    private ESP32CSISerial csiSerial = new ESP32CSISerial();

    private TextView textView;
    private ConstraintLayout background;
    Timer timer;
    private TextView frameRateTextView;
    private TextView repetitionsTextView;

    BaseDataCollectorService dataCollectorService = new FileDataCollectorService();
    int actionIndex = 0;
    int actionsRepetitions = 0;
    boolean inTransition = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stand_walk_run);

        textView = findViewById(R.id.textView);
        background = findViewById(R.id.background);
        frameRateTextView = findViewById(R.id.frameRateTextView);
        repetitionsTextView = findViewById(R.id.repetitionsTextView);

        dataCollectorService.setup(this);

        dataCollectorService.handle("type,smartphone_id,timestamp,current_action\n");

        TimerMainActivity activity = this;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String currentAction;
                inTransition = false;
                currentAction = actions[actionIndex];
                activity.runOnUiThread(() -> {
                    textView.setText(currentAction);
                    textView.setTextColor(Color.BLACK);
                    background.setBackgroundColor(Color.WHITE);
                    repetitionsTextView.setText("Total Reps: " + Integer.toString(actionsRepetitions) + " + " + Integer.toString(actionIndex) + "/" + Integer.toString(actions.length));
                });

                // currentAction
                activity.dataCollectorService.handle(updateCsiString(activity, currentAction));

                // Setup for the next iteration
                actionIndex = (actionIndex + 1) % actions.length;
                if (actionIndex == 0) {
                    actionsRepetitions++;
                }
            }
        }, 0, (long) (timer_pause_seconds * 1000));

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

    public String updateCsiString(Activity activity, String currentAction) {
        String deviceId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        return String.format("'CURRENT_ACTION','%s',%d,'%s'\n", deviceId, System.currentTimeMillis(), currentAction);
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
