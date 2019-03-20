package com.brufino.android.playground.databinding;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.databinding.*;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager.widget.ViewPager;
import com.brufino.android.playground.BR;
import com.brufino.android.playground.R;
import com.brufino.android.playground.extensions.AndroidUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.brufino.android.common.utils.Preconditions.checkState;

public class Adapters {
    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean value) {
        view.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("android:foo")
    public static void setFoo(TextView view, String text) {
        view.setText(text);
    }

    @BindingAdapter("currentTab")
    public static void setCurrentTab(ViewPager view, int tab) {
        if (tab != view.getCurrentItem()) {
            view.setCurrentItem(tab);
        }
    }

    @InverseBindingAdapter(attribute = "currentTab", event = "currentTabChanged")
    public static int getCurrentTab(ViewPager view) {
        return view.getCurrentItem();
    }

    @BindingAdapter("currentTabChanged")
    public static void setListeners(
            ViewPager view, final InverseBindingListener attrChange) {
        view.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                attrChange.onChange();
            }
        });
    }

    private Adapters() {}
}
