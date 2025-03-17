package com.business.techassist;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.business.techassist.adapters.AdminAdapter;
import com.business.techassist.menucomponents.messages.menu_message;
import com.business.techassist.models.AdminDatabase;
import com.business.techassist.models.SQL_AdminModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

public class home extends Fragment {

    RecyclerView itView;
    TextView planFeatures, userPlan, userName;
    RelativeLayout upgradeBtn;
    ImageView notifBtn, messageBtn, profileHomeBtn;
    ProgressBar progressBarCat,progressBarIT;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public home() {
        // Required empty public constructor
    }

    public static home newInstance(String param1, String param2) {
        home fragment = new home();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        itView = view.findViewById(R.id.itView);
        planFeatures = view.findViewById(R.id.planFeatures);
        userPlan = view.findViewById(R.id.userPlan);
        upgradeBtn = view.findViewById(R.id.upgradeBtn);
        notifBtn = view.findViewById(R.id.notifBtn);
        progressBarIT = view.findViewById(R.id.progressBarIT);
        userName = view.findViewById(R.id.userName);
        messageBtn = view.findViewById(R.id.messageBtn);
        profileHomeBtn = view.findViewById(R.id.profileHomeBtn);

        progressBarIT.setVisibility(View.VISIBLE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        messageBtn.setOnClickListener(v ->{
            startActivity(new Intent(getActivity(), menu_message.class));
        });

        profileHomeBtn.setOnClickListener( v ->{
            startActivity(new Intent(getActivity(), profileHome.class));
        });


        fetchUserName();

        setUpITExperts();

        itView.setNestedScrollingEnabled(false);
        return view;
    }

    private void fetchUserName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("Name");
                            String firstName = (fullName != null && !fullName.isEmpty()) ? fullName.split(" ")[0] : "User";
                            userName.setText(getGreeting() + " " + firstName + "!");
                        } else {
                            userName.setText(getGreeting() + " User!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        userName.setText(getGreeting() + " User!");
                        e.printStackTrace();
                    });
        } else {
            userName.setText(getGreeting() + " Guest!");
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good Morning,";
        } else if (hour >= 12 && hour < 18) {
            return "Good Afternoon,";
        } else {
            return "Good Evening,";
        }
    }

    private void setUpITExperts() {
        new Thread(() -> {
            try {
                Context context = getActivity();
                AdminDatabase dbHelper = new AdminDatabase(context);

                // Insert data only if table is empty
                if (dbHelper.getAllAdmins().isEmpty()) {
                    insertAdmin(context,"FfDfy5CmnpNkudflMhiwajZSY9D3" , "Gary Aguilar", "5", "IT Generalist", 5, getBytesFromDrawable(R.drawable.admin_gary));
                    insertAdmin(context,"BtGNySpvJUOYkAtm5w4PfZsKVHw1" ,"Maai Brazas", "5", "Field Service Technician", 3, getBytesFromDrawable(R.drawable.admin_maai_2));
                    insertAdmin(context,"e9KWX5aQWzbUhsSvuHPBnIxCLVR2" ,"Kyl Angelo", "5", "IT Support Specialist", 3, getBytesFromDrawable(R.drawable.admin_kyl));
                    insertAdmin(context,"kUuUM24rOkg3r2m9b9ubbdKzCXg1" ,"John Ian", "5", "Cybersecurity Specialist", 3, getBytesFromDrawable(R.drawable.admin_ian));
                    insertAdmin(context, "2dXoQiLEuWNlSvYLzQKvinbI15s1" ,"Paul Dominic", "5", "Mobile Device Technician", 3, getBytesFromDrawable(R.drawable.admin_pauldominic));
                }

                List<SQL_AdminModel> adminList = dbHelper.getAllAdmins();

                requireActivity().runOnUiThread(() -> {
                    AdminAdapter adapter = new AdminAdapter(adminList, context);
                    itView.setLayoutManager(new LinearLayoutManager(context));
                    itView.setAdapter(adapter);
                    progressBarIT.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                Log.e("TechAssist", "Error setting up IT experts", e);
            }
        }).start();
    }


    private byte[] getBytesFromDrawable(int drawableId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        return stream.toByteArray();
    }

    public void insertAdmin(Context context,String adminID, String name, String ratings, String specialized, int yearsExp, byte[] image) {
        AdminDatabase dbHelper = new AdminDatabase(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("adminID", adminID);
        values.put(AdminDatabase.COLUMN_NAME, name);
        values.put(AdminDatabase.COLUMN_RATINGS, ratings);
        values.put(AdminDatabase.COLUMN_SPECIALIZED, specialized);
        values.put(AdminDatabase.COLUMN_YEARS_EXP, yearsExp);
        values.put(AdminDatabase.COLUMN_IMAGE, image);

        db.insert(AdminDatabase.TABLE_ADMIN, null, values);
        db.close();
    }


}