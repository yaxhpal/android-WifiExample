package com.yashpalmeena.wifiexample;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Subclass this activity when you want to force the connection to a specific wifi network at
 * the start of the activity
 */
public abstract class WifiBaseActivity extends AppCompatActivity {

    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String OPEN = "Open";

    private static final int REQUEST_ENABLE_WIFI = 10;

    private final ScheduledExecutorService mWorker = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture mTaskHandler;
    private ScanReceiver mScanReceiver;
    private ProgressDialog mProgressDialog;
    private ConnectionReceiver mConnectionReceiver;
    private WifiManager mWifiManager;

    /**
     * Get the timeout in seconds for connecting to the wifi network
     *
     * @return wifi network connection timeout
     */
    protected abstract int getSecondsTimeout();

    /**
     * Get the SSID of the wifi network
     *
     * @return SSID of wifi network to connect to
     */
    protected abstract String getWifiSSID();

    /**
     * Get the password/key of the wifi network
     *
     * @return passwork/key of wifi network
     */
    protected abstract String getWifiPass();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    }

    /**
     * Start connecting to specific wifi network
     */
    protected void handleWIFI() {
        if (mWifiManager.isWifiEnabled()) {
            connectToSpecificNetwork();
        } else {
            showWifiDisabledDialog();
        }
    }

    /**
     * Ask user to go to settings and enable wifi
     */
    private void showWifiDisabledDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.wifi_disabled))
                .setPositiveButton(getString(R.string.enable_wifi), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // open settings screen
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        startActivityForResult(intent, REQUEST_ENABLE_WIFI);
                    }
                })
                .setNegativeButton(getString(R.string.exit_app), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    /**
     * Get the security type of the wireless network
     *
     * @param scanResult the wifi scan result
     * @return one of WEP, PSK of OPEN
     */
    private String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = {WEP, PSK};
        for (String model: securityModes) {
            if (cap.contains(model)) {
                return model;
            }
        }
        return OPEN;
    }

    // User has returned from settings screen. Check if wifi is enabled
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_WIFI && resultCode == 0) {
            WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
            if (wifi.isWifiEnabled() || wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                connectToSpecificNetwork();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }


    /**
     * Start to connect to a specific wifi network
     */
    private void connectToSpecificNetwork() {
        final
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (networkInfo.isConnected() && wifiInfo.getSSID().replace("\"", "").equals(getWifiSSID())) {
            return;
        } else {
            mWifiManager.disconnect();
        }
        mProgressDialog = ProgressDialog.show(this, getString(R.string.connecting), String.format(getString(R.string.connecting_to_wifi), getWifiSSID()));
        mTaskHandler = mWorker.schedule(new TimeoutTask(), getSecondsTimeout(), TimeUnit.SECONDS);
        mScanReceiver = new ScanReceiver();
        registerReceiver(mScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }


    /**
     * Broadcast receiver for connection related events
     */
    private class ConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (networkInfo.isConnected()) {
                if (wifiInfo.getSSID().replace("\"", "").equals(getWifiSSID())) {
                    unregisterReceiver(this);
                    if (mTaskHandler != null) {
                        mTaskHandler.cancel(true);
                    }
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                }
            }
        }
    }


    /**
     * Broadcast receiver for wifi scanning related events
     */
    private class ScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("WIFI", "Scanning");
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResultList = wifi.getScanResults();
            boolean found = false;
            String security = null;
            for (ScanResult scanResult : scanResultList) {
                Log.e("WIFI", scanResult.SSID);
                if (scanResult.SSID.equals(getWifiSSID())) {
                    security = getScanResultSecurity(scanResult);
                    found = true;
                    break; // found don't need continue
                }
            }
            if (!found) {
                // if no wifi network with the specified ssid is not found exit
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                new AlertDialog.Builder(WifiBaseActivity.this)
                        .setCancelable(false)
                        .setMessage(String.format(getString(R.string.wifi_not_found), getWifiSSID()))
                        .setPositiveButton(getString(R.string.exit_app), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                unregisterReceiver(ScanReceiver.this);
                                finish();
                            }
                        })
                        .show();
            } else {
                // configure based on security
                final WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + getWifiSSID() + "\"";
                switch (security) {
                    case WEP:
                        conf.wepKeys[0] = "\"" + getWifiPass() + "\"";
                        conf.wepTxKeyIndex = 0;
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        break;
                    case PSK:
                        conf.preSharedKey = "\"" + getWifiPass() + "\"";
                        break;
                    case OPEN:
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        break;
                }
                mConnectionReceiver = new ConnectionReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                registerReceiver(mConnectionReceiver, intentFilter);
                int netId = wifi.addNetwork(conf);
                wifi.disconnect();
                wifi.enableNetwork(netId, true);
                wifi.reconnect();
                unregisterReceiver(this);
            }
        }
    }

    /**
     * Timeout task. Called when timeout is reached
     */
    private class TimeoutTask implements Runnable {
        @Override
        public void run() {
            WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            if (networkInfo.isConnected() && wifiInfo.getSSID().replace("\"", "").equals(getWifiSSID())) {
                try {
                    unregisterReceiver(mConnectionReceiver);
                } catch (Exception ex) {
                    // ignore if receiver already unregistered
                }
                WifiBaseActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                    }
                });
            } else {
                try {
                    unregisterReceiver(mConnectionReceiver);
                } catch (Exception ex) {
                    // ignore if receiver already unregistered
                }
                WifiBaseActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        new AlertDialog.Builder(WifiBaseActivity.this)
                                .setCancelable(false)
                                .setMessage(String.format(getString(R.string.wifi_not_connected), getWifiSSID()))
                                .setPositiveButton(getString(R.string.exit_app), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .show();
                    }

                });
            }
        }
    }


}
