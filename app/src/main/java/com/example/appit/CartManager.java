package com.example.appit;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class CartManager {

    private static final String TAG = "CartManager";
    private static CartManager instance;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference userDocRef;
    private ListenerRegistration cartListener;

    private CartManager() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userDocRef = db.collection("users").document(currentUser.getUid());
        }
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void loadCartItems(Consumer<List<CartItem>> onCartLoaded) {
        if (userDocRef == null) return;

        cartListener = userDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                onCartLoaded.accept(new ArrayList<>());
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Object cartObject = snapshot.get("cart");

                if (cartObject instanceof List) {
                    migrateCartFromListToMap((List<String>) cartObject, onCartLoaded);
                    return;
                }

                if (cartObject instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Long> cartData = (Map<String, Long>) cartObject;
                    fetchProductDetails(cartData, onCartLoaded);
                } else {
                    onCartLoaded.accept(new ArrayList<>());
                }
            } else {
                 onCartLoaded.accept(new ArrayList<>());
            }
        });
    }

    private void migrateCartFromListToMap(List<String> oldCart, Consumer<List<CartItem>> onCartLoaded) {
        Map<String, Long> newCart = new HashMap<>();
        for (String productId : oldCart) {
            newCart.put(productId, 1L);
        }
        userDocRef.update("cart", newCart).addOnSuccessListener(aVoid -> {
            fetchProductDetails(newCart, onCartLoaded);
        });
    }

    private void fetchProductDetails(Map<String, Long> cartData, Consumer<List<CartItem>> onCartLoaded) {
        cartItems.clear();
        if (cartData.isEmpty()) {
            onCartLoaded.accept(new ArrayList<>());
            return;
        }

        CollectionReference productsRef = db.collection("products");
        List<Long> numericProductIds = new ArrayList<>();
        for (String id : cartData.keySet()) {
            try {
                numericProductIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid product ID in cart: " + id);
            }
        }

        if(numericProductIds.isEmpty()){
             onCartLoaded.accept(new ArrayList<>());
            return;
        }
        
        productsRef.whereIn("id", numericProductIds).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                 cartItems.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    try {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            product.setDocumentId(doc.getId());
                            int quantity = Objects.requireNonNull(cartData.get(String.valueOf(product.getId()))).intValue();
                            cartItems.add(new CartItem(product, quantity));
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error converting product document: " + doc.getId(), ex);
                    }
                }
            }
            onCartLoaded.accept(new ArrayList<>(cartItems));
        });
    }

    public void addProduct(Product product) {
        if (userDocRef == null || product.getId() == null) return;
        String fieldPath = "cart." + product.getId();
        userDocRef.update(fieldPath, FieldValue.increment(1));
    }

    public void updateQuantity(Product product, int newQuantity) {
        if (userDocRef == null || newQuantity <= 0 || product.getId() == null) return;
        String fieldPath = "cart." + product.getId();
        userDocRef.update(fieldPath, newQuantity);
    }

    public void removeProduct(Product product) {
        if (userDocRef == null || product.getId() == null) return;
        String fieldPath = "cart." + product.getId();
        userDocRef.update(fieldPath, FieldValue.delete());
    }

    public void clearAllCart() {
        if (userDocRef == null) return;
        userDocRef.update("cart", new HashMap<>());
    }
    
    public void clearPurchasedItems(List<CartItem> itemsToClear) {
        if (userDocRef == null) return;
        WriteBatch batch = db.batch();
        for (CartItem item : itemsToClear) {
            String fieldPath = "cart." + item.getProduct().getId();
            batch.update(userDocRef, fieldPath, FieldValue.delete());
        }
        batch.commit();
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public List<CartItem> getSelectedItems() {
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    public int getTotalItemCount() {
        return cartItems.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double calculateTotalPrice() {
        double total = 0;
        for (CartItem item : getSelectedItems()) {
            try {
                String priceString = item.getProduct().getPrice().replaceAll("[^\\d]", "");
                double price = Double.parseDouble(priceString);
                total += price * item.getQuantity(); 
            } catch (Exception e) {
                Log.e(TAG, "Error calculating total price", e);
            }
        }
        return total;
    }

    public static void destroyInstance() {
        if (instance != null) {
            if (instance.cartListener != null) {
                instance.cartListener.remove();
            }
            instance = null;
        }
    }
}
