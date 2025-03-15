package com.business.techassist.utilities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.business.techassist.models.AdminModel;

public class AndroidUtil {

    public static void passAdminDataMessages(Intent intent, AdminModel model){
        intent.putExtra("name", model.getName());
        intent.putExtra("email", model.getEmail());
        intent.putExtra("userID", model.getUserID());
        intent.putExtra("fcmToken",model.getFcmTokens());
    }

    public static AdminModel getAdminDataMessages(Intent intent){
        AdminModel model = new AdminModel();
        model.setEmail(intent.getStringExtra("email"));
        model.setName(intent.getStringExtra("name"));
        model.setUserID(intent.getStringExtra("userID"));
        model.setFcmTokens(intent.getStringExtra("fcmToken"));
        return model;
    }

    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView){
        Glide.with(context).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView);
    }

}
