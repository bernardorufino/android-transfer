package com.brufino.android.playground.databinding;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.LifecycleOwner;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.playground.BR;
import com.brufino.android.playground.R;
import com.brufino.android.playground.extensions.AndroidUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;

public class TableAdapters {
    @BindingAdapter({"content", "cell", "onCellClick", "onCellLongClick"})
    public static <T> void setTableContent(
            TableLayout table,
            @Nullable T[][] oldContent,
            @LayoutRes int oldLayout,
            View.OnClickListener oldOnClick,
            View.OnLongClickListener oldOnLongClick,
            @Nullable T[][] newContent,
            @LayoutRes int newLayout,
            View.OnClickListener newOnClick,
            View.OnLongClickListener newOnLongClick) {
        long startTime = System.nanoTime();
        String action = "exception";
        try {
            if (newLayout == oldLayout
                    && Objects.equals(newOnClick, oldOnClick)
                    && Objects.equals(newOnLongClick, oldOnLongClick)) {
                if (Arrays.equals(newContent, oldContent)) {
                    action = "nop";
                    return;
                }
                if (newContent != null
                        && oldContent != null
                        && newContent.length > 0
                        && oldContent.length > 0
                        && newContent[0].length == oldContent[0].length
                        && oldContent[0].length > 0) {
                    if (newContent.length == oldContent.length) {
                        if (updateTableContent(table, newContent)) {
                            action = "update";
                            return;
                        }
                    }
                     if (newContent.length > oldContent.length) {
                         addRows(
                              table, oldContent, newContent, newLayout, newOnClick, newOnLongClick);
                         if (updateTableContent(table, newContent)) {
                             action = "add_rows->update";
                             return;
                         }
                     }
                }
            }
            action = "reset";
            resetTableContent(
                    table, newContent, newLayout, newOnClick, newOnLongClick);
        } finally {
            long timeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            Log.d(TAG, "table = " + timeMs + " ms " + "(" + action + ")");
        }
    }

    private static <T> void addRows(
            TableLayout table,
            T[][] oldContent,
            T[][] newContent,
            @LayoutRes int layout,
            View.OnClickListener onClick,
            View.OnLongClickListener onLongClick) {
        Context context = table.getContext();
        int index = getFirstDifferentRowIndex(oldContent, newContent);
        int rows = newContent.length - oldContent.length;
        for (int i = index; i < index + rows; i++) {
            T[] rowContent = checkNotNull(newContent[i]);
            TableRow row = getTableRow(context, i, rowContent, layout, onClick, onLongClick);
            table.addView(row, i);
        }
    }

    private static <T> int getFirstDifferentRowIndex(T[][] oldContent, T[][] newContent) {
        int i = 0;
        for (int n = oldContent.length; i < n; i++) {
            if (!Arrays.equals(oldContent[i], newContent[i])) {
                break;
            }
        }
        return i;
    }

    private static <T> boolean updateTableContent(TableLayout table, T[][] content) {
        for (int i = 0, n = table.getChildCount(); i < n; i++) {
            View view = table.getChildAt(i);
            if (!(view instanceof TableRow)) {
                // Log.d("BML", "raw-level view not instanceof TableRow, view = " + view);
                return false;
            }
            TableRow row = (TableRow) view;
            for (int j = 0, m = row.getChildCount(); j < m; j++) {
                View cellView = row.getChildAt(j);
                Object tag = cellView.getTag(R.id.table_cell);
                if (!(tag instanceof TableCell)) {
                    // Log.d("BML", "tag not instanceof TableCell, tag = " + tag);
                    return false;
                }
                ViewDataBinding binding = DataBindingUtil.getBinding(cellView);
                if (binding == null) {
                    // Log.d("BML", "binding(cellView) == null");
                    return false;
                }
                @SuppressWarnings("unchecked")
                TableCell<T> oldCell = (TableCell<T>) tag;
                T newContent = content[i][j];
                if (!Objects.equals(oldCell.content, newContent)) {
                    //Log.d("BML", "Changing view (" + i + ", " + j + ") => " + newContent);
                    TableCell<T> newCell = oldCell.setContent(newContent);
                    cellView.setTag(R.id.table_cell, newCell);
                    binding.setVariable(BR.cell, newCell);
                    binding.executePendingBindings();
                }
            }
        }
        return true;
    }

    private static <T> void resetTableContent(
            TableLayout table,
            @Nullable T[][] content,
            @LayoutRes int layout,
            View.OnClickListener onClick,
            View.OnLongClickListener onLongClick) {
        table.removeAllViews();
        if (content == null || content.length == 0 || content[0].length == 0) {
            return;
        }
        Context context = table.getContext();
        for (int i = 0, n = content.length; i < n; i++) {
            T[] rowContent = checkNotNull(content[i]);
            TableRow row = getTableRow(context, i, rowContent, layout, onClick, onLongClick);
            table.addView(row);
        }
    }

    private static <T> TableRow getTableRow(
            Context context,
            int rowIndex,
            T[] rowContent,
            @LayoutRes int layout,
            View.OnClickListener onClickListener,
            View.OnLongClickListener onLongClickListener) {
        LifecycleOwner owner = AndroidUtils.getSpecializedContext(context, LifecycleOwner.class);
        LayoutInflater inflater = LayoutInflater.from(context);
        TableRow row = new TableRow(context);
        for (int j = 0, m = rowContent.length; j < m; j++) {
            ViewDataBinding binding =
                    DataBindingUtil.inflate(inflater, layout, row, false);
            if (owner != null) {
                binding.setLifecycleOwner(owner);
            }
            TableCell<T> cell = new TableCell<>(rowIndex, j, rowContent[j]);
            binding.setVariable(BR.cell, cell);
            // To prevent delaying to next frame and flickering screen
            binding.executePendingBindings();
            View cellView = binding.getRoot();
            cellView.setTag(R.id.table_cell, cell);
            cellView.setOnClickListener(onClickListener);
            cellView.setOnLongClickListener(onLongClickListener);
            row.addView(cellView);
        }
        return row;
    }

    private TableAdapters() {}
}
