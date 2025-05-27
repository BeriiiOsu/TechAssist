package com.business.techassist.utilities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.business.techassist.admin_utils.AdminModel;

public class AndroidUtil {

    public static void passAdminDataMessages(Intent intent, AdminModel model){
        intent.putExtra("name", model.getName());
        intent.putExtra("email", model.getEmail());
        intent.putExtra("userID", model.getUserID());
        intent.putExtra("fcmToken",model.getFcmTokens());
    }
    
    public static void passUserDataMessages(Intent intent, AdminModel model){
        intent.putExtra("name", model.getName());
        intent.putExtra("email", model.getEmail());
        intent.putExtra("userID", model.getUserID());
        intent.putExtra("fcmToken",model.getFcmTokens());
    }

    public static AdminModel getAdminDataMessages(Intent intent) {
        if (intent == null) {
            Log.e("AndroidUtil", "Intent is null in getAdminDataMessages");
            return null;
        }
        
        AdminModel model = new AdminModel();
        
        try {
            // Get extras with null checks
            String email = intent.getStringExtra("email");
            String name = intent.getStringExtra("name");
            String userID = intent.getStringExtra("userID");
            String fcmToken = intent.getStringExtra("fcmToken");
            
            // Log received data for debugging
            Log.d("AndroidUtil", "Received admin data - " +
                  "userID: " + (userID != null ? userID : "null") + 
                  ", name: " + (name != null ? name : "null") + 
                  ", email: " + (email != null ? email : "null"));
            
            // Set values with defaults for null values
            model.setEmail(email != null ? email : "");
            model.setName(name != null ? name : "User");
            model.setUserID(userID);
            model.setFcmTokens(fcmToken != null ? fcmToken : "");
            
            // Validate critical field
            if (userID == null || userID.isEmpty()) {
                Log.e("AndroidUtil", "Critical data missing: userID is null or empty");
                return null; // Return null if critical data is missing
            }
            
            return model;
        } catch (Exception e) {
            Log.e("AndroidUtil", "Exception in getAdminDataMessages: " + e.getMessage());
            return null;
        }
    }

    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView){
        Glide.with(context).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView);
    }

}
