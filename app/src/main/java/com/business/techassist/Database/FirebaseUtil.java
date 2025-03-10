package com.business.techassist.Database;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtil {

    public static String currentUserID(){
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn(){
        if(currentUserID() != null){
            return true;
        }
        return false;
    }

    public static CollectionReference allUser(){
        return FirebaseFirestore.getInstance().collection("Users");
    }

    public static DocumentReference getChatroomID(String chatroomID){
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomID);
    }

    public static CollectionReference getChatroomMessage(String chatroomID){
        return getChatroomID(chatroomID).collection("chats");
    }

    public static String getChatroomIDUser(String user1, String user2){
        if(user1.hashCode()<user2.hashCode()){
            return user1 + "_" + user2;
        }else{
            return user2 + "_" + user1;
        }
    }


    public DocumentReference currentUserDetails(){
        return FirebaseFirestore.getInstance().collection("Users").document(currentUserID());
    }

}
