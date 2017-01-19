package com.example.yisha.findstandalone;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


public class MainActivity extends AppCompatActivity {
    // Constants
    private static final String URL_STRING = "http://158.132.237.122:8003";
    private static final String GROUP = "CRESCENDO";
    private static final String USERNAME = "YISHA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.Track);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click

                JSONObject obj = startTracking();
                if(obj != null) {
                    JSONObject fingerprint = getFingerprint(obj);
                    sendData(fingerprint);
//                    getLocation();
                }
            }
        });
    }

    private JSONObject getFingerprint(JSONObject obj) {
        // Create fingerprints for tracking Post request
        JSONObject result = new JSONObject();
        JSONArray network_data;
        JSONObject tmp_json;
        JSONArray result_fp = new JSONArray();

        try {
            network_data = obj.getJSONArray("available");
            for(int i = 0; i < network_data.length(); i++) {
                tmp_json = network_data.getJSONObject(i);

                JSONObject ap = new JSONObject();
                ap.put("mac", tmp_json.getString("BSSID"));
                ap.put("rssi", tmp_json.getInt("level"));

                result_fp.put(ap);
            }

            result.put("group", GROUP);
            result.put("username", USERNAME);
            result.put("location", "tracking");
            result.put("time", (new Date()).getTime());
            result.put("wifi-fingerprint", result_fp);

//            Log.d("mainlog", "Result: " + result);
            return result;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendData(JSONObject obj) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = URL_STRING + "/track";

        Log.d("mainlog", "Track obj: " + obj);

        // Request a string response from the provided URL.
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST, url, obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("mainlog", response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d("mainlog", "Error: " + error.getMessage());
                    }
                }) {
            // Passing some request headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        queue.add(jsonObjReq);
    }

    private void getLocation() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = URL_STRING + "?group=" + GROUP + "&username=" + USERNAME;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("mainlog", "Get: response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("mainlog", "Get didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private JSONObject startTracking() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        JSONObject obj = new JSONObject();
        try {
            JSONObject activity = new JSONObject();
            activity.put("BSSID", wifiInfo.getBSSID());
            activity.put("HiddenSSID", wifiInfo.getHiddenSSID());
            activity.put("SSID", wifiInfo.getSSID());
            activity.put("MacAddress", wifiInfo.getMacAddress());
            activity.put("IpAddress", wifiInfo.getIpAddress());
            activity.put("NetworkId", wifiInfo.getNetworkId());
            activity.put("RSSI", wifiInfo.getRssi());
            activity.put("LinkSpeed", wifiInfo.getLinkSpeed());
            obj.put("activity", activity);

            JSONArray available = new JSONArray();
//			List<ScanResult> tmp_scan_result = wifiManager.getScanResults();

            for (ScanResult scanResult : wifiManager.getScanResults()) {
                JSONObject ap = new JSONObject();
                ap.put("BSSID", scanResult.BSSID);
                ap.put("SSID", scanResult.SSID);
                ap.put("frequency", scanResult.frequency);
                ap.put("level", scanResult.level);
                //netwrok.put("timestamp", String.valueOf(scanResult.timestamp));
                ap.put("capabilities", scanResult.capabilities);
                available.put(ap);
            }
            Log.d("wifid", "scan result\n");
            obj.put("available", available);

            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


}
