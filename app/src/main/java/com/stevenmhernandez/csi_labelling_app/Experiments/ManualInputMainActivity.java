package com.stevenmhernandez.csi_labelling_app.Experiments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.csi_labelling_app.Services.BaseDataCollectorService;
import com.stevenmhernandez.csi_labelling_app.Services.FileDataCollectorService;
import com.stevenmhernandez.esp32csiserial.CSIDataInterface;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class ManualInputMainActivity extends AppCompatActivity implements CSIDataInterface {

    /**
     * END Custom Parameters
     */

    private String currentAction = "no action";

    private ESP32CSISerial csiSerial = new ESP32CSISerial();

    private ConstraintLayout background;
    private TextView textView;
    private TextView frameRateTextView;
    private TextView repetitionsTextView;
    private TextView dateTextView;
    private Button button;
    private EditText manualInputEditText;

    BaseDataCollectorService dataCollectorService = new FileDataCollectorService();

    int actionIndex = 0;
    int actionsRepetitions = 0;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_input);

        background = findViewById(R.id.background);
        textView = findViewById(R.id.textView);
        frameRateTextView = findViewById(R.id.frameRateTextView);
        repetitionsTextView = findViewById(R.id.repetitionsTextView);
        dateTextView = findViewById(R.id.dateTextView);
        button = findViewById(R.id.button);
        manualInputEditText = findViewById(R.id.manualInputEditText);

        ManualInputMainActivity activity = this;

        dataCollectorService.setup(this);

        dataCollectorService.handle("type,smartphone_id,timestamp,current_action\n");

        textView.setText("Current Action: '" + currentAction + "'");
        dateTextView.setText(DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()));
        button.setText("Set Action");

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (currentAction.equals("no action")) {
                        currentAction = manualInputEditText.getText().toString();

                        if (currentAction.isEmpty()) {
                            return false;
                        }

                        button.setText("Clear Action");
                    } else {
                        currentAction = "no action";
                        button.setText("Set Action");
                    }

                    activity.dataCollectorService.handle(updateCsiString(activity, currentAction));

                    textView.setText("Current Action: '" + currentAction + "'");
                    dateTextView.setText(DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()));
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
