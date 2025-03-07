package com.business.techassist.UserCredentials;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.business.techassist.MainActivity;
import com.business.techassist.R;
import com.google.firebase.auth.FirebaseAuth;
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

        nameSignupTxt = findViewById(R.id.nameSignupTxt);
        emailSignupTxt = findViewById(R.id.emailSignupTxt);
        passSignupTxt = findViewById(R.id.passSignupTxt);
        signUpBtn = findViewById(R.id.signUpBtn);
        googleSignupBtn = findViewById(R.id.googleSignupBtn);
        loginSignupBtn = findViewById(R.id.loginSignupBtn);


        signUpBtn.setOnClickListener(view -> registerUser(nameSignupTxt.getText().toString(), emailSignupTxt.getText().toString(), passSignupTxt.getText().toString()));
        loginSignupBtn.setOnClickListener(view -> loginUser());
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
               startActivity(new Intent(SignupScreen.this, MainActivity.class));
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