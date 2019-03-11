package com.brufino.android.playground.components.main.pages.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import com.brufino.android.playground.databinding.StatisticsFragmentBinding;
import com.brufino.android.playground.provision.Provisioners;

public class StatisticsFragment extends Fragment {
    private final StatisticsFragmentProvisioner mProvisioner;
    private ViewModelProvider.Factory mViewModelFactory;
    private StatisticsFragmentBinding mBinding;

    public StatisticsFragment() {
        mProvisioner = Provisioners.get().getStatisticsFragmentProvisioner(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModelFactory = mProvisioner.getViewModelFactory();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = StatisticsFragmentBinding.inflate(inflater, container, false);
        mBinding.setLifecycleOwner(this);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        StatisticsViewModel data =
                ViewModelProviders.of(this, mViewModelFactory).get(StatisticsViewModel.class);
        mBinding.setViewModel(data);
    }
}
