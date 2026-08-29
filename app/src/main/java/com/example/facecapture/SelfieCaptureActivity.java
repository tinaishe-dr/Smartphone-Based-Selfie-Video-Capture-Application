package com.example.facecapture;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.TextView;

public class SelfieCaptureActivity extends AppCompatActivity {
    private TextView subjectIdText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);

        subjectIdText = findViewById(R.id.subjectIdText);
        String subjectId = getIntent()
                .getStringExtra("SUBJECT_ID");
        subjectIdText.setText(
                "Subject: " + subjectId
        );

    }
}