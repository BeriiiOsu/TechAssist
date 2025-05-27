package com.business.techassist;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

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

import com.bumptech.glide.Glide;
import com.business.techassist.UserCredentials.LoginScreen;
import com.business.techassist.menucomponents.cart.cart;
import com.business.techassist.menucomponents.messages.menu_message;
import com.business.techassist.menucomponents.messages.messagesMenu;
import com.business.techassist.menucomponents.profileMenu;
import com.business.techassist.menucomponents.trackOrderMenu;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.business.techassist.subscription.FeatureLockManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link menu#newInstance} factory method to
 * create an instance of this fragment.
 */
public class menu extends Fragment {

    RelativeLayout logoutProfileBtn,
            settingsProfileBtn,
            supportProfileBtn,
            profileMenuBtn,
            cartMenuBtn,
            trackOrderMenuBtn,
            messageMenuBtn;

    TextView nameUser, emailUser, PP_Btn, TOS_Btn;
    ImageView userPicture;
    String currentUserId = "";

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

        loadComponents(view);

        cartMenuBtn.setOnClickListener(v ->{
            startActivity(new Intent(getActivity(), cart.class));
        });

        trackOrderMenuBtn.setOnClickListener(v ->{
            startActivity(new Intent(getActivity(), trackOrderMenu.class));
        });

        messageMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Direct access to messages without subscription check
                startActivity(new Intent(getActivity(), menu_message.class));
            }
        });

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }
        loadProfileImage();
        profileMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), profileMenu.class));
            }
        });

        if(firebaseUser != null){

            String googleName = firebaseUser.getDisplayName(); // Google Name
            String email = firebaseUser.getEmail();           // Google Email

            if (googleName != null && !googleName.isEmpty()) {
                String[] nameParts = googleName.trim().split("\\s+");
                nameUser.setText(nameParts[0]); // First name only
            } else {
                nameUser.setText("User"); // if Google name is missing
            }

            emailUser.setText(email != null ? email : "Email not found");


            firestore.collection("Users").document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String dbName = documentSnapshot.getString("Name");
                            String dbEmail = documentSnapshot.getString("Email");

                            if (dbName != null && !dbName.isEmpty()) {
                                String[] nameParts = dbName.trim().split("\\s+");
                                nameUser.setText(nameParts[0]);
                            }

                            if (dbEmail != null && !dbEmail.isEmpty()) {
                                emailUser.setText(dbEmail);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }else{
            nameUser.setText("Guest");
            emailUser.setText("Not logged in");
        }

        PP_Btn.setOnClickListener(view1 -> showPP(getActivity()));

        TOS_Btn.setOnClickListener(view12 -> showTOS(getActivity()));

        logoutProfileBtn.setOnClickListener(view13 -> {
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(task -> {
               if(task.isSuccessful()){
                   GoogleSignIn.getClient(requireActivity(), new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                           .revokeAccess()  // Completely disconnects Google Sign-In
                           .addOnCompleteListener(tk -> {
                               FirebaseAuth.getInstance().signOut();
                               Intent intent = new Intent(getActivity(), LoginScreen.class);
                               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Ensures a fresh start
                               startActivity(intent);
                               requireActivity().finish();
                           });
                }
            });

        });


        // Inflate the layout for this fragment
        return view;
    }

    private void loadProfileImage() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        if (currentUserId == null || currentUserId.isEmpty()) {
            loadDefaultImage();
            return;
        }

        firestore.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("ProfilePic");
                        if (imageUrl != null && !imageUrl.isEmpty() && isAdded()) {
                            Glide.with(requireActivity())
                                    .load(imageUrl)
                                    .into(userPicture);
                        } else {
                            loadDefaultImage();
                        }
                    } else {
                        loadDefaultImage();
                    }
                })
                .addOnFailureListener(e -> {
                    loadDefaultImage();
                    Toast.makeText(getContext(), "Failed to load profile image", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadDefaultImage() {
        if (isAdded()) {
            Glide.with(requireActivity())
                    .load(R.drawable.user_icon)
                    .into(userPicture);
        }
    }

    private  void loadComponents(View view){
        userPicture = view.findViewById(R.id.userPicture);
        logoutProfileBtn = view.findViewById(R.id.logoutProfileBtn);
        nameUser = view.findViewById(R.id.nameUser);
        emailUser = view.findViewById(R.id.emailUser);
        PP_Btn = view.findViewById(R.id.PP_Btn);
        TOS_Btn = view.findViewById(R.id.TOS_Btn);
        profileMenuBtn = view.findViewById(R.id.profileMenuBtn);
        cartMenuBtn = view.findViewById(R.id.cartMenuBtn);
        trackOrderMenuBtn = view.findViewById(R.id.trackOrderMenuBtn);
        messageMenuBtn = view.findViewById(R.id.messageMenuBtn);
        settingsProfileBtn = view.findViewById(R.id.settingsProfileBtn);
        supportProfileBtn = view.findViewById(R.id.supportProfileBtn);
    }

    private void showPP(Context context){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.popup_privacy_policy);

        TextView privacyTextView = dialog.findViewById(R.id.privacy_policy_text);

        privacyTextView.setText(Html.fromHtml(getString(R.string.privacy_policy_text), Html.FROM_HTML_MODE_LEGACY));

        privacyTextView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void showTOS(Context context){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.popup_tos);

        TextView privacyTextView = dialog.findViewById(R.id.terms_of_service_text);

        privacyTextView.setText(Html.fromHtml(getString(R.string.terms_of_service_text), Html.FROM_HTML_MODE_LEGACY));

        privacyTextView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close_tos);
        closeButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }
}