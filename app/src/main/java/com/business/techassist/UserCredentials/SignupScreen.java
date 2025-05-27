package com.business.techassist.UserCredentials;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class SignupScreen extends AppCompatActivity {

    EditText nameSignupTxt, emailSignupTxt, passSignupTxt;
    MaterialButton signUpBtn, googleSignupBtn;
    TextView loginSignupBtn;
    ProgressBar progressBar;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    private static final int GOOGLE_SIGN_IN_CODE = 100;
    GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        nameSignupTxt = findViewById(R.id.nameSignupTxt);
        emailSignupTxt = findViewById(R.id.emailSignupTxt);
        passSignupTxt = findViewById(R.id.passSignupTxt);
        signUpBtn = findViewById(R.id.signUpBtn);
        googleSignupBtn = findViewById(R.id.googleSignupBtn);
        loginSignupBtn = findViewById(R.id.loginSignupBtn);
        progressBar = findViewById(R.id.progressBar1);

        googleSignupBtn.setOnClickListener(view -> signInWithGoogle());
        signUpBtn.setOnClickListener(view -> registerUser(
                nameSignupTxt.getText().toString(),
                emailSignupTxt.getText().toString(),
                passSignupTxt.getText().toString(),
                "User",
                "N/A",
                "0"));
        loginSignupBtn.setOnClickListener(view -> loginUser());
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            signUpBtn.setEnabled(!isLoading);
            googleSignupBtn.setEnabled(!isLoading);
            loginSignupBtn.setEnabled(!isLoading);
            emailSignupTxt.setEnabled(!isLoading);
            passSignupTxt.setEnabled(!isLoading);
            nameSignupTxt.setEnabled(!isLoading);
        }
    }

    private void signInWithGoogle() {
        setLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN_CODE) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    authenticateWithFirebase(account);
                } else {
                    setLoading(false);
                }
            } catch (ApiException e) {
                setLoading(false);
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void authenticateWithFirebase(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkUserInFirestore(firebaseUser);
                        } else {
                            setLoading(false);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(SignupScreen.this, "Google Authentication Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        firestore.collection("Users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        startActivity(new Intent(SignupScreen.this, MainActivity.class));
                        finish();
                    } else {
                        saveUserToFirestore(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(SignupScreen.this, "Error checking Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        String userID = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String profilePicUrl = (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "";

        HashMap<String, Object> user = new HashMap<>();
        user.put("userID", userID);
        user.put("Name", name);
        user.put("Role", "User");
        user.put("Email", email);
        user.put("ProfilePic", profilePicUrl);
        user.put("Level", 1);
        user.put("Total Exp", 0);
        user.put("Exp", 0);
        user.put("Ratings", 0);
        user.put("Current Tier", "Bronze");
        user.put("Current Plan", "Basic");

        firestore.collection("Users").document(userID).set(user).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                startActivity(new Intent(SignupScreen.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(SignupScreen.this, "Firestore Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void registerUser(String name, String email, String pass, String isAdmin, String specialized, String experience) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || pass.length() < 8) {
            if (name.isEmpty()) {
                nameSignupTxt.setError("Name is required");
                nameSignupTxt.requestFocus();
            } else if (email.isEmpty()) {
                emailSignupTxt.setError("Email is required");
                emailSignupTxt.requestFocus();
            } else if (pass.isEmpty()) {
                passSignupTxt.setError("Password is required");
                passSignupTxt.requestFocus();
            } else {
                passSignupTxt.setError("Password must be at least 8 characters");
                passSignupTxt.requestFocus();
            }
            return;
        }

        setLoading(true);

        if(isAdmin.equals("User")){
            User(name, email, isAdmin, specialized, experience, pass);
        }else{
            Admin(name, email, isAdmin, specialized, experience, pass);
        }
    }

    private void User(String name, String email, String isAdmin, String specialized, String experience, String pass){
        firestore.collection("Users")
                .whereEqualTo("Email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        setLoading(false);
                        emailSignupTxt.setError("User already exists!");
                    } else {
                        firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                        if (firebaseUser != null) {
                                            firebaseUser.sendEmailVerification().addOnCompleteListener(isSent -> {
                                                if (isSent.isSuccessful()) {
                                                    Toast.makeText(SignupScreen.this, "Verification email sent.", Toast.LENGTH_LONG).show();
                                                    saveUserToFirestore(firebaseUser, name, email, isAdmin, specialized, experience);
                                                } else {
                                                    setLoading(false);
                                                }
                                            });
                                        } else {
                                            setLoading(false);
                                        }
                                    } else {
                                        setLoading(false);
                                        Toast.makeText(SignupScreen.this, "Signup Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(SignupScreen.this, "Error checking Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void Admin(String name, String email, String isAdmin, String specialized, String experience, String pass) {
        setLoading(true);

        // Initialize secondary FirebaseApp
        FirebaseApp secondaryApp;
        try {
            FirebaseApp defaultApp = FirebaseApp.getInstance();
            FirebaseOptions options = defaultApp.getOptions();
            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary_App");
        } catch (IllegalStateException e) {
            // Retrieve existing instance if already initialized
            secondaryApp = FirebaseApp.getInstance("Secondary_App");
        }

        FirebaseAuth tempAuth = FirebaseAuth.getInstance(secondaryApp);

        firestore.collection("Users")
                .whereEqualTo("Email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        emailSignupTxt.setError("Admin already exists!");
                    } else {
                        tempAuth.createUserWithEmailAndPassword(email, pass)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        FirebaseUser newAdminUser = tempAuth.getCurrentUser();
                                        if (newAdminUser != null) {
                                            newAdminUser.sendEmailVerification()
                                                    .addOnCompleteListener(isSent -> {
                                                        if (isSent.isSuccessful()) {
                                                            Toast.makeText(SignupScreen.this,
                                                                    "Admin account created. Verification email sent.",
                                                                    Toast.LENGTH_LONG).show();
                                                            saveAdminToFirestore(tempAuth, newAdminUser,
                                                                    name, email, isAdmin, specialized, experience);
                                                        } else {
                                                            Toast.makeText(SignupScreen.this,
                                                                    "Failed to send verification email.",
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(SignupScreen.this,
                                                "Signup Failed: " + task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(SignupScreen.this,
                            "Error checking Firestore: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveAdminToFirestore(FirebaseAuth tempAuth, FirebaseUser newAdminUser,
                                      String name, String email, String isAdmin, String specialized, String experience) {

        String userID = newAdminUser.getUid();
        HashMap<String, Object> admin = new HashMap<>();
        admin.put("userID", userID);
        admin.put("Name", name);
        admin.put("Role", isAdmin);
        admin.put("Email", email);
        admin.put("Specialized", specialized);
        admin.put("Experience", experience);
        admin.put("Ratings", 0);
        admin.put("Level", 1);
        admin.put("Exp", 0);
        admin.put("Status", "Inactive");
        admin.put("CurrentTimer", 0);
        admin.put("CurrentStatus", "In Admin");

        firestore.collection("Users").document(userID).set(admin).addOnCompleteListener(task -> {
            setLoading(false);
            tempAuth.signOut();
            if (task.isSuccessful()) {
                Toast.makeText(SignupScreen.this, "Admin account registered successfully! Please wait for approval.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(SignupScreen.this, LoginScreen.class));
                finish();
            } else {
                Toast.makeText(SignupScreen.this, "Admin Firestore Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email,
                                     String isAdmin, String specialized, String experience) {

        String userID = firebaseUser.getUid();
        HashMap<String, Object> user = new HashMap<>();
        user.put("userID", userID);
        user.put("Name", name);
        user.put("Role", isAdmin);
        user.put("Email", email);
        user.put("Level", 1);
        user.put("Total Exp", experience);
        user.put("Exp", 0);
        user.put("Ratings", 0);
        user.put("Specialized", specialized);
        user.put("Devices Checked", 0);
        user.put("Current Tier", "Bronze");
        user.put("Current Plan", "Basic");

        firestore.collection("Users").document(userID).set(user)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        if (isAdmin.equals("User")) {
                            startActivity(new Intent(SignupScreen.this, LoginScreen.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(SignupScreen.this,
                                "Error saving user data: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser() {
        startActivity(new Intent(SignupScreen.this, LoginScreen.class));
        finish();
    }
}