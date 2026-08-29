package com.example.facecapture;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText subjectIdInput;
    private Button startCaptureButton;
    private Button startScanButton;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private PreviewView cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subjectIdInput = findViewById(R.id.subjectIdInput);
        startCaptureButton = findViewById(R.id.startCaptureButton);
        startScanButton = findViewById(R.id.startScanButton);
        cameraPreview = findViewById(R.id.cameraPreview);

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
            startCamera();
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
        } else {
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

    private boolean isValidSubjectId(String subjectId) {
        return !subjectId.isEmpty();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else Toast.makeText(this,
                    "Camera permission denied", Toast.LENGTH_SHORT).show();
        }

    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        Preview preview = new Preview.Builder().build();
                        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
                        cameraProvider.unbindAll();
                        cameraProvider.bindToLifecycle(
                                this,
                                cameraSelector, preview
                        );
                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Unable to start camera", Toast.LENGTH_SHORT).show();
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    ;
}