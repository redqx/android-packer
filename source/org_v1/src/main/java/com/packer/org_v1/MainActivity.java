package com.packer.org_v1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends Activity
{
    static {
        System.loadLibrary("demo");
    }
    public native String stringFromJNI();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.i("source", "[source-onCreate] =>i am source apk" );
        findViewById(R.id.bt1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, stringFromJNI()+"功德无量", Toast.LENGTH_SHORT).show();
            }
        });

    }
}