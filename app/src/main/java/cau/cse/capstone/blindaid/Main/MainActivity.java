package cau.cse.capstone.blindaid.Main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

import cau.cse.capstone.blindaid.R;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    public static int detect_count = 0;
    public static boolean detect_state = false;
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private String find_str;
    private Mat matInput;
    private Mat matResult;
    private Mat matLegacy;
    private Vector<Classifier.Recognition> all_find = new Vector<>();

    private Size previewSize;
    private int rotation;
    private boolean flagUpdown = false;
    private boolean flagLeftright = false;
    private Detector detector;

    private Handler handler;
    private HandlerThread handlerThread;

    // Text to speech
    private TextToSpeech tts;
    private int[] lr_flag = {0, 0, 0};
    private int[] ud_flag = {0, 0, 0};

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //grantCameraPermission();

        // Text To Speech
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.i("OnUtteranceProgresserListener", "onStart");
                        }

                        @Override
                        public void onDone(String utteranceId) {

                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.i("OnUtteranceProgresserListener", "onError");
                        }
                    });
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setSpeechRate((float) 0.7);
                }
            }
        });

        find_str = getIntent().getExtras().getString("Text").toString();

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
                toastMesage = "Java Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        previewSize = new Size(width, height);
        Log.i("previewSize_H : ", previewSize.getHeight() + "");
        Log.i("previewSize_W : ", previewSize.getWidth() + "");
        //rotation = (int)mOpenCvCameraView.getRotation();
        detector = new Detector(getApplicationContext(), previewSize, 90, find_str);

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        detector.setHandler(handler);
    }

    public void onCameraViewStopped() {

    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        if (matResult != null) matResult.release();

        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        Bitmap bmp = Bitmap.createBitmap(matInput.cols(), matInput.rows(), Bitmap.Config.ARGB_8888);


        try {
            Utils.matToBitmap(matInput, bmp);
        } catch (Exception e) {
            Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            return null;
        }

        detector.processImage(bmp);

        if (matResult != null) {
            Utils.bitmapToMat(drawRect(bmp), matResult);
            matLegacy = matResult;
            matResult = null;
        }
        return matLegacy;
    }

    // draw 4 guide lines using 2 rectangles
    private void drawCenter(Canvas canvas) {

        final int guideLeft = canvas.getWidth() / 2 - 50;
        final int guideRight = canvas.getWidth() / 2 + 50;
        final int guideTop = canvas.getHeight() / 2 - 50;
        final int guideBottom = canvas.getHeight() / 2 + 50;


        Paint paintCenter = new Paint();
        final RectF rectCenter = new RectF(guideLeft, -50, guideRight, canvas.getHeight() + 50); //세로사각형 캔버스 크기로 받아오기
        final RectF rectCenter2 = new RectF(-50, guideTop, canvas.getWidth() + 50, guideBottom); //가로사각형

        int color = Color.argb(255, 255, 187, 0);
        paintCenter.setColor(color);
        paintCenter.setStyle(Paint.Style.STROKE);
        paintCenter.setStrokeWidth(10);
        //paintCenter.setStrokeCap(Paint.Cap.ROUND);
        paintCenter.setAntiAlias(true);
        canvas.drawRoundRect(rectCenter, 0, 0, paintCenter);
        canvas.drawRoundRect(rectCenter2, 0, 0, paintCenter);

    }

    private Bitmap drawRect(Bitmap bmp) {
        // Processing Frame which is converted to bitmap ARGB_9999 format

        Canvas canvas = new Canvas(bmp);

        final int guideLeft = canvas.getWidth() / 2 - 50;
        final int guideRight = canvas.getWidth() / 2 + 50;
        final int guideTop = canvas.getHeight() / 2 - 50;
        final int guideBottom = canvas.getHeight() / 2 + 50;

        Paint paint = new Paint();
        Paint paintText = new Paint();
        Paint guideText = new Paint();
        android.graphics.Rect bounds = new android.graphics.Rect();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));


        paintText.setColor(Color.BLUE);
        paintText.setTextSize(50);
        drawCenter(canvas);

        List<Classifier.Recognition> mappedRecognitions = TensorFlowObjectDetectionAPIModel.getResults();
        if (mappedRecognitions == null) {
            return bmp;
        }

        // Detected Object > 0
        if (mappedRecognitions.size() > 0) {
            for (Classifier.Recognition recognition : mappedRecognitions) {
                final RectF location = recognition.getLocation();

                // Test
                //String sample = "mouse";
                String sample = find_str.toLowerCase().toString();
                String word = recognition.getTitle();
                Log.i("Detected Object : ", word);

                //내가 찾고자하는 물체가 카메라 프레임 안에 있는 경우
                if (sample.equals(word)) {  // SpeechText와 원래 비교하지만 sample타겟 저장.
                    //guideText : 좌우상하 타겟존재여부 텍스트로 알려주는 테스트용
                    guideText.setColor(Color.RED);
                    guideText.setTextSize(100);
                    canvas.drawText("Target Detect", 100, 900, guideText);
                    if (detect_state == false && !tts.isSpeaking()) {
                        tts.speak("Target Detected", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    detect_count = 0;
                    detect_state = true;
                    paint.setColor(Color.RED);
                    paintText.setColor(Color.RED); ///  final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); vibrator.vibrate(500);


                    //좌우비교
                    //location.left >> 포착된 타켓 물체의 바운더리사각형의 왼쪽 변. right,top,bottom 마찬가지
                    //guideRight는 가이드 세로선 2개 중 오른쪽, left,top,bottom 마찬가지
                    if ((location.left > guideRight) || (location.left >= guideLeft && location.left <= guideRight && location.right > guideRight)) {
                        canvas.drawText("우", 100, 100, guideText);
                        flagLeftright = false;
                        if(lr_flag[0] == 0 && !tts.isSpeaking()){
                            lr_flag[0] = 1;
                            lr_flag[1] = 0;
                            lr_flag[2] = 0;
                            tts.speak("Right", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else if ((location.right < guideLeft) || (location.right >= guideLeft && location.right <= guideRight && location.left < guideLeft)) {
                        canvas.drawText("좌", 100, 100, guideText);
                        flagLeftright = false;
                        if(lr_flag[1] == 0 && !tts.isSpeaking()){
                            lr_flag[1] = 1;
                            lr_flag[0] = 0;
                            lr_flag[2] = 0;
                            tts.speak("Left", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else if (location.left < guideLeft && location.right > guideRight) {
                        canvas.drawText("좌우맞아", 100, 100, guideText);
                        flagLeftright = true;
                        if(lr_flag[2] == 0 && !tts.isSpeaking()){
                            lr_flag[2] = 1;
                            lr_flag[0] = 0;
                            lr_flag[1] = 0;
                            ud_flag[0] = 0;
                            ud_flag[1] = 0;
                            tts.speak("left right good", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }

                    //상하비교
                    if ((location.top > guideBottom) || (location.top >= guideTop && location.top <= guideBottom && location.bottom > guideBottom)) {
                        canvas.drawText("하", 100, 200, guideText);
                        flagUpdown = false;
                        if(ud_flag[0] == 0 && !tts.isSpeaking()){
                            ud_flag[0] = 1;
                            ud_flag[1] = 0;
                            ud_flag[2] = 0;
                            tts.speak("Down", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    } else if ((location.bottom < guideTop) || (location.bottom >= guideTop && location.bottom <= guideBottom && location.top < guideTop)) {
                        canvas.drawText("상", 100, 200, guideText);
                        flagUpdown = false;
                        if(ud_flag[1] == 0 && !tts.isSpeaking()){
                            ud_flag[1] = 1;
                            ud_flag[0] = 0;
                            ud_flag[2] = 0;
                            tts.speak("Up", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else if (location.top < guideTop && location.bottom > guideBottom) {
                        canvas.drawText("상하맞아", 100, 200, guideText);
                        flagUpdown = true;
                        if(ud_flag[2] == 0 && !tts.isSpeaking()){
                            ud_flag[2] = 1;
                            ud_flag[0] = 0;
                            ud_flag[1] = 0;
                            lr_flag[0] = 0;
                            lr_flag[1] = 0;
                            tts.speak( "up down good", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    //좌우도 맞고 상하도 맞는 경우 ->  진동
                    if (flagUpdown && flagLeftright) {
                        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(500);
                    }

                    //찾고자 하는 물체가 프레임에 없는 경우
                } else {
                    detect_count++;

                    if (detect_count == 70) {
                        detect_state = false;
                        detect_count = 0;
                        guideText.setColor(Color.RED);
                        guideText.setTextSize(100);
                        canvas.drawText("Target Disappear", 100, 900, guideText);
                        if (!tts.isSpeaking()) {
                            tts.speak("Target Disappear", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    paint.setColor(Color.BLUE);
                    paintText.setColor(Color.BLUE);
                }
                if (location != null) {
                    // Draw rect on canvas
                    canvas.drawRoundRect(location, 30, 30, paint);

                    // Draw label on canvas
                    paintText.getTextBounds(word, 0, word.length(), bounds);
                    canvas.drawText(word, location.centerX(), location.bottom - 15, paintText);
                }
            }
        }
        return bmp;
    }

    private boolean grantCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        } else {
            Toast.makeText(this, "CAMERA Permission is Grant", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "CAMERA Permission is Grant ");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                //resume tasks needing this permission
            }
        }
    }

    public void sayDistancefromScreen() {

    }
}