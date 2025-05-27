package com.business.techassist.admin_utils;

import com.google.firebase.firestore.PropertyName;

public class AdminModel {
    String name;
    String email;
    String ratings;
    String userID;
    String fcmTokens;
    String specialized;
    int yearsExp;



    public AdminModel() {
    }

    public AdminModel(String email, String ratings, String name, String userID, String specialized, int yearsExp) {
        this.email = email;
        this.ratings = ratings;
        this.name = name;
        this.userID = userID;
        this.specialized = specialized;
        this.yearsExp = yearsExp;
    }
    @PropertyName("Email")
    public String getEmail() {
        return email;
    }
    @PropertyName("Email")
    public void setEmail(String email) {
        this.email = email;
    }
    @PropertyName("Name")
    public String getName() {
        return name;
    }
    @PropertyName("Name")
    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialized() {
        return specialized;
    }

    public void setSpecialized(String specialized) {
        this.specialized = specialized;
    }

    public int getYearsExp() {
        return yearsExp;
    }

    public void setYearsExp(int yearsExp) {
        this.yearsExp = yearsExp;
    }

    public String getRatings() {
        return ratings;
    }
    @PropertyName("userID")
    public String getUserID() {
        return userID;
    }
    @PropertyName("userID")
    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setRatings(String ratings) {
        this.ratings = ratings;
    }

    public String getFcmTokens() {
        return fcmTokens;
    }

    public void setFcmTokens(String fcmTokens) {
        this.fcmTokens = fcmTokens;
    }
}
