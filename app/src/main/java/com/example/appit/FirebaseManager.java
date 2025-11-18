package com.example.appit;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final DatabaseReference databaseReference;

    private FirebaseManager() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public DatabaseReference getProductsReference() {
        return databaseReference.child("products");
    }

    public DatabaseReference getUsersReference() {
        return databaseReference.child("users");
    }

    public DatabaseReference getUserCartReference(String uid) {
        return getUsersReference().child(uid).child("cart");
    }
}
