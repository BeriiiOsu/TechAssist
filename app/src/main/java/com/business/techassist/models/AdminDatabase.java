package com.business.techassist.models;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AdminDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "adminDB.db";
    public static final int DATABASE_VERSION = 10;
    public static final String TABLE_ADMIN = "admin";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_RATINGS = "ratings";
    public static final String COLUMN_SPECIALIZED = "specialized";
    public static final String COLUMN_YEARS_EXP = "yearsExp";
    public static final String COLUMN_IMAGE = "image";

    public AdminDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_ADMIN + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_RATINGS + " TEXT, " +
                COLUMN_SPECIALIZED + " TEXT, " +
                COLUMN_YEARS_EXP + " INTEGER, " +
                COLUMN_IMAGE + " BLOB)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN);
        onCreate(db);
    }

    public List<SQL_AdminModel> getAllAdmins() {
        List<SQL_AdminModel> adminList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ADMIN, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String ratings = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RATINGS));
                String specialized = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIALIZED));
                int yearsExp = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEARS_EXP));
                byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));

                adminList.add(new SQL_AdminModel(name, ratings, specialized, yearsExp, image));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return adminList;
    }
}
