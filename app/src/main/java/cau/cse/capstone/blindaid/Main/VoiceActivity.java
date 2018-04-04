package cau.cse.capstone.blindaid.Main;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Locale;

import cau.cse.capstone.blindaid.R;

public class VoiceActivity extends Activity {
    private static final String TAG = "VoiceActivity";
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int REQ_CODE_CHECK_SPEECH = 101;
    private MediaPlayer mMediaPlayer;
    private TextToSpeech tts;
    private String answer;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        grantPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        // Set Media File to MediaPlayer
        mMediaPlayer = MediaPlayer.create(this, R.raw.tellmewhatyouwant);

        // Set OnClickListener on btnSpeak
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        // Auto Start
        letUserSaying();
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechInput.setText(result.get(0));

                    // Ask user to Check the text you want to find
                    tts.speak("You said" + txtSpeechInput.getText().toString() + "Right?" +
                            "Please say yes or no", TextToSpeech.QUEUE_ADD, null);

                    // Speech Recognition( Delay 1.5s )
                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() {
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                                    getString(R.string.speech_prompt));
                            try {
                                startActivityForResult(intent, REQ_CODE_CHECK_SPEECH);
                            } catch (ActivityNotFoundException a) {
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.speech_not_supported),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, 3000);
                }
                break;
            }
            case REQ_CODE_CHECK_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    answer = result.get(0).toString();
                    System.out.println(answer);
                    if(checkSpeech(answer) == true){
                        // Release resource
                        mMediaPlayer.release();
                        tts.shutdown();

                        // Start MainActivity
                        Intent i = new Intent(this, MainActivity.class);
                        i.putExtra("Text", txtSpeechInput.getText().toString());
                        startActivity(i);
                        finish();
                    }
                    else
                    {
                        letUserSaying();
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private boolean grantPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    && (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED)
                    && (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        } else {
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                //resume tasks needing this permission
            }
        }
    }

    public boolean checkSpeech(String answer){
        // if say yes
        if(answer.equals("yes"))
            return true;
        // if say no or anything
        else
            return false;
    }
    public void letUserSaying(){
        mMediaPlayer.start();
        // Speech Recognition(Auto Click Button using performClick() method)
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                btnSpeak.performClick();
            }
        }, 2000);
    }
}