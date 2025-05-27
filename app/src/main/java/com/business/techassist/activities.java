package com.business.techassist;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.business.techassist.admin_utils.AppointmentModel;
import com.business.techassist.transactionhistory.Transaction;
import com.business.techassist.transactionhistory.TransactionAdapter;
import com.business.techassist.transactionhistory.TransactionHistoryActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import androidx.appcompat.app.AlertDialog;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class activities extends Fragment implements TopServicesAdapter.OnServiceClickListener, TransactionAdapter.OnTransactionClickListener, UserAppointmentAdapter.OnAppointmentActionListener {

    private static final String TAG = "ActivitiesFragment";
    private RecyclerView transactionsRecyclerView;
    private RecyclerView servicesRecyclerView;
    private RecyclerView appointmentsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private TopServicesAdapter topServicesAdapter;
    private UserAppointmentAdapter appointmentAdapter;
    private TextView emptyTransactionsView;
    private TextView emptyServicesView;
    private TextView emptyAppointmentsView;
    private MaterialCardView viewAllTransactionsCard;
    private MaterialCardView viewAllAppointmentsCard;
    private Button bookServiceButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    // Maintain a list of active Firestore listeners to clean up
    private List<ListenerRegistration> firestoreListeners = new ArrayList<>();
    
    // List to store appointments
    private List<AppointmentModel> appointmentsList = new ArrayList<>();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public activities() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static activities newInstance(String param1, String param2) {
        activities fragment = new activities();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activities, container, false);
        
        // Initialize views
        transactionsRecyclerView = view.findViewById(R.id.transactionRecyclerView);
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView);
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsRecyclerView);
        emptyTransactionsView = view.findViewById(R.id.emptyTransactionsState);
        emptyServicesView = view.findViewById(R.id.emptyServicesState);
        emptyAppointmentsView = view.findViewById(R.id.emptyAppointmentsState);
        viewAllTransactionsCard = view.findViewById(R.id.viewAllTransactionsCard);
        viewAllAppointmentsCard = view.findViewById(R.id.viewAllAppointmentsCard);
        bookServiceButton = view.findViewById(R.id.bookServiceButton);
        
        // Set up SwipeRefreshLayout for pull-to-refresh functionality
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.secondary,
                R.color.black);
        
        // Set up transactions section with real-time adapter
        setupTransactionRecyclerView();
        
        // Set up services section with real-time adapter
        setupTopServicesRecyclerView();
        
        // Set up appointments section
        setupAppointmentsRecyclerView();
        
        // Set up click listeners
        setupClickListeners();
        
        return view;
    }
    
    /**
     * Refresh data in both recycler views
     */
    private void refreshData() {
        // Clear any existing listeners to avoid duplicates
        clearListeners();
        
        // Reload data for all sections
        setupTransactionRecyclerView();
        setupTopServicesRecyclerView();
        setupAppointmentsRecyclerView();
        
        // Show a message
        if (getView() != null) {
          //  Snackbar.make(getView(), "Activities refreshed", Snackbar.LENGTH_SHORT).show();
        }
        
        // Stop the refresh animation
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
    
    /**
     * Clear all active Firestore listeners to avoid memory leaks
     */
    private void clearListeners() {
        for (ListenerRegistration registration : firestoreListeners) {
            registration.remove();
        }
        firestoreListeners.clear();
        
        // Also stop the adapters from listening if they exist
        if (transactionAdapter != null) {
            transactionAdapter.stopListening();
        }
    }
    
    private void setupClickListeners() {
        viewAllTransactionsCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TransactionHistoryActivity.class);
            startActivity(intent);
        });
        
        viewAllAppointmentsCard.setOnClickListener(v -> {
            // For now, just show all appointments in a dialog
            showAllAppointmentsDialog();
        });
        
        bookServiceButton.setOnClickListener(v -> {
            // Open service booking dialog or activity
            showServiceBookingDialog();
        });
    }
    
    private void showAllAppointmentsDialog() {
        if (getActivity() == null) return;
        
        // Create a dialog to show all appointments with light theme
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("All Appointments");
        
        // Inflate the layout with a RecyclerView
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_all_appointments, null);
        RecyclerView appointmentsRecyclerView = dialogView.findViewById(R.id.allAppointmentsRecyclerView);
        TextView emptyView = dialogView.findViewById(R.id.emptyAppointmentsText);
        
        // Set up RecyclerView
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        UserAppointmentAdapter adapter = new UserAppointmentAdapter(getActivity(), appointmentsList, this);
        appointmentsRecyclerView.setAdapter(adapter);
        
        // Show empty view if needed
        if (appointmentsList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            appointmentsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            appointmentsRecyclerView.setVisibility(View.VISIBLE);
        }
        
        builder.setView(dialogView);
        builder.setPositiveButton("Close", null);
        AlertDialog dialog = builder.create();
        
        // Force dialog background to be white
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }
        
        dialog.show();
    }
    
    private void showServiceBookingDialog() {
        if (getActivity() == null) return;
        
        // Only fetch distinct services by ID from Firestore
        db.collection("services")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // Create a list to store unique services (preventing duplicates)
                    Map<String, DocumentSnapshot> uniqueServicesMap = new HashMap<>();
                    
                    // Process all services to ensure uniqueness by name
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String serviceName = doc.getString("name");
                        if (serviceName != null && !uniqueServicesMap.containsKey(serviceName)) {
                            uniqueServicesMap.put(serviceName, doc);
                        }
                    }
                    
                    // Convert map to list
                    List<DocumentSnapshot> uniqueServices = new ArrayList<>(uniqueServicesMap.values());
                    
                    // Create and show service selection dialog with unique services
                    ServiceBookingDialog dialog = new ServiceBookingDialog(
                            getActivity(),
                            uniqueServices
                    );
                    dialog.show();
                    
                    // Log service count
                    Log.d(TAG, "Found " + uniqueServices.size() + " unique services (from " + 
                        queryDocumentSnapshots.size() + " total services)");
                } else {
                    Toast.makeText(getActivity(), "No services found in the database", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching services", e);
                Toast.makeText(getActivity(), "Failed to load services: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void setupTransactionRecyclerView() {
        if (currentUser == null) {
            transactionsRecyclerView.setVisibility(View.GONE);
            emptyTransactionsView.setVisibility(View.VISIBLE);
            emptyTransactionsView.setText("Please sign in to view your transactions");
            viewAllTransactionsCard.setVisibility(View.GONE);
            return;
        }
        
        try {
            // Clear previous adapter
            if (transactionAdapter != null) {
                transactionAdapter.stopListening();
                transactionsRecyclerView.setAdapter(null);
            }
            
            // Always limit to last 3 transactions for the summary view
            Query query = db.collection("Users")
                    .document(currentUser.getUid())
                    .collection("Transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(3); // Always show only 3 recent transactions
            
            Log.d(TAG, "Setting up recent transactions view with limit of 3");
            
            FirestoreRecyclerOptions<Transaction> options = new FirestoreRecyclerOptions.Builder<Transaction>()
                    .setQuery(query, Transaction.class)
                    .build(); // Remove lifecycle owner to manage listening manually
            
            transactionAdapter = new TransactionAdapter(options);
            transactionAdapter.setOnTransactionClickListener(this);
            
            // Prevent inconsistencies with fixed size
            transactionsRecyclerView.setHasFixedSize(true);
            transactionsRecyclerView.setItemAnimator(null); // Disable animations to prevent glitches
            transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            transactionsRecyclerView.setAdapter(transactionAdapter);
            
            // Initially hide both until we have data
            transactionsRecyclerView.setVisibility(View.GONE);
            emptyTransactionsView.setVisibility(View.VISIBLE);
            viewAllTransactionsCard.setVisibility(View.GONE);
            
            // Register adapter data observer
            transactionAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    checkEmptyTransactionsState();
                }
                
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    checkEmptyTransactionsState();
                }
                
                @Override
                public void onChanged() {
                    checkEmptyTransactionsState();
                }
            });
            
            // Start listening
            transactionAdapter.startListening();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up transaction recycler view", e);
            transactionsRecyclerView.setVisibility(View.GONE);
            emptyTransactionsView.setVisibility(View.VISIBLE);
            emptyTransactionsView.setText("Error loading transactions");
            viewAllTransactionsCard.setVisibility(View.GONE);
        }
    }
    
    private void setupAppointmentsRecyclerView() {
        if (currentUser == null) {
            appointmentsRecyclerView.setVisibility(View.GONE);
            emptyAppointmentsView.setVisibility(View.VISIBLE);
            emptyAppointmentsView.setText("Please sign in to view your appointments");
            viewAllAppointmentsCard.setVisibility(View.GONE);
            return;
        }
        
        // Initially show loading state
        appointmentsRecyclerView.setVisibility(View.GONE);
        emptyAppointmentsView.setVisibility(View.VISIBLE);
        emptyAppointmentsView.setText("Loading appointments...");
        viewAllAppointmentsCard.setVisibility(View.GONE);
        
        // Create adapter if not exists
        if (appointmentAdapter == null) {
            appointmentAdapter = new UserAppointmentAdapter(getActivity(), appointmentsList, this);
            appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            appointmentsRecyclerView.setAdapter(appointmentAdapter);
        }
        
        // Clear existing appointments
        appointmentsList.clear();
        
        try {
            // Query Firestore for user's appointments - modified to handle missing index
            // Option 1: Remove the ordering to avoid needing a composite index
            ListenerRegistration appointmentsListener = db.collection("Appointments")
                    .whereEqualTo("userId", currentUser.getUid())
                    // Removing the orderBy to avoid needing a composite index
                    .limit(10) // Increased limit since we'll sort locally
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Listen failed for appointments", e);
                            emptyAppointmentsView.setText("Error loading appointments");
                            appointmentsRecyclerView.setVisibility(View.GONE);
                            emptyAppointmentsView.setVisibility(View.VISIBLE);
                            viewAllAppointmentsCard.setVisibility(View.GONE);
                            return;
                        }
                        
                        // Clear previous appointments
                        appointmentsList.clear();
                        
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                AppointmentModel appointment = document.toObject(AppointmentModel.class);
                                if (appointment != null) {
                                    appointment.setId(document.getId());
                                    appointmentsList.add(appointment);
                                }
                            }
                            
                            // Sort appointments locally by date (descending)
                            appointmentsList.sort((a1, a2) -> {
                                if (a1.getAppointmentDate() == null && a2.getAppointmentDate() == null) {
                                    return 0;
                                } else if (a1.getAppointmentDate() == null) {
                                    return 1;
                                } else if (a2.getAppointmentDate() == null) {
                                    return -1;
                                }
                                return a2.getAppointmentDate().compareTo(a1.getAppointmentDate());
                            });
                            
                            // Limit to 3 most recent appointments (after sorting)
                            if (appointmentsList.size() > 3) {
                                appointmentsList = appointmentsList.subList(0, 3);
                            }
                            
                            // Show appointments
                            appointmentsRecyclerView.setVisibility(View.VISIBLE);
                            emptyAppointmentsView.setVisibility(View.GONE);
                            viewAllAppointmentsCard.setVisibility(View.VISIBLE);
                            
                            // Notify adapter
                            appointmentAdapter.notifyDataSetChanged();
                        } else {
                            // No appointments found
                            appointmentsRecyclerView.setVisibility(View.GONE);
                            emptyAppointmentsView.setVisibility(View.VISIBLE);
                            emptyAppointmentsView.setText("No appointments yet");
                            viewAllAppointmentsCard.setVisibility(View.GONE);
                        }
                    });
            
            // Store the listener to clean up later
            firestoreListeners.add(appointmentsListener);
            
            // Display instructions for creating the index if needed in the future
            Log.i(TAG, "If you want to use the composite index in the future, create it in the Firebase console");
            Log.i(TAG, "Collection: Appointments, Fields: userId (Ascending), appointmentDate (Descending)");
            
        } catch (Exception ex) {
            Log.e(TAG, "Error setting up appointments listener", ex);
            emptyAppointmentsView.setText("Error loading appointments");
            appointmentsRecyclerView.setVisibility(View.GONE);
            emptyAppointmentsView.setVisibility(View.VISIBLE);
            viewAllAppointmentsCard.setVisibility(View.GONE);
        }
    }
    
    private void setupTopServicesRecyclerView() {
        // Initially show loading state
        servicesRecyclerView.setVisibility(View.GONE);
        emptyServicesView.setVisibility(View.VISIBLE);
        emptyServicesView.setText("Loading services...");
        
        // Set up real-time listener for services - showing all services, not just active
        ListenerRegistration servicesListener = db.collection("services")
                .limit(3)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for service updates", e);
                        emptyServicesView.setText("Error loading services");
                        servicesRecyclerView.setVisibility(View.GONE);
                        emptyServicesView.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<DocumentSnapshot> services = queryDocumentSnapshots.getDocuments();
                        
                        // Create and set adapter
                        topServicesAdapter = new TopServicesAdapter(getActivity(), services);
                        topServicesAdapter.setOnServiceClickListener(this);
                        
                        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                        servicesRecyclerView.setAdapter(topServicesAdapter);
                        
                        // Show the RecyclerView and hide empty state
                        servicesRecyclerView.setVisibility(View.VISIBLE);
                        emptyServicesView.setVisibility(View.GONE);
                        
                        Log.d(TAG, "Loaded " + services.size() + " services");
                    } else {
                        // No services found
                        emptyServicesView.setText("No services available");
                        servicesRecyclerView.setVisibility(View.GONE);
                        emptyServicesView.setVisibility(View.VISIBLE);
                    }
                });
        
        // Store the listener to clean up later
        firestoreListeners.add(servicesListener);
    }
    
    private void checkEmptyTransactionsState() {
        if (transactionAdapter == null) return;
        
        // Check if adapter has items
        boolean isEmpty = transactionAdapter.getItemCount() == 0;
        
        // Show/hide views based on data state
        transactionsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyTransactionsView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        viewAllTransactionsCard.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            emptyTransactionsView.setText("No transactions yet");
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (transactionAdapter != null) {
            transactionAdapter.startListening();
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (transactionAdapter != null) {
            transactionAdapter.stopListening();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible again
        refreshData();
        
        // Set up refresh layout colors if needed
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.secondary,
                R.color.black);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up all Firestore listeners
        clearListeners();
    }
    
    // TopServicesAdapter click listeners
    @Override
    public void onServiceClick(DocumentSnapshot serviceSnapshot) {
        // Show service details - we'll reuse the booking dialog which already has all the details
        if (getActivity() != null) {
            List<DocumentSnapshot> singleService = new ArrayList<>();
            singleService.add(serviceSnapshot);
            
            ServiceBookingDialog dialog = new ServiceBookingDialog(getActivity(), singleService);
            dialog.show();
        }
    }
    
    @Override
    public void onBookClick(DocumentSnapshot serviceSnapshot) {
        // Handle booking directly
        if (getActivity() != null) {
            List<DocumentSnapshot> singleService = new ArrayList<>();
            singleService.add(serviceSnapshot);
            
            ServiceBookingDialog dialog = new ServiceBookingDialog(getActivity(), singleService);
            dialog.show();
        }
    }

    @Override
    public void onTransactionClick(DocumentSnapshot documentSnapshot, Transaction transaction) {
        if (getActivity() == null) return;
        
        // Create and show a dialog with transaction details
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);
        
        // Set up dialog views
        TextView titleView = dialogView.findViewById(R.id.transactionTitle);
        TextView amountView = dialogView.findViewById(R.id.transactionAmount);
        TextView dateView = dialogView.findViewById(R.id.transactionDate);
        TextView statusView = dialogView.findViewById(R.id.transactionStatus);
        TextView typeView = dialogView.findViewById(R.id.transactionType);
        TextView descriptionView = dialogView.findViewById(R.id.transactionDescription);
        TextView idView = dialogView.findViewById(R.id.transactionId);
        
        // Populate views with transaction data
        titleView.setText(transaction.getDescription() != null ? transaction.getDescription() : "Transaction");
        amountView.setText(String.format("₱%,.2f", transaction.getAmount()));
        
        // Format and set date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
        if (transaction.getTimestamp() != null) {
            try {
                dateView.setText(sdf.format(transaction.getTimestamp().toDate()));
            } catch (Exception e) {
                Log.e(TAG, "Error formatting timestamp: " + e.getMessage(), e);
                dateView.setText("Invalid date");
            }
        } else {
            dateView.setText("N/A");
        }
        
        statusView.setText(transaction.getStatus());
        typeView.setText(transaction.getType());
        descriptionView.setText(transaction.getDescription());
        idView.setText(documentSnapshot.getId());
        
        // Set status color based on status
        int bgColorRes;
        switch (transaction.getStatus().toLowerCase()) {
            case "completed":
                bgColorRes = R.color.success;
                break;
            case "pending":
                bgColorRes = R.color.warning;
                break;
            case "failed":
                bgColorRes = R.color.error;
                break;
            case "cancelled":
                bgColorRes = R.color.textSecondary;
                break;
            default:
                bgColorRes = R.color.gray;
                break;
        }
        statusView.getBackground().setTint(getActivity().getColor(bgColorRes));
        
        // Show the dialog
        builder.setView(dialogView)
               .setPositiveButton("Close", null)
               .create()
               .show();
    }
    
    // UserAppointmentAdapter callbacks
    @Override
    public void onViewDetails(AppointmentModel appointment) {
        if (getActivity() == null) return;
        
        // Create and show appointment details dialog with light theme
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_appointment_details, null);
        
        // Set up views
        TextView serviceTypeView = dialogView.findViewById(R.id.appointmentServiceType);
        TextView dateTimeView = dialogView.findViewById(R.id.appointmentDateTime);
        TextView statusView = dialogView.findViewById(R.id.appointmentStatus);
        TextView priceView = dialogView.findViewById(R.id.appointmentPrice);
        TextView notesView = dialogView.findViewById(R.id.appointmentNotes);
        TextView paymentMethodView = dialogView.findViewById(R.id.paymentMethod);
        TextView appointmentIdView = dialogView.findViewById(R.id.appointmentId);
        
        // Populate views
        serviceTypeView.setText(appointment.getServiceType());
        dateTimeView.setText(appointment.getFormattedDateTime());
        statusView.setText(appointment.getStatus());
        
        // Format price
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        priceView.setText(currencyFormat.format(appointment.getDiscountedPrice() > 0 ? 
                appointment.getDiscountedPrice() : appointment.getBasePrice()));
        
        // Show discount if applicable
        if (appointment.getDiscountPercentage() > 0) {
            priceView.append(" (" + appointment.getDiscountPercentage() + "% discount applied)");
        }
        
        // Set notes text or show placeholder
        if (appointment.getNotes() != null && !appointment.getNotes().isEmpty()) {
            notesView.setText(appointment.getNotes());
        } else {
            notesView.setText("No additional notes");
        }
        
        // Set payment method
        paymentMethodView.setText(appointment.getPaymentMethod() != null ? 
                appointment.getPaymentMethod() : "Not specified");
        
        // Set appointment ID
        appointmentIdView.setText(appointment.getId());
        
        // Set status color
        int statusColor;
        switch (appointment.getStatus().toLowerCase()) {
            case "completed":
                statusColor = getActivity().getColor(R.color.success);
                break;
            case "cancelled":
                statusColor = getActivity().getColor(R.color.error);
                break;
            case "in progress":
                statusColor = getActivity().getColor(R.color.warning);
                break;
            default: // Pending
                statusColor = getActivity().getColor(R.color.primary);
                break;
        }
        statusView.getBackground().setTint(statusColor);
        
        // Show dialog
        builder.setTitle("Appointment Details")
               .setView(dialogView)
               .setPositiveButton("Close", null);
        
        // Add cancel button if appointment is not completed or cancelled
        if (!"completed".equalsIgnoreCase(appointment.getStatus()) && 
            !"cancelled".equalsIgnoreCase(appointment.getStatus())) {
            builder.setNegativeButton("Cancel Appointment", (dialog, which) -> {
                // Handle appointment cancellation
                cancelAppointment(appointment);
            });
        }
        
        AlertDialog dialog = builder.create();
        
        // Force dialog background to be white
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }
        
        dialog.show();
    }
    
    @Override
    public void onCancelAppointment(AppointmentModel appointment, int position) {
        cancelAppointment(appointment);
    }
    
    private void cancelAppointment(AppointmentModel appointment) {
        if (getActivity() == null) return;
        
        // Show confirmation dialog
        new AlertDialog.Builder(getActivity())
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                // Show progress dialog
                ProgressDialog progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage("Cancelling appointment...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                
                // Update appointment status in Firestore
                db.collection("Appointments").document(appointment.getId())
                    .update(
                        "status", "Cancelled",
                        "cancelledAt", Timestamp.now()
                    )
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        // Show success message
                        Snackbar.make(getView(), "Appointment cancelled successfully", Snackbar.LENGTH_LONG).show();
                        // Refresh appointments
                        setupAppointmentsRecyclerView();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        // Show error message
                        Snackbar.make(getView(), "Failed to cancel appointment: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                        Log.e(TAG, "Error cancelling appointment", e);
                    });
            })
            .setNegativeButton("No", null)
            .show();
    }
}