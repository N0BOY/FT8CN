package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.databinding.FragmentApplicationLogBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Fragment for displaying application logs with tabs for different log types.
 * @author BGY70Z
 */
public class ApplicationLogFragment extends Fragment {
    private static final String TAG = "ApplicationLogFragment";
    private FragmentApplicationLogBinding binding;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentApplicationLogBinding.inflate(inflater, container, false);
        
        // Set up ViewPager2 with tabs
        LogPagerAdapter pagerAdapter = new LogPagerAdapter(requireActivity());
        binding.logViewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.logTabLayout, binding.logViewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText(getString(R.string.log_tab_toast_notifications));
                    } else if (position == 1) {
                        tab.setText(getString(R.string.log_tab_external_calls));
                    }
                }).attach();
        
        return binding.getRoot();
    }
    
    /**
     * FragmentStateAdapter for managing log tab fragments
     */
    private static class LogPagerAdapter extends FragmentStateAdapter {
        public LogPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new ToastLogFragment();
            } else {
                return new ExternalCallsLogFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 2; // Toast notifications and External calls
        }
    }
    
    /**
     * Fragment for displaying toast notifications log
     */
    public static class ToastLogFragment extends LogViewFragment {
        @Override
        protected com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType getLogType() {
            return com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType.TOAST_NOTIFICATIONS;
        }
    }
    
    /**
     * Fragment for displaying external calls log
     */
    public static class ExternalCallsLogFragment extends LogViewFragment {
        @Override
        protected com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType getLogType() {
            return com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType.EXTERNAL_CALLS;
        }
    }

    /**
     * Base fragment for displaying log content
     */
    public static abstract class LogViewFragment extends Fragment {
        private com.bg7yoz.ft8cn.log.ApplicationLogManager logManager;
        private android.widget.TextView logTextView;
        private android.widget.Button refreshButton;
        private android.widget.Button clearButton;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLog();
            }
        };
        
        protected abstract com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType getLogType();
        
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_log_view, container, false);
            
            logManager = new com.bg7yoz.ft8cn.log.ApplicationLogManager(requireContext());
            logTextView = view.findViewById(R.id.logTextView);
            refreshButton = view.findViewById(R.id.refreshButton);
            clearButton = view.findViewById(R.id.clearButton);
            
            refreshButton.setOnClickListener(v -> refreshLog());
            clearButton.setOnClickListener(v -> {
                logManager.clearLog(getLogType());
                refreshLog();
            });
            
            // Initial load
            refreshLog();
            
            // Auto-refresh every 2 seconds
            handler.postDelayed(refreshRunnable, 2000);
            
            return view;
        }
        
        @Override
        public void onDestroyView() {
            super.onDestroyView();
            handler.removeCallbacks(refreshRunnable);
        }
        
        private void refreshLog() {
            if (logManager != null && logTextView != null) {
                String logText = logManager.getAllLogText(getLogType());
                logTextView.setText(logText);
                
                // Scroll to bottom
                if (logTextView.getLayout() != null) {
                    int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
                    if (scrollAmount > 0) {
                        logTextView.scrollTo(0, scrollAmount);
                    } else {
                        logTextView.scrollTo(0, 0);
                    }
                }
                
                // Schedule next refresh
                handler.removeCallbacks(refreshRunnable);
                handler.postDelayed(refreshRunnable, 2000);
            }
        }
    }
}
