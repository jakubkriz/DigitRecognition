/* 
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
 * Copyright 2015 Jakub Kříž
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.digitrecognition;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the text correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback, 
  ShutterButton.OnShutterButtonListener {

  private static final String TAG = CaptureActivity.class.getSimpleName();


    /** Whether to use autofocus by default. */
//  public static final boolean DEFAULT_TOGGLE_AUTO_FOCUS = false;

  /** Whether to initially disable continuous-picture and continuous-video focus modes. */
  //public static final boolean DEFAULT_DISABLE_CONTINUOUS_FOCUS = true;
  
  /** Whether to initially reverse the image returned by the camera. */
  //public static final boolean DEFAULT_TOGGLE_REVERSED_IMAGE = false;
  
  /** Whether the light should be initially activated by default. */
  public static final boolean DEFAULT_TOGGLE_LIGHT = false;


  public static final String DEFAULT_SCRIPT_PATH = "http://www.fi.muni.cz/~xkriz9/recognition.cgi";

  /** Flag to display recognition-related statistics on the scanning screen. */
  //private static final boolean CONTINUOUS_DISPLAY_METADATA = true;
  
  /** Flag to enable display of the on-screen shutter button. */
  private static final boolean DISPLAY_SHUTTER_BUTTON = true;


  /** Resource to use for data file downloads. */
  //static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

  /** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
  //static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results
  
  // Context menu
  private static final int SETTINGS_ID = Menu.FIRST;
  private static final int ABOUT_ID = Menu.FIRST + 1;
  
  // Options menu, for copy to clipboard
  private static final int OPTIONS_COPY_RECOGNIZED_TEXT_ID = Menu.FIRST;
  private static final int OPTIONS_SHARE_RECOGNIZED_TEXT_ID = Menu.FIRST + 2;

  private CameraManager cameraManager;
  private CaptureActivityHandler handler;
  private ViewfinderView viewfinderView;
  private SurfaceView surfaceView;
  private SurfaceHolder surfaceHolder;
  private TextView statusViewBottom;
  private TextView statusViewTop;
  private TextView sentDataView;
//  private ImageView statusViewImage;
  private TextView ocrResultView;
  private View cameraButtonView;
  private View resultView;
  private View progressView;
  private OcrResult lastResult;
  private Bitmap lastBitmap;
  private boolean hasSurface;
  private ShutterButton shutterButton;
//  private boolean isEngineReady;
  //private boolean isPaused; //for continous mode
  private ProgressDialog indeterminateDialog;
  private SharedPreferences.OnSharedPreferenceChangeListener listener;
  private SharedPreferences prefs;
  private static boolean isFirstLaunch; // True if this is the first time the app is being run

  private float totalDataSent;

  private ImageButton buttonAgain;
  private ImageButton buttonDone;

  private String scriptPath;

  Handler getHandler() {
    return handler;
  }

  
  CameraManager getCameraManager() {
    return cameraManager;
  }
////bg  
//private BaseLoaderCallback  mOpenCVCallback = new BaseLoaderCallback(this) {
//      @Override
//      public void onManagerConnected(int status) {
//          switch (status) {
//              case LoaderCallbackInterface.SUCCESS:
//            	  Log.i(TAG, "OpenCV loaded successfully");
//            	  break;
//    	      default:
//    	    	  super.onManagerConnected(status);
//    	    	  break;
//          }
//    }
//};
////end
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    checkFirstLaunch(); //show help when new version is generated, ...
//      if (isFirstLaunch) {
          setDefaultPreferences();
//      }
//bg
//    Log.i(TAG, "Trying to load OpenCV library");
//    if (!OpenCVLoader.initAsync(OpenCVLoader..OPENCV_VERSION_2_4_2, this, mOpenCVCallback))
//    {
//      Log.e(TAG, "Cannot connect to OpenCV Manager");
//    }
//end  
    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); //progress bar used for loading of OCRHMM
    setContentView(R.layout.capture);
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    cameraButtonView = findViewById(R.id.camera_button_view);
    resultView = findViewById(R.id.result_view);
    
    statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
    registerForContextMenu(statusViewBottom);
    statusViewTop = (TextView) findViewById(R.id.status_view_top);
    registerForContextMenu(statusViewTop);

    buttonDone = (ImageButton) findViewById(R.id.button_done);
    buttonAgain = (ImageButton) findViewById(R.id.button_again);

    buttonDone.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
            if(getIntent() != null && "com.example.digitrecognition.GAUGE_RECOGNITION".equals(getIntent().getAction())) {
                Intent intent = new Intent("com.example.digitrecognition.GAUGE_RECOGNITION");

                if ("".equals(ocrResultView.getText().toString())) {
                }else{
                    int data = Integer.parseInt(ocrResultView.getText().toString());
                    Log.e("SENDING DATA CHECK ME", Integer.toString(data));
                    intent.putExtra("GAUGE", data);
                }
                setResult(Activity.RESULT_OK, intent);
            }
            finish();
     	}
	});
    buttonAgain.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
//            v.setBackgroundResource(R.drawable.back_pressed);
			resetStatusView();
		}
	});

//    statusViewImage = (ImageView) findViewById(R.id.status_view_image);
//    statusViewImage.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    
    handler = null;
    lastResult = null;
    hasSurface = false;
    
    // Camera shutter button
    if (DISPLAY_SHUTTER_BUTTON) {
      shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
      shutterButton.setOnShutterButtonListener(this);
    }

    ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
    registerForContextMenu(ocrResultView);

      sentDataView = (TextView) findViewById(R.id.sent_data_view);
      registerForContextMenu(sentDataView);

    progressView = (View) findViewById(R.id.indeterminate_progress_indicator_view);

    cameraManager = new CameraManager(getApplication());
    viewfinderView.setCameraManager(cameraManager);
    
    // Set listener to change the size of the viewfinder rectangle.
    viewfinderView.setOnTouchListener(new View.OnTouchListener() {
      int lastX = -1;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          lastX = -1;
          return true;
        case MotionEvent.ACTION_MOVE:
          int currentX = (int) event.getX();

          try {
            Rect rect = cameraManager.getFramingRect();

            final int BUFFER = 50;
            final int BIG_BUFFER = 60;
            if (lastX >= 0) {
            	if ((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER)) {
            		// Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
            		cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
            		//viewfinderView.removeResultText();
            	} else if ((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER)) {
            		// Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
            		cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
            		//viewfinderView.removeResultText();
            	}    
            }
          } catch (NullPointerException e) {
            Log.e(TAG, "Framing rect not available", e);
          }
          v.invalidate();
          lastX = currentX;
          return true;
        case MotionEvent.ACTION_UP:
          lastX = -1;
          return true;
        }
        return false;
      }
    });
    
//    isEngineReady = false;

    totalDataSent = 0;
  }


  @Override
  protected void onResume() {
    super.onResume();   
    resetStatusView();
    
//    String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
    //int previousOcrEngineMode = ocrEngineMode;
    
    retrievePreferences();
    
    // Set up the camera preview surface.
    surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    surfaceHolder = surfaceView.getHolder();
    if (!hasSurface) {
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    } else {
        // The activity was paused but not stopped, so the surface still exists. Therefore
        // surfaceCreated() won't be called, so init the camera here.
        initCamera(surfaceHolder);
    }
    
      // We already have the engine initialized, so just start the camera.
//      resumeOCR();
//    }
  }
  
  /** 
   * Method to start or restart recognition after the OCR engine has been initialized,
   * or after the app regains focus. Sets state related settings and OCR engine parameters,
   * and requests camera initialization.
   */
//  void resumeOCR() {
//    Log.d(TAG, "resumeOCR()");
//
//    // This method is called when Tesseract has already been successfully initialized, so set
//    // isEngineReady = true here.
////    isEngineReady = true;
//
//    //isPaused = false;
//
////    if (handler != null) {
////      handler.resetState();
////    }
////    if (baseApi != null) {
////      baseApi.setPageSegMode(pageSegmentationMode);
////      baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
////      baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
////    }
//
//    if (hasSurface) {
//      // The activity was paused but not stopped, so the surface still exists. Therefore
//      // surfaceCreated() won't be called, so init the camera here.
//      initCamera(surfaceHolder);
//    }
//  }
  
  /** Called when the shutter button is pressed in continuous mode. */
//  void onShutterButtonPressContinuous() {
//    isPaused = true;
//    handler.stop();  
//    if (lastResult != null) {
//      handleOcrDecode(lastResult);
//    } else {
//      Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
//      toast.setGravity(Gravity.TOP, 0, 0);
//      toast.show();
//      resumeContinuousDecoding();
//    }
//  }

  /** Called to resume recognition after translation in continuous mode. */
//  @SuppressWarnings("unused")
//  void resumeContinuousDecoding() {
//    isPaused = false;
//    resetStatusView();
//    setStatusViewForContinuous();
//    DecodeHandler.resetDecodeState();
//    handler.resetState();
//    if (shutterButton != null && DISPLAY_SHUTTER_BUTTON) {
//      shutterButton.setVisibility(View.VISIBLE);
//    }
//  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    Log.d(TAG, "surfaceCreated()");
    
    if (holder == null) {
      Log.e(TAG, "surfaceCreated gave us a null surface");
    }
    
    // Only initialize the camera if the OCR engine is ready to go.
    if (!hasSurface) {
      Log.d(TAG, "surfaceCreated(): calling initCamera()...");
      initCamera(holder);
    }
    hasSurface = true;
  }
  
  /** Initializes the camera and starts the handler to begin previewing. */
  private void initCamera(SurfaceHolder surfaceHolder) {
    Log.d(TAG, "initCamera()");
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }
    try {

      // Open and initialize the camera
      cameraManager.openDriver(surfaceHolder);
      
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      handler = new CaptureActivityHandler(this, cameraManager);
      
    } catch (IOException ioe) {
      showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
    }   
  }
  
  @Override
  protected void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
    }
    
    // Stop using the camera, to avoid conflicting with other camera-based apps
    cameraManager.closeDriver();

    if (!hasSurface) {
      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(this);
    }
    super.onPause();
  }

  void stopHandler() {
    if (handler != null) {
      handler.stop();
    }
  }

  @Override
  protected void onDestroy() {
//    if (baseApi != null) {
//      baseApi.end();
//    }
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {

      // First check if we're paused in continuous mode, and if so, just unpause.
//      if (isPaused) {
//        Log.d(TAG, "only resuming continuous recognition, not quitting...");
//        resumeContinuousDecoding();
//        return true;
//      }

      // Exit the app if we're not viewing an OCR result.
      if (lastResult == null) {
          if(getIntent() != null && "com.example.digitrecognition.GAUGE_RECOGNITION".equals(getIntent().getAction())) {
              Intent intent = new Intent("com.example.digitrecognition.GAUGE_RECOGNITION");
              //int data = Integer.parseInt(ocrResultView.getText().toString());
              //intent.putExtra("GAUGE", data);
              setResult(Activity.RESULT_CANCELED, intent);
          }
          finish();
        return true;
      } else {
        // Go back to previewing in regular OCR mode.
        resetStatusView();
        if (handler != null) {
          handler.sendEmptyMessage(R.id.restart_preview);
        }
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
//      if (isContinuousModeActive) {
//        onShutterButtonPressContinuous();
//      } else {

        if(!isNetworkAvailable(this)) {
            Toast toast = Toast.makeText(this,"No Internet connection available.",Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            finish(); //Calling this method to close this activity when internet is not available.
        }

        handler.hardwareShutterButtonClick();
//      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {      
      // Only perform autofocus if user is not holding down the button.
      if (event.getRepeatCount() == 0) {
        cameraManager.requestAutoFocus(500L);
      }
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //    MenuInflater inflater = getMenuInflater();
    //    inflater.inflate(R.menu.options_menu, menu);
    super.onCreateOptionsMenu(menu);
    menu.add(0, SETTINGS_ID, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
    menu.add(0, ABOUT_ID, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
    case SETTINGS_ID: {
      intent = new Intent().setClass(this, PreferencesActivity.class);
      startActivity(intent);
      break;
    }
    case ABOUT_ID: {
      intent = new Intent(this, HelpActivity.class);
      intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.ABOUT_PAGE);
      startActivity(intent);
      break;
    }
    }
    return super.onOptionsItemSelected(item);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

//  /** Sets the necessary language code values for the given OCR language. */
//  private boolean setSourceLanguage(String languageCode) {
//    sourceLanguageCodeOcr = languageCode;
//    sourceLanguageCodeTranslation = LanguageCodeHelper.mapLanguageCode(languageCode);
//    sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, languageCode);
//    return true;
//  }

//  /** Sets the necessary language code values for the translation target language. */
//  private boolean setTargetLanguage(String languageCode) {
//    targetLanguageCodeTranslation = languageCode;
//    targetLanguageReadable = LanguageCodeHelper.getTranslationLanguageName(this, languageCode);
//    return true;
//  }

  /** Finds the proper location on the SD card where we can save files. */
  private File getStorageDirectory() {
    //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));
    
    String state = null;
    try {
      state = Environment.getExternalStorageState();
    } catch (RuntimeException e) {
      Log.e(TAG, "Is the SD card visible?", e);
      showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
    }
    
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

      // We can read and write the media
      //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
      // For Android 2.2 and above
      
      try {
        return getExternalFilesDir(Environment.MEDIA_MOUNTED);
      } catch (NullPointerException e) {
        // We get an error here if the SD card is visible, but full
        Log.e(TAG, "External storage is unavailable");
        showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
      }
      
      //        } else {
      //          // For Android 2.1 and below, explicitly give the path as, for example,
      //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
      //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator + 
      //                  "Android" + File.separator + "data" + File.separator + getPackageName() + 
      //                  File.separator + "files" + File.separator);
      //        }
    
    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	// We can only read the media
    	Log.e(TAG, "External storage is read-only");
      showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
    } else {
    	// Something else is wrong. It may be one of many other states, but all we need
      // to know is we can neither read nor write
    	Log.e(TAG, "External storage is unavailable");
    	showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
    }
    return null;
  }

  /**
   * Requests initialization of the OCR engine with the given parameters.
   * 
   * @param storageRoot Path to location of the tessdata directory to use
   * @param languageCode Three-letter ISO 639-3 language code for OCR 
   * @param languageName Name of the language for OCR, for example, "English"
   */
//  private void initOcrEngine(File storageRoot, String languageCode, String languageName) {    
//    isEngineReady = false;
//    
//    // Set up the dialog box for the thermometer-style download progress indicator
//    if (dialog != null) {
//      dialog.dismiss();
//    }
//    dialog = new ProgressDialog(this);
    
//    // If we have a language that only runs using Cube, then set the ocrEngineMode to Cube
//    if (ocrEngineMode != TessBaseAPI.OEM_CUBE_ONLY) {
//      for (String s : CUBE_REQUIRED_LANGUAGES) {
//        if (s.equals(languageCode)) {
//          ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
//          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//          prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
//        }
//      }
//    }
//
//    // If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
//    if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
//      boolean cubeOk = false;
//      for (String s : CUBE_SUPPORTED_LANGUAGES) {
//        if (s.equals(languageCode)) {
//          cubeOk = true;
//        }
//      }
//      if (!cubeOk) {
//        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
//      }
//    }
//    
    // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
//    indeterminateDialog = new ProgressDialog(this);
//    indeterminateDialog.setTitle("Please wait");
//    String ocrEngineModeName = getOcrEngineModeName();
//    if (ocrEngineModeName.equals("Both")) {
//      indeterminateDialog.setMessage("Initializing Cube and Tesseract OCR engines for " + languageName + "...");
//    } else {
//      indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
//    }
//    indeterminateDialog.setCancelable(false);
//    indeterminateDialog.show();
//    
//    if (handler != null) {
//      handler.quitSynchronously();     
//    }

    // Disable continuous mode if we're using Cube. This will prevent bad states for devices 
    // with low memory that crash when running OCR with Cube, and prevent unwanted delays.
//    if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY || ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
//      Log.d(TAG, "Disabling continuous preview");
//      isContinuousModeActive = false;
//      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//      prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, false);
//    }
//    
//    // Start AsyncTask to install language data and init OCR
//    baseApi = new TessBaseAPI();
//    new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
//      .execute(storageRoot.toString());
//  }
  
  /**
   * Displays information relating to the result of OCR, and requests a translation if necessary.
   * 
   * @param ocrResult Object representing successful OCR results
   * @return True if a non-null result was received for OCR
   */
  boolean handleOcrDecode(OcrResult ocrResult) {
    lastResult = ocrResult;

    String recognizedText = ocrResult.getText();

    // Test whether the result is null
    if (recognizedText == null || recognizedText.equals("")) {
      Toast toast = Toast.makeText(this, "OCR failed due to unrecognized text. Please try again.", Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.TOP, 0, 0);
      toast.show();
      return false;
    } else {
        int index = 0;
        while(recognizedText.charAt(index) != ']' && index < recognizedText.length()) index++;
        index += 2;
        //if the bounding box was found, do not include it in the output
        if(index != recognizedText.length())
          recognizedText = recognizedText.substring(index); // to skip the white space
    }
    
    // Turn off capture-related UI elements
    shutterButton.setVisibility(View.GONE);
    statusViewBottom.setVisibility(View.GONE);
    statusViewTop.setVisibility(View.GONE);
    cameraButtonView.setVisibility(View.GONE);
    viewfinderView.setVisibility(View.GONE);
    resultView.setVisibility(View.VISIBLE);


    buttonDone.setVisibility(View.VISIBLE);
    buttonAgain.setVisibility(View.VISIBLE);

    ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
    lastBitmap = ocrResult.getBitmap();
    if (lastBitmap == null) {
      bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
          R.drawable.ic_launcher));
    } else {
      bitmapImageView.setImageBitmap(lastBitmap);
    }

    // Display the recognized text
//    TextView sourceLanguageTextView = (TextView) findViewById(R.id.source_language_text_view);
//    sourceLanguageTextView.setText(sourceLanguageReadable);
    TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);
    ocrResultTextView.setText(recognizedText);
    totalDataSent += ocrResult.getConsumedData() / 1000f;
    sentDataView.setText(String.format(Locale.US, "%.2f", (ocrResult.getConsumedData() / 1000f))
                          + "/" + String.format(Locale.US, "%.2f", totalDataSent) + "KB");
    // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
    int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
    ocrResultTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

////    TextView translationLanguageLabelTextView = (TextView) findViewById(R.id.translation_language_label_text_view);
////    TextView translationLanguageTextView = (TextView) findViewById(R.id.translation_language_text_view);
////    TextView translationTextView = (TextView) findViewById(R.id.translation_text_view);
////    if (isTranslationActive) {
////      // Handle translation text fields
////      translationLanguageLabelTextView.setVisibility(View.VISIBLE);
////      translationLanguageTextView.setText(targetLanguageReadable);
////      translationLanguageTextView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL), Typeface.NORMAL);
////      translationLanguageTextView.setVisibility(View.VISIBLE);
////
////      // Activate/re-activate the indeterminate progress indicator
////      translationTextView.setVisibility(View.GONE);
////      progressView.setVisibility(View.VISIBLE);
////      setProgressBarVisibility(true);
////      
////      // Get the translation asynchronously
//////      new TranslateAsyncTask(this, sourceLanguageCodeTranslation, targetLanguageCodeTranslation, 
//////          ocrResult.getText()).execute();
////    } else {
//      translationLanguageLabelTextView.setVisibility(View.GONE);
//      translationLanguageTextView.setVisibility(View.GONE);
//      translationTextView.setVisibility(View.GONE);
      progressView.setVisibility(View.GONE);
      setProgressBarVisibility(false);
//    }
    
    
    return true;
  }
  
  /**
   * Displays information relating to the results of a successful real-time OCR request.
   * 
   * @param ocrResult Object representing successful OCR results
   */
//  void handleOcrContinuousDecode(OcrResult ocrResult) {
//   
//    lastResult = ocrResult;
//    
//    // Send an OcrResultText object to the ViewfinderView for text rendering
//    viewfinderView.addResultText(new OcrResultText(ocrResult.getText(), 
//                                                   ocrResult.getWordConfidences(),
//                                                   ocrResult.getMeanConfidence(),
//                                                   ocrResult.getBitmapDimensions(),
//                                                   ocrResult.getRegionBoundingBoxes(),
//                                                   ocrResult.getTextlineBoundingBoxes(),
//                                                   ocrResult.getStripBoundingBoxes(),
//                                                   ocrResult.getWordBoundingBoxes(),
//                                                   ocrResult.getCharacterBoundingBoxes()));
//
//    Integer meanConfidence = ocrResult.getMeanConfidence();
//    
//    if (CONTINUOUS_DISPLAY_RECOGNIZED_TEXT) {
//      // Display the recognized text on the screen
//      statusViewTop.setText(ocrResult.getText());
//      int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
//      statusViewTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//      statusViewTop.setTextColor(Color.BLACK);
//      statusViewTop.setBackgroundResource(R.color.status_top_text_background);
//
//      statusViewTop.getBackground().setAlpha(meanConfidence * (255 / 100));
//    }
//
//    if (CONTINUOUS_DISPLAY_METADATA) {
//      // Display recognition-related metadata at the bottom of the screen
//      long recognitionTimeRequired = ocrResult.getRecognitionTimeRequired();
//      statusViewBottom.setTextSize(14);
//      statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - Mean confidence: " + 
//          meanConfidence.toString() + " - Time required: " + recognitionTimeRequired + " ms");
//    }
//    
//    if (CONTINUOUS_DISPLAY_IMAGE_TO_RECOGNIZE) {
//    	// Display the bitmap sent to recognition
//    	statusViewImage.setImageBitmap(ocrResult.getBitmapForRecognition()); //new, JK
//    }
//  }
//  
  /**
   * Version of handleOcrContinuousDecode for failed OCR requests. Displays a failure message.
   * 
   * @param obj Metadata for the failed OCR request.
   */
//  void handleOcrContinuousDecode(OcrResultFailure obj) {
//    lastResult = null;
//    viewfinderView.removeResultText();
//    
//    // Reset the text in the recognized text box.
//    statusViewTop.setText("");
//
//    if (CONTINUOUS_DISPLAY_METADATA) {
//      // Color text delimited by '-' as red.
//      statusViewBottom.setTextSize(14);
//      CharSequence cs = setSpanBetweenTokens("OCR: " + sourceLanguageReadable + " - OCR failed - Time required: " 
//          + obj.getTimeRequired() + " ms", "-", new ForegroundColorSpan(0xFFFF0000));
//      statusViewBottom.setText(cs);
//    }
//  }
  
  /**
   * Given either a Spannable String or a regular String and a token, apply
   * the given CharacterStyle to the span between the tokens.
   * 
   * NOTE: This method was adapted from:
   *  http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
   * 
   * <p>
   * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##", new
   * ForegroundColorSpan(0xFFFF0000));} will return a CharSequence {@code
   * "Hello world!"} with {@code world} in red.
   * 
   */
  private CharSequence setSpanBetweenTokens(CharSequence text, String token,
      CharacterStyle... cs) {
    // Start and end refer to the points where the span will apply
    int tokenLen = token.length();
    int start = text.toString().indexOf(token) + tokenLen;
    int end = text.toString().indexOf(token, start);

    if (start > -1 && end > -1) {
      // Copy the spannable string to a mutable spannable string
      SpannableStringBuilder ssb = new SpannableStringBuilder(text);
      for (CharacterStyle c : cs)
        ssb.setSpan(c, start, end, 0);
      text = ssb;
    }
    return text;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    if (v.equals(ocrResultView)) {
      menu.add(Menu.NONE, OPTIONS_COPY_RECOGNIZED_TEXT_ID, Menu.NONE, "Copy recognized text");
      menu.add(Menu.NONE, OPTIONS_SHARE_RECOGNIZED_TEXT_ID, Menu.NONE, "Share recognized text");
    } 
//    else if (v.equals(translationView)){
//      menu.add(Menu.NONE, OPTIONS_COPY_TRANSLATED_TEXT_ID, Menu.NONE, "Copy translated text");
//      menu.add(Menu.NONE, OPTIONS_SHARE_TRANSLATED_TEXT_ID, Menu.NONE, "Share translated text");
//    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    switch (item.getItemId()) {

    case OPTIONS_COPY_RECOGNIZED_TEXT_ID:
        clipboardManager.setText(ocrResultView.getText());
      if (clipboardManager.hasText()) {
        Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
      }
      return true;
    case OPTIONS_SHARE_RECOGNIZED_TEXT_ID:
    	Intent shareRecognizedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
    	shareRecognizedTextIntent.setType("text/plain");
    	shareRecognizedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, ocrResultView.getText());
    	startActivity(Intent.createChooser(shareRecognizedTextIntent, "Share via"));
    	return true;
//    case OPTIONS_COPY_TRANSLATED_TEXT_ID:
//        clipboardManager.setText(translationView.getText());
//      if (clipboardManager.hasText()) {
//        Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
//        toast.setGravity(Gravity.BOTTOM, 0, 0);
//        toast.show();
//      }
//      return true;
//    case OPTIONS_SHARE_TRANSLATED_TEXT_ID:
//    	Intent shareTranslatedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
//    	shareTranslatedTextIntent.setType("text/plain");
//    	shareTranslatedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, translationView.getText());
//    	startActivity(Intent.createChooser(shareTranslatedTextIntent, "Share via"));
//    	return true;
    default:
      return super.onContextItemSelected(item);
    }
  }

  /**
   * Resets view elements.
   */
  public void resetStatusView() {
    resultView.setVisibility(View.GONE);
//    if (CONTINUOUS_DISPLAY_METADATA) {
//      statusViewBottom.setText("");
//      statusViewBottom.setTextSize(14);
//      statusViewBottom.setTextColor(getResources().getColor(R.color.status_text));
//      statusViewBottom.setVisibility(View.VISIBLE);
//    }
//    if (CONTINUOUS_DISPLAY_RECOGNIZED_TEXT) {
//      statusViewTop.setText("");
//      statusViewTop.setTextSize(14);
//      statusViewTop.setVisibility(View.VISIBLE);
//    }
    viewfinderView.setVisibility(View.VISIBLE);
    cameraButtonView.setVisibility(View.VISIBLE);
    if (DISPLAY_SHUTTER_BUTTON) {
      shutterButton.setVisibility(View.VISIBLE);

      buttonDone.setVisibility(View.GONE);
      buttonAgain.setVisibility(View.GONE);
    }
    lastResult = null;
    //viewfinderView.removeResultText();
  }
  
  /** Displays a pop-up message showing the name of the current OCR source language. */
//  void showLanguageName() {   
//    Toast toast = Toast.makeText(this, "OCR: " + sourceLanguageReadable, Toast.LENGTH_LONG);
//    toast.setGravity(Gravity.TOP, 0, 0);
//    toast.show();
//  }
  
  /**
   * Displays an initial message to the user while waiting for the first OCR request to be
   * completed after starting realtime OCR.
   */
//  void setStatusViewForContinuous() {
//    viewfinderView.removeResultText();
//    if (CONTINUOUS_DISPLAY_METADATA) {
//      statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - waiting for OCR...");
//    }
//  }


  @SuppressWarnings("unused")
  void setButtonVisibility(boolean visible) {
    if (shutterButton != null && visible == true && DISPLAY_SHUTTER_BUTTON) {
      shutterButton.setVisibility(View.VISIBLE);

        buttonDone.setVisibility(View.GONE);
        buttonAgain.setVisibility(View.GONE);
    } else if (shutterButton != null) {
      shutterButton.setVisibility(View.GONE);

        buttonDone.setVisibility(View.VISIBLE);
        buttonAgain.setVisibility(View.VISIBLE);
    }
  }

  
  /**
   * Enables/disables the shutter button to prevent double-clicks on the button.
   * 
   * @param clickable True if the button should accept a click
   */
  void setShutterButtonClickable(boolean clickable) {
    shutterButton.setClickable(clickable);
  }

  /** Request the viewfinder to be invalidated. */
  void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }
  
  @Override
  public void onShutterButtonClick(ShutterButton b) {
//    if (isContinuousModeActive) {
//      onShutterButtonPressContinuous();
//    } else {
//      if(!isNetworkAvailable(this)) {
//          Toast toast = Toast.makeText(this,"No Internet connection available.",Toast.LENGTH_LONG);
//          toast.setGravity(Gravity.CENTER, 0, 0);
//          toast.show();
//          finish(); //Calling this method to close this activity when internet is not available.
//          return;
//      }

      if (handler != null) {
         handler.shutterButtonClick();
      }
//    }
  }

  @Override
  public void onShutterButtonFocus(ShutterButton b, boolean pressed) {
    if(!isNetworkAvailable(this)) {
          Toast toast = Toast.makeText(this,"No Internet connection available.",Toast.LENGTH_LONG);
          toast.setGravity(Gravity.CENTER, 0, 0);
          toast.show();
          finish(); //Calling this method to close this activity when internet is not available.
          return;
    }
    requestDelayedAutoFocus();
  }
  
  /**
   * Requests autofocus after a 350 ms delay. This delay prevents requesting focus when the user 
   * just wants to click the shutter button without focusing. Quick button press/release will 
   * trigger onShutterButtonClick() before the focus kicks in.
   */
  private void requestDelayedAutoFocus() {
    // Wait 350 ms before focusing to avoid interfering with quick button presses when
    // the user just wants to take a picture without focusing.
    cameraManager.requestAutoFocus(350L);
  }
  
  static boolean getFirstLaunch() {
    return isFirstLaunch;
  }
  
  /**
   * We want the help screen to be shown automatically the first time a new version of the app is
   * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
   * it to a value stored as a preference.
   */
  private boolean checkFirstLaunch() {
    try {
      PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
      int currentVersion = info.versionCode;
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//      int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
      int lastVersion = 1;
      if (lastVersion == 0) {
        isFirstLaunch = true;
      } else {
        isFirstLaunch = false;
      }
      if (currentVersion > lastVersion) {
        
        // Record the last version for which we last displayed the What's New (Help) page
//        prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
        Intent intent = new Intent(this, HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        
        // Show the default page on a clean install, and the what's new page on an upgrade.
        String page = lastVersion == 0 ? HelpActivity.DEFAULT_PAGE : HelpActivity.WHATS_NEW_PAGE;
        intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
        startActivity(intent);
        return true;
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
    }
    return false;
  }
  
  /**
   * Returns a string that represents which OCR engine(s) are currently set to be run.
   * 
   * @return OCR engine mode
   */
//  String getOcrEngineModeName() {
//    String ocrEngineModeName = "";
//    String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
//    if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_ONLY) {
//      ocrEngineModeName = ocrEngineModes[0];
//    } else if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY) {
//      ocrEngineModeName = ocrEngineModes[1];
//    } else if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
//      ocrEngineModeName = ocrEngineModes[2];
//    }
//    return ocrEngineModeName;
//  }
  
  /**
   * Gets values from shared preferences and sets the corresponding data members in this activity.
   */
  private void retrievePreferences() {
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
      // Retrieve from preferences, and set in this Activity, the language preferences
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//      setSourceLanguage(prefs.getString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE));
//      setTargetLanguage(prefs.getString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_TARGET_LANGUAGE_CODE));
//      isTranslationActive = prefs.getBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, false);
      
      // Retrieve from preferences, and set in this Activity, the capture mode preference
//      if (prefs.getBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, CaptureActivity.DEFAULT_TOGGLE_CONTINUOUS)) {
//        isContinuousModeActive = true;
//      } else {
//        isContinuousModeActive = false;
//      }

      // Retrieve from preferences, and set in this Activity, the page segmentation mode preference
//      String[] pageSegmentationModes = getResources().getStringArray(R.array.pagesegmentationmodes);
//      String pageSegmentationModeName = prefs.getString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, pageSegmentationModes[0]);
//      if (pageSegmentationModeName.equals(pageSegmentationModes[0])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[1])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[2])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[3])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[4])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[5])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[6])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_WORD;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[7])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT;
//      } else if (pageSegmentationModeName.equals(pageSegmentationModes[8])) {
//        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT;
//      }
      
      // Retrieve from preferences, and set in this Activity, the OCR engine mode
//      String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
//      String ocrEngineModeName = prefs.getString(PreferencesActivity.KEY_OCR_ENGINE_MODE, ocrEngineModes[0]);
//      if (ocrEngineModeName.equals(ocrEngineModes[0])) {
//        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
//      } else if (ocrEngineModeName.equals(ocrEngineModes[1])) {
//        ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
//      } else if (ocrEngineModeName.equals(ocrEngineModes[2])) {
//        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED;
//      }
      
      // Retrieve from preferences, and set in this Activity, the character blacklist and whitelist
//      characterBlacklist = OcrCharacterHelper.getBlacklist(prefs, sourceLanguageCodeOcr);
//      characterWhitelist = OcrCharacterHelper.getWhitelist(prefs, sourceLanguageCodeOcr);
//      
     prefs.registerOnSharedPreferenceChangeListener(listener);
//      
  }
  
  /**
   * Sets default values for preferences. To be called the first time this app is run.
   */
  private void setDefaultPreferences() {
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//    // Continuous preview
//    prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, CaptureActivity.DEFAULT_TOGGLE_CONTINUOUS).commit();
//
//    // Recognition language
//    prefs.edit().putString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE).commit();
//
//    // Translation
//    prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, CaptureActivity.DEFAULT_TOGGLE_TRANSLATION).commit();
//
//    // Translation target language
//    prefs.edit().putString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, CaptureActivity.DEFAULT_TARGET_LANGUAGE_CODE).commit();
//
//    // Translator
//    prefs.edit().putString(PreferencesActivity.KEY_TRANSLATOR, CaptureActivity.DEFAULT_TRANSLATOR).commit();
//
//    // OCR Engine
//    prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, CaptureActivity.DEFAULT_OCR_ENGINE_MODE).commit();
//
//    // Autofocus
//    prefs.edit().putBoolean(PreferencesActivity.KEY_AUTO_FOCUS, CaptureActivity.DEFAULT_TOGGLE_AUTO_FOCUS).commit();
//    
//    // Disable problematic focus modes
//    prefs.edit().putBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, CaptureActivity.DEFAULT_DISABLE_CONTINUOUS_FOCUS).commit();
//    
//    // Beep
//    prefs.edit().putBoolean(PreferencesActivity.KEY_PLAY_BEEP, CaptureActivity.DEFAULT_TOGGLE_BEEP).commit();
//    
//    // Character blacklist
//    prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_BLACKLIST, 
//        OcrCharacterHelper.getDefaultBlacklist(CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();
//
//    // Character whitelist
//    prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_WHITELIST, 
//        OcrCharacterHelper.getDefaultWhitelist(CaptureActivity.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();
//
//    // Page segmentation mode
//    prefs.edit().putString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, CaptureActivity.DEFAULT_PAGE_SEGMENTATION_MODE).commit();
//
//    // Reversed camera image
//    prefs.edit().putBoolean(PreferencesActivity.KEY_REVERSE_IMAGE, CaptureActivity.DEFAULT_TOGGLE_REVERSED_IMAGE).commit();
//
//    // Light
    prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_LIGHT, CaptureActivity.DEFAULT_TOGGLE_LIGHT).commit();


    prefs.edit().putString(PreferencesActivity.KEY_SCRIPT_PATH, CaptureActivity.DEFAULT_SCRIPT_PATH).commit();


  }
//  
  void displayProgressDialog() {
    // Set up the indeterminate progress dialog box
    indeterminateDialog = new ProgressDialog(this);
    indeterminateDialog.setTitle("Please wait");        
//    String ocrEngineModeName = getOcrEngineModeName();
//    if (ocrEngineModeName.equals("Both")) {
//      indeterminateDialog.setMessage("Performing OCR using Cube and Tesseract...");
//    } else {
//      indeterminateDialog.setMessage("Performing OCR using " + ocrEngineModeName + "...");
//    }
    indeterminateDialog.setMessage("Performing OCR using Neumann-Matas algorithm ...");
    indeterminateDialog.setCancelable(false);
    indeterminateDialog.show();
  }
  
  ProgressDialog getProgressDialog() {
    return indeterminateDialog;
  }
  
  /**
   * Displays an error message dialog box to the user on the UI thread.
   * 
   * @param title The title for the dialog box
   * @param message The error message to be displayed
   */
  void showErrorMessage(String title, String message) {
	  new AlertDialog.Builder(this)
	    .setTitle(title)
	    .setMessage(message)
	    .setOnCancelListener(new FinishListener(this))
	    .setPositiveButton( "Done", new FinishListener(this))
	    .show();
  }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(conMan.getActiveNetworkInfo() != null && conMan.getActiveNetworkInfo().isConnected())
            return true;
        else
            return false;
    }


}
