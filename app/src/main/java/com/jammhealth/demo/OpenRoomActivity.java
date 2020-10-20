package com.jammhealth.demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

class JSBridge {
    AppCompatActivity activity = null;

    public JSBridge(AppCompatActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void postMessage(String dataJson) {
        try {
            JSONObject data = new JSONObject(dataJson);
            Log.d("OpenRoom", "Received Message " + data.getString("type"));
            if (data.getString("type").equals("end") || data.getString("type").equals("disconnected")) {
                // Finish the activity if they click the end button or disconnect
                this.activity.finish();
            }
            if (data.getString("type").equals("recorded") || data.getString("type").equals("converted")) {
                Log.d("OpenRoom", "Saving recorded video to external storage");
                ActivityCompat.requestPermissions(this.activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                byte[] mediaBlob = Base64.decode(data.getString("mediaBlob"), Base64.DEFAULT);
                File filepath = Environment.getExternalStorageDirectory();
                File dir = new File(filepath.getAbsolutePath() + "/cliniscape/");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File newFile;
                if (data.getString("mediaType").equals("video/webm")) {
                    newFile = new File(dir, "save_" + System.currentTimeMillis() + ".webm");
                } else if (data.getString("mediaType").equals("audio/wav")) {
                    newFile = new File(dir, "save_" + System.currentTimeMillis() + ".wav");
                } else if (data.getString("mediaType").equals("video/mp4")) {
                    newFile = new File(dir, "save_" + System.currentTimeMillis() + ".mp4");
                } else {
                    Log.v("OpenRoom", "Unknown file type: " + data.getString("mediaType"));
                    return;
                }

                if (newFile.exists()) newFile.delete();

                OutputStream out = new FileOutputStream(newFile);

                // Copy the bits
                out.write(mediaBlob, 0, mediaBlob.length);
                out.close();
                Log.v("OpenRoom", "Copy file successful.");
            }
        } catch(Exception e) {
            Log.e("OpenRoom", "Failed to parse JSON message " + dataJson);
        }
    }
}

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
    public void onConfigurationChanged(Configuration newConfig) {
        // This prevents the view being recreated when the orientation changes
        super.onConfigurationChanged(newConfig);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_room);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String roomURL = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        Log.v("OpenRoom", "Got room URL: " + roomURL);

        WebView.setWebContentsDebuggingEnabled(true);
        WebView webView = findViewById(R.id.webView);
        webView.addJavascriptInterface(new JSBridge(this), "JSBridge");
        WebSettings settings = webView.getSettings();

        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setBlockNetworkLoads(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setDatabasePath("/data/data/" + webView.getContext().getPackageName() + "/databases/");
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // Listen for requests to access camera and microphone and grant them
                // may want to add a check to verify the domain here
                request.grant(request.getResources());
            }
        });

        webView.setWebViewClient(new WebViewClient(){
            private boolean loaded = false;
            @Override
            public void onPageFinished(WebView view, String url) {
                // Page has finished loading, inject the javascript to forward messages to our bridge
                if (!loaded) {
                    view.evaluateJavascript("(function() { window.addEventListener('message', function(event) { if (event.data.type === 'recorded' || event.data.type === 'converted') { var reader = new window.FileReader();reader.readAsDataURL(event.data.mediaBlob);reader.onloadend = () => { const base64data = reader.result;JSBridge.postMessage(JSON.stringify({ type: event.data.type, mediaBlob: base64data, mediaType: event.data.mediaBlob.type }));}} else { JSBridge.postMessage(JSON.stringify(event.data)); }});})();", null);
                    loaded = true;
                }
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