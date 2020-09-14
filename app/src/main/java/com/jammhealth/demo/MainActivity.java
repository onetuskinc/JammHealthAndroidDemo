package com.jammhealth.demo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.jammhealth.demo.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openRoom(View view) {
        Intent intent = new Intent(this, OpenRoomActivity.class);
        EditText roomText = (EditText) findViewById(R.id.roomText);
        String roomURL = roomText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, roomURL);
        startActivity(intent);
    }
}