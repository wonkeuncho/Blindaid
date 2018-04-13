package cau.cse.capstone.blindaid.Main;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import cau.cse.capstone.blindaid.R;

public class VoiceActivity extends Activity {
    private static final String TAG = "VoiceActivity";
    private TextView txtSpeechInput;
    private TextToSpeech tts;
    private SpeechRecognizer sr;
    private ImageButton btn_speak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        grantPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        btn_speak = (ImageButton) findViewById(R.id.btnSpeak);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                    letUserSaying();
                }
            }
        });
        // Set SpeechRecognizer
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(recognitionListener);
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
        btn_speak.setImageResource(R.drawable.ico_mic);
        tts.speak("Tell me what you want to find", TextToSpeech.QUEUE_FLUSH, null);

        // Speech Recognition
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        getString(R.string.speech_prompt));
                sr.startListening(intent);
                btn_speak.setImageResource(R.drawable.ico_speak);
            }
        }, 2000);
    }

    public void letUserCheckSaying(){
        btn_speak.setImageResource(R.drawable.ico_mic);
        // Ask user to Check the text you want to find
        tts.speak("You said " + txtSpeechInput.getText().toString() + "  Right?" +
                "Please say yes or no", TextToSpeech.QUEUE_FLUSH, null);

        // Speech Recognition( Delay 1.5s )
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        getString(R.string.speech_prompt));
                sr.startListening(intent);
                btn_speak.setImageResource(R.drawable.ico_speak);
            }
        }, 3000);

    }

    private RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {
            Log.i("Speech", "Error_" + error);
            switch (error) {
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(VoiceActivity.this, "Check Permission, Use Android Settings", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    if(txtSpeechInput.getText().toString().equals("")){
                        letUserSaying();
                    }
                    else
                        letUserCheckSaying();
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, result.get(0));

            if(txtSpeechInput.getText().toString().equals("")){
                txtSpeechInput.setText(result.get(0).toString());
                letUserCheckSaying();
            }
            else{
                if(checkSpeech(result.get(0).toString())){
                    // Release resource
                    //mMediaPlayer.release();
                    tts.shutdown();

                    // Start MainActivity
                    Intent i = new Intent(VoiceActivity.this, MainActivity.class);
                    i.putExtra("Text", txtSpeechInput.getText().toString());
                    startActivity(i);
                    finish();
                }
                else{
                    letUserSaying();
                    txtSpeechInput.setText("");
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    };
}