/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cau.cse.capstone.blindaid.Main;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by seowo on 2018-04-03.
 */

// Detector Class based on "Tensorflow Object Detection API"
public class Detector {

    // Crop original frame by (300x300) input cropped frame
    private static final int TF_OD_API_INPUT_SIZE = 300;
    // Model file trained by ImageNet for Mobile Ver.
    private static String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    // Label file matched with Model file
    private static String TF_OD_API_LABELS_FILE =
            "file:///android_asset/coco_labels_list.txt";

    // Minimum detection confidence to track a detection
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    private static final boolean MAINTAIN_ASPECT = false;

    private Integer sensorOrientation;

    private Classifier detector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    public boolean computingDetection = false;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    Context context;

    int previewWidth = 0;
    int previewHeight = 0;

    Handler handler = null;

    public Detector() {
    }

    public Detector(Context context, final Size size, final int rotation, String target){
        this.context = context;

        AssetManager assetManager = context.getAssets();
        InputStream labelsInput = null;
        try {
            labelsInput = assetManager.open("fruit_labels_list.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = br.readLine()) != null) {
                if(line.compareTo(target) == 0){
                    Log.i("Textfile line : ", line);
                    // Model file trained by ImageNet Fruits for Mobile Ver.
                    /*TF_OD_API_MODEL_FILE =
                            "file:///android_asset/fruit_model.pb";*/
                    TF_OD_API_MODEL_FILE =
                            "file:///android_asset/test.pb";
                    // Label file matched with Model file
                    TF_OD_API_LABELS_FILE =
                            "file:///android_asset/fruit_labels_list.txt";
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            detector =
                    TensorFlowObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE);
        } catch (IOException e) {
            Toast toast = Toast.makeText(context.getApplicationContext()
                    , "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            e.printStackTrace();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation(context);

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    public void processImage(Bitmap rgbFrameBitmap){

        if(computingDetection){
            /**TODO
             * 이전 프레임의 처리가 아직 끝나지 않았을 때
             * Runnable로 processImage()를 미리 실행시킨다
             **/
            return;
        }
        computingDetection = true;

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final List<Classifier.Recognition> results
                                = detector.recognizeImage(croppedBitmap);

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        for(final Classifier.Recognition result : results){
                            final RectF location = result.getLocation();
                            if(location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API){
                                /**TODO
                                 * location의 위치를 이용하여 네모 그리기
                                 */
                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }

                        TensorFlowObjectDetectionAPIModel.setResults(mappedRecognitions);
                        computingDetection = false;
                    }
                }
        );
    }

    private synchronized void runInBackground(final Runnable r){
        if(handler != null){
            handler.post(r);
        }
    }

    private int getScreenOrientation(Context context){
        switch (((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation()){
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
}
