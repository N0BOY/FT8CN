package com.bg7yoz.ft8cn.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * ViewPager2 adapter for settings tabs
 * Manages the 3 simplified settings category fragments
 *
 * @author BGY70Z
 * @date 2026-02-02
 */
public class SettingsPagerAdapter extends FragmentStateAdapter {

    public static final int TAB_COUNT = 3;

    // Tab indices
    public static final int TAB_BASIC = 0;      // Station + Radio + Transmit
    public static final int TAB_ADVANCED = 1;   // Audio + Time + Operating
    public static final int TAB_CLOUD = 2;      // Cloud services + Help

    public SettingsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_BASIC:
                return new BasicSettingsFragment();
            case TAB_ADVANCED:
                return new AdvancedSettingsFragment();
            case TAB_CLOUD:
                return new CloudSettingsFragment();
            default:
                return new BasicSettingsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    /**
     * Get tab title for the specified position
     * @param position Tab index
     * @return Tab title resource ID
     */
    public static int getTabTitle(int position) {
        switch (position) {
            case TAB_BASIC:
                return com.bg7yoz.ft8cn.R.string.settings_tab_basic;
            case TAB_ADVANCED:
                return com.bg7yoz.ft8cn.R.string.settings_tab_advanced;
            case TAB_CLOUD:
                return com.bg7yoz.ft8cn.R.string.settings_tab_cloud;
            default:
                return com.bg7yoz.ft8cn.R.string.settings_tab_basic;
        }
    }
}
