package com.business.techassist.menucomponents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.business.techassist.R;
import com.business.techassist.utilities.AndroidUtil;
import com.business.techassist.utilities.LocationDataProvider;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class profileMenu extends AppCompatActivity {
    private static final String TAG = "ProfileMenu";
    
    AutoCompleteTextView dropdownTextView, cityDropdown, provinceDropdown, barangayDropdown;
    ShapeableImageView profilePicture;
    TextInputEditText nameProfileTxt, emailProfileTxt, streetAddressProfileTxt, postalProfileTxt;
    MaterialButton uploadPhotoProfileBtn, saveProfileBtn, cancelProfileBtn;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String currentUserId = "sampleUserId";
    TextView removePhotoBtn;
    FirebaseFirestore db;
    DocumentReference userRef;
    private ConstraintLayout rootLayout; // Root layout for Snackbar

    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;
    
    // Location data provider
    private LocationDataProvider locationDataProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_profile);
        
        // Prevent automatic keyboard popup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Initialize the location data provider
        locationDataProvider = LocationDataProvider.getInstance();

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result ->{
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                    if(data != null && data.getData() != null){
                        selectedImageUri = data.getData();
                        AndroidUtil.setProfilePic(getApplicationContext(), selectedImageUri, profilePicture);
                        imageUri = selectedImageUri;
                        removePhotoBtn.setEnabled(true);
                        removePhotoBtn.setVisibility(View.VISIBLE);
                        showSnackbar("Profile picture updated");
                    }
                }
            }
        );

        // Initialize UI elements
        rootLayout = findViewById(R.id.profile_root_layout);
        uploadPhotoProfileBtn = findViewById(R.id.uploadPhotoProfileBtn);
        removePhotoBtn = findViewById(R.id.removePhotoBtn);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        cancelProfileBtn = findViewById(R.id.cancelProfileBtn);
        profilePicture = findViewById(R.id.profilePicture);
        nameProfileTxt = findViewById(R.id.nameProfileTxt);
        emailProfileTxt = findViewById(R.id.emailProfileTxt);
        streetAddressProfileTxt = findViewById(R.id.streetAddressProfileTxt);
        cityDropdown = findViewById(R.id.cityDropdown);
        provinceDropdown = findViewById(R.id.provinceDropdown);
        barangayDropdown = findViewById(R.id.barangayDropdown);
        postalProfileTxt = findViewById(R.id.postalProfileTxt);
        dropdownTextView = findViewById(R.id.dropdownTextView);

        // Enhanced focus prevention for dropdown fields
        dropdownTextView.setFocusable(false);
        provinceDropdown.setFocusable(false);
        cityDropdown.setFocusable(false);
        barangayDropdown.setFocusable(false);

        // Make fields clickable but not focusable
        dropdownTextView.setFocusableInTouchMode(false);
        provinceDropdown.setFocusableInTouchMode(false);
        cityDropdown.setFocusableInTouchMode(false);
        barangayDropdown.setFocusableInTouchMode(false);
        
        // Set threshold to prevent auto-filtering showing dropdown
        dropdownTextView.setThreshold(Integer.MAX_VALUE);
        provinceDropdown.setThreshold(Integer.MAX_VALUE);
        cityDropdown.setThreshold(Integer.MAX_VALUE);
        barangayDropdown.setThreshold(Integer.MAX_VALUE);
        
        // Clear any focus immediately
        dropdownTextView.clearFocus();
        provinceDropdown.clearFocus();
        cityDropdown.clearFocus();
        barangayDropdown.clearFocus();
        
        // Request focus on the ScrollView instead
        findViewById(R.id.scrollView).requestFocus();
        
        // Immediately dismiss any dropdowns that might show
        dropdownTextView.dismissDropDown();
        provinceDropdown.dismissDropDown();
        cityDropdown.dismissDropDown();
        barangayDropdown.dismissDropDown();
        
        // Set white background for all dropdowns
        dropdownTextView.setDropDownBackgroundResource(android.R.color.white);
        provinceDropdown.setDropDownBackgroundResource(android.R.color.white);
        cityDropdown.setDropDownBackgroundResource(android.R.color.white);
        barangayDropdown.setDropDownBackgroundResource(android.R.color.white);
        
        // Disable dependent dropdowns until their parent is selected
        provinceDropdown.setEnabled(false);
        cityDropdown.setEnabled(false);
        barangayDropdown.setEnabled(false);

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

        // Set up dropdowns
        setupDropdowns();

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

        // Initialize country dropdown without showing it
        initializeCountryDropdown();
        
        // Add click listeners to make dropdown fields open on click
        dropdownTextView.setOnClickListener(v -> dropdownTextView.showDropDown());
        provinceDropdown.setOnClickListener(v -> {
            if (provinceDropdown.isEnabled()) {
                provinceDropdown.showDropDown();
            } else {
                showSnackbar("Please select a country first");
            }
        });
        cityDropdown.setOnClickListener(v -> {
            if (cityDropdown.isEnabled()) {
                cityDropdown.showDropDown();
            } else {
                showSnackbar("Please select a province first");
            }
        });
        barangayDropdown.setOnClickListener(v -> {
            if (barangayDropdown.isEnabled()) {
                barangayDropdown.showDropDown();
            } else {
                showSnackbar("Please select a city first");
            }
        });
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        
        if (hasFocus) {
            // Ensure no dropdown is showing when window gains focus
            hideAllDropdowns();
        }
    }

    private void hideAllDropdowns() {
        // Dismiss all dropdowns
        if (dropdownTextView != null) dropdownTextView.dismissDropDown();
        if (provinceDropdown != null) provinceDropdown.dismissDropDown();
        if (cityDropdown != null) cityDropdown.dismissDropDown();
        if (barangayDropdown != null) barangayDropdown.dismissDropDown();
        
        // Clear focus from all dropdowns
        if (dropdownTextView != null) dropdownTextView.clearFocus();
        if (provinceDropdown != null) provinceDropdown.clearFocus();
        if (cityDropdown != null) cityDropdown.clearFocus();
        if (barangayDropdown != null) barangayDropdown.clearFocus();
        
        // Also clear focus from input fields
        if (nameProfileTxt != null) nameProfileTxt.clearFocus();
        if (emailProfileTxt != null) emailProfileTxt.clearFocus();
        if (streetAddressProfileTxt != null) streetAddressProfileTxt.clearFocus();
        
        // Force the parent to take focus
        View scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) scrollView.requestFocus();
        
        // Hide keyboard
        hideKeyboard();
    }

    private void hideKeyboard() {
        // Hide keyboard if showing
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Hide keyboard immediately
        hideKeyboard();
        
        // Hide dropdowns immediately
        hideAllDropdowns();
        
        // Post a delayed action to hide any open dropdowns after the screen is fully rendered
        rootLayout.postDelayed(() -> {
            hideAllDropdowns();
        }, 100); 
        
        // Post another delayed action for safety
        rootLayout.postDelayed(() -> {
            hideAllDropdowns();
        }, 300);
    }
    
    private void setupDropdowns() {
        // Set high threshold to prevent auto-popup
        dropdownTextView.setThreshold(Integer.MAX_VALUE);
        provinceDropdown.setThreshold(Integer.MAX_VALUE);
        cityDropdown.setThreshold(Integer.MAX_VALUE);
        barangayDropdown.setThreshold(Integer.MAX_VALUE);
        
        // Set up province dropdown listener
        provinceDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedProvince = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Province selected: " + selectedProvince);
                showSnackbar("Selected province/state: " + selectedProvince);
                updateCityDropdown(selectedProvince);
            }
        });
        
        // Set up city dropdown listener
        cityDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "City selected: " + selectedCity);
                showSnackbar("Selected city: " + selectedCity);
                updateBarangayDropdown(selectedCity);
            }
        });
        
        // Set up barangay dropdown listener
        barangayDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedBarangay = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Barangay selected: " + selectedBarangay);
                showSnackbar("Selected barangay/neighborhood: " + selectedBarangay);
                updatePostalCode(selectedBarangay);
            }
        });
        
        // Add a listener for province changes to handle non-click edits
        provinceDropdown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // This catches manual text entry or programmatic changes
                if (s != null && s.length() > 0 && provinceDropdown.isEnabled()) {
                    String province = s.toString();
                    // Only update if it's a valid option to avoid recursive calls
                    ArrayList<String> provinces = locationDataProvider.getProvinces(dropdownTextView.getText().toString());
                    if (provinces != null && provinces.contains(province)) {
                        Log.d(TAG, "Province text changed to: " + province);
                        updateCityDropdown(province);
                    }
                }
            }
        });
    }
    
    private void updateProvinceDropdown(String country) {
        // Clear dependent fields
        cityDropdown.setText("", false);
        cityDropdown.setEnabled(false);
        barangayDropdown.setText("", false);
        barangayDropdown.setEnabled(false);
        postalProfileTxt.setText("");
        
        // Load province options and show dropdown (for user interaction)
        loadProvinceOptions(country, true);
        
        if (provinceDropdown.isEnabled()) {
            showSnackbar("Please select a province/state");
        } else {
            showSnackbar("No provinces/states available for " + country);
        }
    }
    
    private void updateCityDropdown(String province) {
        // Clear dependent fields
        barangayDropdown.setText("", false);
        barangayDropdown.setEnabled(false);
        postalProfileTxt.setText("");
        
        // Load city options and show dropdown (for user interaction)
        loadCityOptions(province, true);
        
        if (cityDropdown.isEnabled()) {
            showSnackbar("Please select a city");
        } else {
            showSnackbar("No cities available for " + province);
        }
    }
    
    private void updateBarangayDropdown(String city) {
        // Clear postal code
        postalProfileTxt.setText("");
        
        // Load barangay options and show dropdown (for user interaction)
        loadBarangayOptions(city, true);
        
        if (barangayDropdown.isEnabled()) {
            showSnackbar("Please select a barangay/neighborhood");
        } else {
            showSnackbar("No barangays/neighborhoods available for " + city);
        }
    }
    
    private void updatePostalCode(String barangay) {
        String postalCode = locationDataProvider.getPostalCode(barangay);
        Log.d(TAG, "Looking up postal code for: " + barangay);
        
        if (postalCode != null && !postalCode.isEmpty()) {
            Log.d(TAG, "Found postal code: " + postalCode + " for " + barangay);
            postalProfileTxt.setText(postalCode);
            showSnackbar("Postal code updated automatically");
        } else {
            Log.d(TAG, "No postal code found for " + barangay);
            postalProfileTxt.setText("");
            showSnackbar("No postal code available for this location");
        }
    }

    // Helper method to show Snackbar messages
    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        snackbar.show();
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
            showSnackbar("Profile picture selected");
        }
    }

    // Load Profile Data from Firebase
    private void loadProfileData() {
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Load basic info
                nameProfileTxt.setText(documentSnapshot.getString("Name"));
                emailProfileTxt.setText(documentSnapshot.getString("Email"));
                streetAddressProfileTxt.setText(documentSnapshot.getString("streetAddress"));
                
                // Get address data
                String country = documentSnapshot.getString("country");
                String province = documentSnapshot.getString("province");
                String city = documentSnapshot.getString("city");
                String barangay = documentSnapshot.getString("barangay");
                
                Log.d(TAG, "Loading saved profile data:");
                Log.d(TAG, "  Country: " + country);
                Log.d(TAG, "  Province: " + province);
                Log.d(TAG, "  City: " + city);
                Log.d(TAG, "  Barangay: " + barangay);
                
                // We need to set these in order with delays to let dropdown data load properly
                if (country != null && !country.equals("N/A")) {
                    // Set country first without showing dropdown
                    dropdownTextView.setText(country, false);
                    
                    // Need to slightly delay province loading to ensure adapters update properly
                    dropdownTextView.post(() -> {
                        // Prevent dropdown from showing
                        dropdownTextView.clearFocus();
                        dropdownTextView.dismissDropDown();
                        
                        // Load provinces for this country without showing dropdown
                        loadProvinceOptions(country, false);
                        
                        // Set province if available
                        if (province != null && !province.equals("N/A")) {
                            provinceDropdown.setText(province, false);
                            
                            // Need another slight delay for city loading
                            provinceDropdown.post(() -> {
                                // Prevent dropdown from showing
                                provinceDropdown.clearFocus();
                                provinceDropdown.dismissDropDown();
                                
                                // Load cities for this province without showing dropdown
                                loadCityOptions(province, false);
                                
                                // Set city if available
                                if (city != null && !city.equals("N/A")) {
                                    cityDropdown.setText(city, false);
                                    
                                    // Need another slight delay for barangay loading
                                    cityDropdown.post(() -> {
                                        // Prevent dropdown from showing
                                        cityDropdown.clearFocus();
                                        cityDropdown.dismissDropDown();
                                        
                                        // Load barangays for this city without showing dropdown
                                        loadBarangayOptions(city, false);
                                        
                                        // Set barangay if available
                                        if (barangay != null && !barangay.equals("N/A")) {
                                            barangayDropdown.setText(barangay, false);
                                            
                                            // Prevent dropdown from showing
                                            barangayDropdown.clearFocus();
                                            barangayDropdown.dismissDropDown();
                                            
                                            // Update postal code
                                            barangayDropdown.post(() -> {
                                                updatePostalCode(barangay);
                                                // Final focus cleanup after all data is loaded
                                                hideAllDropdowns();
                                            });
                                        } else {
                                            // Try to load postal code directly if available
                                            postalProfileTxt.setText(documentSnapshot.getString("postalCode"));
                                            // Final focus cleanup
                                            hideAllDropdowns();
                                        }
                                    });
                                } else {
                                    // Final focus cleanup if we don't have a city
                                    hideAllDropdowns();
                                }
                            });
                        } else {
                            // Final focus cleanup if we don't have a province
                            hideAllDropdowns();
                        }
                    });
                }

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
                
//                showSnackbar("Profile data loaded successfully");
            }
        }).addOnFailureListener(e -> {
            showSnackbar("Failed to load profile data!");
        });
    }

    // Modified method to load province options without automatically showing dropdown
    private void loadProvinceOptions(String country, boolean showDropdown) {
        ArrayList<String> provinces = locationDataProvider.getProvinces(country);
        Log.d(TAG, "Loading provinces for country: " + country);
        
        if (provinces != null && !provinces.isEmpty()) {
            Log.d(TAG, "Found " + provinces.size() + " provinces for " + country);
            
            // Create and set adapter with custom layout
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item_layout, provinces);
            provinceDropdown.setAdapter(adapter);
            provinceDropdown.setEnabled(true);
            
            // Ensure correct styling and background
            provinceDropdown.setDropDownBackgroundResource(android.R.color.white);
            
            // Only show dropdown if requested (for user interaction, not loading)
            if (showDropdown) {
                provinceDropdown.showDropDown();
            }
        } else {
            // No provinces for this country
            Log.d(TAG, "No provinces found for " + country);
            provinceDropdown.setEnabled(false);
            provinceDropdown.setText("", false);
        }
    }
    
    // Modified method to load city options without automatically showing dropdown
    private void loadCityOptions(String province, boolean showDropdown) {
        ArrayList<String> cities = locationDataProvider.getCities(province);
        Log.d(TAG, "Loading cities for province: " + province);
        
        if (cities != null && !cities.isEmpty()) {
            Log.d(TAG, "Found " + cities.size() + " cities for " + province);
            
            // Create and set adapter with custom layout
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item_layout, cities);
            cityDropdown.setAdapter(adapter);
            cityDropdown.setEnabled(true);
            
            // Ensure correct styling and background
            cityDropdown.setDropDownBackgroundResource(android.R.color.white);
            
            // Only show dropdown if requested (for user interaction, not loading)
            if (showDropdown) {
                cityDropdown.showDropDown();
            }
        } else {
            // No cities for this province
            Log.d(TAG, "No cities found for " + province);
            cityDropdown.setEnabled(false);
            cityDropdown.setText("", false);
        }
    }
    
    // Modified method to load barangay options without automatically showing dropdown
    private void loadBarangayOptions(String city, boolean showDropdown) {
        ArrayList<String> barangays = locationDataProvider.getNeighborhoods(city);
        Log.d(TAG, "Loading barangays for city: " + city);
        
        if (barangays != null && !barangays.isEmpty()) {
            Log.d(TAG, "Found " + barangays.size() + " barangays for " + city);
            
            // Create and set adapter with custom layout
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item_layout, barangays);
            barangayDropdown.setAdapter(adapter);
            barangayDropdown.setEnabled(true);
            
            // Ensure correct styling and background
            barangayDropdown.setDropDownBackgroundResource(android.R.color.white);
            
            // Only show dropdown if requested (for user interaction, not loading)
            if (showDropdown) {
                barangayDropdown.showDropDown();
            }
        } else {
            // No barangays for this city
            Log.d(TAG, "No barangays found for " + city);
            barangayDropdown.setEnabled(false);
            barangayDropdown.setText("", false);
        }
    }

    // Save Profile Data to Firebase
    private void saveProfileData() {
        String name = nameProfileTxt.getText().toString().trim();
        String email = emailProfileTxt.getText().toString().trim();
        String streetAddress = streetAddressProfileTxt.getText().toString().trim();
        String city = cityDropdown.getText().toString().trim();
        String province = provinceDropdown.getText().toString().trim();
        String barangay = barangayDropdown.getText().toString().trim();
        String postalCode = postalProfileTxt.getText().toString().trim();
        String country = dropdownTextView.getText().toString().trim();

        // Save data using HashMap
        Map<String, Object> userData = new HashMap<>();
        userData.put("Name", name.isEmpty() ? "N/A" : name);
        userData.put("Email", email.isEmpty() ? "N/A" : email);
        userData.put("streetAddress", streetAddress.isEmpty() ? "N/A" : streetAddress);
        userData.put("city", city.isEmpty() ? "N/A" : city);
        userData.put("province", province.isEmpty() ? "N/A" : province);
        userData.put("barangay", barangay.isEmpty() ? "N/A" : barangay);
        userData.put("postalCode", postalCode.isEmpty() ? "N/A" : postalCode);
        userData.put("country", country.isEmpty() ? "N/A" : country);

        if (selectedImageUri != null) {
            uploadImageToFirebase(userData);
        } else {
            userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        showSnackbar("Profile updated successfully!");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showSnackbar("Update failed: " + e.getMessage());
                    });
        }
    }

    // Upload Profile Image
    private void uploadImageToFirebase(Map<String, Object> userData) {
        if (imageUri == null) {
            showSnackbar("No image selected!");
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
                                    showSnackbar("Profile updated successfully!");
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    showSnackbar("Update failed: " + e.getMessage());
                                });
                    } else {
                        showSnackbar("Failed to get image URL!");
                    }
                }))
                .addOnFailureListener(e -> {
                    showSnackbar("Upload failed: " + e.getMessage());
                });
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
                            showSnackbar("Profile image removed");
                        })
                        .addOnFailureListener(e -> {
                            showSnackbar("Failed to remove image URL!");
                        });
            } else {
                showSnackbar("No image found or failed to remove!");
            }
        });
    }

    // Initialize country dropdown without showing it
    private void initializeCountryDropdown() {
        // Country dropdown setup - get countries from the provider
        ArrayList<String> countries = locationDataProvider.getCountries();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item_layout, countries);
        
        // Set threshold to a high number to prevent automatic filtering dropdown
        dropdownTextView.setThreshold(Integer.MAX_VALUE);
        
        // Set adapter and ensure white background
        dropdownTextView.setAdapter(adapter);
        dropdownTextView.setDropDownBackgroundResource(android.R.color.white);
        
        // Ensure dropdown doesn't show automatically
        dropdownTextView.dismissDropDown();
        dropdownTextView.clearFocus();
        
        // Explicitly clear focus and hide keyboard
        hideKeyboard();
        
        // Add listener for country selection to update provinces
        dropdownTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = parent.getItemAtPosition(position).toString();
                showSnackbar("Selected country: " + selectedCountry);
                updateProvinceDropdown(selectedCountry);
            }
        });
    }
}
