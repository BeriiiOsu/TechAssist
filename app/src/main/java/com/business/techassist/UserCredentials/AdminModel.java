package com.business.techassist.UserCredentials;

import com.google.firebase.firestore.PropertyName;

public class AdminModel {
    String name;
    String email;
    int ratings;
    String userID;

    public AdminModel() {
    }

    public AdminModel(String email, int ratings, String name, String userID) {
        this.email = email;
        this.ratings = ratings;
        this.name = name;
        this.userID = userID;
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

    public int getRatings() {
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

    public void setRatings(int ratings) {
        this.ratings = ratings;
    }
}
