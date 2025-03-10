package com.business.techassist.utilities;

import android.content.Intent;

import com.business.techassist.UserCredentials.AdminModel;

public class AndroidUtil {

    public static void passAdminDataMessages(Intent intent, AdminModel model){
        intent.putExtra("name", model.getName());
        intent.putExtra("email", model.getEmail());
        intent.putExtra("userID", model.getUserID());
    }

    public static AdminModel getAdminDataMessages(Intent intent){
        AdminModel model = new AdminModel();
        model.setEmail(intent.getStringExtra("email"));
        model.setName(intent.getStringExtra("name"));
        model.setUserID(intent.getStringExtra("userID"));
        return model;
    }

}
