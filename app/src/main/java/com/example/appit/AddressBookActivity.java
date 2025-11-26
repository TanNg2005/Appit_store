package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AddressBookActivity extends BaseActivity implements AddressBookAdapter.OnAddressActionListener {

    private RecyclerView recyclerView;
    private AddressBookAdapter adapter;
    private List<User.ShippingAddress> addressList = new ArrayList<>();
    private TextView textEmpty;
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private boolean isSelectionMode = false; // Chế độ chọn địa chỉ (từ Cart/Payment)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        // Kiểm tra nếu được gọi để chọn địa chỉ
        if (getIntent().hasExtra("SELECT_MODE")) {
            isSelectionMode = getIntent().getBooleanExtra("SELECT_MODE", false);
        }

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = db.collection("users").document(currentUser.getUid());
        }

        Toolbar toolbar = findViewById(R.id.address_book_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(isSelectionMode ? R.string.address_book_select_title : R.string.address_book_title);

        recyclerView = findViewById(R.id.recycler_view_addresses);
        textEmpty = findViewById(R.id.text_empty_address);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add_address);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressBookAdapter(this, addressList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddressDialog(null, -1));

        loadAddresses();
    }

    private void loadAddresses() {
        if (userRef == null) return;
        userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            User user = snapshot.toObject(User.class);
            if (user != null && user.getShippingAddresses() != null) {
                addressList.clear();
                addressList.addAll(user.getShippingAddresses());
                adapter.notifyDataSetChanged();
                
                if (addressList.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showAddressDialog(User.ShippingAddress existingAddress, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_address_full, null);
        builder.setView(dialogView);
        // Không set title và buttons mặc định của AlertDialog nữa vì mình sẽ dùng custom layout
        
        final AlertDialog dialog = builder.create();

        // Tìm các view trong dialogView
        // Bỏ dòng này vì trong layout không có ID này: TextView title = dialogView.findViewById(R.id.address_dialog_title); 
        
        EditText editName = dialogView.findViewById(R.id.edit_recipient_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_recipient_phone);
        EditText editStreet = dialogView.findViewById(R.id.edit_street);
        EditText editDistrict = dialogView.findViewById(R.id.edit_district);
        EditText editCity = dialogView.findViewById(R.id.edit_city);
        CheckBox cbDefault = dialogView.findViewById(R.id.cb_set_default);
        Button btnSave = dialogView.findViewById(R.id.btn_save_address);

        // Set data
        if (existingAddress != null) {
            editName.setText(existingAddress.getRecipientName());
            editPhone.setText(existingAddress.getPhone());
            editStreet.setText(existingAddress.getStreet());
            editDistrict.setText(existingAddress.getDistrict());
            editCity.setText(existingAddress.getCity());
            cbDefault.setChecked(existingAddress.isDefault());
        } else {
            if (addressList.isEmpty()) cbDefault.setChecked(true);
        }

        // Xử lý sự kiện click nút Lưu
        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String street = editStreet.getText().toString().trim();
            String district = editDistrict.getText().toString().trim();
            String city = editCity.getText().toString().trim();
            boolean isDefault = cbDefault.isChecked();

            if (name.isEmpty() || phone.isEmpty() || street.isEmpty() || district.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            User.ShippingAddress newAddress = new User.ShippingAddress();
            newAddress.setRecipientName(name);
            newAddress.setPhone(phone);
            newAddress.setStreet(street);
            newAddress.setDistrict(district);
            newAddress.setCity(city);
            newAddress.setDefault(isDefault);

            saveAddress(newAddress, position);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveAddress(User.ShippingAddress newAddress, int position) {
        List<User.ShippingAddress> updatedList = new ArrayList<>(addressList);

        if (newAddress.isDefault()) {
            // Bỏ default của các địa chỉ khác
            for (User.ShippingAddress addr : updatedList) {
                addr.setDefault(false);
            }
        }

        if (position >= 0) {
            // Edit
            updatedList.set(position, newAddress);
        } else {
            // Add new
            updatedList.add(newAddress);
        }

        // Nếu chỉ có 1 địa chỉ, luôn là default
        if (updatedList.size() == 1) {
            updatedList.get(0).setDefault(true);
        }

        userRef.update("shippingAddresses", updatedList)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onEdit(int position, User.ShippingAddress address) {
        showAddressDialog(address, position);
    }

    @Override
    public void onDelete(int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.address_delete_title)
                .setMessage(R.string.address_delete_message)
                .setPositiveButton(R.string.address_delete_confirm, (dialog, which) -> {
                    List<User.ShippingAddress> updatedList = new ArrayList<>(addressList);
                    updatedList.remove(position);
                    
                    // Nếu xóa địa chỉ mặc định và còn địa chỉ khác, set cái đầu tiên làm mặc định
                    if (!updatedList.isEmpty()) {
                         boolean hasDefault = false;
                         for(User.ShippingAddress addr : updatedList) {
                             if(addr.isDefault()) {
                                 hasDefault = true;
                                 break;
                             }
                         }
                         if(!hasDefault) updatedList.get(0).setDefault(true);
                    }

                    userRef.update("shippingAddresses", updatedList)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.address_cancel, null)
                .show();
    }

    @Override
    public void onSetDefault(int position) {
        List<User.ShippingAddress> updatedList = new ArrayList<>(addressList);
        for (int i = 0; i < updatedList.size(); i++) {
            updatedList.get(i).setDefault(i == position);
        }
        userRef.update("shippingAddresses", updatedList)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã đặt làm mặc định", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSelect(int position, User.ShippingAddress address) {
        if (isSelectionMode) {
            // Trả kết quả về activity gọi (CartActivity hoặc PaymentActivity)
            // Tuy nhiên, hiện tại cấu trúc User lưu địa chỉ, nên khi chọn xong
            // ta có thể update địa chỉ này làm Default tạm thời hoặc truyền object về
            
            // Đơn giản nhất: Set default và finish
            onSetDefault(position);
            
            // Hoặc truyền data về nếu activity kia cần lấy ngay mà ko cần reload user
            // Intent resultIntent = new Intent();
            // resultIntent.putExtra("SELECTED_ADDRESS_INDEX", position);
            // setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
