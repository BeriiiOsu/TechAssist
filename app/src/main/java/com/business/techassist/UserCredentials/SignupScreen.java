package com.business.techassist.UserCredentials;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class SignupScreen extends AppCompatActivity {

    EditText nameSignupTxt, emailSignupTxt, passSignupTxt;
    RelativeLayout signUpBtn, googleSignupBtn;
    TextView loginSignupBtn;
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

        googleSignupBtn.setOnClickListener(view -> signInWithGoogle());
        signUpBtn.setOnClickListener(view -> registerUser(nameSignupTxt.getText().toString(), emailSignupTxt.getText().toString(), passSignupTxt.getText().toString()));
        loginSignupBtn.setOnClickListener(view -> loginUser());
    }

    private void signInWithGoogle() {
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
                }
            } catch (ApiException e) {
                // Handle error
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
                        }
                    } else {
                        Toast.makeText(SignupScreen.this, "Google Authentication Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        firestore.collection("Users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User already exists, proceed to main screen
                        startActivity(new Intent(SignupScreen.this, MainActivity.class));
                        finish();
                    } else {
                        // User does not exist, save details in Firestore
                        saveUserToFirestore(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
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
        user.put("ProfilePic", profilePicUrl); // Store profile picture

        firestore.collection("Users").document(userID).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(SignupScreen.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(SignupScreen.this, "Firestore Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String name, String email, String pass) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || pass.length() < 8) {
            Toast.makeText(this, "Please fill out all fields correctly.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the user already exists in Firestore before signing up
        firestore.collection("Users")
                .whereEqualTo("Email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // User already exists in Firestore
                        emailSignupTxt.setError("User already exists!");
//                        Toast.makeText(SignupScreen.this, "User already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        // User does NOT exist, proceed with Firebase Authentication signup
                        firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                        if (firebaseUser != null) {
                                            firebaseUser.sendEmailVerification().addOnCompleteListener(isSent -> {
                                                if (isSent.isSuccessful()) {
                                                    Toast.makeText(SignupScreen.this, "Verification email sent.", Toast.LENGTH_LONG).show();
                                                    saveUserToFirestore(firebaseUser, name, email);
                                                }
                                            });
                                        }
                                    } else {
                                        Toast.makeText(SignupScreen.this, "Signup Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupScreen.this, "Error checking Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email) {
        String userID = firebaseUser.getUid();
        HashMap<String, Object> user = new HashMap<>();
        user.put("userID", userID);
        user.put("Name", name);
        user.put("Role", "User");
        user.put("Email", email);

        firestore.collection("Users").document(userID).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(SignupScreen.this, LoginScreen.class));
                finish();
            }
        });
    }

    private void loginUser() {
        startActivity(new Intent(SignupScreen.this, LoginScreen.class));
        finish();
    }
}
