package com.business.techassist;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.business.techassist.shopitems.DatabaseHelper;
import com.business.techassist.utilities.FirebaseUtil;
import com.business.techassist.UserCredentials.LoginScreen;
import com.business.techassist.menucomponents.messages.messageActivity;
import com.business.techassist.admin_utils.AdminModel;
import com.business.techassist.utilities.AndroidUtil;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.business.techassist.subscription.SubscriptionUtils;

public class SplashActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        if (FirebaseApp.initializeApp(this) == null) {
            Log.e("FirebaseInit", "Firebase initialization failed!");
        } else {
            Log.d("FirebaseInit", "Firebase initialized successfully!");
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!isInternetAvailable()) {
            showNoInternetDialog();
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("GlobalException", "Uncaught Exception: ", throwable);
        });

        if(FirebaseUtil.isLoggedIn() && getIntent().getExtras() != null){
            String userID = getIntent().getExtras().getString("userID");
            FirebaseUtil.allUserCollectionReference().document(userID).get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            AdminModel model = task.getResult().toObject(AdminModel.class);

                            Intent mainIntent = new Intent(this, MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(mainIntent);

                                Intent intent = new Intent(this, messageActivity.class);
                                AndroidUtil.passAdminDataMessages(intent,model);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                        }
                    });

        }else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(FirebaseUtil.isLoggedIn()){
                        DatabaseHelper dbHelper = new DatabaseHelper(SplashActivity.this);
                        dbHelper.insertHardwareAndSoftwareData(SplashActivity.this);
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    }else{
                        DatabaseHelper dbHelper = new DatabaseHelper(SplashActivity.this);
                        dbHelper.insertHardwareAndSoftwareData(SplashActivity.this);
                        startActivity(new Intent(SplashActivity.this, LoginScreen.class));
                    }
                    finish();
                }
            }, 2000);
        }
    }

    void addHardwareToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Map<String, Object>> hardwareList = new ArrayList<>();

        hardwareList.add(createHardwareComponent("Intel Core i9-14900K", 25, 35990, "24 Cores (8P+16E), 32 Threads, Base Clock 3.2GHz, Boost Clock 6.0GHz, 36MB Intel Smart Cache, DDR5 & DDR4 Support, LGA 1700 Socket."));
        hardwareList.add(createHardwareComponent("AMD Ryzen 9 7950X3D", 30, 35900, "16 Cores, 32 Threads, Base Clock 4.2GHz, Boost Clock 5.7GHz, 144MB Cache, DDR5 Support, PCIe 5.0, AM5 Socket."));
        hardwareList.add(createHardwareComponent("Intel Core i7-13700K", 40, 27990, "16 Cores (8P+8E), 24 Threads, Base Clock 3.4GHz, Boost Clock 5.4GHz, 30MB Intel Smart Cache, DDR5 & DDR4 Support, LGA 1700 Socket."));
        hardwareList.add(createHardwareComponent("AMD Ryzen 7 7700X", 35, 27000, "8 Cores, 16 Threads, Base Clock 4.5GHz, Boost Clock 5.4GHz, 40MB Cache, DDR5 Support, PCIe 5.0, AM5 Socket."));
        hardwareList.add(createHardwareComponent("Intel Core i5-13400F", 50, 14990, "10 Cores (6P+4E), 16 Threads, Base Clock 2.5GHz, Boost Clock 4.6GHz, 20MB Intel Smart Cache, DDR5 & DDR4 Support, LGA 1700 Socket."));
        hardwareList.add(createHardwareComponent("AMD Ryzen 5 5600", 45, 12990, "6 Cores, 12 Threads, Base Clock 3.5GHz, Boost Clock 4.4GHz, 35MB Cache, DDR4 Support, PCIe 4.0, AM4 Socket."));

        hardwareList.add(createHardwareComponent("64GB DDR5 (2x32GB) 6000MHz+", 20, 18500, "High-speed DDR5 RAM, 6000MHz CL30, Dual-channel, Optimized for Intel XMP 3.0 & AMD EXPO."));
        hardwareList.add(createHardwareComponent("32GB DDR5 (2x16GB) 5200MHz", 35, 8990, "DDR5 5200MHz, Dual-channel, Optimized for gaming and productivity."));
        hardwareList.add(createHardwareComponent("16GB DDR4 (2x8GB) 3200MHz", 60, 3990, "DDR4 3200MHz CL16, Reliable memory for smooth computing."));

        hardwareList.add(createHardwareComponent("2TB NVMe SSD (PCIe 5.0)", 15, 13990, "Next-gen PCIe 5.0 NVMe SSD, Read speeds up to 12,000MB/s, Write speeds up to 10,000MB/s."));
        hardwareList.add(createHardwareComponent("1TB NVMe SSD (PCIe 4.0)", 30, 6790, "High-performance PCIe 4.0 NVMe SSD, Read speeds up to 7000MB/s, Write speeds up to 6000MB/s."));
        hardwareList.add(createHardwareComponent("512GB NVMe SSD (PCIe 3.0)", 50, 3790, "Affordable PCIe 3.0 NVMe SSD, Read speeds up to 3500MB/s, Write speeds up to 3000MB/s."));

        hardwareList.add(createHardwareComponent("RTX 4090", 10, 99990, "24GB GDDR6X, 16384 CUDA Cores, Ray Tracing & DLSS 3, PCIe 4.0, 450W TDP."));
        hardwareList.add(createHardwareComponent("RX 7900XTX", 12, 95000, "24GB GDDR6, 6144 Stream Processors, AMD RDNA 3, PCIe 4.0, 355W TDP."));
        hardwareList.add(createHardwareComponent("RTX 4070", 25, 44990, "12GB GDDR6X, 5888 CUDA Cores, Ray Tracing & DLSS 3, PCIe 4.0, 200W TDP."));
        hardwareList.add(createHardwareComponent("RX 7700XT", 28, 41990, "12GB GDDR6, 3456 Stream Processors, AMD RDNA 3, PCIe 4.0, 245W TDP."));
        hardwareList.add(createHardwareComponent("RTX 3060", 40, 24990, "12GB GDDR6, 3584 CUDA Cores, Ray Tracing & DLSS, PCIe 4.0, 170W TDP."));
        hardwareList.add(createHardwareComponent("RX 6600", 50, 20990, "8GB GDDR6, 1792 Stream Processors, AMD RDNA 2, PCIe 4.0, 132W TDP."));

        db.collection("products").document("hardware").set(Map.of("items", hardwareList))
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Hardware added successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding hardware", e));
    }

    private Map<String, Object> createHardwareComponent(String name, int quantity, int price, String description) {
        Map<String, Object> component = new HashMap<>();
        component.put("name", name);
        component.put("quantity", quantity);
        component.put("price", price);
        component.put("description", description);
        return component;
    }

    void addSoftwareToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Map<String, Object>> softwareList = new ArrayList<>();

        // Operating Systems
        softwareList.add(createSoftwareComponent("Windows 11 Pro", 50, 11990, "64-bit, Secure Boot, TPM 2.0 required, DirectX 12, Virtual Desktops, Advanced Security Features, AI-powered Snap Layouts."));
        softwareList.add(createSoftwareComponent("Ubuntu 22.04 LTS", 70, 0, "64-bit, Linux Kernel 5.15+, GNOME 42, Wayland support, Snap packages, Built-in Firewall, Developer-friendly CLI tools."));
        softwareList.add(createSoftwareComponent("macOS Ventura", 40, 0, "64-bit, Apple M-series and Intel Support, Stage Manager, Continuity Camera, Metal 3, System-wide Live Text, Focus Mode."));
        softwareList.add(createSoftwareComponent("Windows 10 Home", 30, 8990, "64-bit, DirectX 12, Cortana Integration, Microsoft Store, Virtual Desktops, Secure Boot, Windows Hello."));
        softwareList.add(createSoftwareComponent("Linux Mint", 60, 0, "64-bit, Cinnamon Desktop, Flatpak Support, X-Apps, Timeshift Backup, Secure Boot, Open-source Software Repository."));

        // Video Editing Software
        softwareList.add(createSoftwareComponent("Adobe Premiere Pro", 35, 24990, "Timeline-based editing, AI-powered auto-reframe, VR Editing, Lumetri Color Correction, Multi-camera Editing, Motion Graphics Support."));
        softwareList.add(createSoftwareComponent("DaVinci Resolve Studio", 40, 14990, "Professional color grading, Fairlight Audio, Fusion VFX, 8K Editing, AI-powered object removal, HDR and RAW Editing Support."));
        softwareList.add(createSoftwareComponent("Final Cut Pro", 25, 14990, "Mac-exclusive, Magnetic Timeline, 360-degree VR Editing, HDR and ProRes Support, Multicam Editing, AI-based Color Correction."));
        softwareList.add(createSoftwareComponent("Sony Vegas Pro", 20, 12990, "GPU-accelerated Video Editing, Nested Timelines, HDR Color Grading, AI-based Scene Detection, Motion Tracking, Audio Synchronization."));
        softwareList.add(createSoftwareComponent("HitFilm Pro", 30, 9990, "Advanced VFX Compositing, Particle Simulation, Motion Tracking, 3D Model Support, Keyframe Animations, Built-in Color Grading."));

        // Development Tools
        softwareList.add(createSoftwareComponent("JetBrains IntelliJ IDEA", 45, 8990, "Java, Kotlin, and Groovy Support, Smart Code Completion, Refactoring Tools, Built-in Debugger, Integrated Terminal, Version Control Support."));
        softwareList.add(createSoftwareComponent("Microsoft Visual Studio", 60, 0, "C#, .NET, Python, C++ support, IntelliSense, Cloud Development Tools, AI-powered Code Suggestions, Debugging & Profiling, Extensions Marketplace."));
        softwareList.add(createSoftwareComponent("Eclipse IDE", 80, 0, "Java, C/C++, Python support, Plugin-Based Architecture, Built-in Debugger, Maven Integration, Code Navigation Features, Git Support."));
        softwareList.add(createSoftwareComponent("PyCharm Professional", 35, 7990, "Advanced Python Debugging, Integrated Jupyter Notebooks, AI-based Code Completion, Django & Flask Support, Database Tools, Virtual Environments."));
        softwareList.add(createSoftwareComponent("Android Studio", 50, 0, "Official Android Development IDE, Jetpack Compose, Kotlin & Java Support, Layout Inspector, Firebase Integration, AI-powered Code Suggestions."));
        softwareList.add(createSoftwareComponent("Xcode", 40, 0, "Apple's IDE for macOS & iOS Development, Swift & Objective-C Support, Interface Builder, Metal API Debugging, Continuous Integration Tools."));

        // Security Software
        softwareList.add(createSoftwareComponent("Bitdefender Total Security", 55, 3490, "Real-time Protection, Multi-layer Ransomware Defense, VPN Included, Web Filtering, Advanced Threat Defense, Parental Controls."));
        softwareList.add(createSoftwareComponent("Norton 360 Deluxe", 45, 3990, "AI-driven Threat Protection, Secure VPN, Dark Web Monitoring, Password Manager, Cloud Backup, Cross-Device Support."));
        softwareList.add(createSoftwareComponent("Malwarebytes Premium", 60, 1990, "AI-powered Malware Detection, Web Protection, Ransomware Shield, Anti-Exploit Protection, Adware & Spyware Removal."));
        softwareList.add(createSoftwareComponent("McAfee Total Protection", 50, 2990, "Real-time Antivirus, Encrypted Storage, Identity Theft Protection, Firewall, Performance Optimization, Secure Browsing."));
        softwareList.add(createSoftwareComponent("Kaspersky Internet Security", 40, 3290, "Multi-layered Protection, Safe Money Online Transactions, Webcam & Microphone Security, Anti-Phishing, Cloud-assisted Scanning."));

        // Productivity Software
        softwareList.add(createSoftwareComponent("Microsoft Office 2021", 40, 10990, "Includes Word, Excel, PowerPoint, Outlook, OneNote, Teams, AI-based Writing Assistant, Cloud Collaboration Features."));
        softwareList.add(createSoftwareComponent("Adobe Photoshop", 50, 14990, "AI-powered Image Editing, Layer-based Compositing, RAW Editing, Smart Object Support, Content-Aware Fill, Advanced Color Correction."));
        softwareList.add(createSoftwareComponent("CorelDRAW Graphics Suite", 30, 15990, "Vector Illustration, AI-powered Tracing, Advanced Typography, Non-destructive Effects, 4K and Multi-monitor Support, Pantone Color System."));
        softwareList.add(createSoftwareComponent("Autodesk AutoCAD", 25, 79990, "2D and 3D Design Tools, Cloud-based Collaboration, Smart Dimensioning, DWG File Support, AI-powered Drawing Insights, Industry Toolsets."));
        softwareList.add(createSoftwareComponent("Affinity Designer", 35, 4990, "Precision Vector Editing, Unlimited Artboards, Pen & Node Tools, Real-time Effects, PSD Compatibility, GPU-accelerated Performance."));

        // Game Development Software
        softwareList.add(createSoftwareComponent("Unity Pro", 25, 16990, "C# Scripting, Physics-based Rendering, AI-driven NPCs, Cross-platform Development, VR & AR Support, Real-time Ray Tracing."));
        softwareList.add(createSoftwareComponent("Unreal Engine 5", 30, 0, "Nanite Virtualized Geometry, Lumen Global Illumination, Blueprint Scripting, AI-driven NPC Behaviors, Real-time Rendering, VR & AR Support."));
        softwareList.add(createSoftwareComponent("Godot Engine", 40, 0, "2D and 3D Game Development, Open-source, GDScript Support, Visual Scripting, Lightweight Engine, Cross-platform Deployment."));
        softwareList.add(createSoftwareComponent("RPG Maker MV", 20, 8490, "Drag-and-drop Game Creation, JavaScript Scripting, Tile-based Mapping, Built-in Asset Library, Multiplatform Exporting, Real-time Previews."));

        // Upload Data to Firestore
        db.collection("products").document("software").set(Map.of("items", softwareList))
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Software added successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding software", e));
    }

    private Map<String, Object> createSoftwareComponent(String name, int quantity, int price, String description) {
        Map<String, Object> component = new HashMap<>();
        component.put("name", name);
        component.put("quantity", quantity);
        component.put("price", price);
        component.put("description", description);
        return component;
    }
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> recreate())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }
}