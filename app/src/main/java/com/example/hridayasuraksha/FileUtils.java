package com.example.hridayasuraksha;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public class FileUtils {

    /**
     * Get the real file path from a Uri
     *
     * @param context context
     * @param uri     content Uri
     * @return absolute file path
     */
    public static String getPath(Context context, Uri uri) {
        String filePath = null;

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.MediaColumns.DATA};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    filePath = cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
        }

        if (filePath != null) {
            return filePath;
        } else {
            // Fallback: copy to cache directory
            return copyUriToCache(context, uri);
        }
    }

    private static String copyUriToCache(Context context, Uri uri) {
        try {
            File cacheFile = new File(context.getCacheDir(), "temp_audio_" + System.currentTimeMillis() + ".mp3");
            java.io.InputStream input = context.getContentResolver().openInputStream(uri);
            java.io.OutputStream output = new java.io.FileOutputStream(cacheFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            output.close();
            input.close();
            return cacheFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
