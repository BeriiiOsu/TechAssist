package com.business.techassist;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.business.techassist.UserCredentials.LoginScreen;
import com.business.techassist.menucomponents.cart.cart;
import com.business.techassist.menucomponents.messages.menu_message;
import com.business.techassist.menucomponents.profileMenu;
import com.business.techassist.menucomponents.trackOrderMenu;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class profileHome extends AppCompatActivity {

    RelativeLayout logoutProfileBtn, profileMenuBtn, cartMenuBtn, trackOrderMenuBtn, messageMenuBtn;
    TextView nameUser, emailUser, PP_Btn, TOS_Btn;
    ImageView userPicture;
    String currentUserId = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_home);

        loadComponents();
        setupListeners();
        loadProfileImage();
        loadUserData();
    }

    private void loadComponents() {
        userPicture = findViewById(R.id.userPicture);
        logoutProfileBtn = findViewById(R.id.logoutProfileBtn);
        nameUser = findViewById(R.id.nameUser);
        emailUser = findViewById(R.id.emailUser);
        PP_Btn = findViewById(R.id.PP_Btn);
        TOS_Btn = findViewById(R.id.TOS_Btn);
        profileMenuBtn = findViewById(R.id.profileMenuBtn);
        cartMenuBtn = findViewById(R.id.cartMenuBtn);
        trackOrderMenuBtn = findViewById(R.id.trackOrderMenuBtn);
        messageMenuBtn = findViewById(R.id.messageMenuBtn);
    }

    private void setupListeners() {
        cartMenuBtn.setOnClickListener(v -> startActivity(new Intent(this, cart.class)));
        trackOrderMenuBtn.setOnClickListener(v -> startActivity(new Intent(this, trackOrderMenu.class)));
        messageMenuBtn.setOnClickListener(v -> startActivity(new Intent(this, menu_message.class)));
        profileMenuBtn.setOnClickListener(v -> startActivity(new Intent(this, profileMenu.class)));

        PP_Btn.setOnClickListener(v -> showPPDialog(this, R.layout.popup_privacy_policy, R.id.privacy_policy_text, R.string.privacy_policy_text));
        TOS_Btn.setOnClickListener(v -> showTOSDialog(this, R.layout.popup_tos, R.id.terms_of_service_text, R.string.terms_of_service_text));

        logoutProfileBtn.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            String googleName = firebaseUser.getDisplayName();
            String email = firebaseUser.getEmail();

            nameUser.setText(googleName != null ? googleName.split(" ")[0] : "User");
            emailUser.setText(email != null ? email : "Email not found");
        } else {
            nameUser.setText("Guest");
            emailUser.setText("Not logged in");
        }
    }

    private void loadProfileImage() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null || firebaseUser.getUid() == null || firebaseUser.getUid().isEmpty()) {
            loadDefaultImage();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseUser.getUid();

        firestore.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("ProfilePic");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
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
                    Toast.makeText(this, "Failed to load profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDefaultImage() {
        Glide.with(this)
                .load(R.drawable.user_icon)
                .into(userPicture);
    }

    private void showPPDialog(Context context, int layoutId, int textViewId, int textResId) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutId);
        TextView textView = dialog.findViewById(textViewId);
        textView.setText(Html.fromHtml(getString(textResId), Html.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showTOSDialog(Context context, int layoutId, int textViewId, int textResId) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutId);
        TextView textView = dialog.findViewById(textViewId);
        textView.setText(Html.fromHtml(getString(textResId), Html.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(new ScrollingMovementMethod());

        ImageView closeButton = dialog.findViewById(R.id.btn_close_tos);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void logout() {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                        .revokeAccess()
                        .addOnCompleteListener(tk -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(this, LoginScreen.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
            }
        });
    }
}
