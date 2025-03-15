package com.business.techassist.utilities;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {

    public static String currentUserID() {
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn() {
        return currentUserID() != null;
    }

    public static CollectionReference allUser() {
        return FirebaseFirestore.getInstance().collection("Users");
    }

    public static DocumentReference getChatroomID(String chatroomID) {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomID);
    }

    public static CollectionReference getChatroomMessage(String chatroomID) {
        return getChatroomID(chatroomID).collection("chats");
    }

    public static String getChatroomIDUser(String user1, String user2) {
        return (user1.hashCode() < user2.hashCode()) ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        return allUser().document(userIds.get(0).equals(currentUserID()) ? userIds.get(1) : userIds.get(0));
    }

    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("MMMM dd 'AT' hh:mm a").format(timestamp.toDate());
    }

    public static DocumentReference currentUserDetails() {
        return FirebaseFirestore.getInstance().collection("Users").document(currentUserID());
    }

    public static void getCurrentUserRole(OnSuccessListener<String> listener) {
        currentUserDetails().get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String role = documentSnapshot.getString("Role");
                listener.onSuccess(role != null ? role : "");
            } else {
                listener.onSuccess("");
            }
        }).addOnFailureListener(e -> {
            listener.onSuccess(""); // Return empty if an error occurs
        });
    }

}
