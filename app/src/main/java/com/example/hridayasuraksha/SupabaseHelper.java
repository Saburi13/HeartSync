package com.example.hridayasuraksha;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseHelper {

    private static final String TAG = "SupabaseHelper";

    // 🔹 Replace with your project values
    private static final String SUPABASE_URL = "https://iwjhyzlvjlaafdtuuxuj.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml3amh5emx2amxhYWZkdHV1eHVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1NDU3MDcsImV4cCI6MjA3MjEyMTcwN30.XoUmmRFZGc3sZb-d7UvpbOYoueE7Rfm-UJj0m8leNtw";

    private static final String BUCKET_NAME = "recordings"; // 🔹 Make sure this bucket exists in Supabase

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SupabaseHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    /**
     * Uploads recording file to Supabase Storage and inserts metadata in Postgres.
     */
    public void uploadRecording(File file, String duration, String notes) {
        if (file == null || !file.exists()) {
            showToast("File does not exist");
            return;
        }

        String userId = SupabaseSession.getUserId(context); // 🔹 Implemented separately
        if (userId == null) {
            showToast("User not logged in");
            return;
        }

        String objectPath = userId + "/" + System.currentTimeMillis() + ".3gp";
        String storageUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + objectPath;

        try {
            byte[] fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            int read = fis.read(fileBytes);
            fis.close();

            if (read <= 0) {
                showToast("Failed to read file");
                return;
            }

            RequestBody body = RequestBody.create(fileBytes, MediaType.parse("audio/3gpp"));

            Request request = new Request.Builder()
                    .url(storageUrl)
                    .addHeader("Authorization", "Bearer " + SupabaseSession.getAccessToken(context))
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "audio/3gpp")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    showToast("Upload failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String respBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + objectPath;
                        Log.d(TAG, "File uploaded: " + publicUrl);
                        insertRecordingToDB(userId, objectPath, publicUrl, duration, notes);
                    } else {
                        Log.e(TAG, "Upload error: " + response.code() + " " + respBody);
                        showToast("Upload error: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Upload exception: " + e.getMessage());
        }
    }

    /**
     * Inserts metadata row into Supabase Postgres "recordings" table.
     */
    private void insertRecordingToDB(String userId, String filePath, String fileUrl, String duration, String notes) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("file_path", filePath);
            json.put("file_url", fileUrl);
            json.put("duration", duration);
            json.put("notes", notes);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/recordings")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseSession.getAccessToken(context))
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    showToast("DB insert failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String respBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        showToast("Recording saved in DB!");
                        Log.d(TAG, "DB insert success: " + respBody);
                    } else {
                        Log.e(TAG, "DB insert error: " + response.code() + " " + respBody);
                        showToast("DB error: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showToast("DB exception: " + e.getMessage());
        }
    }

    private void showToast(String msg) {
        mainHandler.post(() ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
