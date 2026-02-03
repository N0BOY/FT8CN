# How to Enable Settings Tabs

## Current Status

The tab infrastructure is in place but not yet activated:

✅ **Created:**
- SettingsPagerAdapter.java - Manages 3 tabs
- BasicSettingsFragment.java - Stub for Basic tab
- AdvancedSettingsFragment.java - Stub for Advanced tab
- CloudSettingsFragment.java - Stub for Cloud tab
- Tab title strings

❌ **Not Yet Done:**
- ConfigFragment still uses old ScrollView layout
- Settings haven't been split into tabs
- Navigation still goes to old ConfigFragment

## Why You Don't See Tabs Yet

The current `ConfigFragment` is still showing the old single-page layout with all settings in one ScrollView. To see the tabs, you need to update ConfigFragment to use TabLayout with ViewPager2.

## How to Enable Tabs (2 Options)

### Option 1: Minimal Change - Just Add Tab UI

Keep all existing logic, just wrap it in tabs.

**Step 1:** Rename current ConfigFragment
```bash
# In your IDE or file manager:
ConfigFragment.java → ConfigFragmentContent.java
fragment_config.xml → fragment_config_content.xml
```

**Step 2:** Create new ConfigFragment with tabs

```java
// New ConfigFragment.java
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

public class ConfigFragment extends Fragment {
    private FragmentConfigBinding binding;

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

        SettingsPagerAdapter adapter = new SettingsPagerAdapter(requireActivity());
        binding.settingsViewPager.setAdapter(adapter);

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

**Step 3:** Create new fragment_config.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

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
            app:tabMode="fixed"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/config_fragment_bar" />

        <!-- ViewPager2 -->
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

**Step 4:** Update BasicSettingsFragment to show all settings

```java
public class BasicSettingsFragment extends Fragment {
    @Override
    public View onCreateView(...) {
        // Include the old settings content
        return inflater.inflate(R.layout.fragment_config_content, container, false);
    }
}
```

Now all settings will appear in the Basic tab, and you'll see 3 tabs!

---

### Option 2: Complete Reorganization

Split settings across the 3 tabs as designed. This requires:
1. Splitting fragment_config_content.xml into 3 layout files
2. Moving settings logic to appropriate fragments
3. More work but cleaner result

See `SETTINGS_3TAB_STRUCTURE.md` for the complete plan.

---

## Quick Test

To quickly see if tabs are working without breaking anything:

1. Add ViewPager2 dependency to `app/build.gradle`:
```gradle
implementation 'androidx.viewpager2:viewpager2:1.0.0'
```

2. The stub fragments will show placeholder text in Advanced and Cloud tabs
3. Basic tab will show placeholder until you move settings there

---

## Recommendation

Since ConfigFragment is 2237 lines and very complex, I recommend **Option 1** (minimal change):
- Keep all existing logic working
- Just add tab wrapper
- Gradually reorganize settings into tabs later
- Low risk, immediate visual improvement

