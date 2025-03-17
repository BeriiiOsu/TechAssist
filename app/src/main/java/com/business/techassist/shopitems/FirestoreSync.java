//package com.business.techassist.shopitems;
//
//import android.content.Context;
//import android.util.Base64;
//import android.util.Log;
//
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//public class FirestoreSync {
//    private FirebaseFirestore firestore;
//    private DatabaseHelper dbHelper;
//
//    public FirestoreSync(Context context) {
//        firestore = FirebaseFirestore.getInstance();
//        dbHelper = new DatabaseHelper(context);
//    }
//
//    public void syncImagesToFirestore() {
//        firestore.collection("products").get().addOnSuccessListener(queryDocumentSnapshots -> {
//            for (DocumentSnapshot doc : queryDocumentSnapshots) {
//                String productName = doc.getString("name");
//                byte[] imageBytes = dbHelper.getProductImage(productName);
//
//                if (imageBytes != null) {
//                    String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
//
//                    firestore.collection("products").document(productName)
//                            .update("image", base64Image)
//                            .addOnSuccessListener(aVoid -> Log.d("Sync", "Image synced for " + productName))
//                            .addOnFailureListener(e -> Log.e("Sync", "Failed: " + e.getMessage()));
//                }
//            }
//        });
//    }
//}
