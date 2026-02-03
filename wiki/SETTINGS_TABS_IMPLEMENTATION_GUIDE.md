# Settings Tabs - Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing the tabbed settings interface in FT8CN.

---

## Files Created So Far

✅ **SettingsPagerAdapter.java** - ViewPager2 adapter for managing tab fragments
✅ **strings.xml** - Added tab title strings
✅ **SETTINGS_REORGANIZATION_PLAN.md** - Tab structure planning document

---

## Remaining Implementation Steps

### Step 1: Create Base Settings Fragment

Create a base class that all settings tab fragments will extend:

**File:** `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/BaseSettingsFragment.java`

```java
package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bg7yoz.ft8cn.MainViewModel;

/**
 * Base class for settings tab fragments
 * Provides common functionality for all settings tabs
 */
public abstract class BaseSettingsFragment extends Fragment {

    protected MainViewModel mainViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get MainViewModel from activity
        mainViewModel = MainViewModel.getInstance(this);

        // Initialize settings UI
        initializeSettings();

        // Set up listeners
        setupListeners();
    }

    /**
     * Initialize settings UI components
     * Subclasses should implement this to set up their specific settings
     */
    protected abstract void initializeSettings();

    /**
     * Set up event listeners
     * Subclasses should implement this to set up event handlers
     */
    protected abstract void setupListeners();

    /**
     * Utility method to write config to database
     */
    protected void writeConfig(String key, String value) {
        if (mainViewModel != null && mainViewModel.databaseOpr != null) {
            mainViewModel.databaseOpr.writeConfig(key, value, null);
        }
    }
}
```

---

### Step 2: Create Fragment Classes

Create 6 fragment classes (one per tab):

#### 2.1 Station Settings Fragment

**File:** `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/StationSettingsFragment.java`

```java
package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.databinding.FragmentStationSettingsBinding;

/**
 * Station identification settings tab
 * - My Callsign
 * - Callsign Modifier
 * - Park Number
 * - Maidenhead Grid
 */
public class StationSettingsFragment extends BaseSettingsFragment {

    private FragmentStationSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentStationSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void initializeSettings() {
        // Initialize callsign
        binding.inputMycallEdit.setText(GeneralVariables.myCallsign);

        // Initialize modifier
        binding.modifierEdit.setText(GeneralVariables.toModifier);

        // Initialize park number
        binding.parkNumberEdit.setText(GeneralVariables.parkNumber);

        // Initialize grid
        binding.inputMyGridEdit.setText(GeneralVariables.myMaidenheadGrid);
    }

    @Override
    protected void setupListeners() {
        // Callsign change listener
        binding.inputMycallEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String callsign = binding.inputMycallEdit.getText().toString().trim().toUpperCase();
                GeneralVariables.myCallsign = callsign;
                writeConfig("myCall", callsign);
            }
        });

        // Modifier change listener
        binding.modifierEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String modifier = binding.modifierEdit.getText().toString().trim();
                GeneralVariables.toModifier = modifier;
                writeConfig("toModifier", modifier);
            }
        });

        // Park number change listener
        binding.parkNumberEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String parkNumber = binding.parkNumberEdit.getText().toString().trim();
                GeneralVariables.parkNumber = parkNumber;
                writeConfig("parkNumber", parkNumber);
            }
        });

        // Grid change listener
        binding.inputMyGridEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String grid = binding.inputMyGridEdit.getText().toString().trim().toUpperCase();
                GeneralVariables.myMaidenheadGrid = grid;
                writeConfig("myGrid", grid);
            }
        });

        // Get location button
        binding.configGetGridImageButton.setOnClickListener(v -> {
            // Trigger location fetch (copy logic from ConfigFragment)
            if (mainViewModel != null) {
                mainViewModel.getMyGrid();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

#### 2.2-2.6 Other Fragment Classes

Similar structure for:
- **RadioSettingsFragment.java** - Radio configuration
- **TransmitSettingsFragment.java** - Transmission settings
- **AudioTimeSettingsFragment.java** - Audio and time sync
- **OperatingSettingsFragment.java** - Operating modes and behavior
- **CloudAdvancedSettingsFragment.java** - Cloud services and advanced

---

### Step 3: Create Layout XML Files

Create 6 layout files (one per fragment):

#### 3.1 Station Settings Layout

**File:** `ft8cn/app/src/main/res/layout/fragment_station_settings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true">

        <androidx.constraintlayout.widget.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="16dp">

            <!-- My Callsign Section -->
            <TextView
                android:id="@+id/callsignLabel"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:text="@string/pl_myCallsign"
                android:textColor="@color/text_view_color"
                android:textSize="14sp"
                android:textStyle="bold"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />

            <EditText
                android:id="@+id/inputMycallEdit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="@string/pl_myCallsign"
                android:inputType="textCapCharacters"
                android:maxLength="12"
                app:layout_constraintEnd_toStartOf="@+id/configGetCallsignImageButton"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/callsignLabel"
                tools:ignore="Autofill,LabelFor" />

            <ImageButton
                android:id="@+id/configGetCallsignImageButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:background="@drawable/imagebutton_transparent_style"
                android:contentDescription="@string/help"
                app:layout_constraintBottom_toBottomOf="@+id/inputMycallEdit"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintTop_toTopOf="@+id/inputMycallEdit"
                app:srcCompat="@drawable/ic_baseline_info_32"
                tools:ignore="TouchTargetSizeCheck" />

            <!-- Callsign Modifier Section -->
            <TextView
                android:id="@+id/modifierLabel"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/pl_modifier"
                android:textColor="@color/text_view_color"
                android:textSize="14sp"
                android:textStyle="bold"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/inputMycallEdit" />

            <EditText
                android:id="@+id/modifierEdit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="@string/pl_modifier_hint"
                android:inputType="textCapCharacters"
                android:maxLength="6"
                app:layout_constraintEnd_toStartOf="@+id/modifierHelpButton"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/modifierLabel"
                tools:ignore="Autofill,LabelFor" />

            <ImageButton
                android:id="@+id/modifierHelpButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:background="@drawable/imagebutton_transparent_style"
                android:contentDescription="@string/help"
                app:layout_constraintBottom_toBottomOf="@+id/modifierEdit"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintTop_toTopOf="@+id/modifierEdit"
                app:srcCompat="@drawable/ic_baseline_info_32"
                tools:ignore="TouchTargetSizeCheck" />

            <!-- Park Number Section -->
            <TextView
                android:id="@+id/parkNumberLabel"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/pl_park_number"
                android:textColor="@color/text_view_color"
                android:textSize="14sp"
                android:textStyle="bold"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/modifierEdit" />

            <EditText
                android:id="@+id/parkNumberEdit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="@string/pl_park_number_hint"
                android:inputType="text"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/parkNumberLabel"
                tools:ignore="Autofill,LabelFor" />

            <!-- Maidenhead Grid Section -->
            <TextView
                android:id="@+id/gridLabel"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/pl_myGrid"
                android:textColor="@color/text_view_color"
                android:textSize="14sp"
                android:textStyle="bold"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/parkNumberEdit" />

            <EditText
                android:id="@+id/inputMyGridEdit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="@string/pl_myGrid"
                android:inputType="textCapCharacters"
                android:maxLength="6"
                app:layout_constraintEnd_toStartOf="@+id/configGetGridImageButton"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@+id/gridLabel"
                tools:ignore="Autofill,LabelFor" />

            <ImageButton
                android:id="@+id/configGetGridImageButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:background="@drawable/imagebutton_transparent_style"
                android:contentDescription="@string/get_location"
                app:layout_constraintBottom_toBottomOf="@+id/inputMyGridEdit"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintTop_toTopOf="@+id/inputMyGridEdit"
                app:srcCompat="@drawable/baseline_location_on_24"
                tools:ignore="TouchTargetSizeCheck" />

        </androidx.constraintlayout.widget.ConstraintLayout>
    </ScrollView>
</layout>
```

#### 3.2-3.6 Other Layout Files

Create similar layouts for other tabs:
- **fragment_radio_settings.xml** - Radio configuration UI
- **fragment_transmit_settings.xml** - Transmission settings UI
- **fragment_audio_time_settings.xml** - Audio and time UI
- **fragment_operating_settings.xml** - Operating settings UI
- **fragment_cloud_advanced_settings.xml** - Cloud and advanced UI

---

### Step 4: Update Main Config Fragment

Modify `ConfigFragment.java` to use TabLayout with ViewPager2:

```java
package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.databinding.FragmentConfigBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Main settings fragment with tabbed interface
 */
public class ConfigFragment extends Fragment {

    private FragmentConfigBinding binding;
    private SettingsPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up ViewPager2 with adapter
        pagerAdapter = new SettingsPagerAdapter(requireActivity());
        binding.settingsViewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.settingsTabLayout, binding.settingsViewPager,
                (tab, position) -> tab.setText(SettingsPagerAdapter.getTabTitle(position))
        ).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

---

### Step 5: Create New Main Config Layout

Replace `fragment_config.xml` with a tabbed layout:

**File:** `ft8cn/app/src/main/res/layout/fragment_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/configFrameLayout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".ui.ConfigFragment">

        <!-- Title Bar -->
        <androidx.constraintlayout.widget.ConstraintLayout
            android:id="@+id/config_fragment_bar"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:background="@color/purple_500"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/nav_menu_title_config"
                android:textColor="@color/bar_text_view_color"
                android:textSize="20sp"
                app:layout_constraintBottom_toBottomOf="parent"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />
        </androidx.constraintlayout.widget.ConstraintLayout>

        <!-- Tab Layout -->
        <com.google.android.material.tabs.TabLayout
            android:id="@+id/settingsTabLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:background="@color/purple_500"
            app:tabTextColor="@color/bar_text_view_color"
            app:tabSelectedTextColor="@android:color/white"
            app:tabIndicatorColor="@android:color/white"
            app:tabMode="scrollable"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/config_fragment_bar" />

        <!-- ViewPager2 for Tab Content -->
        <androidx.viewpager2.widget.ViewPager2
            android:id="@+id/settingsViewPager"
            android:layout_width="0dp"
            android:layout_height="0dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/settingsTabLayout" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
```

---

### Step 6: Add Required Dependencies

Ensure `build.gradle` (app level) includes:

```gradle
dependencies {
    // ViewPager2 for tabbed interface
    implementation 'androidx.viewpager2:viewpager2:1.0.0'

    // Material Components (TabLayout)
    implementation 'com.google.android.material:material:1.9.0'

    // Existing dependencies...
}
```

---

## Implementation Checklist

- [x] Create SettingsPagerAdapter
- [x] Add tab title strings
- [ ] Create BaseSettingsFragment
- [ ] Create 6 fragment classes:
  - [ ] StationSettingsFragment
  - [ ] RadioSettingsFragment
  - [ ] TransmitSettingsFragment
  - [ ] AudioTimeSettingsFragment
  - [ ] OperatingSettingsFragment
  - [ ] CloudAdvancedSettingsFragment
- [ ] Create 6 layout XML files:
  - [ ] fragment_station_settings.xml
  - [ ] fragment_radio_settings.xml
  - [ ] fragment_transmit_settings.xml
  - [ ] fragment_audio_time_settings.xml
  - [ ] fragment_operating_settings.xml
  - [ ] fragment_cloud_advanced_settings.xml
- [ ] Update ConfigFragment.java
- [ ] Replace fragment_config.xml
- [ ] Test all functionality
- [ ] Verify all settings work correctly
- [ ] Verify VOX mode dependencies
- [ ] Verify conditional UI elements
- [ ] Test on various screen sizes

---

## Migration Strategy

### Option 1: Incremental Migration (Recommended)
1. Keep old ConfigFragment as ConfigFragmentOld
2. Create new tabbed ConfigFragment alongside
3. Migrate settings tab-by-tab
4. Test each tab thoroughly
5. Switch to new fragment when complete

### Option 2: Complete Replacement
1. Backup current ConfigFragment
2. Implement all 6 tabs at once
3. Test comprehensively before deployment

---

## Testing Plan

1. **Station Tab**: Verify callsign, grid, park number save/load
2. **Radio Tab**: Test rig selection, control modes, serial settings
3. **Transmit Tab**: Test frequency, delays, fake it, offsets
4. **Audio & Time Tab**: Test audio formats, NTP, GPS sync
5. **Operating Tab**: Test decode modes, auto-follow, alarms
6. **Cloud Tab**: Test Cloudlog, QRZ, PSK Reporter, cache clear

---

## Benefits After Implementation

✅ **Better Organization** - Settings grouped logically
✅ **Less Scrolling** - 6 focused tabs vs one long list
✅ **Easier to Find** - Clear category labels
✅ **Better UX** - Swipe between tabs
✅ **Scalable** - Easy to add new settings to appropriate tab
✅ **Mobile Friendly** - Less overwhelming interface

