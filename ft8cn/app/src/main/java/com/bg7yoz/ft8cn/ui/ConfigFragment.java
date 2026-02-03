package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bg7yoz.ft8cn.databinding.FragmentConfigBinding;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Settings fragment with tabbed interface
 * Manages 3 tabs: Basic, Advanced, Cloud & Help
 *
 * @author BGY70Z
 * @date 2026-02-02
 */
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

        // Set up ViewPager2 with adapter
        SettingsPagerAdapter adapter = new SettingsPagerAdapter(requireActivity());
        binding.settingsViewPager.setAdapter(adapter);

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
