package com.brufino.android.playground.extensions;

import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

public class DataBindingViewHolder extends RecyclerView.ViewHolder {
    public final ViewDataBinding binding;

    public DataBindingViewHolder(ViewDataBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
