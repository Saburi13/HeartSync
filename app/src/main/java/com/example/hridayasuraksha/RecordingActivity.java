package com.example.hridayasuraksha;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

public class RecordingActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 200;
    private static final int REQUEST_AUDIO_FILE = 300;

    private MediaRecorder recorder;
    private String filePath;

    private Button btnRecord, btnPause, btnSubmit;
    private TextView statusText, timerText;
    private ProgressBar levelBar;
    private View bar1, bar2, bar3, bar4, bar5;
    private ImageButton btnBack;

    private Handler handler = new Handler();
    private int secondsElapsed = 0;
    private boolean isRecording = false;

    private Runnable timerRunnable;

    private SupabaseHelper supabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        // UI components
        btnRecord = findViewById(R.id.btnRecord);
        btnPause = findViewById(R.id.btnPause);
        btnSubmit = findViewById(R.id.btnSubmit);
        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        levelBar = findViewById(R.id.levelBar);
        btnBack = findViewById(R.id.btnBack);

        // Waveform bars
        bar1 = findViewById(R.id.bar1);
        bar2 = findViewById(R.id.bar2);
        bar3 = findViewById(R.id.bar3);
        bar4 = findViewById(R.id.bar4);
        bar5 = findViewById(R.id.bar5);

        levelBar.setMax(100);
        levelBar.setProgress(0);

        btnPause.setEnabled(false);
        btnSubmit.setEnabled(false);

        // Initialize Supabase helper
        supabaseHelper = new SupabaseHelper(this);

        // Back button → return to Dashboard
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(RecordingActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Start recording
        btnRecord.setOnClickListener(v -> {
            if (checkPermissions()) {
                startRecording();
            } else {
                requestPermissions();
            }
        });

        // Pause button (optional)
        btnPause.setOnClickListener(v ->
                Toast.makeText(this, "Pause not supported yet", Toast.LENGTH_SHORT).show()
        );

        // Submit button → upload to Supabase
        btnSubmit.setOnClickListener(v -> {
            if (filePath != null) {
                File audioFile = new File(filePath);
                new Thread(() -> {
                    try {
                        supabaseHelper.uploadRecording(
                                audioFile,
                                secondsElapsed + "s",
                                "Chest auscultation"
                        );
                        runOnUiThread(() ->
                                Toast.makeText(this, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                }).start();
            } else {
                Toast.makeText(this, "No recording available", Toast.LENGTH_SHORT).show();
            }
        });

        // Upload from phone
        findViewById(R.id.cardUpload).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(Intent.createChooser(intent, "Select Audio"), REQUEST_AUDIO_FILE);
        });
    }

    private void startRecording() {
        filePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/heartbeat_record.3gp";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(filePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
            statusText.setText("🎙 Recording...");
            isRecording = true;
            btnRecord.setEnabled(false);
            btnPause.setEnabled(false);
            btnSubmit.setEnabled(false);

            secondsElapsed = 0;
            levelBar.setProgress(0);

            startTimer();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRecording) return;

                secondsElapsed++;
                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                timerText.setText(String.format("%02d:%02d", minutes, seconds));

                int progress = (secondsElapsed * 100) / 10;
                levelBar.setProgress(progress);

                if (recorder != null) {
                    int amplitude = recorder.getMaxAmplitude();
                    animateWaveform(amplitude);
                }

                if (secondsElapsed >= 10) {
                    stopRecording();
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(timerRunnable);
    }

    private void animateWaveform(int amplitude) {
        int level = amplitude / 200;

        animateBarHeight(bar1, 10 + (level % 30));
        animateBarHeight(bar2, 18 + (level % 40));
        animateBarHeight(bar3, 14 + (level % 35));
        animateBarHeight(bar4, 20 + (level % 45));
        animateBarHeight(bar5, 12 + (level % 25));
    }

    private void animateBarHeight(View bar, int newHeight) {
        int oldHeight = bar.getLayoutParams().height;
        ValueAnimator animator = ValueAnimator.ofInt(oldHeight, newHeight);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            bar.getLayoutParams().height = (int) animation.getAnimatedValue();
            bar.requestLayout();
        });
        animator.start();
    }

    private void stopRecording() {
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            statusText.setText("✅ Saved: " + filePath);
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }

        isRecording = false;
        btnRecord.setEnabled(true);
        btnSubmit.setEnabled(true);
        btnPause.setEnabled(false);

        handler.removeCallbacks(timerRunnable);
        levelBar.setProgress(100);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AUDIO_FILE && resultCode == RESULT_OK && data != null) {
            Uri selectedAudio = data.getData();
            if (selectedAudio != null) {
                File audioFile = new File(FileUtils.getPath(this, selectedAudio)); // implement FileUtils.getPath
                new Thread(() -> {
                    try {
                        supabaseHelper.uploadRecording(audioFile, "20s", "Uploaded recording");
                        runOnUiThread(() ->
                                Toast.makeText(this, "Uploaded from phone!", Toast.LENGTH_SHORT).show()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                }).start();
            }
        }
    }
}
