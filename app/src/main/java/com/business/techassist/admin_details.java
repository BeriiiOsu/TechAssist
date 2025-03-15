package com.business.techassist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.imageview.ShapeableImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class admin_details extends AppCompatActivity {

    ShapeableImageView img2;
    TextView nameTxt, specialTxt, experinceTxt, ratingTxt2;
    ImageView backBtnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        img2 = findViewById(R.id.img2);
        nameTxt = findViewById(R.id.nameTxt);
        specialTxt = findViewById(R.id.specialTxt);
        experinceTxt = findViewById(R.id.experinceTxt);
        ratingTxt2 = findViewById(R.id.ratingTxt2);
        backBtnAdmin = findViewById(R.id.backBtnAdmin);

        backBtnAdmin.setOnClickListener(view -> finish());

        Intent intent = getIntent();

        if (intent != null) {
            nameTxt.setText(intent.getStringExtra("name") != null ? intent.getStringExtra("name") : "Unknown");
            specialTxt.setText(intent.getStringExtra("specialization") != null ? intent.getStringExtra("specialization") : "N/A");
            experinceTxt.setText(String.valueOf(intent.getIntExtra("experience", 0)));
            ratingTxt2.setText(intent.getStringExtra("rating") != null ? intent.getStringExtra("rating") : "N/A");

            byte[] imageBytes = intent.getByteArrayExtra("image");
            if (imageBytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                img2.setImageBitmap(bitmap);
            } else {
                Log.e("AdminDetails", "Image is null, setting placeholder");
                img2.setImageResource(R.drawable.user_icon);
            }
        } else {
            Log.e("AdminDetails", "Intent is null");
        }
    }
}
