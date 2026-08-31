
package com.example.facecapture;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class CaptureSuccess extends AppCompatActivity {

    private TextView tvSubjectId, tvFilename, tvDuration, tvResolution;
    private MaterialButton btnScanNext, btnRetake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_success);

        // Bind layout views
        tvSubjectId = findViewById(R.id.tvSubjectId);
        tvFilename = findViewById(R.id.tvFilename);
        tvDuration = findViewById(R.id.tvDuration);
        tvResolution = findViewById(R.id.tvResolution);
        btnScanNext = findViewById(R.id.btnScanNext);
        btnRetake = findViewById(R.id.btnRetake);

        // Set dynamic data (can also be passed from Intent extras)
        populateMetadata("SUBJ-94821", "SUBJ-94821_2026-08-30_183821.mp4", "5.0s", "1080p (60fps)");

        // Set click behavior
        btnScanNext.setOnClickListener(v -> {
            Toast.makeText(this, "Scan Next Subject ID clicked", Toast.LENGTH_SHORT).show();
            // Implement next step logic here (e.g., return to barcode/QR scanner)
        });

        btnRetake.setOnClickListener(v -> {
            Toast.makeText(this, "Retake Video clicked", Toast.LENGTH_SHORT).show();
            finish(); // Returns to camera capture activity
        });
    }

    private void populateMetadata(String subjectId, String filename, String duration, String resolution) {
        tvSubjectId.setText(subjectId);
        tvFilename.setText(filename);
        tvDuration.setText("Duration: " + duration);
        tvResolution.setText("Resolution: " + resolution);
    }
}