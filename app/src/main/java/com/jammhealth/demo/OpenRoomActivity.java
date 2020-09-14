package com.jammhealth.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class OpenRoomActivity extends AppCompatActivity {
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private String roomURL;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    WebView webView = findViewById(R.id.webView);
                    webView.loadUrl(this.roomURL);

                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_room);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String roomURL = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        Log.v("OpenRoom", "Got room URL: " + roomURL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        WebView webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();

        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setBlockNetworkLoads(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            settings.setDatabasePath("/data/data/" + webView.getContext().getPackageName() + "/databases/");
        }

        webView.setWebChromeClient(new WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            webView.loadUrl(roomURL);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
            }
            // Ask for the permission.
            this.roomURL = roomURL;
            requestPermissions(new String[] { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO }, MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }
}