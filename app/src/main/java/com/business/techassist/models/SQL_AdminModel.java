package com.business.techassist.models;

public class SQL_AdminModel {
    String ratings;
    String name;
    String specialized;
    int yearsExp;
    byte[] image;



    public SQL_AdminModel(String name, String ratings, String specialized, int yearsExp, byte[] image) {
        this.name = name;
        this.ratings = ratings;
        this.specialized = specialized;
        this.yearsExp = yearsExp;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRatings() {
        return ratings;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public void setRatings(String ratings) {
        this.ratings = ratings;
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
}
