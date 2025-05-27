package com.business.techassist.admin_utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.business.techassist.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    Context context;
    List<SQL_AdminModel> adminModelList;

    public AdminAdapter(List<SQL_AdminModel> adminModelList, Context context) {
        this.adminModelList = adminModelList;
        this.context = context;
        
        // Log how many admins we're adapting
        Log.d("TechAssist", "AdminAdapter created with " + adminModelList.size() + " admins");
        // Log each admin for debugging
        for (int i = 0; i < adminModelList.size(); i++) {
            SQL_AdminModel admin = adminModelList.get(i);
            Log.d("TechAssist", "Admin #" + i + ": " + admin.getName() + 
                ", availability: " + admin.getAvailability());
        }
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
        
        // Format ratings with "★"
        String formattedRating = admin.getRatings() + " ★";
        holder.ratingTxt.setText(formattedRating);
        
        holder.specialTxt.setText(admin.getSpecialized());
        
        // Format years experience with "Years"
        String formattedExp = admin.getYearsExp() + " Years";
        holder.yearsTxt.setText(formattedExp);
        
        // Display completed jobs count
        if (holder.completedJobsTxt != null) {
            holder.completedJobsTxt.setText(String.valueOf(admin.getCompletedJobs()));
        }

        if (admin.getImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(admin.getImage(), 0, admin.getImage().length);
            holder.img.setImageBitmap(bitmap);
        }
        
        // Set status indicator color based on status
        if (holder.statusIndicator != null) {
            int statusColor;
            String status = admin.getStatus() != null ? admin.getStatus().toLowerCase() : "";
            
            if (status.contains("online") || status.contains("active")) {
                // Online/Active - Green
                statusColor = context.getResources().getColor(R.color.success, context.getTheme());
            } else if (status.contains("away") || status.contains("busy") || status.contains("do-not-disturb")) {
                // Away/Busy/DND - Yellow/Orange
                statusColor = context.getResources().getColor(R.color.warning, context.getTheme());
            } else if (status.contains("offline") || status.contains("inactive")) {
                // Offline/Inactive - Red
                statusColor = context.getResources().getColor(R.color.error, context.getTheme());
            } else {
                // Default - Grey
                statusColor = context.getResources().getColor(R.color.outline, context.getTheme());
            }
            
            // Set background color of the indicator
            GradientDrawable background = (GradientDrawable) holder.statusIndicator.getBackground();
            if (background != null) {
                background.setColor(statusColor);
            } else {
                // If unable to get background as GradientDrawable, set background color directly
                holder.statusIndicator.setBackgroundColor(statusColor);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Log.d("TechAssist", "Technician clicked: " + admin.getName() + ", ID: " + admin.getAdminID());
            Intent intent = new Intent(context, admin_details.class);
            
            // Pass the technician ID for Firestore query
            intent.putExtra("technicianId", admin.getAdminID());
            Log.d("TechAssist", "Passing technicianId: " + admin.getAdminID());
            
            // Also include the bundled data as fallback
            intent.putExtra("name", admin.getName());
            intent.putExtra("specialization", admin.getSpecialized());
            intent.putExtra("experience", admin.getYearsExp());
            intent.putExtra("rating", admin.getRatings());
            intent.putExtra("deviceChecked", admin.getDeviceChecked());
            intent.putExtra("schedule", admin.getSchedule());
            
            // Pass availability and status if available
            String availability = "Unknown";
            if (admin.getAvailability() != null && !admin.getAvailability().isEmpty()) {
                availability = admin.getAvailability();
            }
            intent.putExtra("availability", availability);
            
            String status = "Unknown";
            if (admin.getStatus() != null && !admin.getStatus().isEmpty()) {
                status = admin.getStatus();
            }
            intent.putExtra("status", status);
            
            // Pass completed jobs
            intent.putExtra("completedJobs", admin.getCompletedJobs());

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
        TextView ratingTxt, yearsTxt, specialTxt, nameTxt, completedJobsTxt;
        View statusIndicator;
        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);

            img = itemView.findViewById(R.id.img);
            ratingTxt = itemView.findViewById(R.id.ratingTxt);
            yearsTxt = itemView.findViewById(R.id.yearsTxt);
            specialTxt = itemView.findViewById(R.id.specialTxt);
            nameTxt = itemView.findViewById(R.id.nameTxt);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            completedJobsTxt = itemView.findViewById(R.id.completedJobsTxt);
        }
    }
}
