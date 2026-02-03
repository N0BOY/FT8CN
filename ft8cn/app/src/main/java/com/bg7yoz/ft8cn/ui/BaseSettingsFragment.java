package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bg7yoz.ft8cn.MainViewModel;

/**
 * Base class for all settings tab fragments
 * Provides common functionality and access to MainViewModel
 *
 * @author BGY70Z
 * @date 2026-02-02
 */
public abstract class BaseSettingsFragment extends Fragment {

    protected MainViewModel mainViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get MainViewModel instance
        mainViewModel = MainViewModel.getInstance(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize settings UI components
        initializeSettings();
        // Set up event listeners
        setupListeners();
    }

    /**
     * Initialize all settings UI components
     * Subclasses must implement this to set up their specific settings
     */
    protected abstract void initializeSettings();

    /**
     * Set up all event listeners for settings
     * Subclasses must implement this to set up event handlers
     */
    protected abstract void setupListeners();

    /**
     * Utility method to write config to database
     * @param key Config key
     * @param value Config value
     */
    protected void writeConfig(String key, String value) {
        if (mainViewModel != null && mainViewModel.databaseOpr != null) {
            mainViewModel.databaseOpr.writeConfig(key, value, null);
        }
    }

    /**
     * Show a toast message
     * @param message Message to display
     */
    protected void showToast(String message) {
        ToastMessage.show(message);
    }

    /**
     * Get string resource
     * @param resId Resource ID
     * @return String value
     */
    protected String getStringResource(int resId) {
        if (getContext() != null) {
            return getContext().getString(resId);
        }
        return "";
    }
}
