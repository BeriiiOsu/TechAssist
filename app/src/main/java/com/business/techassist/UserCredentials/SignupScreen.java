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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class SignupScreen extends AppCompatActivity {

    EditText nameSignupTxt, emailSignupTxt, passSignupTxt;
    RelativeLayout signUpBtn, googleSignupBtn;
    TextView loginSignupBtn;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    private static final int GOOGLE_SIGN_IN_CODE = 100; // Request Code for Google Sign-In
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
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Make sure you have this in strings.xml
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
//                Toast.makeText(this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            saveUserToDatabase(firebaseUser);
                        }
                    } else {
                        Toast.makeText(SignupScreen.this, "Google Authentication Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void saveUserToDatabase(FirebaseUser firebaseUser) {
        String userID = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();

        HashMap<String, Object> map = new HashMap<>();
        map.put("userID", userID);
        map.put("Name", name);
        map.put("Email", email);

        databaseReference.child(userID).setValue(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(SignupScreen.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(SignupScreen.this, "Database Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void registerUser(String name, String email, String pass) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(pass.length() < 8){
            Toast.makeText(this, "Password must 8 or more characters long!", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task ->{
                    if(task.isSuccessful()){
                        Objects.requireNonNull(firebaseAuth.getCurrentUser()).sendEmailVerification().addOnCompleteListener(isSent ->{
                            if(isSent.isSuccessful()){
                                Toast.makeText(SignupScreen.this, "Verification email sent. Please check your email.", Toast.LENGTH_LONG).show();
                                String userID = firebaseAuth.getCurrentUser().getUid();
                                createUserDatabase(userID, name, email);
                            }else {
                                Toast.makeText(SignupScreen.this, "Failed to send verification email: " + Objects.requireNonNull(isSent.getException()).getMessage(), Toast.LENGTH_LONG).show();
                            }

                        });

                    }else {
                        Toast.makeText(SignupScreen.this, "Signup Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserDatabase(String userID, String name, String email) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("userID", userID);
        map.put("Name", name);
        map.put("Email", email);

        databaseReference.child(userID).setValue(map).addOnCompleteListener(task ->{
            if(task.isSuccessful()){
               Toast.makeText(SignupScreen.this, "Account Created!", Toast.LENGTH_SHORT).show();
               startActivity(new Intent(SignupScreen.this, LoginScreen.class));
               finish();
            } else {
                Toast.makeText(SignupScreen.this, "Database Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(){
        startActivity(new Intent(SignupScreen.this, LoginScreen.class));
        finish();
    }
}