package com.android.kinoko.camera2.util;

import android.util.Log;

public class JniCameraCall {
	static {
		Log.d("JNI" ,"JNI CAMERA CALL init");
		System.loadLibrary("hdmiinput_jni");
	}
	public static native void openDevice();
	public static native void closeDevice();
	public static native int[] getFormat();
}

