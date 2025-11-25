package com.example.appit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AddressBookAdapter extends RecyclerView.Adapter<AddressBookAdapter.ViewHolder> {

    private final Context context;
    private final List<User.ShippingAddress> addressList;
    private final OnAddressActionListener listener;

    public interface OnAddressActionListener {
        void onEdit(int position, User.ShippingAddress address);
        void onDelete(int position);
        void onSetDefault(int position);
        void onSelect(int position, User.ShippingAddress address);
    }

    public AddressBookAdapter(Context context, List<User.ShippingAddress> addressList, OnAddressActionListener listener) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User.ShippingAddress address = addressList.get(position);

        holder.recipientName.setText(address.getRecipientName());
        holder.recipientPhone.setText(address.getPhone());
        holder.addressDetail.setText(String.format("%s, %s, %s", address.getStreet(), address.getDistrict(), address.getCity()));

        if (address.isDefault()) {
            holder.defaultBadge.setVisibility(View.VISIBLE);
            holder.btnSetDefault.setVisibility(View.GONE);
        } else {
            holder.defaultBadge.setVisibility(View.GONE);
            holder.btnSetDefault.setVisibility(View.VISIBLE);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(position, address));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(position));
        holder.btnSetDefault.setOnClickListener(v -> listener.onSetDefault(position));
        
        holder.itemView.setOnClickListener(v -> listener.onSelect(position, address));
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recipientName, recipientPhone, addressDetail, defaultBadge;
        Button btnEdit, btnDelete, btnSetDefault;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipientName = itemView.findViewById(R.id.text_recipient_name);
            recipientPhone = itemView.findViewById(R.id.text_recipient_phone);
            addressDetail = itemView.findViewById(R.id.text_address_detail);
            defaultBadge = itemView.findViewById(R.id.text_default_badge);
            btnEdit = itemView.findViewById(R.id.btn_edit_address);
            btnDelete = itemView.findViewById(R.id.btn_delete_address);
            btnSetDefault = itemView.findViewById(R.id.btn_set_default);
        }
    }
}
