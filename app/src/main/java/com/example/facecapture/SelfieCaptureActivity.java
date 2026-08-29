package com.example.facecapture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class SelfieCaptureActivity extends AppCompatActivity {
    private TextView subjectIdText;
    private PreviewView selfiePreview;
    private FaceDetector faceDetector;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);
        statusText = findViewById(R.id.statusText);
        subjectIdText = findViewById(R.id.subjectIdText);
        selfiePreview = findViewById(R.id.selfiePreview);

        String subjectId = getIntent()
                .getStringExtra("SUBJECT_ID");
        subjectIdText.setText(
                "Subject: " + subjectId
        );

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(
                                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                            )
                            .build();

            faceDetector = FaceDetection.getClient(options);

            startSelfieCamera();
        }
    }

    private void startSelfieCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {

                ProcessCameraProvider cameraProvider =
                        cameraProviderFuture.get();

                // Camera preview
                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        selfiePreview.getSurfaceProvider()
                );

                // Image analysis
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build();

                // Analyzer
                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        imageProxy -> {

                            Log.d(
                                    "FACE_DEBUG",
                                    "Frame received"
                            );

                            processFaceImage(imageProxy);
                        }
                );

                // Front camera
                CameraSelector cameraSelector =
                        new CameraSelector.Builder()
                                .requireLensFacing(
                                        CameraSelector.LENS_FACING_FRONT
                                )
                                .build();

                // Remove previous camera bindings
                cameraProvider.unbindAll();

                // Bind BOTH preview and analysis
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {

                Log.e(
                        "FACE_DEBUG",
                        "Camera startup failed",
                        e
                );

                Toast.makeText(
                        this,
                        "Unable to start selfie camera",
                        Toast.LENGTH_SHORT
                ).show();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void processFaceImage(ImageProxy imageProxy) {

        Log.d("FACE_DEBUG", "Frame received");

        if (imageProxy.getImage() == null) {
            Log.d("FACE_DEBUG", "Image is null");
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {

                    Log.d(
                            "FACE_DEBUG",
                            "Faces detected: " + faces.size()
                    );

                    if (faces.isEmpty()) {

                        Log.d("FACE_DEBUG", "No face detected");

                        runOnUiThread(() -> {
                            statusText.setText(
                                    "No face detected"
                            );
                        });

                    } else {

                        Log.d(
                                "FACE_DEBUG",
                                "Faces detected: " + faces.size()
                        );

                        runOnUiThread(() -> {
                            statusText.setText(
                                    "Face detected"
                            );
                        });
                    }
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            "FACE_DEBUG",
                            "Face detection failed",
                            e
                    );

                })
                .addOnCompleteListener(task -> {

                    imageProxy.close();

                });
    }
}