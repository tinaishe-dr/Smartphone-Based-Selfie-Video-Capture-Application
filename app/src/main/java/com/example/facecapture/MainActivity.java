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
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.content.Intent;
import androidx.documentfile.provider.DocumentFile;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.File;
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
    private FrameLayout barcodeScannerContainer;
    private boolean barcodeScanned = false;
    private ImageAnalysis barcodeImageAnalysis;
    private ProcessCameraProvider cameraProvider;
    private TextView storageDirectoryText;
    private TextView storageSpaceText;
    private Button changeStorageButton;


    private static final int DIRECTORY_PICKER_REQUEST = 200;
    private static final String PREFS_NAME = "storage_preferences";
    private static final String STORAGE_URI_KEY = "storage_uri";

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int BARCODE_PERMISSION_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

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
        barcodeScannerContainer = findViewById(R.id.barcodeScannerContainer);
        //barcodeScannerContainer.setVisibility(View.GONE);
        storageDirectoryText = findViewById(R.id.storageDirectoryText);
        storageSpaceText = findViewById(R.id.storageSpaceText);
        changeStorageButton = findViewById(R.id.changeStorageButton);

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

        // Change storage directory
        changeStorageButton.setOnClickListener(v -> {

            openStorageDirectoryPicker();

        });
        // Display storage information
        updateStorageInformation();

        startScanButton.setOnClickListener(v -> {

            barcodeScanned = false;

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {

                barcodeScannerContainer.setVisibility(View.VISIBLE);

                scanInstruction.setText(
                        "Place barcode inside the box"
                );

                startScanButton.setText("Scanning...");

                startBarcodeScanner();

            } else {

                // Keep scanner UI visible
                barcodeScannerContainer.setVisibility(View.VISIBLE);

                // Tell user why scanning cannot start
                scanInstruction.setText(
                        "Camera permission required"
                );

                startScanButton.setText("Scan Barcode");

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

    private void openStorageDirectoryPicker() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                STORAGE_PERMISSION_CODE
        );
    }
    private void chooseStorageDirectory() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                DIRECTORY_PICKER_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == STORAGE_PERMISSION_CODE &&
                resultCode == RESULT_OK &&
                data != null) {

            Uri treeUri =
                    data.getData();

            if (treeUri != null) {

                try {

                    final int takeFlags =
                            data.getFlags()
                                    &
                                    (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    getContentResolver()
                            .takePersistableUriPermission(
                                    treeUri,
                                    takeFlags
                            );

                } catch (Exception e) {

                    Log.e(
                            "STORAGE_DEBUG",
                            "Could not persist storage permission",
                            e
                    );
                }


                // Save selected directory
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                )
                        .edit()
                        .putString(
                                STORAGE_URI_KEY,
                                treeUri.toString()
                        )
                        .apply();


                updateStorageInformation();


                Toast.makeText(
                        this,
                        "Storage directory changed",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void updateStorageInformation() {

        // Display directory
        String storageLocation =
                getStorageLocationDescription();

        storageDirectoryText.setText(
                "Location: " + storageLocation
        );


        // Display phone storage
        String storageInfo =
                getPhoneStorageInformation();

        storageSpaceText.setText(
                storageInfo
        );
    }


    private String getStorageLocationDescription() {

        String savedUri =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                )
                        .getString(
                                STORAGE_URI_KEY,
                                null
                        );


        // No custom directory
        if (savedUri == null) {

            File defaultDirectory =
                    new File(
                            getExternalFilesDir(
                                    Environment.DIRECTORY_MOVIES
                            ),
                            "FaceCapture"
                    );

            return defaultDirectory
                    .getAbsolutePath();
        }


        try {

            Uri uri =
                    Uri.parse(savedUri);

            DocumentFile directory =
                    DocumentFile.fromTreeUri(
                            this,
                            uri
                    );

            if (directory != null) {

                String name =
                        directory.getName();

                if (name != null) {

                    return name;
                }
            }

        } catch (Exception e) {

            Log.e(
                    "STORAGE_DEBUG",
                    "Unable to read storage directory",
                    e
            );
        }

        return savedUri;
    }

    private String getPhoneStorageInformation() {

        try {

            File storage =
                    Environment.getExternalStorageDirectory();

            StatFs statFs =
                    new StatFs(
                            storage.getPath()
                    );

            long blockSize =
                    statFs.getBlockSizeLong();

            long totalBlocks =
                    statFs.getBlockCountLong();

            long availableBlocks =
                    statFs.getAvailableBlocksLong();

            long totalBytes =
                    totalBlocks * blockSize;

            long availableBytes =
                    availableBlocks * blockSize;

            long usedBytes =
                    totalBytes - availableBytes;


            return "Phone storage: "
                    + formatStorageSize(usedBytes)
                    + " Used / "
                    + formatStorageSize(totalBytes)
                    + "\nTotal: "
                    + formatStorageSize(availableBytes)
                    + " Free";

        } catch (Exception e) {

            Log.e(
                    "STORAGE_DEBUG",
                    "Unable to read storage information",
                    e
            );

            return "Storage information unavailable";
        }
    }


    private String formatStorageSize(long bytes) {

        if (bytes <= 0) {
            return "0 B";
        }

        double value = bytes;

        String[] units = {
                "B",
                "KB",
                "MB",
                "GB",
                "TB"
        };

        int unitIndex = 0;

        while (value >= 1024 &&
                unitIndex < units.length - 1) {

            value /= 1024;

            unitIndex++;
        }

        return String.format(
                Locale.US,
                "%.1f %s",
                value,
                units[unitIndex]
        );
    }

    private void startScanAnimation() {

        scanLine.post(() -> {

            float distance =
                    scannerBox.getHeight() -
                            scanLine.getHeight();

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
                grantResults
        );

        if (requestCode == CAMERA_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startCamera();

            } else {

                Toast.makeText(
                        this,
                        "Camera permission denied",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        if (requestCode == BARCODE_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Permission granted
                barcodeScannerContainer.setVisibility(View.VISIBLE);

                scanInstruction.setText(
                        "Place barcode inside the box"
                );

                startScanButton.setText("Scanning...");

                startBarcodeScanner();

            } else {

                // Permission denied
                barcodeScannerContainer.setVisibility(View.VISIBLE);

                scanInstruction.setText(
                        "Camera permission required"
                );

                startScanButton.setText("Scan Barcode");
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

                cameraProvider =
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
                startScanAnimation();

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

                            // Stop barcode analysis
                            if (barcodeImageAnalysis != null) {
                                barcodeImageAnalysis.clearAnalyzer();
                            }

                            // Stop scan-line animation
                            scanInstruction.setText("Barcode scanned successfully");
                            scanLine.animate().cancel();

                            // Hide scanner UI
                            //barcodeScannerContainer.setVisibility(View.GONE);

                            // Stop camera
                            if (cameraProvider != null) {
                                cameraProvider.unbindAll();
                            }

                            // Change button
                            startScanButton.setText("Scan Again");

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