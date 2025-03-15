package com.business.techassist.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.business.techassist.admin_details;
import com.business.techassist.models.AdminModel;
import com.business.techassist.models.SQL_AdminModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    Context context;
    List<SQL_AdminModel> adminModelList;

    public AdminAdapter(List<SQL_AdminModel> adminModelList, Context context) {
        this.adminModelList = adminModelList;
        this.context = context;
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_tabs, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        SQL_AdminModel admin = adminModelList.get(position);

        holder.nameTxt.setText(admin.getName());
        holder.ratingTxt.setText(admin.getRatings());
        holder.specialTxt.setText(admin.getSpecialized());
        holder.yearsTxt.setText(String.valueOf(admin.getYearsExp()));

        if (admin.getImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(admin.getImage(), 0, admin.getImage().length);
            holder.img.setImageBitmap(bitmap);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context ,admin_details.class);

            intent.putExtra("name", admin.getName());
            intent.putExtra("specialization", admin.getSpecialized());
            intent.putExtra("experience", admin.getYearsExp());
            intent.putExtra("rating", admin.getRatings());

            if (admin.getImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(admin.getImage(), 0, admin.getImage().length);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream); // Compress at 50% quality
            byte[] compressedImage = stream.toByteArray();
            intent.putExtra("image", compressedImage);
        }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return adminModelList.size();
    }

    public static class AdminViewHolder extends RecyclerView.ViewHolder{
        ShapeableImageView img;
        TextView ratingTxt, yearsTxt, specialTxt, nameTxt;
        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);

            img = itemView.findViewById(R.id.img);
            ratingTxt = itemView.findViewById(R.id.ratingTxt);
            yearsTxt = itemView.findViewById(R.id.yearsTxt);
            specialTxt = itemView.findViewById(R.id.specialTxt);
            nameTxt = itemView.findViewById(R.id.nameTxt);
        }
    }
}
