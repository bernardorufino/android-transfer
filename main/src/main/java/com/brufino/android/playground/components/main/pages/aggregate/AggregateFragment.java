package com.brufino.android.playground.components.main.pages.aggregate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import com.brufino.android.playground.provision.Provisioners;
import com.brufino.android.playground.R;
import com.brufino.android.playground.components.main.MainActivity;
import com.brufino.android.playground.databinding.AggregateFragmentBinding;

public class AggregateFragment extends Fragment {
    private final AggregateFragmentProvisioner mProvisioner;
    private AggregateFragmentBinding mBinding;
    private ViewModelProvider.Factory mViewModelFactory;

    public AggregateFragment() {
        mProvisioner = Provisioners.get().getAggregateFragmentProvisioner(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModelFactory = mProvisioner.getViewModelFactory();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = AggregateFragmentBinding.inflate(inflater, container, false);
        mBinding.setLifecycleOwner(this);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AggregateViewModel data =
                ViewModelProviders.of(this, mViewModelFactory).get(AggregateViewModel.class);
        data.onActivityCreate(getOwnerActivity());
        mBinding.setViewModel(data);
    }

    private MainActivity getOwnerActivity() {
        return (MainActivity) getActivity();
    }
}
