package com.brufino.android.playground.extensions;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FixedFragmentsPagerAdapter extends FragmentPagerAdapter {
    public static Builder builder(FragmentManager fragmentManager) {
        return new Builder(fragmentManager);
    }

    private final List<Entry> mEntries;

    private FixedFragmentsPagerAdapter(
            FragmentManager fm,
            List<Entry> entries) {
        super(fm);
        mEntries = entries;
    }

    @Override
    public Fragment getItem(int position) {
        return mEntries.get(position).supplier.get();
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mEntries.get(position).title;
    }

    public static class Builder {
        private final List<Entry> mEntries = new ArrayList<>();
        private final FragmentManager mFragmentManager;

        private Builder(FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
        }

        public Builder add(CharSequence title, Supplier<? extends Fragment> supplier) {
            mEntries.add(new Entry(title, supplier));
            return this;
        }

        public FixedFragmentsPagerAdapter build() {
            return new FixedFragmentsPagerAdapter(mFragmentManager, mEntries);
        }
    }

    private static class Entry {
        private final CharSequence title;
        private final Supplier<? extends Fragment> supplier;

        private Entry(CharSequence title, Supplier<? extends Fragment> supplier) {
            this.title = title;
            this.supplier = supplier;
        }
    }
}
