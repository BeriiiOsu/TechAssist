package com.business.techassist.admin_utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "adminDB.db";
    public static final int DATABASE_VERSION = 24; // Incremented version
    public static final String TABLE_ADMIN = "admin";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_RATINGS = "ratings";
    public static final String COLUMN_SPECIALIZED = "specialized";
    public static final String COLUMN_YEARS_EXP = "yearsExp";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_AVAILABILITY = "availability";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_COMPLETED_JOBS = "completedJobs";

    private static final String[] BASIC_COLUMNS = {
            "adminID",
            COLUMN_NAME,
            COLUMN_RATINGS,
            COLUMN_SPECIALIZED,
            COLUMN_YEARS_EXP,
            "schedule",
            "deviceChecked",
            COLUMN_AVAILABILITY,
            COLUMN_STATUS,
            COLUMN_COMPLETED_JOBS
    };

    public AdminDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_ADMIN + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "adminID TEXT UNIQUE," +
                COLUMN_NAME + " TEXT, " +
                COLUMN_RATINGS + " TEXT, " +
                COLUMN_SPECIALIZED + " TEXT, " +
                COLUMN_YEARS_EXP + " INTEGER, " +
                COLUMN_IMAGE + " BLOB, " +
                "schedule TEXT," +
                "deviceChecked INTEGER," +
                COLUMN_AVAILABILITY + " TEXT," +
                COLUMN_STATUS + " TEXT," +
                COLUMN_COMPLETED_JOBS + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 23) {
            // If upgrading from a version before v23, add the new columns
            try {
                // Add the new columns if they don't exist
                db.execSQL("ALTER TABLE " + TABLE_ADMIN + " ADD COLUMN " + COLUMN_AVAILABILITY + " TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_ADMIN + " ADD COLUMN " + COLUMN_STATUS + " TEXT DEFAULT ''");
            } catch (Exception e) {
                // Column might already exist, recreate table
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN);
                onCreate(db);
            }
        }
        
        if (oldVersion < 24) {
            // Add completedJobs column if upgrading to version 24
            try {
                db.execSQL("ALTER TABLE " + TABLE_ADMIN + " ADD COLUMN " + COLUMN_COMPLETED_JOBS + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist, or other error occurred
                // Log error but continue - don't recreate the table as it would lose data
            }
        }
    }

    public List<SQL_AdminModel> getAllAdmins() {
        List<SQL_AdminModel> adminList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ADMIN, BASIC_COLUMNS,
                null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String adminID = cursor.getString(cursor.getColumnIndexOrThrow("adminID"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String ratings = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RATINGS));
                String specialized = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIALIZED));
                int yearsExp = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEARS_EXP));
                String schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
                int deviceChecked = cursor.getInt(cursor.getColumnIndexOrThrow("deviceChecked"));
                
                // Get availability and status (with fallback for older database versions)
                String availability = "";
                String status = "";
                try {
                    availability = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AVAILABILITY));
                    status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                } catch (Exception e) {
                    // These columns might not exist in older database versions
                }
                
                // Get completedJobs (with fallback for older database versions)
                int completedJobs = 0;
                try {
                    completedJobs = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_JOBS));
                } catch (Exception e) {
                    // This column might not exist in older database versions
                }

                SQL_AdminModel admin = new SQL_AdminModel(
                        adminID,
                        name,
                        ratings,
                        specialized,
                        yearsExp,
                        null, // Image loaded separately
                        deviceChecked,
                        schedule,
                        availability,
                        status,
                        completedJobs
                );
                
                // Load the admin's image
                admin.setImage(getAdminImage(adminID));
                
                adminList.add(admin);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return adminList;
    }

    public byte[] getAdminImage(String adminId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_IMAGE};
        Cursor cursor = db.query(TABLE_ADMIN, columns,
                "adminID=?", new String[]{adminId},
                null, null, null);

        byte[] image = null;
        if (cursor.moveToFirst()) {
            image = cursor.getBlob(0);
        }
        cursor.close();
        db.close();
        return image;
    }

    public boolean insertAdmin(String adminID, String name, String ratings,
                               String specialized, int yearsExp, byte[] image) throws IOException {
        return insertAdmin(adminID, name, ratings, specialized, yearsExp, image, "", "");
    }
    
    public boolean insertAdmin(String adminID, String name, String ratings,
                               String specialized, int yearsExp, byte[] image,
                               String availability, String status) throws IOException {
        return insertAdmin(adminID, name, ratings, specialized, yearsExp, image, availability, status, 0);
    }
    
    public boolean insertAdmin(String adminID, String name, String ratings,
                              String specialized, int yearsExp, byte[] image,
                              String availability, String status, int completedJobs) throws IOException {

        Bitmap bitmap = handleImageRotation(BitmapFactory.decodeByteArray(image, 0, image.length));
        Bitmap resized = resizeBitmap(bitmap, 400); // Max width 400px

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.WEBP, 70, stream);
        byte[] compressedImage = stream.toByteArray();

        if (compressedImage.length > 512000) { // 500KB limit
            throw new IOException("Image size exceeds 500KB limit");
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("adminID", adminID);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_RATINGS, ratings);
        values.put(COLUMN_SPECIALIZED, specialized);
        values.put(COLUMN_YEARS_EXP, yearsExp);
        values.put(COLUMN_IMAGE, compressedImage);
        values.put("schedule", "");
        values.put("deviceChecked", 0);
        values.put(COLUMN_AVAILABILITY, availability);
        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_COMPLETED_JOBS, completedJobs);

        long result = db.insertWithOnConflict(TABLE_ADMIN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result != -1;
    }

    private Bitmap resizeBitmap(Bitmap source, int maxWidth) {
        if (source.getWidth() <= maxWidth) return source;

        float ratio = (float) source.getHeight() / (float) source.getWidth();
        int newHeight = (int) (maxWidth * ratio);
        return Bitmap.createScaledBitmap(source, maxWidth, newHeight, true);
    }

    private Bitmap handleImageRotation(Bitmap bitmap) throws IOException {
        ExifInterface exif = new ExifInterface(new ByteArrayInputStream(
                bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 70))
        );

        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
        );

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private byte[] bitmapToByteArray(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(format, quality, stream);
        return stream.toByteArray();
    }

    public boolean isAdminExists(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT adminID FROM " + TABLE_ADMIN + " WHERE adminID = ?",
                new String[]{uid}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public List<SQL_AdminModel> getAvailableAdmins() {
        List<SQL_AdminModel> adminList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ADMIN, BASIC_COLUMNS,
                COLUMN_AVAILABILITY + "=?", new String[]{"Available"}, 
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String adminID = cursor.getString(cursor.getColumnIndexOrThrow("adminID"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String ratings = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RATINGS));
                String specialized = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIALIZED));
                int yearsExp = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEARS_EXP));
                String schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
                int deviceChecked = cursor.getInt(cursor.getColumnIndexOrThrow("deviceChecked"));
                
                // Get availability and status (with fallback for older database versions)
                String availability = "Available"; // We know it's Available because of the query
                String status = "";
                try {
                    status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                } catch (Exception e) {
                    // This column might not exist in older database versions
                }
                
                // Get completedJobs (with fallback for older database versions)
                int completedJobs = 0;
                try {
                    completedJobs = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_JOBS));
                } catch (Exception e) {
                    // This column might not exist in older database versions
                }

                SQL_AdminModel admin = new SQL_AdminModel(
                        adminID,
                        name,
                        ratings,
                        specialized,
                        yearsExp,
                        null, // Image loaded separately
                        deviceChecked,
                        schedule,
                        availability,
                        status,
                        completedJobs
                );
                
                // Load the admin's image
                admin.setImage(getAdminImage(adminID));
                
                adminList.add(admin);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return adminList;
    }

    /**
     * Updates the status of an admin in the SQLite database
     * @param adminId the ID of the admin to update
     * @param status the new status to set
     * @return true if update was successful, false otherwise
     */
    public boolean updateAdminStatus(String adminId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status);
        
        // Update where adminID matches
        int rowsAffected = db.update(
            TABLE_ADMIN, 
            values, 
            "adminID=?", 
            new String[]{adminId}
        );
        
        db.close();
        return rowsAffected > 0;
    }
    
    /**
     * Updates both status and availability of an admin in the SQLite database
     * @param adminId the ID of the admin to update
     * @param status the new status to set
     * @param availability the new availability to set
     * @return true if update was successful, false otherwise
     */
    public boolean updateAdminStatusAndAvailability(String adminId, String status, String availability) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_AVAILABILITY, availability);
        
        // Update where adminID matches
        int rowsAffected = db.update(
            TABLE_ADMIN, 
            values, 
            "adminID=?", 
            new String[]{adminId}
        );
        
        db.close();
        return rowsAffected > 0;
    }
}