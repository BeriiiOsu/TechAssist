package com.business.techassist.shopitems;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class Product {
    private String name;
    private int quantity;
    private double price;
    private String description;
    private Bitmap imagePath; // Store image as a file path

    public Product() {}

    public Product(String name, int quantity, double price, String description, Bitmap imagePath) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.description = description;
        this.imagePath = imagePath;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Bitmap getImage() { return imagePath; }

    public String getDescription() { return description; }

    public String getName() { return name; }

    public int getQuantity() { return quantity; }

    public double getPrice() { return price; }

}
