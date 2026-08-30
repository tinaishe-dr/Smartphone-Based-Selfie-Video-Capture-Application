package com.example.facecapture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Camera;

import com.google.mlkit.vision.common.InputImage;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText subjectIdInput;
    private Button startCaptureButton;
    private Button startScanButton;
    private PreviewView cameraPreview;
    private PreviewView barcodePreview;
    private Button continueButton;
    private View scanLine;
    private View barcodeOverlay;
    private View scannerBox;
    private TextView scanInstruction;
    private boolean barcodeScanned = false;
    private ImageAnalysis barcodeImageAnalysis;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int BARCODE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subjectIdInput = findViewById(R.id.subjectIdInput);
        //startCaptureButton = findViewById(R.id.startCaptureButton);
        startScanButton = findViewById(R.id.startScanButton);
        //cameraPreview = findViewById(R.id.cameraPreview);
        barcodePreview = findViewById(R.id.barcodePreview);
        continueButton = findViewById(R.id.continueButton);
        barcodeOverlay = findViewById(R.id.barcodeOverlay);
        scannerBox = findViewById(R.id.scannerBox);
        scanLine = findViewById(R.id.scanLine);
        scanInstruction = findViewById(R.id.scanInstruction);

        startScanAnimation();

//        startCaptureButton.setOnClickListener(v ->
//        {
//            String subjectId = subjectIdInput.getText().toString().trim();
//
//            if (subjectId.isEmpty()) {
//
//                Toast.makeText(
//                        MainActivity.this,
//                        "Please enter Subject ID",
//                        Toast.LENGTH_SHORT
//                ).show();
//
//                return;
//            }
//            String filename = generateFileName(subjectId);
//
//            Toast.makeText(
//                    MainActivity.this,
//                    "Preparing Capture: " + subjectId + "\nFilename: " + filename,
//                    Toast.LENGTH_SHORT
//            ).show();
//            checkCameraPermission();
//            startCamera();
//        });

        startScanButton.setOnClickListener(v -> {

            barcodeScanned = false;

            // Show scanner UI
            barcodeOverlay.setVisibility(View.VISIBLE);
            scannerBox.setVisibility(View.VISIBLE);
            scanInstruction.setVisibility(View.VISIBLE);

            startScanAnimation();

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {

                startBarcodeScanner();

            } else {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        BARCODE_PERMISSION_CODE
                );
            }
        });

        continueButton.setOnClickListener(v -> {

            String subjectId = subjectIdInput.getText()
                    .toString()
                    .trim();

            if (subjectId.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please scan a barcode first",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Intent intent = new Intent(
                    MainActivity.this,
                    SelfieCaptureActivity.class
            );

            intent.putExtra("SUBJECT_ID", subjectId);

            startActivity(intent);
        });
    }

    private void startScanAnimation() {

        scanLine.post(() -> {

            float distance =
                    ((View) scanLine.getParent()).getHeight()
                            - scanLine.getHeight();

            scanLine.animate()
                    .translationY(distance)
                    .setDuration(1500)
                    .withEndAction(() -> {

                        scanLine.animate()
                                .translationY(0)
                                .setDuration(1500)
                                .withEndAction(this::startScanAnimation)
                                .start();

                    })
                    .start();
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
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
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                )) {
                    Toast.makeText(this,
                            "Camera permission disabled. Enable it in Settings", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
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

    private void startBarcodeScanner() {

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_QR_CODE
                        )
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {

                ProcessCameraProvider cameraProvider =
                        cameraProviderFuture.get();

                Preview preview =
                        new Preview.Builder().build();

                preview.setSurfaceProvider(
                        barcodePreview.getSurfaceProvider()
                );

                CameraSelector cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA;

                barcodeImageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build();

                barcodeImageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        imageProxy -> {

                            processBarcodeImage(
                                    scanner,
                                    imageProxy
                            );
                        }
                );

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        barcodeImageAnalysis
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Unable to start barcode scanner",
                        Toast.LENGTH_SHORT
                ).show();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void processBarcodeImage(
            BarcodeScanner scanner,
            ImageProxy imageProxy) {

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {

                    if (barcodeScanned) {
                        return;
                    }

                    for (Barcode barcode : barcodes) {

                        String rawValue = barcode.getRawValue();

                        if (rawValue != null && !rawValue.isEmpty()) {

                            barcodeScanned = true;

                            subjectIdInput.setText(rawValue);

                            if (barcodeImageAnalysis != null) {
                                barcodeImageAnalysis.clearAnalyzer();
                            }

                            // Stop scan-line animation
                            scanLine.animate().cancel();

                            // Hide scanner UI
                            barcodeOverlay.setVisibility(View.GONE);
                            scannerBox.setVisibility(View.GONE);
                            scanInstruction.setVisibility(View.GONE);

                            Toast.makeText(
                                    this,
                                    "Subject ID: " + rawValue,
                                    Toast.LENGTH_SHORT
                            ).show();

                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    // Barcode could not be processed
                })
                .addOnCompleteListener(task -> {

                    imageProxy.close();
                });
    }
}