package com.example.facecapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.CountDownTimer;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;

import androidx.camera.video.FileDescriptorOutputOptions;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.PendingRecording;

import androidx.core.content.ContextCompat;

import androidx.documentfile.provider.DocumentFile;

import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SelfieCaptureActivity extends AppCompatActivity {

    private TextView subjectIdText;
    private PreviewView selfiePreview;
    private FaceDetector faceDetector;
    private TextView statusText;
    private View faceGuide;

    private VideoCapture<Recorder> videoCapture;
    private Recording recording;

    private Button startRecordingButton;
    private TextView countdownText;
    private Button backButton;

    // Storage UI
    private TextView storageDirectoryText;
    private TextView storageSpaceText;
    private Button changeStorageButton;

    private CountDownTimer countdownTimer;

    private boolean isCountdownRunning = false;
    private boolean isRecording = false;
    private boolean faceReady = false;
    private boolean faceWasGoodDuringRecording = true;

    private String subjectId;

    private static final int STORAGE_PERMISSION_CODE = 200;

    private static final String PREFS_NAME =
            "storage_preferences";

    private static final String STORAGE_URI_KEY =
            "storage_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_selfie_capture);

        // Existing UI
        statusText = findViewById(R.id.statusText);
        subjectIdText = findViewById(R.id.subjectIdText);
        selfiePreview = findViewById(R.id.selfiePreview);
        faceGuide = findViewById(R.id.faceGuide);
        startRecordingButton = findViewById(R.id.startRecordingButton);
        countdownText = findViewById(R.id.countdownText);
        backButton = findViewById(R.id.backButton);

        // New storage UI
        storageDirectoryText = findViewById(R.id.storageDirectoryText);

        storageSpaceText = findViewById(R.id.storageSpaceText);

        changeStorageButton = findViewById(R.id.changeStorageButton);


        // Back button
        backButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    SelfieCaptureActivity.this,
                    MainActivity.class
            );

            startActivity(intent);

            finish();
        });


        // Change storage directory
        changeStorageButton.setOnClickListener(v -> {

            openStorageDirectoryPicker();

        });


        // Start recording button
        startRecordingButton.setOnClickListener(v -> {

            if (recording == null &&
                    !isCountdownRunning &&
                    !isRecording) {

                if (faceReady) {

                    startCountdown();

                } else {

                    Toast.makeText(
                            this,
                            "Please position your face correctly first",
                            Toast.LENGTH_SHORT
                    ).show();
                }

            } else if (recording != null) {

                stopRecording();
            }

        });


        // Subject ID
        subjectId = getIntent()
                .getStringExtra("SUBJECT_ID");

        if (subjectId == null || subjectId.isEmpty()) {

            subjectId = "UNKNOWN";
        }

        subjectIdText.setText(
                "Subject: " + subjectId
        );


        // Display storage information
        updateStorageInformation();


        // Start camera
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

            faceDetector =
                    FaceDetection.getClient(options);

            startSelfieCamera();

        } else {

            Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================================================
    // STORAGE DIRECTORY PICKER
    // =========================================================

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


    // =========================================================
    // STORAGE INFORMATION
    // =========================================================

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


    // =========================================================
    // PHONE STORAGE
    // =========================================================

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


    // =========================================================
    // COUNTDOWN
    // =========================================================

    private void startCountdown() {

        if (isCountdownRunning || isRecording) {
            return;
        }

        isCountdownRunning = true;

        countdownText.setVisibility(
                View.VISIBLE
        );

        countdownTimer =
                new CountDownTimer(
                        3000,
                        1000
                ) {

                    @Override
                    public void onTick(
                            long millisUntilFinished) {

                        int seconds =
                                (int) Math.ceil(
                                        millisUntilFinished / 1000.0
                                );

                        countdownText.setText(
                                String.valueOf(seconds)
                        );
                    }


                    @Override
                    public void onFinish() {

                        countdownText.setText(
                                "GO!"
                        );

                        isCountdownRunning = false;

                        startRecording();

                        countdownText.postDelayed(
                                () -> countdownText.setVisibility(
                                        View.GONE
                                ),
                                500
                        );
                    }

                }.start();
    }


    // =========================================================
    // CAMERA
    // =========================================================

    private void startSelfieCamera() {

        ListenableFuture<ProcessCameraProvider>
                cameraProviderFuture =
                ProcessCameraProvider.getInstance(
                        this
                );

        cameraProviderFuture.addListener(
                () -> {

                    try {

                        ProcessCameraProvider cameraProvider =
                                cameraProviderFuture.get();


                        Preview preview =
                                new Preview.Builder()
                                        .build();

                        preview.setSurfaceProvider(
                                selfiePreview
                                        .getSurfaceProvider()
                        );


                        QualitySelector qualitySelector =
                                QualitySelector.from(
                                        Quality.HD
                                );

                        Recorder recorder =
                                new Recorder.Builder()
                                        .setQualitySelector(
                                                qualitySelector
                                        )
                                        .build();

                        videoCapture =
                                VideoCapture.withOutput(
                                        recorder
                                );


                        ImageAnalysis imageAnalysis =
                                new ImageAnalysis.Builder()
                                        .setBackpressureStrategy(
                                                ImageAnalysis
                                                        .STRATEGY_KEEP_ONLY_LATEST
                                        )
                                        .build();


                        imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(
                                        this
                                ),
                                imageProxy -> {

                                    processFaceImage(
                                            imageProxy
                                    );
                                }
                        );


                        CameraSelector cameraSelector =
                                new CameraSelector.Builder()
                                        .requireLensFacing(
                                                CameraSelector
                                                        .LENS_FACING_FRONT
                                        )
                                        .build();


                        cameraProvider.unbindAll();


                        cameraProvider.bindToLifecycle(
                                this,
                                cameraSelector,
                                preview,
                                imageAnalysis,
                                videoCapture
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

                },
                ContextCompat.getMainExecutor(
                        this
                )
        );
    }


    // =========================================================
    // START RECORDING
    // =========================================================

    private void startRecording() {

        if (isRecording) {
            return;
        }

        if (videoCapture == null) {

            Log.e(
                    "VIDEO_DEBUG",
                    "VideoCapture is not ready"
            );

            return;
        }

        isRecording = true;


        String savedUri =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                )
                        .getString(
                                STORAGE_URI_KEY,
                                null
                        );


        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                ).format(
                        new Date()
                );

        String fileName =
                subjectId +
                        "_" +
                        timeStamp +
                        ".mp4";


        // =====================================================
        // CUSTOM DIRECTORY SELECTED
        // =====================================================

        if (savedUri != null) {

            startRecordingToSelectedDirectory(
                    Uri.parse(savedUri),
                    fileName
            );

            return;
        }


        // =====================================================
        // DEFAULT DIRECTORY
        // =====================================================

        File videoDir =
                new File(
                        getExternalFilesDir(
                                Environment.DIRECTORY_MOVIES
                        ),
                        "FaceCapture"
                );


        if (!videoDir.exists()) {

            boolean created =
                    videoDir.mkdirs();

            if (!created) {

                Log.e(
                        "VIDEO_DEBUG",
                        "Could not create video directory"
                );
            }
        }


        File videoFile =
                new File(
                        videoDir,
                        fileName
                );


        FileOutputOptions outputOptions =
                new FileOutputOptions.Builder(
                        videoFile
                ).build();


        beginRecording(
                videoCapture
                        .getOutput()
                        .prepareRecording(
                                this,
                                outputOptions
                        ),
                videoFile.getAbsolutePath()
        );
    }


    // =========================================================
    // RECORD TO USER SELECTED DIRECTORY
    // =========================================================

    private void startRecordingToSelectedDirectory(
            Uri treeUri,
            String fileName) {

        try {

            DocumentFile directory =
                    DocumentFile.fromTreeUri(
                            this,
                            treeUri
                    );


            if (directory == null ||
                    !directory.canWrite()) {

                Toast.makeText(
                        this,
                        "Selected directory cannot be written to",
                        Toast.LENGTH_LONG
                ).show();

                isRecording = false;

                return;
            }


            DocumentFile videoFile =
                    directory.createFile(
                            "video/mp4",
                            fileName
                    );


            if (videoFile == null) {

                Toast.makeText(
                        this,
                        "Could not create video file",
                        Toast.LENGTH_LONG
                ).show();

                isRecording = false;

                return;
            }


            ParcelFileDescriptor pfd =
                    getContentResolver()
                            .openFileDescriptor(
                                    videoFile.getUri(),
                                    "w"
                            );


            if (pfd == null) {

                Toast.makeText(
                        this,
                        "Could not open video file",
                        Toast.LENGTH_LONG
                ).show();

                isRecording = false;

                return;
            }


            FileDescriptorOutputOptions
                    outputOptions =
                    new FileDescriptorOutputOptions.Builder(
                            pfd
                    ).build();


            PendingRecording pendingRecording =
                    videoCapture
                            .getOutput()
                            .prepareRecording(
                                    this,
                                    outputOptions
                            );


            beginRecording(
                    pendingRecording,
                    videoFile.getName()
            );


            // The descriptor must remain open while recording.
            // It is closed after Finalize.

            videoFileDescriptor = pfd;


        } catch (Exception e) {

            Log.e(
                    "VIDEO_DEBUG",
                    "Could not prepare selected storage",
                    e
            );

            isRecording = false;

            Toast.makeText(
                    this,
                    "Unable to use selected directory",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private ParcelFileDescriptor videoFileDescriptor;


    // =========================================================
    // COMMON RECORDING LOGIC
    // =========================================================

    private void beginRecording(
            PendingRecording pendingRecording,
            String filePath) {


        recording =
                pendingRecording.start(
                        ContextCompat.getMainExecutor(
                                this
                        ),
                        videoRecordEvent -> {


                            if (videoRecordEvent
                                    instanceof VideoRecordEvent.Start) {

                                Log.d(
                                        "VIDEO_DEBUG",
                                        "Recording started"
                                );


                                runOnUiThread(() -> {

                                    startRecordingButton
                                            .setText(
                                                    "Recording..."
                                            );

                                    startRecordingButton
                                            .setEnabled(
                                                    false
                                            );

                                    statusText.setText(
                                            "Recording..."
                                    );
                                });


                                new CountDownTimer(
                                        5000,
                                        1000
                                ) {

                                    @Override
                                    public void onTick(
                                            long millisUntilFinished) {

                                        int seconds =
                                                (int) Math.ceil(
                                                        millisUntilFinished
                                                                / 1000.0
                                                );

                                        countdownText
                                                .setVisibility(
                                                        View.VISIBLE
                                                );

                                        countdownText.setText(
                                                "Recording: "
                                                        + seconds
                                        );
                                    }


                                    @Override
                                    public void onFinish() {

                                        countdownText
                                                .setVisibility(
                                                        View.GONE
                                                );

                                        if (recording != null) {

                                            Log.d(
                                                    "VIDEO_DEBUG",
                                                    "5 seconds reached - stopping recording"
                                            );

                                            recording.stop();
                                        }
                                    }

                                }.start();
                            }


                            if (videoRecordEvent
                                    instanceof VideoRecordEvent.Finalize) {

                                VideoRecordEvent.Finalize
                                        finalizeEvent =
                                        (VideoRecordEvent.Finalize)
                                                videoRecordEvent;


                                if (!finalizeEvent.hasError()) {

                                    Log.d(
                                            "VIDEO_DEBUG",
                                            "Video saved: "
                                                    + filePath
                                    );


                                    runOnUiThread(() -> {

                                        statusText.setText(
                                                "Video saved"
                                        );


                                        Toast.makeText(
                                                SelfieCaptureActivity.this,
                                                "Video saved successfully",
                                                Toast.LENGTH_LONG
                                        ).show();


                                        startRecordingButton
                                                .setText(
                                                        "Start Recording"
                                                );

                                        startRecordingButton
                                                .setEnabled(
                                                        true
                                                );


                                        // Refresh storage display
                                        updateStorageInformation();

                                    });


                                } else {

                                    Log.e(
                                            "VIDEO_DEBUG",
                                            "Recording failed: "
                                                    + finalizeEvent
                                                    .getError()
                                    );


                                    runOnUiThread(() -> {

                                        Toast.makeText(
                                                SelfieCaptureActivity.this,
                                                "Video recording failed",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        startRecordingButton
                                                .setText(
                                                        "Start Recording"
                                                );

                                        startRecordingButton
                                                .setEnabled(
                                                        true
                                                );
                                    });
                                }


                                // Close the selected-folder file
                                // descriptor after recording.
                                if (videoFileDescriptor != null) {

                                    try {

                                        videoFileDescriptor.close();

                                    } catch (Exception e) {

                                        Log.e(
                                                "VIDEO_DEBUG",
                                                "Could not close file descriptor",
                                                e
                                        );
                                    }

                                    videoFileDescriptor = null;
                                }


                                recording = null;

                                isRecording = false;
                            }
                        }
                );
    }


    // =========================================================
    // STOP RECORDING
    // =========================================================

    private void stopRecording() {

        if (recording != null) {

            Log.d(
                    "VIDEO_DEBUG",
                    "Stopping recording"
            );

            recording.stop();

            startRecordingButton.setText(
                    "Start Recording"
            );
        }
    }


    // =========================================================
    // FACE DETECTION
    // =========================================================

    private void processFaceImage(
            ImageProxy imageProxy) {

        if (imageProxy.getImage() == null) {

            imageProxy.close();

            return;
        }


        InputImage image =
                InputImage.fromMediaImage(
                        imageProxy.getImage(),
                        imageProxy.getImageInfo()
                                .getRotationDegrees()
                );


        faceDetector.process(image)

                .addOnSuccessListener(faces -> {

                    if (faces.isEmpty()) {

                        runOnUiThread(() ->
                                statusText.setText(
                                        "No face detected"
                                )
                        );

                    } else if (faces.size() > 1) {

                        runOnUiThread(() ->
                                statusText.setText(
                                        "Only one person should be visible"
                                )
                        );

                    } else {

                        Face face =
                                faces.get(0);

                        Rect bounds =
                                face.getBoundingBox();


                        int guideLeft =
                                faceGuide.getLeft();

                        int guideTop =
                                faceGuide.getTop();

                        int guideRight =
                                faceGuide.getRight();

                        int guideBottom =
                                faceGuide.getBottom();


                        ImageProxyTransformFactory factory =
                                new ImageProxyTransformFactory();


                        OutputTransform imageTransform =
                                factory.getOutputTransform(
                                        imageProxy
                                );


                        OutputTransform previewTransform =
                                selfiePreview
                                        .getOutputTransform();


                        CoordinateTransform
                                coordinateTransform =
                                new CoordinateTransform(
                                        imageTransform,
                                        previewTransform
                                );


                        Matrix matrix =
                                new Matrix();

                        coordinateTransform.transform(
                                matrix
                        );


                        RectF faceRect =
                                new RectF(bounds);

                        matrix.mapRect(
                                faceRect
                        );


                        float transformedCenterX =
                                faceRect.centerX();

                        float transformedCenterY =
                                faceRect.centerY();


                        boolean faceCenterInsideGuide =
                                transformedCenterX >= guideLeft &&
                                        transformedCenterX <= guideRight &&
                                        transformedCenterY >= guideTop &&
                                        transformedCenterY <= guideBottom;


                        int faceWidth =
                                bounds.width();


                        int faceCenterX =
                                bounds.centerX();

                        int faceCenterY =
                                bounds.centerY();


                        int imageWidth =
                                imageProxy.getWidth();

                        int imageHeight =
                                imageProxy.getHeight();


                        int imageCenterX =
                                imageWidth / 2;

                        int imageCenterY =
                                imageHeight / 2;


                        int differenceX =
                                Math.abs(
                                        faceCenterX -
                                                imageCenterX
                                );


                        int differenceY =
                                Math.abs(
                                        faceCenterY -
                                                imageCenterY
                                );


                        int toleranceX =
                                imageWidth / 8;

                        int toleranceY =
                                imageHeight / 8;


                        boolean centered =
                                differenceX <= toleranceX &&
                                        differenceY <= toleranceY;


                        int minFaceWidth = 250;
                        int maxFaceWidth = 600;


                        String distanceMessage;


                        if (faceWidth < minFaceWidth) {

                            distanceMessage =
                                    "Move closer";

                        } else if (faceWidth > maxFaceWidth) {

                            distanceMessage =
                                    "Move farther away";

                        } else {

                            distanceMessage =
                                    "Distance is good";
                        }


                        boolean goodDistance =
                                faceWidth >= minFaceWidth &&
                                        faceWidth <= maxFaceWidth;


                        boolean ready =
                                faceCenterInsideGuide &&
                                        centered &&
                                        goodDistance;


                        faceReady = ready;


                        if (isRecording) {

                            faceWasGoodDuringRecording =
                                    ready;
                        }


                        runOnUiThread(() -> {

                            if (isRecording) {

                                if (ready) {

                                    statusText.setText(
                                            "Recording - Hold still"
                                    );

                                } else {

                                    statusText.setText(
                                            "Please hold still and stay in the guide"
                                    );
                                }

                            } else if (ready) {

                                statusText.setText(
                                        "Ready - Hold still"
                                );

                            } else if (!centered) {

                                statusText.setText(
                                        "Please center your face"
                                );

                            } else if (!goodDistance) {

                                statusText.setText(
                                        distanceMessage
                                );
                            }
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


    // =========================================================
    // ACTIVITY CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        if (countdownTimer != null) {

            countdownTimer.cancel();
        }


        if (recording != null) {

            recording.stop();

            recording = null;
        }


        if (videoFileDescriptor != null) {

            try {

                videoFileDescriptor.close();

            } catch (Exception ignored) {
            }

            videoFileDescriptor = null;
        }


        if (faceDetector != null) {

            faceDetector.close();
        }


        super.onDestroy();
    }
}