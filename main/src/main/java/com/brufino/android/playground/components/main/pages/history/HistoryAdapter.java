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
import com.brufino.android.playground.extensions.DataBindingViewHolder;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class HistoryAdapter extends RecyclerView.Adapter<DataBindingViewHolder> {
    private final LifecycleOwner mOwner;
    private List<Item> mHistory;

    public HistoryAdapter(LifecycleOwner owner) {
        mOwner = owner;
        mHistory = getAdapterHistory(emptyList());
    }

    void notifyHistoryChanged(List<HistoryEntryViewModel> history) {
        List<Item> previous = mHistory;
        mHistory = getAdapterHistory(history);
        // A history is either cleared or grows in the head.  Either way the header is fixed at 0.
        if (history.isEmpty()) {
            notifyItemRangeRemoved(1, previous.size() - 1);
        } else {
            notifyItemRangeInserted(1, mHistory.size() - previous.size());
        }
        // Header always change
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
        Item item = mHistory.get(position);
        ViewDataBinding binding = holder.binding;
        binding.setVariable(BR.viewModel, item.viewModel);
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

    /**
     * We use an extra item at the top, the header, to make animations work when items added and
     * scroll is resting at the top while not animating if user is somewhere in the middle.
     */
    private static List<Item> getAdapterHistory(List<HistoryEntryViewModel> history) {
        return Stream
                .concat(
                        Stream.of(getHeaderItem(history)),
                        history.stream().map(HistoryAdapter::getEntryItem))
                .collect(toList());
    }

    private static Item getHeaderItem(List<HistoryEntryViewModel> history) {
        return new Item(history, R.layout.history_header);
    }

    private static Item getEntryItem(HistoryEntryViewModel entry) {
        return new Item(entry, R.layout.history_entry);
    }

    private static class Item {
        @Nullable public final Object viewModel;
        @LayoutRes public final int layoutRes;

        private Item(
                @Nullable Object viewModel, @LayoutRes int layoutRes) {
            this.viewModel = viewModel;
            this.layoutRes = layoutRes;
        }
    }
}
