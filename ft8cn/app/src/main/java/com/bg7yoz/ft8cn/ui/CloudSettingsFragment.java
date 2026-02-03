package com.bg7yoz.ft8cn.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bg7yoz.ft8cn.FAQActivity;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.SettingsBackupManager;
import com.bg7yoz.ft8cn.databinding.FragmentCloudSettingsBinding;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Cloud & Help settings tab: Cloud services + Maintenance
 * Contains PSK Reporter, Cloudlog, QRZ integrations, and help
 *
 * @author BGY70Z
 * @date 2026-02-02
 */
public class CloudSettingsFragment extends Fragment {

    private FragmentCloudSettingsBinding binding;
    private MainViewModel mainViewModel;
    private SettingsBackupManager backupManager;

    // Activity result launcher for backup (create document)
    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performBackup(uri);
                    }
                }
            });

    // Activity result launcher for restore (open document)
    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performRestore(uri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentCloudSettingsBinding.inflate(inflater, container, false);
        mainViewModel = MainViewModel.getInstance(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize backup manager
        if (mainViewModel != null && mainViewModel.databaseOpr != null) {
            backupManager = new SettingsBackupManager(requireContext(), mainViewModel.databaseOpr);
        }

        initializeControls();
        setupListeners();
    }

    private void initializeControls() {
        // PSK Reporter
        binding.pskReporterReceiveSwitch.setChecked(GeneralVariables.enablePskReporterReceive);

        // Cloudlog
        binding.enableCloudlogSwitch.setChecked(GeneralVariables.enableCloudlog);
        binding.cloudlogServerAddressEdit.setText(GeneralVariables.getCloudlogServerAddress());
        binding.cloudlogServerApiKeyEdit.setText(GeneralVariables.getCloudlogServerApiKey());
        binding.cloudlogStationIdEdit.setText(GeneralVariables.getCloudlogStationID());

        // QRZ
        binding.enableQrzSwitch.setChecked(GeneralVariables.enableQRZ);
        binding.qrzApiKeyTextEdit.setText(GeneralVariables.getQrzApiKey());
    }

    private void setupListeners() {
        // PSK Reporter Receive Switch
        binding.pskReporterReceiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.enablePskReporterReceive = isChecked;
            writeConfig("enablePskReporterReceive", isChecked ? "1" : "0");
            if (mainViewModel != null && mainViewModel.pskReporterMqtt != null) {
                if (isChecked) {
                    mainViewModel.startPskReporterMqtt();
                } else {
                    mainViewModel.stopPskReporterMqtt();
                }
            }
        });

        // View PSK Spots Button
        binding.pskReporterViewSpotsButton.setOnClickListener(v -> {
            PskReporterSpotsDialog dialog = new PskReporterSpotsDialog(getContext(), requireActivity());
            dialog.show();
        });

        // Cloudlog Enable Switch
        binding.enableCloudlogSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.enableCloudlog = isChecked;
            writeConfig("enableCloudlog", isChecked ? "1" : "0");
        });

        // Cloudlog Server Address
        binding.cloudlogServerAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.cloudlogServerAddress = s.toString();
                writeConfig("cloudlogServerAddress", GeneralVariables.getCloudlogServerAddress());
            }
        });

        // Cloudlog API Key
        binding.cloudlogServerApiKeyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.cloudlogApiKey = s.toString();
                writeConfig("cloudlogApiKey", GeneralVariables.getCloudlogServerApiKey());
            }
        });

        // Cloudlog Station ID
        binding.cloudlogStationIdEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.cloudlogStationID = s.toString();
                writeConfig("cloudlogStationID", GeneralVariables.getCloudlogStationID());
            }
        });

        // Cloudlog Test Button
        binding.cloudlogTestButton.setOnClickListener(v -> {
            testCloudlogConnection();
        });

        // QRZ Enable Switch
        binding.enableQrzSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.enableQRZ = isChecked;
            writeConfig("enableQRZ", isChecked ? "1" : "0");
        });

        // QRZ API Key
        binding.qrzApiKeyTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.qrzApiKey = s.toString();
                writeConfig("qrzApiKey", GeneralVariables.getQrzApiKey());
            }
        });

        // QRZ Test Button
        binding.qrzTestButton.setOnClickListener(v -> {
            testQrzConnection();
        });

        // Backup Settings Button
        binding.backupSettingsButton.setOnClickListener(v -> {
            startBackup();
        });

        // Restore Settings Button
        binding.restoreSettingsButton.setOnClickListener(v -> {
            startRestore();
        });

        // Clear Cache Button - shows dialog for clearing follow list data
        binding.clearCacheButton.setOnClickListener(v -> {
            if (mainViewModel != null && mainViewModel.databaseOpr != null) {
                new ClearCacheDataDialog(requireContext(), requireActivity(),
                        mainViewModel.databaseOpr, ClearCacheDataDialog.CACHE_MODE.FOLLOW_DATA);
            }
        });

        // Clear Cache Help Button
        binding.clearCacheImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "clear_cache_data.txt", true);
        });

        // Application Log Button
        binding.applicationLogButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.applicationLogFragment);
        });

        // FAQ Button
        binding.faqButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FAQActivity.class);
            startActivity(intent);
        });

        // About Button
        binding.aboutButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "readme.txt", true);
        });

        // Cloudlog Help Button
        binding.cloudlogImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "cloudlog.txt", true);
        });

        // QRZ Help Button
        binding.qrzImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "qrz.txt", true);
        });
    }

    private void startBackup() {
        if (backupManager == null) {
            ToastMessage.show("Backup not available");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, backupManager.getDefaultBackupFilename());
        backupLauncher.launch(intent);
    }

    private void performBackup(Uri uri) {
        if (backupManager == null) {
            return;
        }

        try {
            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                backupManager.writeBackupToStream(outputStream, new SettingsBackupManager.BackupCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            ToastMessage.show(getString(R.string.backup_success, message));
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            ToastMessage.show(getString(R.string.backup_failed, error));
                        });
                    }
                });
            }
        } catch (Exception e) {
            ToastMessage.show(getString(R.string.backup_failed, e.getMessage()));
        }
    }

    private void startRestore() {
        if (backupManager == null) {
            ToastMessage.show("Restore not available");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        restoreLauncher.launch(intent);
    }

    private void performRestore(Uri uri) {
        if (backupManager == null) {
            return;
        }

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                backupManager.readBackupFromStream(inputStream, new SettingsBackupManager.BackupCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            ToastMessage.show(getString(R.string.restore_success, message));
                            // Refresh the UI with restored settings
                            initializeControls();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            ToastMessage.show(getString(R.string.restore_failed, error));
                        });
                    }
                });
            }
        } catch (Exception e) {
            ToastMessage.show(getString(R.string.restore_failed, e.getMessage()));
        }
    }

    private void testCloudlogConnection() {
        // Show testing message - actual test to be implemented
        String server = GeneralVariables.getCloudlogServerAddress();
        String apiKey = GeneralVariables.getCloudlogServerApiKey();

        if (server == null || server.isEmpty()) {
            ToastMessage.show("Please enter server address first");
            return;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            ToastMessage.show("Please enter API key first");
            return;
        }

        ToastMessage.show("Cloudlog settings saved. Test when uploading QSO.");
    }

    private void testQrzConnection() {
        // Show testing message - actual test to be implemented
        String apiKey = GeneralVariables.getQrzApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            ToastMessage.show("Please enter QRZ API key first");
            return;
        }

        ToastMessage.show("QRZ settings saved. Test when uploading QSO.");
    }

    private void writeConfig(String key, String value) {
        if (mainViewModel != null && mainViewModel.databaseOpr != null) {
            mainViewModel.databaseOpr.writeConfig(key, value, null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
