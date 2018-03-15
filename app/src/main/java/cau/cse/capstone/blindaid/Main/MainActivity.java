package cau.cse.capstone.blindaid.Main;


import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import java.util.ArrayList;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;


import cau.cse.capstone.blindaid.R;

public class MainActivity extends AppCompatActivity {

    private TextureView mCameraTextureView;
    private Preview mPreview;
    private Intent i;
    private String sst_result = "";

    Activity mainActivity = this;
    private static final String TAG = "MAINACTIVITY";
    static final int REQUEST_CAMERA = 1;
    private static final int RESULT_SPEECH = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Speech to Text
        while(sst_result.equals("")) {
            i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak word");

            try {
                startActivityForResult(i, RESULT_SPEECH);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(), "No support speech to text", Toast.LENGTH_LONG).show();
                e.getStackTrace();
            }
        }
        //Display Preview
        mCameraTextureView = (TextureView) findViewById(R.id.cameraTextureView);
        mPreview = new Preview(this, mCameraTextureView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            mCameraTextureView = (TextureView) findViewById(R.id.cameraTextureView);
                            mPreview = new Preview(mainActivity, mCameraTextureView);
                            Log.d(TAG,"mPreview set");
                        } else {
                            Toast.makeText(this,"Should have camera permission to run", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && (requestCode == RESULT_SPEECH)){
            ArrayList<String> sstResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            sst_result = sstResult.get(0);
        }
    }
}

