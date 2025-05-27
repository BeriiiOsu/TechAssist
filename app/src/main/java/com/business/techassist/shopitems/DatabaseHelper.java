package com.business.techassist.shopitems;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.business.techassist.R;

import java.io.ByteArrayOutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "products.db";
    private static final int DATABASE_VERSION = 33;

    private static final String TABLE_PRODUCTS = "products";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMAGE = "image";

    private static final String CREATE_TABLE_PRODUCTS =
            "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_QUANTITY + " INTEGER, " +
                    COLUMN_PRICE + " REAL, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_IMAGE + " BLOB)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PRODUCTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    public void insertProduct(SQLiteDatabase db, String name, int quantity, double price, String description, byte[] imageBytes) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_QUANTITY, quantity);
        values.put(COLUMN_PRICE, price);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_IMAGE, imageBytes);

        db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getProductByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_NAME + "=?", new String[]{name});
    }

    public byte[] getProductImage(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_IMAGE + " FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_NAME + "=?", new String[]{productName});

        byte[] imageData = null;
        if (cursor.moveToFirst()) {
            imageData = cursor.getBlob(0);
        }
        cursor.close();
        return imageData;
    }

    public String getProductImagePath(String productName) {
        // Since we don't have actual image paths stored, we'll use the product name as an identifier
        // In a real app, you might store the actual file path in the database
        return productName;
    }

    private byte[] getBytesFromDrawable(Context context, int drawableId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
        if (bitmap == null) {
            Log.e("DatabaseHelper", "Failed to decode resource: " + drawableId);
            return new byte[0];
        }

        Bitmap resizedBitmap = resizeImage(bitmap, 325, 350);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
        return stream.toByteArray();
    }

    private Bitmap resizeImage(Bitmap original, int maxWidth, int maxHeight) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }


    public void insertHardwareAndSoftwareData(Context context) {
        if (!isDatabaseEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        // Hardware Components
        insertProduct(db, "Intel Core i9-14900K", 25, 35990, "High-performance processor for gaming and productivity.", getBytesFromDrawable(context, R.drawable.inteli9));
        insertProduct(db, "AMD Ryzen 9 7950X3D", 30, 35900, "Top-tier processor optimized for multitasking and high-end applications.", getBytesFromDrawable(context, R.drawable.amd9));
        insertProduct(db, "Intel Core i7-13700K", 40, 27990, "Powerful mid-to-high-end processor with great performance.", getBytesFromDrawable(context, R.drawable.i7));
        insertProduct(db, "AMD Ryzen 7 7700X", 35, 27000, "Great for gaming and productivity with excellent efficiency.", getBytesFromDrawable(context, R.drawable.ry7));
        insertProduct(db, "Intel Core i5-13400F", 50, 14990, "Budget-friendly gaming and office processor.", getBytesFromDrawable(context, R.drawable.i5));
        insertProduct(db, "AMD Ryzen 5 5600", 45, 12990, "Affordable option with solid performance for everyday use.", getBytesFromDrawable(context, R.drawable.ry6));

        insertProduct(db, "64GB DDR5 (2x32GB) 6000MHz+", 20, 18500, "Ultra-fast memory ideal for heavy workloads and gaming.", getBytesFromDrawable(context, R.drawable.gb64));
        insertProduct(db, "32GB DDR5 (2x16GB) 5200MHz", 35, 8990, "High-speed RAM suitable for gaming and productivity.", getBytesFromDrawable(context, R.drawable.gb32));
        insertProduct(db, "16GB DDR4 (2x8GB) 3200MHz", 60, 3990, "Reliable memory for everyday computing tasks.", getBytesFromDrawable(context, R.drawable.gb8));

        insertProduct(db, "2TB NVMe SSD (PCIe 5.0)", 15, 13990, "Ultra-fast SSD with high-speed read and write capabilities.", getBytesFromDrawable(context, R.drawable.tb2));
        insertProduct(db, "1TB NVMe SSD (PCIe 4.0)", 30, 6790, "High-performance storage for gaming and content creation.", getBytesFromDrawable(context, R.drawable.tb1));
        insertProduct(db, "512GB NVMe SSD (PCIe 3.0)", 50, 3790, "Affordable storage option with decent speeds.", getBytesFromDrawable(context, R.drawable.gb512));

        insertProduct(db, "RTX 4090", 10, 99990, "Flagship GPU with extreme performance for 4K gaming and AI tasks.", getBytesFromDrawable(context, R.drawable.rtx4090));
        insertProduct(db, "RX 7900XTX", 12, 95000, "High-end AMD GPU for gaming and professional workloads.", getBytesFromDrawable(context, R.drawable.xtx7900));
        insertProduct(db, "RTX 4070", 25, 44990, "Balanced GPU for high refresh rate gaming.", getBytesFromDrawable(context, R.drawable.rtx4070));
        insertProduct(db, "RX 7700XT", 28, 41990, "Great mid-range GPU with excellent efficiency.", getBytesFromDrawable(context, R.drawable.xt7700));
        insertProduct(db, "RTX 3060", 40, 24990, "Affordable GPU for entry-level gaming.", getBytesFromDrawable(context, R.drawable.rtx3060));
        insertProduct(db, "RX 6600", 50, 20990, "Budget-friendly GPU with solid 1080p performance.", getBytesFromDrawable(context, R.drawable.rx6600));

        insertProduct(db, "Z790 Motherboard", 18, 19990, "High-end motherboard with PCIe 5.0 support.", getBytesFromDrawable(context, R.drawable.z790motherboard));
        insertProduct(db, "B760 Motherboard", 30, 11990, "Mid-range motherboard with PCIe 4.0 support.", getBytesFromDrawable(context, R.drawable.b760motherboard));
        insertProduct(db, "B550 Motherboard", 45, 7990, "Budget-friendly motherboard with good expansion options.", getBytesFromDrawable(context, R.drawable.b550motherboard));

        insertProduct(db, "1000W+ 80 Plus Platinum PSU", 20, 14500, "High-efficiency power supply for high-end systems.", getBytesFromDrawable(context, R.drawable.w1000));
        insertProduct(db, "750W 80 Plus Gold PSU", 35, 7890, "Reliable power supply for gaming PCs.", getBytesFromDrawable(context, R.drawable.w750));
        insertProduct(db, "600W 80 Plus Bronze PSU", 50, 4890, "Budget-friendly PSU with decent efficiency.", getBytesFromDrawable(context, R.drawable.w600));

        insertProduct(db, "360mm AIO Liquid Cooler", 25, 9990, "High-performance cooling solution for overclocking.", getBytesFromDrawable(context, R.drawable.mm360));
        insertProduct(db, "240mm AIO Cooler", 40, 5790, "Efficient cooling for gaming setups.", getBytesFromDrawable(context, R.drawable.mm240));
        insertProduct(db, "Budget Air Cooler", 60, 1890, "Affordable air cooling solution.", getBytesFromDrawable(context, R.drawable.budget));

        insertProduct(db, "32'' 4K 144Hz IPS Monitor", 12, 49990, "Premium gaming and content creation display.", getBytesFromDrawable(context, R.drawable.m144));
        insertProduct(db, "27'' 1440p 144Hz IPS Monitor", 20, 24990, "Great for gaming and professional work.", getBytesFromDrawable(context, R.drawable.m1440p));
        insertProduct(db, "24'' 1080p 75Hz IPS Monitor", 35, 9990, "Budget monitor for everyday use.", getBytesFromDrawable(context, R.drawable.m75));

        insertProduct(db, "Wireless Mechanical Keyboard + High-End Mouse", 25, 14990, "Premium peripheral set for professionals.", getBytesFromDrawable(context, R.drawable.wirelessmechanicalkeyboardhighendmouse));
        insertProduct(db, "Mechanical Keyboard + Precision Mouse", 40, 6990, "Perfect for gamers and power users.", getBytesFromDrawable(context, R.drawable.mechanicalkeyboardprecisionmouse));
        insertProduct(db, "Membrane Keyboard + Budget Mouse", 50, 2990, "Affordable peripherals for casual use.", getBytesFromDrawable(context, R.drawable.membranekeyboardbudgetmouse));

        // Software Components
        insertProduct(db, "Windows 11 Pro", 50, 11990, "Latest Windows OS with advanced security and features.", getBytesFromDrawable(context, R.drawable.windows11pro));
        insertProduct(db, "Ubuntu 22.04 LTS", 70, 0, "Free and open-source Linux distribution.", getBytesFromDrawable(context, R.drawable.ubuntu));
        insertProduct(db, "macOS Ventura", 40, 0, "Apple's latest macOS with seamless ecosystem integration.", getBytesFromDrawable(context, R.drawable.macosventura));
        insertProduct(db, "Windows 10 Home", 30, 8990, "Stable and widely used Windows version.", getBytesFromDrawable(context, R.drawable.windows10home));
        insertProduct(db, "Linux Mint", 60, 0, "User-friendly Linux distro based on Ubuntu.", getBytesFromDrawable(context, R.drawable.linuxmint));

        insertProduct(db, "Adobe Premiere Pro", 35, 24990, "Industry-leading video editing software.", getBytesFromDrawable(context, R.drawable.adobepremierepro));
        insertProduct(db, "DaVinci Resolve Studio", 40, 14990, "Professional color grading and editing tool.", getBytesFromDrawable(context, R.drawable.davinci));

        insertProduct(db, "Microsoft Office 2021", 40, 10990, "Essential suite for office productivity.", getBytesFromDrawable(context, R.drawable.microsoftoffice2021));
        insertProduct(db, "Adobe Photoshop", 50, 14990, "Industry-leading photo editing software.", getBytesFromDrawable(context, R.drawable.adobephotoshop));
        insertProduct(db, "CorelDRAW Graphics Suite", 30, 15990, "Graphic design software for professionals.", getBytesFromDrawable(context, R.drawable.coreldraw));

        db.close();
    }

    public boolean isDatabaseEmpty() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        boolean isEmpty = true;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                isEmpty = cursor.getInt(0) == 0;
            }
            cursor.close();
        }
        return isEmpty;
    }

}

