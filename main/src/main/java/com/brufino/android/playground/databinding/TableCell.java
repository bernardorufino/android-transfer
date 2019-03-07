package com.brufino.android.playground.databinding;

import android.view.View;
import com.brufino.android.playground.R;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;

public class TableCell<T> {
    public static <T> TableCell<T> from(View view) {
        //noinspection unchecked
        return (TableCell<T>) checkNotNull(view.getTag(R.id.table_cell));
    }

    public final int i;
    public final int j;
    public final T content;

    TableCell(int i, int j, T content) {
        this.i = i;
        this.j = j;
        this.content = content;
    }

    public boolean isHeader() {
        return i == 0 || j == 0;
    }

    public boolean isEven() {
        return i % 2 == 0;
    }

    public <U extends T> TableCell<U> setContent(U content) {
        return new TableCell<>(i, j, content);
    }


}
