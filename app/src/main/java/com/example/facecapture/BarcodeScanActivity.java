package com.example.facecapture;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class BarcodeScanActivity extends AppCompatActivity {

    private View scanLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_barcode_scan);

        scanLine = findViewById(R.id.scanLine);

        startScanAnimation();
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
}
