package com.business.techassist.UserCredentials;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;

import com.business.techassist.MainActivity;
import com.business.techassist.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.type.DateTime;

import java.util.HashMap;
import java.util.Map;

public class LoginScreen extends AppCompatActivity {

    EditText emailLoginTxt, passLoginTxt;
    MaterialButton loginBtn, googleLoginBtn;
    TextView forgotpassLoginBtn, signupLoginBtn, welcomeTextView;
    FirebaseAuth firebaseAuth;
    GoogleSignInClient googleSignInClient;
    ProgressBar progressBar;
    SharedPreferences prefs;

    private static final int GOOGLE_SIGN_IN_CODE = 100;
    private static final String TAG = "LoginScreen";
    private static final String PREFS_NAME = "TechAssistPrefs";
    private static final String KEY_HAS_LOGGED_IN = "hasLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login_screen);

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Apply insets for system bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Google Sign-In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(LoginScreen.this, options);

        firebaseAuth = FirebaseAuth.getInstance();

        // Find views by ID
        emailLoginTxt = findViewById(R.id.emailLoginTxt);
        passLoginTxt = findViewById(R.id.passLoginTxt);
        loginBtn = findViewById(R.id.loginBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);
        forgotpassLoginBtn = findViewById(R.id.forgotpassLoginBtn);
        signupLoginBtn = findViewById(R.id.signupLoginBtn);
        progressBar = findViewById(R.id.progressBar);
        
        // Find the welcome text view
        welcomeTextView = findViewById(R.id.welcomeTextView);
        
        // Set welcome text based on user history
        setWelcomeText();

        // Set up Google Login button click listener
        googleLoginBtn.setOnClickListener(view -> {
            showLoading(true);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            });
        });

        // Set up Login button click listener
        loginBtn.setOnClickListener(view -> loginUser());

        // Set up Sign Up button click listener
        signupLoginBtn.setOnClickListener(view -> signupUser());
        
        // Set up Forgot Password button click listener
        forgotpassLoginBtn.setOnClickListener(view -> showForgotPasswordDialog());
    }
    
    private void setWelcomeText() {
        boolean hasLoggedIn = prefs.getBoolean(KEY_HAS_LOGGED_IN, false);
        
        if (welcomeTextView != null) {
            if (hasLoggedIn) {
                welcomeTextView.setText("Welcome Back");
            } else {
                welcomeTextView.setText("Welcome");
            }
        }
    }
    
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(view);
        
        EditText emailField = view.findViewById(R.id.reset_email);
        MaterialButton resetButton = view.findViewById(R.id.reset_button);
        MaterialButton cancelButton = view.findViewById(R.id.cancel_button);
        ProgressBar dialogProgressBar = view.findViewById(R.id.dialog_progress);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        resetButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            
            if (email.isEmpty()) {
                emailField.setError("Email is required");
                return;
            }
            
            dialogProgressBar.setVisibility(View.VISIBLE);
            resetButton.setEnabled(false);
            cancelButton.setEnabled(false);
            
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        dialogProgressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginScreen.this, 
                                    "Password reset email sent to " + email, 
                                    Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            resetButton.setEnabled(true);
                            cancelButton.setEnabled(true);
                            Toast.makeText(LoginScreen.this, 
                                    "Failed to send reset email: " + task.getException().getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error sending reset email", task.getException());
                        }
                    });
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                        AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                        showLoading(true);
                        firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                            showLoading(false);
                            if (task.isSuccessful()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) {
                                    addUserToFirestore(user);
                                }
                            } else {
                                Toast.makeText(LoginScreen.this, "Sign In Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (ApiException e) {
                        showLoading(false);
                        Toast.makeText(LoginScreen.this, "Google Sign-In Error: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                        Log.e("GoogleSignIn", "ApiException: ", e);
                    }
                } else {
                    showLoading(false);
                }
            });

    private void addUserToFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("Users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("userID", uid);
                userData.put("Name", user.getDisplayName());
                userData.put("Email", user.getEmail());
                userData.put("ProfilePic", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                userData.put("Role", "User");
                userData.put("Level", 1);
                userData.put("Exp", 0);
                userData.put("Current Tier", "Bronze Member");
                userData.put("Current Plan", "Basic");

                db.collection("Users").document(uid).set(userData)
                        .addOnSuccessListener(aVoid -> {
                            checkRole(uid);
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                        });
            } else {
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

        showLoading(true);

        firebaseAuth.signInWithEmailAndPassword(emailLoginTxt.getText().toString(), passLoginTxt.getText().toString()).addOnCompleteListener(isComplete -> {
            if (isComplete.isSuccessful()) {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    if (currentUser.isEmailVerified()) {
                        checkRole(currentUser.getUid());
                    } else {
                        showLoading(false);
                        Toast.makeText(getApplicationContext(), "Please verify your email first!", Toast.LENGTH_SHORT).show();
                        firebaseAuth.signOut();
                    }
                }
            } else {
                showLoading(false);
                Toast.makeText(LoginScreen.this, "Account not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkRole(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            showLoading(false);
            if (documentSnapshot.exists()) {
                String role = documentSnapshot.getString("Role");
                String name = documentSnapshot.getString("Name");
                if ("Admin".equals(role)) {
                    Toast.makeText(LoginScreen.this, "Hello " + name + "\n"
                            + "Time in: " + DateTime.getDefaultInstance().toString(), Toast.LENGTH_SHORT).show();
                }
                
                // Save that user has logged in
                prefs.edit().putBoolean(KEY_HAS_LOGGED_IN, true).apply();
                
                startActivity(new Intent(LoginScreen.this, MainActivity.class));
                finish();
            } else {
                firebaseAuth.signOut();
                googleSignInClient.signOut();
                emailLoginTxt.setError("User not found!");
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            firebaseAuth.signOut();
            googleSignInClient.signOut();
            Toast.makeText(LoginScreen.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void signupUser() {
        startActivity(new Intent(LoginScreen.this, SignupScreen.class));
        finish();
    }

    private void setInteractionEnabled(boolean enabled) {
        loginBtn.setEnabled(enabled);
        googleLoginBtn.setEnabled(enabled);
        signupLoginBtn.setEnabled(enabled);
        forgotpassLoginBtn.setEnabled(enabled);
        emailLoginTxt.setEnabled(enabled);
        passLoginTxt.setEnabled(enabled);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        setInteractionEnabled(!isLoading);
    }
}
