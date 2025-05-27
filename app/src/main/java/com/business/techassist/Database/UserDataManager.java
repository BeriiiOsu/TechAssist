package com.business.techassist.Database;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class UserDataManager {

    public interface FirestoreCallback {
        void onSuccess(Map<String, String> userData);
        void onFailure(String errorMessage);
    }

    public interface SQLiteImageCallback {
        void onImageLoaded(Bitmap image);
        void onError(String error);
    }

    // Load user info (name, email, role) from Firestore
    public static void loadUserDataFromFirestore(FirestoreCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in.");
            return;
        }

        String uid = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, String> userData = new HashMap<>();
                        userData.put("name", documentSnapshot.getString("Name"));
                        userData.put("email", documentSnapshot.getString("Email"));
                        userData.put("role", documentSnapshot.getString("Role"));
                        callback.onSuccess(userData);
                    } else {
                        callback.onFailure("User document not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error loading Firestore data: " + e.getMessage());
                });
    }

    // Load profile image from SQLite by UID
    public static void loadImageFromSQLite(Context context, String uid, SQLiteImageCallback callback) {
        try {
            SQLiteDatabase db = context.openOrCreateDatabase("adminDB.db", Context.MODE_PRIVATE, null);

            Cursor cursor = db.rawQuery("SELECT image FROM admin WHERE uid = ?", new String[]{uid});
            if (cursor.moveToFirst()) {
                byte[] imageBytes = cursor.getBlob(0);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                callback.onImageLoaded(bitmap);
            } else {
                callback.onError("No image found for user.");
            }

            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("UserDataManager", "SQLite Error: " + e.getMessage());
            callback.onError("SQLite Error: " + e.getMessage());
        }
    }
}
