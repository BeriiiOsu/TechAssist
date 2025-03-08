package com.business.techassist;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.business.techassist.UserCredentials.LoginScreen;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link menu#newInstance} factory method to
 * create an instance of this fragment.
 */
public class menu extends Fragment {

    RelativeLayout logoutProfileBtn;
    TextView nameUser, emailUser, PP_Btn, TOS_Btn;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public menu() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment menu.
     */
    // TODO: Rename and change types and number of parameters
    public static menu newInstance(String param1, String param2) {
        menu fragment = new menu();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        logoutProfileBtn = view.findViewById(R.id.logoutProfileBtn);
        nameUser = view.findViewById(R.id.nameUser);
        emailUser = view.findViewById(R.id.emailUser);
        PP_Btn = view.findViewById(R.id.PP_Btn);
        TOS_Btn = view.findViewById(R.id.TOS_Btn);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        if(firebaseUser != null){
            String userID = firebaseUser.getUid();

            databaseReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        String name = snapshot.child("Name").getValue(String.class);
                        String email = snapshot.child("Email").getValue(String.class);

                        if(name != null){
                            String firstName = name.split(" ")[0];
                            nameUser.setText(firstName);
                        }
                        emailUser.setText(email);
                    }else{
                        nameUser.setText("User not found");
                        emailUser.setText("Email not found");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        PP_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPP(getActivity());
            }
        });

        TOS_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTOS(getActivity());
            }
        });

        logoutProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginScreen.class);
                startActivity(intent);

                if(getActivity() != null){
                    getActivity().finish();
                }
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    private void showPP(Context context){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.activity_popup_privacy_policy);

        TextView privacyTextView = dialog.findViewById(R.id.privacy_policy_text);

        privacyTextView.setText(Html.fromHtml(getString(R.string.privacy_policy_text), Html.FROM_HTML_MODE_LEGACY));

        privacyTextView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void showTOS(Context context){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.activity_popup_tos);

        TextView privacyTextView = dialog.findViewById(R.id.terms_of_service_text);

        privacyTextView.setText(Html.fromHtml(getString(R.string.terms_of_service_text), Html.FROM_HTML_MODE_LEGACY));

        privacyTextView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close_tos);
        closeButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }
}