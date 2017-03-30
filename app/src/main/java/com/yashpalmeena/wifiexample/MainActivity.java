package com.yashpalmeena.wifiexample;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MainActivity extends WifiBaseActivity {

    private static final String DEBUG_TAG = "WifiExampleMainActivity";

    @Override
    protected int getSecondsTimeout() {
        return 30;
    }

    @Override
    protected String getWifiSSID() {
        return "my phone";
    }

    @Override
    protected String getWifiPass() {
        return "9871230550";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        handleWIFI();
    }


    public void testWifi(View v) {
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null) {
            Log.d(DEBUG_TAG, "Wifi connected: " + activeNetwork);
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                Toast.makeText(getApplicationContext(), activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                Toast.makeText(getApplicationContext(), activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),"Not Connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void connectToMeetingRoom(View v) {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        boolean isWifi = wifiManager.isWifiEnabled();
        if (isWifi) {
            wifiManager.disconnect();
            boolean newConfig = setSsidAndPassword(this, "Meeting Room", "Avanti123");
            if (newConfig) {
                wifiManager.reconnect();
                Toast.makeText(getApplicationContext(),"Conected to Meeting Room", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),"Wifi is disabled.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean setSsidAndPassword(Context context, String ssid, String ssidPassword) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            wifiConfig.SSID = ssid;
            wifiConfig.preSharedKey = ssidPassword;

            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
