package com.z3pipe.bigdipper.fragment;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.z3pipe.bigdipper.R;
import com.z3pipe.bigdipper.activity.MainActivity;
import com.z3pipe.bigdipper.ui.dialog.LoginDialog;
import com.z3pipe.z3location.broadcast.AutostartReceiver;
import com.z3pipe.z3location.service.PositionService;

import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.z3pipe.bigdipper.R;
import com.z3pipe.bigdipper.activity.MainActivity;
import com.z3pipe.z3location.broadcast.AutostartReceiver;
import com.z3pipe.z3location.service.PositionService;
import com.z3pipe.z3location.service.WatchDogService;

/**
 * @link https://www.z3pipe.com
 * @author zhengzhuanzi
 * @date 2019-04-10
 */
public class MainFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private static final String TAG = MainFragment.class.getSimpleName();

    private static final int ALARM_MANAGER_INTERVAL = 15000;

    public static final String KEY_DEVICE = "id";
    public static final String KEY_URL = "url";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_ACCURACY = "accuracy";
    public static final String KEY_STATUS = "status";

    private static final int PERMISSIONS_REQUEST_LOCATION = 2;

    private SharedPreferences sharedPreferences;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private LoginDialog dialog;
    private long lastTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //移除App的icon
        //removeLauncherIcon();

        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        addPreferencesFromResource(R.xml.preferences);
        initPreferences();

//        findPreference(KEY_DEVICE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                return newValue != null && !newValue.equals("");
//            }
//        });
//        findPreference(KEY_URL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                return (newValue != null) && validateServerURL(newValue.toString());
//            }
//        });
//
//        findPreference(KEY_INTERVAL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                if (newValue != null) {
//                    try {
//                        int value = Integer.parseInt((String) newValue);
//                        return value > 0;
//                    } catch (NumberFormatException e) {
//                        Log.w(TAG, e);
//                    }
//                }
//                return false;
//            }
//        });
//
//        Preference.OnPreferenceChangeListener numberValidationListener = new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                if (newValue != null) {
//                    try {
//                        int value = Integer.parseInt((String) newValue);
//                        return value >= 0;
//                    } catch (NumberFormatException e) {
//                        Log.w(TAG, e);
//                    }
//                }
//                return false;
//            }
//        };
//        findPreference(KEY_DISTANCE).setOnPreferenceChangeListener(numberValidationListener);
//        findPreference(KEY_ANGLE).setOnPreferenceChangeListener(numberValidationListener);

        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), AutostartReceiver.class), 0);

        if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
            startTrackingService(true, false);
        }
    }

    private void removeLauncherIcon() {
        String className = MainActivity.class.getCanonicalName().replace(".MainActivity", ".Launcher");
        ComponentName componentName = new ComponentName(getActivity().getPackageName(), className);
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            packageManager.setComponentEnabledSetting(
                    componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getString(R.string.hidden_alert));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        showLoginDialog();
    }

    private void showLoginDialog(){
        if(dialog == null) {
            dialog = new LoginDialog(getActivity());
        }

        if(!isNeedShow(lastTime)) {
            return;
        }

        lastTime = System.currentTimeMillis();
        dialog.show();
    }

    private boolean isNeedShow(long lastTime) {
        long diff = System.currentTimeMillis() - lastTime;
        if(diff/1000/60/10 > 0) {
            return true;
        }

        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setPreferencesEnabled(boolean enabled) {
//        findPreference(KEY_DEVICE).setEnabled(enabled);
//        findPreference(KEY_URL).setEnabled(enabled);
//        findPreference(KEY_INTERVAL).setEnabled(enabled);
//        findPreference(KEY_DISTANCE).setEnabled(enabled);
//        findPreference(KEY_ANGLE).setEnabled(enabled);
//        findPreference(KEY_ACCURACY).setEnabled(enabled);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_STATUS)) {
            if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
                startTrackingService(true, false);
            } else {
                stopTrackingService();
            }
        } else if (key.equals(KEY_DEVICE)) {
            findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, null));
        }
    }

    private void initPreferences() {
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

//        if (!sharedPreferences.contains(KEY_DEVICE)) {
//            String id = String.valueOf(new Random().nextInt(900000) + 100000);
//            sharedPreferences.edit().putString(KEY_DEVICE, id).apply();
//            ((EditTextPreference) findPreference(KEY_DEVICE)).setText(id);
//        }
//        findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, null));
    }

    private void startTrackingService(boolean checkPermission, boolean permission) {
        if (checkPermission) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                permission = true;
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
                }
                return;
            }
        }

        if (permission) {
            setPreferencesEnabled(false);
            ContextCompat.startForegroundService(getActivity(), new Intent(getActivity(), PositionService.class));
            ContextCompat.startForegroundService(getActivity(), new Intent(getActivity(), WatchDogService.class));
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    ALARM_MANAGER_INTERVAL, ALARM_MANAGER_INTERVAL, alarmIntent);
        } else {
            sharedPreferences.edit().putBoolean(KEY_STATUS, false).apply();
            TwoStatePreference preference = (TwoStatePreference) findPreference(KEY_STATUS);
            preference.setChecked(false);
        }
    }

    private void stopTrackingService() {
        alarmManager.cancel(alarmIntent);
        PositionService.stopService(getActivity());
        //getActivity().stopService(new Intent(getActivity(), PositionService.class));
        Intent serviceIntent = new Intent(getActivity(), WatchDogService.class);
        getActivity().stopService(serviceIntent);
        setPreferencesEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            startTrackingService(false, granted);
        }
    }

    private boolean validateServerURL(String userUrl) {
        int port = Uri.parse(userUrl).getPort();
        if (URLUtil.isValidUrl(userUrl) && (port == -1 || (port > 0 && port <= 65535))
                && (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))) {
            return true;
        }
        Toast.makeText(getActivity(), R.string.error_msg_invalid_url, Toast.LENGTH_LONG).show();
        return false;
    }

}
