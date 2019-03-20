package com.brufino.android.playground.components.main.pages.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.brufino.android.playground.BR;
import com.brufino.android.playground.R;
import com.brufino.android.playground.components.main.pages.history.viewmodel.HistoryEntryViewModel;
import com.brufino.android.playground.components.main.pages.history.viewmodel.HistoryHeaderViewModel;
import com.brufino.android.playground.components.main.pages.history.viewmodel.HistoryItemViewModel;
import com.brufino.android.playground.components.main.pages.history.viewmodel.HistoryViewModel;
import com.brufino.android.playground.extensions.DataBindingViewHolder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.brufino.android.common.utils.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class HistoryAdapter extends RecyclerView.Adapter<DataBindingViewHolder> {
    private final LifecycleOwner mOwner;
    private List<HistoryItemViewModel> mHistory;

    public HistoryAdapter(LifecycleOwner owner) {
        mOwner = owner;
        mHistory = singletonList(HistoryHeaderViewModel.empty());
    }
    /**
     * We use an extra item at the top, the header, to make animations work when items added and
     * scroll is resting at the top while not animating if user is somewhere in the middle.
     *
     * @see HistoryViewModel
     */
    void notifyHistoryChanged(List<HistoryItemViewModel> history) {
        checkArgument(!history.isEmpty(), "History should have header");
        
        List<HistoryItemViewModel> previous = mHistory;
        mHistory = history;
        // A history is either cleared or grows in the head. Either way the header is fixed at 0.
        if (history.size() == 1) {
            notifyItemRangeRemoved(1, previous.size() - 1);
        } else {
            notifyItemRangeInserted(1, mHistory.size() - previous.size());
        }
        // Header always changes
        notifyItemChanged(0);
    }

    @Override
    public DataBindingViewHolder onCreateViewHolder(ViewGroup parent, int layoutRes) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutRes, parent, false);
        binding.setLifecycleOwner(mOwner);
        return new DataBindingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(DataBindingViewHolder holder, int position) {
        HistoryItemViewModel item = mHistory.get(position);
        ViewDataBinding binding = holder.binding;
        binding.setVariable(BR.viewModel, item);
        binding.executePendingBindings();
    }

    @Override
    public int getItemViewType(int position) {
        return mHistory.get(position).layoutRes;
    }

    @Override
    public int getItemCount() {
        return mHistory.size();
    }
}
