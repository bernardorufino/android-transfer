package com.brufino.android.playground.components.main.pages.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import com.brufino.android.playground.components.main.pages.history.viewmodel.HistoryViewModel;
import com.brufino.android.playground.provision.Provisioners;
import com.brufino.android.playground.databinding.HistoryFragmentBinding;

public class HistoryFragment extends Fragment {
    private final HistoryFragmentProvisioner mProvisioner;
    private HistoryFragmentBinding mBinding;
    private ViewModelProvider.Factory mViewModelFactory;
    private HistoryAdapter mAdapter;

    public HistoryFragment() {
        mProvisioner = Provisioners.get().getHistoryFragmentProvisioner(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModelFactory = mProvisioner.getViewModelFactory();
        mAdapter = mProvisioner.getHistoryAdapter();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = HistoryFragmentBinding.inflate(inflater, container, false);
        mBinding.setLifecycleOwner(this);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        HistoryViewModel data =
                ViewModelProviders.of(this, mViewModelFactory).get(HistoryViewModel.class);
        mBinding.setViewModel(data);
        mBinding.history.setAdapter(mAdapter);
        data.history.observe(this, mAdapter::notifyHistoryChanged);
    }
}
