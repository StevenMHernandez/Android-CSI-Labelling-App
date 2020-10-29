package com.stevenmhernandez.csi_labelling_app.Experiments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.csi_labelling_app.Services.BaseDataCollectorService;
import com.stevenmhernandez.csi_labelling_app.Services.FileDataCollectorService;
import com.stevenmhernandez.esp32csiserial.CSIDataInterface;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;

public class ToggleMainActivity extends AppCompatActivity implements CSIDataInterface {

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
    private TextView frameRateTextView;
    private TextView repetitionsTextView;

    BaseDataCollectorService dataCollectorService = new FileDataCollectorService();

    int actionIndex = 0;
    int actionsRepetitions = 0;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stand_walk_run);

        background = findViewById(R.id.background);
        textView = findViewById(R.id.textView);
        frameRateTextView = findViewById(R.id.frameRateTextView);
        repetitionsTextView = findViewById(R.id.repetitionsTextView);

        ToggleMainActivity activity = this;

        dataCollectorService.setup(this);

        dataCollectorService.handle("type,smartphone_id,timestamp,current_action\n");

        textView.setText("Press once the following state is occurring: " + actions[actionIndex]);

        background.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    activity.dataCollectorService.handle(updateCsiString(activity, actions[actionIndex]));
                    actionIndex = (actionIndex + 1) % actions.length;
                    textView.setText("Press once in state: " + actions[actionIndex]);
                    textView.setTextColor(actionIndex % 2 == 1 ? Color.WHITE : Color.BLACK);
                    background.setBackgroundColor(actionIndex % 2 == 1 ? Color.BLACK : Color.WHITE);
                    if (actionIndex == 0) {
                        actionsRepetitions++;
                    }
                    repetitionsTextView.setText("Total Reps: " + Integer.toString(actionsRepetitions) + " + "  + Integer.toString(actionIndex) + "/" + Integer.toString(actions.length));
                    break;

            }

            return true;
        });


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
