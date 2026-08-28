package com.example.facecapture;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText subjectIdInput;
    private Button startCaptureButton;
    private Button startScanButton;
    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subjectIdInput = findViewById(R.id.subjectIdInput);
        startCaptureButton = findViewById(R.id.startCaptureButton);
        startScanButton = findViewById(R.id.startScanButton);

        startCaptureButton.setOnClickListener(v ->
        {
            String subjectId = subjectIdInput.getText().toString().trim();

            if (subjectId.isEmpty()) {

                Toast.makeText(
                        MainActivity.this,
                        "Please enter Subject ID",
                        Toast.LENGTH_SHORT
                ).show();

                return;
        }
            String filename = generateFileName(subjectId);

            Toast.makeText(
                    MainActivity.this,
                    "Preparing Capture: " + subjectId + "\nFilename: " + filename,
                    Toast.LENGTH_SHORT
            ).show();
            checkCameraPermission();
        });

        startScanButton.setOnClickListener(v -> {
            subjectIdInput.setText("");
        });
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                    this,
                    "Camera permission already granted",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        }
    }

    private String generateFileName(String subjectId) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = formatter.format(new Date());
        return subjectId + "_video_" + timestamp + ".mp4";
    }

    private boolean isValidSubjectId (String subjectId) {
        return !subjectId.isEmpty();
    }
}