package com.example.hridayasuraksha;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ResultActivity extends AppCompatActivity {

    private ImageButton btnShareTop, btnPlay, btnPause, btnStop;
    private ImageView btnBack, timelineHandle;
    private ProgressBar confidenceRing, timelineFill;
    private TextView confidenceText, latestHeartRate, avgHR, maxHR;
    private MaterialButton exportPdfBtn, shareReportBtn;
    private BottomNavigationView bottomNav;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

    private int confidence = 93; // Example analysis result
    private int progressStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // --- Bind views ---
        btnBack = findViewById(R.id.btnBack);
        btnShareTop = findViewById(R.id.btnShareTop);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        confidenceRing = findViewById(R.id.confidenceRing);
        confidenceText = findViewById(R.id.confidenceText);
        latestHeartRate = findViewById(R.id.latestHeartRate);
        avgHR = findViewById(R.id.avgHR);
        maxHR = findViewById(R.id.maxHR);
        exportPdfBtn = findViewById(R.id.exportPdfBtn);
        shareReportBtn = findViewById(R.id.shareReportBtn);
        bottomNav = findViewById(R.id.bottomNav);

        // timeline views
        timelineFill = findViewById(R.id.timelineFill);
        timelineHandle = findViewById(R.id.timelineHandle);

        // ✅ Handle Back Button
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ResultActivity.this, DashboardActivity.class));
            finish();
        });

        // ✅ Top Share button
        btnShareTop.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareMsg = "Heart sound analysis result: Confidence " + confidence + "%, Murmurs present.";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
            startActivity(Intent.createChooser(shareIntent, "Share Results via"));
        });

        // ✅ Animate confidence ring
        animateConfidence(confidence);

        // ✅ Example heart metrics (later replace with Google Fit)
        latestHeartRate.setText("76 BPM");
        avgHR.setText("Avg: 80 BPM");
        maxHR.setText("Max: 96 BPM");

        // ✅ Media player setup
        setupMediaPlayer();

        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.start();
                startTimelineAnimation();
                Toast.makeText(this, "Playing clip...", Toast.LENGTH_SHORT).show();
            }
        });

        btnPause.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0);
                updateTimeline(0); // reset timeline
                Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Export PDF
        exportPdfBtn.setOnClickListener(v ->
                Toast.makeText(this, "Exporting PDF... (feature coming soon)", Toast.LENGTH_SHORT).show()
        );

        // ✅ Share full report
        shareReportBtn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String report = "Heart Sound Analysis Report\n" +
                    "Confidence: " + confidence + "%\n" +
                    "Latest HR: 76 BPM\nAvg HR: 80 BPM\nMax HR: 96 BPM\n" +
                    "Next steps: If symptoms persist, consider consulting a cardiologist.";
            shareIntent.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(shareIntent, "Share Report"));
        });

        // ✅ Bottom Navigation Handling
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(ResultActivity.this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.btnRecord) {
                startActivity(new Intent(ResultActivity.this, RecordingActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_reports) {
                // already here
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(ResultActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Highlight current tab
        bottomNav.setSelectedItemId(R.id.nav_reports);
    }

    private void setupMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.sample_audio); // put sample_audio.mp3 in res/raw
        } catch (Exception e) {
            Log.e("ResultActivity", "MediaPlayer setup failed", e);
        }
    }

    private void animateConfidence(int target) {
        progressStatus = 0;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressStatus < target) {
                    progressStatus++;
                    confidenceRing.setProgress(progressStatus);
                    confidenceText.setText(progressStatus + "%");
                    handler.postDelayed(this, 20); // smooth animation
                }
            }
        }, 20);
    }

    private void startTimelineAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPos = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();

                    if (duration > 0) {
                        int progress = (int) (((float) currentPos / duration) * 100);
                        updateTimeline(progress);
                    }
                    handler.postDelayed(this, 200); // update every 200ms
                }
            }
        }, 200);
    }

    private void updateTimeline(int progress) {
        timelineFill.setProgress(progress);
        // move the handle with the same progress (assuming it's aligned with progress bar)
        timelineHandle.setTranslationX(
                (timelineFill.getWidth() * progress) / 100f - (timelineHandle.getWidth() / 2f)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
