package com.business.techassist.shopitems;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.business.techassist.R;

public class ProductDetails extends AppCompatActivity {
    private TextView productName, priceTxt, descriptionTxt, stockDetails;
    private ImageView backProductBtn, detailsPicProduct;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        productName = findViewById(R.id.productName);
        priceTxt = findViewById(R.id.priceTxt);
        descriptionTxt = findViewById(R.id.descriptionTxt);
        backProductBtn = findViewById(R.id.backProductBtn);
        detailsPicProduct = findViewById(R.id.detailsPicProduct);
        stockDetails = findViewById(R.id.stockDetails);

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();

        if (intent != null) {
            String name = intent.getStringExtra("productName");
            double price = intent.getDoubleExtra("productPrice", 0);
            int quantity = intent.getIntExtra("productQuantity", 0);
            String description = intent.getStringExtra("descriptionTxt");

            if (name != null) productName.setText(name);
            priceTxt.setText(String.format("₱%.2f", price));
            if (description != null) descriptionTxt.setText(description);
            stockDetails.setText("Stock: " + quantity);

            byte[] imageBytes = dbHelper.getProductImage(name);
            if (imageBytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                detailsPicProduct.setImageBitmap(bitmap);
            }
        }

        backProductBtn.setOnClickListener(v -> finish());
    }
}
