package com.example.facecapture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;

import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import android.graphics.Matrix;

import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.PendingRecording;

import android.os.Environment;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.MotionEvent;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import android.graphics.RectF;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import android.graphics.Rect;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

public class SelfieCaptureActivity extends AppCompatActivity {
    private TextView subjectIdText;
    private PreviewView selfiePreview;
    private FaceDetector faceDetector;
    private TextView statusText;
    private View faceGuide;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private Button startRecordingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);
        statusText = findViewById(R.id.statusText);
        subjectIdText = findViewById(R.id.subjectIdText);
        selfiePreview = findViewById(R.id.selfiePreview);
        faceGuide = findViewById(R.id.faceGuide);
        startRecordingButton = findViewById(R.id.startRecordingButton);

        startRecordingButton.setOnClickListener(v -> {

            if (recording == null) {
                startRecording();
            } else {
                stopRecording();
            }

        });

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

                // Add the Recorder
                QualitySelector qualitySelector =
                        QualitySelector.from(Quality.HD);

                Recorder recorder =
                        new Recorder.Builder()
                                .setQualitySelector(qualitySelector)
                                .build();

                videoCapture =
                        VideoCapture.withOutput(recorder);

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

                // Bind camera preview, analysis, and video capture
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

        }, ContextCompat.getMainExecutor(this));
    }

    private void startRecording() {

        if (videoCapture == null) {
            Log.e("VIDEO_DEBUG", "VideoCapture is not ready");
            return;
        }

        File videoDir = new File(
                getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "FaceCapture"
        );

        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }

        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                ).format(new Date());

        File videoFile = new File(
                videoDir,
                "selfie_" + timeStamp + ".mp4"
        );

        FileOutputOptions outputOptions =
                new FileOutputOptions.Builder(videoFile)
                        .build();

        PendingRecording pendingRecording =
                videoCapture.getOutput()
                        .prepareRecording(
                                this,
                                outputOptions
                        );

//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//        ) == PackageManager.PERMISSION_GRANTED) {
//
//            pendingRecording =
//                    pendingRecording.withAudioEnabled();
//        }

        recording =
                pendingRecording.start(
                        ContextCompat.getMainExecutor(this),
                        videoRecordEvent -> {

                            if (videoRecordEvent
                                    instanceof VideoRecordEvent.Start) {

                                Log.d(
                                        "VIDEO_DEBUG",
                                        "Recording started"
                                );

                                runOnUiThread(() -> {

                                    startRecordingButton
                                            .setText("Recording...");

                                    startRecordingButton
                                            .setEnabled(false);

                                    statusText.setText(
                                            "Recording..."
                                    );

                                });

                                new Handler(Looper.getMainLooper()).postDelayed(
                                        () -> {

                                            if (recording != null) {

                                                Log.d(
                                                        "VIDEO_DEBUG",
                                                        "5 seconds reached - stopping recording"
                                                );

                                                recording.stop();
                                            }

                                        },
                                        5000
                                );
                            }

                            if (videoRecordEvent
                                    instanceof VideoRecordEvent.Finalize) {

                                VideoRecordEvent.Finalize finalizeEvent =
                                        (VideoRecordEvent.Finalize)
                                                videoRecordEvent;

                                if (!finalizeEvent.hasError()) {

                                    Log.d(
                                            "VIDEO_DEBUG",
                                            "Video saved: "
                                                    + videoFile.getAbsolutePath()
                                    );

                                    runOnUiThread(() -> {

                                        statusText.setText(
                                                "Video saved"
                                        );

                                        startRecordingButton
                                                .setText(
                                                        "Start Recording"
                                                );

                                        startRecordingButton
                                                .setEnabled(true);

                                    });

                                } else {

                                    Log.e(
                                            "VIDEO_DEBUG",
                                            "Recording failed: "
                                                    + finalizeEvent
                                                    .getError()
                                    );
                                }

                                recording = null;
                            }
                        }
                );
    }

    private void stopRecording() {

        if (recording != null) {

            Log.d(
                    "VIDEO_DEBUG",
                    "Stopping recording"
            );

            recording.stop();
            recording = null;

            startRecordingButton.setText(
                    "Start Recording"
            );
        }
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

                        runOnUiThread(() -> {
                            statusText.setText("No face detected");
                        });

                    } else if (faces.size() > 1) {

                        runOnUiThread(() -> {
                            statusText.setText("Only one person should be visible");
                        });

                    } else {

                        // ONE FACE DETECTED

                        Face face = faces.get(0);
                        Rect bounds = face.getBoundingBox();

                        int guideLeft = faceGuide.getLeft();
                        int guideTop = faceGuide.getTop();
                        int guideRight = faceGuide.getRight();
                        int guideBottom = faceGuide.getBottom();

                        Log.d(
                                "GUIDE_COORD",
                                "Guide: left=" + guideLeft +
                                        " top=" + guideTop +
                                        " right=" + guideRight +
                                        " bottom=" + guideBottom
                        );
                        ImageProxyTransformFactory factory =
                                new ImageProxyTransformFactory();

                        OutputTransform imageTransform =
                                factory.getOutputTransform(imageProxy);

                        OutputTransform previewTransform =
                                selfiePreview.getOutputTransform();

                        CoordinateTransform coordinateTransform =
                                new CoordinateTransform(
                                        imageTransform,
                                        previewTransform
                                );
                        Matrix matrix = new Matrix();

                        coordinateTransform.transform(matrix);

                        RectF faceRect = new RectF(bounds);
                        matrix.mapRect(faceRect);

                        float transformedCenterX = faceRect.centerX();
                        float transformedCenterY = faceRect.centerY();

                        boolean faceCenterInsideGuide =
                                transformedCenterX >= guideLeft &&
                                        transformedCenterX <= guideRight &&
                                        transformedCenterY >= guideTop &&
                                        transformedCenterY <= guideBottom;
                        Log.d(
                                "GUIDE_COORD",
                                "Face center inside guide: " + faceCenterInsideGuide
                        );

                        Log.d(
                                "FACE_COORD",
                                "Transformed face: " + faceRect.toString()
                        );
                        Log.d(
                                "FACE_COORD",
                                "Face bounds: " + bounds.toString()
                        );

                        Log.d(
                                "FACE_COORD",
                                "Image size: " +
                                        imageProxy.getWidth() +
                                        " x " +
                                        imageProxy.getHeight()
                        );

                        Log.d(
                                "FACE_COORD",
                                "Preview size: " +
                                        selfiePreview.getWidth() +
                                        " x " +
                                        selfiePreview.getHeight()
                        );

                        int faceWidth = bounds.width();
                        int faceHeight = bounds.height();

                        int faceCenterX = bounds.centerX();
                        int faceCenterY = bounds.centerY();

                        int imageWidth = imageProxy.getWidth();
                        int imageHeight = imageProxy.getHeight();

                        int imageCenterX = imageWidth / 2;
                        int imageCenterY = imageHeight / 2;

                        int differenceX =
                                Math.abs(faceCenterX - imageCenterX);

                        int differenceY =
                                Math.abs(faceCenterY - imageCenterY);

                        int toleranceX = imageWidth / 8;
                        int toleranceY = imageHeight / 8;

                        boolean centered =
                                differenceX <= toleranceX &&
                                        differenceY <= toleranceY;


                        // #4 — Check face size

                        int minFaceWidth = 250;
                        int maxFaceWidth = 600;

                        String distanceMessage;

                        if (faceWidth < minFaceWidth) {

                            distanceMessage = "Move closer";

                        } else if (faceWidth > maxFaceWidth) {

                            distanceMessage = "Move farther away";

                        } else {

                            distanceMessage = "Distance is good";
                        }


                        // #5 — Combine centering + distance

                        boolean goodDistance =
                                faceWidth >= minFaceWidth &&
                                        faceWidth <= maxFaceWidth;

                        boolean ready =
                                centered &&
                                        goodDistance;

                        // Display the result

                        runOnUiThread(() -> {

                            if (ready) {

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
}