package com.business.techassist.Database;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtil {

    public static String currentUserID(){
        return FirebaseAuth.getInstance().getUid();
    }

    public DocumentReference currentUserDetails(){
        return FirebaseFirestore.getInstance().collection("Users").document(currentUserID());
    }

}
