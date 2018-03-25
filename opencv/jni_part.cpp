#include <stdio.h>
#include <jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace std;
using namespace cv;

extern "C"
{
    JNIEXPORT jdouble JNICALL Java_com_CameraPreview(
        JNIEnv* env, jobject thiz, jint width, jint height,
        jbyteArray NV21FrameData, jintArray outPixels)
    {
        jbyte * pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);
        jint * poutPixels = env->GetIntArrayElements(outPixels, 0);
        


        return true;
    }
}