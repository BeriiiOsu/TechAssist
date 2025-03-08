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
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;

public class LoginScreen extends AppCompatActivity {

    EditText emailLoginTxt, passLoginTxt;
    RelativeLayout loginBtn, googleLoginBtn;
    TextView forgotpassLoginBtn, signupLoginBtn;
    FirebaseAuth firebaseAuth;
    GoogleSignInClient googleSignInClient;

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


        googleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });
        loginBtn.setOnClickListener(view -> loginUser());
        signupLoginBtn.setOnClickListener(view -> signupUser());
    }
//google signin
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
    @Override
    public void onActivityResult(ActivityResult o) {
        if(o.getResultCode() == RESULT_OK){
            Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(o.getData());
            try{
                GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
//                            firebaseAuth = FirebaseAuth.getInstance();
//                            Glide.with(LoginScreen.this).load(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getPhotoUrl()).into(userProfile);
                                startActivity(new Intent(LoginScreen.this, MainActivity.class));
                                finish();
                        }else{
                            Toast.makeText(LoginScreen.this, "Sign In Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (ApiException e) {
                Toast.makeText(LoginScreen.this, "Google Sign-In Error: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                Log.e("GoogleSignIn", "ApiException: ", e);
            }
        }
    }
});


    //normal signin
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
        finish();
    }
}