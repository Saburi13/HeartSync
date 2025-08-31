package com.example.hridayasuraksha;

import android.content.Context;
import android.content.SharedPreferences;

public class SupabaseSession {

    private static final String PREF_NAME = "supabase_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";

    /**
     * Save Supabase session (called after Google login + exchange with Supabase).
     */
    public static void saveSession(Context context, String accessToken, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    /**
     * Get stored access token (needed for Supabase REST/Storage calls).
     */
    public static String getAccessToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Get stored userId (e.g., Supabase `sub` from JWT).
     */
    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * Clear session on logout.
     */
    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Check if a valid session exists.
     */
    public static boolean isLoggedIn(Context context) {
        return getAccessToken(context) != null && getUserId(context) != null;
    }
}
