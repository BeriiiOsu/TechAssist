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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginScreen extends AppCompatActivity {

    EditText emailLoginTxt, passLoginTxt;
    RelativeLayout loginBtn, googleLoginBtn;
    TextView forgotpassLoginBtn, signupLoginBtn;
    FirebaseAuth firebaseAuth;

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

        firebaseAuth = FirebaseAuth.getInstance();

        emailLoginTxt = findViewById(R.id.emailLoginTxt);
        passLoginTxt = findViewById(R.id.passLoginTxt);
        loginBtn = findViewById(R.id.loginBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);
        forgotpassLoginBtn = findViewById(R.id.forgotpassLoginBtn);
        signupLoginBtn = findViewById(R.id.signupLoginBtn);

        //This checks if the user stayed signed in
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            // User is signed in
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }


        loginBtn.setOnClickListener(view -> loginUser());
        signupLoginBtn.setOnClickListener(view -> signupUser());
    }

    private void loginUser(){
        if(emailLoginTxt.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Email is required!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(passLoginTxt.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Password is required!", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseAuth.signInWithEmailAndPassword(emailLoginTxt.getText().toString(), passLoginTxt.getText().toString()).addOnCompleteListener(isComplete ->{
            if(isComplete.isSuccessful()){
                if(Objects.requireNonNull(firebaseAuth.getCurrentUser()).isEmailVerified()){
                    startActivity(new Intent(LoginScreen.this, MainActivity.class));
                    finish();
                }else{
                    Toast.makeText(getApplicationContext(), "Please verify your email first!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(LoginScreen.this, "Login Failed: " + Objects.requireNonNull(isComplete.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void signupUser(){
        startActivity(new Intent(LoginScreen.this, SignupScreen.class));
    }
}