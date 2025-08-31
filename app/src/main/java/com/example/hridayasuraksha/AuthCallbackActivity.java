package com.example.hridayasuraksha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class AuthCallbackActivity extends AppCompatActivity {
    private static final String TAG = "AuthCallbackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data == null) {
            Toast.makeText(this, "No callback data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // The callback from Supabase OAuth comes as a fragment like:
        // com.example.hridayasuraksha://callback#access_token=...&expires_in=3600&token_type=bearer&refresh_token=...
        String full = data.toString();
        String fragment = null;
        int idx = full.indexOf('#');
        if (idx >= 0) {
            fragment = full.substring(idx + 1);
        } else {
            // fallback - sometimes the fragment may be available via Uri.getFragment()
            fragment = data.getFragment();
        }

        if (fragment == null) {
            Toast.makeText(this, "Auth failed (no token fragment)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Map<String, String> map = parseFragment(fragment);
        String accessToken = map.get("access_token");
        String refreshToken = map.get("refresh_token");
        String expiresIn = map.get("expires_in");

        if (accessToken == null) {
            String err = map.get("error_description");
            Toast.makeText(this, "Auth failed: " + (err != null ? err : "no access token"), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Auth fragment: " + fragment);
            finish();
            return;
        }

        // Save tokens locally (SharedPreferences)
        getSharedPreferences("supabase", MODE_PRIVATE)
                .edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putString("expires_in", expiresIn)
                .apply();

        Toast.makeText(this, "Signed in (Supabase)", Toast.LENGTH_SHORT).show();

        // Navigate to Dashboard (or your desired activity)
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private Map<String, String> parseFragment(String fragment) {
        Map<String, String> map = new HashMap<>();
        try {
            String[] pairs = fragment.split("&");
            for (String p : pairs) {
                int eq = p.indexOf('=');
                if (eq > -1) {
                    String k = URLDecoder.decode(p.substring(0, eq), "UTF-8");
                    String v = URLDecoder.decode(p.substring(eq + 1), "UTF-8");
                    map.put(k, v);
                }
            }
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen for UTF-8
        }
        return map;
    }
}
