package com.business.techassist.admin_utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthHelper {
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public FirebaseAuthHelper() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void registerUser(String name, String email, String password, String role,
                             String specialization, String experience,
                             OnRegistrationCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Store additional user data in Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("Email", email);
                            userData.put("Role", role);
                            userData.put("specialization", specialization);
                            userData.put("experience", experience);
                            userData.put("userID", user.getUid());

                            firestore.collection("Users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        try {
                                            listener.onSuccess(user);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .addOnFailureListener(e -> listener.onFailure(e));
                        }
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    public interface OnRegistrationCompleteListener {
        void onSuccess(FirebaseUser user) throws IOException;
        void onFailure(Exception e);
    }
}