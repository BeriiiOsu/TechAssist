package com.business.techassist.UserCredentials;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.business.techassist.MainActivity;
import com.business.techassist.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginScreen extends AppCompatActivity {

    EditText emailLoginTxt, passLoginTxt;
    RelativeLayout loginBtn, googleLoginBtn;
    TextView forgotpassLoginBtn, signupLoginBtn;
    FirebaseAuth firebaseAuth;
    GoogleSignInClient googleSignInClient;
    private static final int GOOGLE_SIGN_IN_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(LoginScreen.this, options);

        firebaseAuth = FirebaseAuth.getInstance();
//        firebaseAuth.addAuthStateListener(firebaseAuth -> {
//            FirebaseUser user = firebaseAuth.getCurrentUser();
//            if (user != null) {
//                // User is logged in
//                startActivity(new Intent(LoginScreen.this, MainActivity.class));
//                finish();
//            }
//        });


        emailLoginTxt = findViewById(R.id.emailLoginTxt);
        passLoginTxt = findViewById(R.id.passLoginTxt);
        loginBtn = findViewById(R.id.loginBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);
        forgotpassLoginBtn = findViewById(R.id.forgotpassLoginBtn);
        signupLoginBtn = findViewById(R.id.signupLoginBtn);

        googleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    activityResultLauncher.launch(signInIntent);
                });
            }
        });
        loginBtn.setOnClickListener(view -> loginUser());
        signupLoginBtn.setOnClickListener(view -> signupUser());
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (o.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(o.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                    firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                addUserToFirestore(user); // Ensure user data is stored in Firestore
                            }
                        } else {
                            Toast.makeText(LoginScreen.this, "Sign In Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (ApiException e) {
                    Toast.makeText(LoginScreen.this, "Google Sign-In Error: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                    Log.e("GoogleSignIn", "ApiException: ", e);
                }
            }
        }
    });


    private void addUserToFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("Users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                // User does not exist, create a new entry
                Map<String, Object> userData = new HashMap<>();
                userData.put("userID", uid);
                userData.put("Name", user.getDisplayName());
                userData.put("Email", user.getEmail());
                userData.put("ProfilePic", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                userData.put("Role", "User"); // Default role

                db.collection("Users").document(uid).set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "User added successfully");
                            checkRole(uid);
                        })
                        .addOnFailureListener(e -> Log.e("Firestore", "Error adding user", e));
            } else {
                // User already exists, just proceed
                checkRole(uid);
            }
        });
    }


    private void loginUser() {
        if (emailLoginTxt.getText().toString().isEmpty()) {
            emailLoginTxt.setError("Email is required.");
            emailLoginTxt.requestFocus();
            return;
        }
        if (passLoginTxt.getText().toString().isEmpty()) {
            passLoginTxt.setError("Password is required.");
            passLoginTxt.requestFocus();
            return;
        }
        firebaseAuth.signInWithEmailAndPassword(emailLoginTxt.getText().toString(), passLoginTxt.getText().toString()).addOnCompleteListener(isComplete -> {
            if (isComplete.isSuccessful()) {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    if (currentUser.isEmailVerified()) {
                        checkRole(currentUser.getUid());
                    } else {
                        Toast.makeText(getApplicationContext(), "Please verify your email first!", Toast.LENGTH_SHORT).show();
                        firebaseAuth.signOut();
                    }
                }
            } else {
                Toast.makeText(LoginScreen.this, "Account not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkRole(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String role = documentSnapshot.getString("Role");
                if ("Admin".equals(role)) {
                    Toast.makeText(LoginScreen.this, "Under development", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginScreen.this, MainActivity.class));
                    finish();
                } else {
                    startActivity(new Intent(LoginScreen.this, MainActivity.class));
                    finish();
                }
            } else {
                // User does NOT exist in Firestore - Sign them out!
                firebaseAuth.signOut();
                googleSignInClient.signOut(); // Also sign out from Google if logged in with Google
                emailLoginTxt.setError("User not found!");
//                Toast.makeText(LoginScreen.this, "User not found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            firebaseAuth.signOut();
            googleSignInClient.signOut();
            Toast.makeText(LoginScreen.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void signupUser() {
        startActivity(new Intent(LoginScreen.this, SignupScreen.class));
        finish();
    }
}