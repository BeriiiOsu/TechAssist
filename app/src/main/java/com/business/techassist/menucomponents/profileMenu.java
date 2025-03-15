package com.business.techassist.menucomponents;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.business.techassist.R;
import com.business.techassist.utilities.AndroidUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class profileMenu extends AppCompatActivity {
    AutoCompleteTextView dropdownTextView;
    ImageView profilePicture;
    EditText nameProfileTxt, emailProfileTxt, streetAddressProfileTxt, cityProfileTxt, provinceProfileTxt, postalProfileTxt;
    RelativeLayout uploadPhotoProfileBtn, saveProfileBtn, cancelProfileBtn;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String currentUserId = "sampleUserId";
    TextView removePhotoBtn;
    FirebaseFirestore db;
    DocumentReference userRef;;

    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result ->{
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = new Intent();
                    if(data != null && data.getData() != null){
                        selectedImageUri = data.getData();
                        AndroidUtil.setProfilePic(getApplicationContext(), selectedImageUri, profilePicture);
                    }
                }
            }
        );

        // Initialize UI elements
        uploadPhotoProfileBtn = findViewById(R.id.uploadPhotoProfileBtn);
        removePhotoBtn = findViewById(R.id.removePhotoBtn);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        cancelProfileBtn = findViewById(R.id.cancelProfileBtn);
        profilePicture = findViewById(R.id.profilePicture);
        nameProfileTxt = findViewById(R.id.nameProfileTxt);
        emailProfileTxt = findViewById(R.id.emailProfileTxt);
        streetAddressProfileTxt = findViewById(R.id.streetAddressProfileTxt);
        cityProfileTxt = findViewById(R.id.cityProfileTxt);
        provinceProfileTxt = findViewById(R.id.provinceProfileTxt);
        postalProfileTxt = findViewById(R.id.postalProfileTxt);
        dropdownTextView = findViewById(R.id.dropdownTextView);

        removePhotoBtn.setEnabled(false);
        removePhotoBtn.setVisibility(TextView.GONE);


        // Get Firebase user ID
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }

        // Set up Firebase database reference
        db = FirebaseFirestore.getInstance();
        userRef = db.collection("Users").document(currentUserId);


        // Load user profile data
        loadProfileData();

        // Click listeners
        uploadPhotoProfileBtn.setOnClickListener(v -> {
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickLauncher.launch(intent);
                            return null;
                        }
                    });
        });
        removePhotoBtn.setOnClickListener(v -> removeProfileImage());
        saveProfileBtn.setOnClickListener(v -> {
            saveProfileData();
        });
        cancelProfileBtn.setOnClickListener(v -> finish());

        // Country dropdown setup
        String[] countries = {
                "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan",
                "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia and Herzegovina",
                "Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina Faso", "Burundi", "Cambodia", "Cameroon", "Canada", "Cape Verde",
                "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica", "Croatia", "Cuba",
                "Cyprus", "Czech Republic", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "Ecuador", "Egypt", "El Salvador",
                "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia",
                "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hungary",
                "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan",
                "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein",
                "Lithuania", "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Mauritania", "Mauritius",
                "Mexico", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal",
                "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway", "Oman", "Pakistan",
                "Palau", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia",
                "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent and the Grenadines", "Samoa", "San Marino", "Sao Tome and Principe",
                "Saudi Arabia", "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands",
                "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden", "Switzerland",
                "Syria", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey",
                "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan",
                "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countries);
        dropdownTextView.setAdapter(adapter);
    }

    // Opens Image Chooser
    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).placeholder(R.drawable.user_icon).error(R.drawable.user_icon).into(profilePicture);
            removePhotoBtn.setEnabled(true);
            removePhotoBtn.setVisibility(View.VISIBLE);
        }
    }

    // Load Profile Data from Firebase
    private void loadProfileData() {
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                nameProfileTxt.setText(documentSnapshot.getString("Name"));
                emailProfileTxt.setText(documentSnapshot.getString("Email"));
                streetAddressProfileTxt.setText(documentSnapshot.getString("streetAddress"));
                cityProfileTxt.setText(documentSnapshot.getString("city"));
                provinceProfileTxt.setText(documentSnapshot.getString("province"));
                postalProfileTxt.setText(documentSnapshot.getString("postalCode"));
                dropdownTextView.setText(documentSnapshot.getString("country"), false);

                // Load profile image
                String imageUrl = documentSnapshot.getString("ProfilePic");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(profileMenu.this).load(imageUrl).into(profilePicture);
                    removePhotoBtn.setVisibility(View.VISIBLE);
                    removePhotoBtn.setEnabled(true);
                } else {
                    profilePicture.setImageResource(R.drawable.user_icon);
                    removePhotoBtn.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(profileMenu.this, "Failed to load profile data!", Toast.LENGTH_SHORT).show()
        );
    }


    // Save Profile Data to Firebase
    private void saveProfileData() {
        String name = nameProfileTxt.getText().toString().trim();
        String email = emailProfileTxt.getText().toString().trim();
        String streetAddress = streetAddressProfileTxt.getText().toString().trim();
        String city = cityProfileTxt.getText().toString().trim();
        String province = provinceProfileTxt.getText().toString().trim();
        String postalCode = postalProfileTxt.getText().toString().trim();
        String country = dropdownTextView.getText().toString().trim();

        // Save data using HashMap
        Map<String, Object> userData = new HashMap<>();
        userData.put("Name", name.isEmpty() ? "N/A" : name);
        userData.put("Email", email.isEmpty() ? "N/A" : email);
        userData.put("streetAddress", streetAddress.isEmpty() ? "N/A" : streetAddress);
        userData.put("city", city.isEmpty() ? "N/A" : city);
        userData.put("province", province.isEmpty() ? "N/A" : province);
        userData.put("postalCode", postalCode.isEmpty() ? "N/A" : postalCode);
        userData.put("country", country.isEmpty() ? "N/A" : country);

        if (imageUri != null) {
            uploadImageToFirebase(userData);
        } else {
            userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(profileMenu.this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(profileMenu.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
            finish();
        }
    }

    // Upload Profile Image
    private void uploadImageToFirebase(Map<String, Object> userData) {
        if (imageUri == null) {
            Toast.makeText(profileMenu.this, "No image selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUserId + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().toString();
                        userData.put("ProfilePic", downloadUrl);

                        userRef.set(userData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Glide.with(profileMenu.this).load(downloadUrl).into(profilePicture);
                                    removePhotoBtn.setVisibility(View.VISIBLE);
                                    removePhotoBtn.setEnabled(true);
                                    Toast.makeText(profileMenu.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(profileMenu.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    } else {
                        Toast.makeText(profileMenu.this, "Failed to get image URL!", Toast.LENGTH_SHORT).show();
                    }
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(profileMenu.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }



    private void removeProfileImage() {
        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images/" + currentUserId + ".jpg");

        fileRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Remove image URL from Firestore
                userRef.update("ProfilePic", FieldValue.delete())
                        .addOnSuccessListener(aVoid -> {
                            profilePicture.setImageResource(R.drawable.user_icon);
                            removePhotoBtn.setVisibility(View.GONE);
                            removePhotoBtn.setEnabled(false);
                            Toast.makeText(profileMenu.this, "Profile image removed", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(profileMenu.this, "Failed to remove image URL!", Toast.LENGTH_SHORT).show()
                        );
            } else {
                Toast.makeText(profileMenu.this, "No image found or failed to remove!", Toast.LENGTH_SHORT).show();
            }
        });
    }



}
