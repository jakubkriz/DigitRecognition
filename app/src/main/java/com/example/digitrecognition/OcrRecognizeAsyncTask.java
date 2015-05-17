/* 
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

//import com.googlecode.leptonica.android.ReadFile;
//import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class to send OCR requests to the OCR engine in a separate thread, send a success/failure message,
 * and dismiss the indeterminate progress dialog box. Used for non-continuous mode OCR only.
 */
final class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {

  //  private static final boolean PERFORM_FISHER_THRESHOLDING = false; 
  //  private static final boolean PERFORM_OTSU_THRESHOLDING = false; 
  //  private static final boolean PERFORM_SOBEL_THRESHOLDING = false; 

  private CaptureActivity activity;
//  private TessBaseAPI baseApi;
  private byte[] data;
  private int width;
  private int height;
//  private Bitmap bitmap;
  private OcrResult ocrResult;
  private long timeRequired;
  private static float consumedData;

  private static String scriptPath;
  

//  OcrRecognizeAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, byte[] data, int width, int height) {
  OcrRecognizeAsyncTask(CaptureActivity activity, byte[] data, int width, int height) {
    this.activity = activity;
//    this.baseApi = baseApi;
    this.data = data;
    this.width = width;
    this.height = height;
    consumedData = 0f;

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    scriptPath = prefs.getString(PreferencesActivity.KEY_SCRIPT_PATH, "http://www.fi.muni.cz/~xkriz9/recognition.cgi");

  }
//  OcrRecognizeAsyncTask(CaptureActivity activity, Bitmap bitmap) {
//	  this.activity = activity;
//	  this.bitmap = bitmap;
//  }
	
  @Override
  protected Boolean doInBackground(Void... arg0) {
    long start = System.currentTimeMillis();
    Bitmap bitmap = activity.getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();
    //BitmapFactory.Options options = new BitmapFactory.Options();
    //options.inMutable = true;
    //Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
    String textResult = null;

    //      if (PERFORM_FISHER_THRESHOLDING) {
    //        Pix thresholdedImage = Thresholder.fisherAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 0.1F, 2.5F);
    //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    //        bitmap = WriteFile.writeBitmap(thresholdedImage);
    //      }
    //      if (PERFORM_OTSU_THRESHOLDING) {
    //        Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 9, 9, 0.1F);
    //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    //        bitmap = WriteFile.writeBitmap(thresholdedImage);
    //      }
    //      if (PERFORM_SOBEL_THRESHOLDING) {
    //        Pix thresholdedImage = Thresholder.sobelEdgeThreshold(ReadFile.readBitmap(bitmap), 64);
    //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    //        bitmap = WriteFile.writeBitmap(thresholdedImage);
    //      }

    try {     
        //Log.e("OCRHMM", "Dimensions:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    	
    	//resize the bitmap to sent the smaller amount of data
    	Bitmap tmp = Bitmap.createScaledBitmap(bitmap, 640, bitmap.getHeight(), false);
    	//convert bitmap to byte array
    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
    	tmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
    	bitmap = tmp;
    	
    	byte[] rslt = stream.toByteArray();

        Log.e("OcrTimeRequired", "time after bitmaps=" + (System.currentTimeMillis() - start));
    	textResult = postFile(rslt);
        Log.e("OCRHMM", "Recognized text:" + textResult);
        //Log.e("OCRHMM", "Dimensions:" + tmp.getWidth() + "x" + tmp.getHeight());
    	// remove the line below///
    	//if(textResult == "") textResult = "[10,10,620,110] 123456789";
    	
    	
    	timeRequired = System.currentTimeMillis() - start;
        Log.e("OcrTimeRequired", "total time=" + timeRequired);

      // Check for failure to recognize text
	  if (textResult == null || textResult.equals("")) {
	    return false;
	  }
	  ocrResult = new OcrResult();
	//      ocrResult.setWordConfidences(baseApi.wordConfidences());
	//      ocrResult.setMeanConfidence( baseApi.meanConfidence());
	//      ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
	//      ocrResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
	//      ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
	//      ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
	  //ocrResult.setCharacterBoundingBoxes(baseApi.getCharacters().getBoxRects());
	} catch (RuntimeException e) {
	  Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to OCRHMM. Setting state to CONTINUOUS_STOPPED.");
	  e.printStackTrace();
	  try {
	//        baseApi.clear();
	    activity.stopHandler();
	  } catch (NullPointerException e1) {
	    // Continue
	  }
	  return false;
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	//timeRequired = System.currentTimeMillis() - start;
	ocrResult.setBitmap(bitmap);
	ocrResult.setText(textResult);
	ocrResult.setRecognitionTimeRequired(timeRequired);
    ocrResult.setConsumedData(consumedData);
	return true;
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);

    Handler handler = activity.getHandler();
    if (handler != null) {
      // Send results for single-shot mode recognition.
      if (result) {
        Message message = Message.obtain(handler, R.id.ocr_decode_succeeded, ocrResult);
        message.sendToTarget();
      } else {
        Message message = Message.obtain(handler, R.id.ocr_decode_failed, ocrResult);
        message.sendToTarget();
      }
      activity.getProgressDialog().dismiss();
    }
//    if (baseApi != null) {
//      baseApi.clear();
//    }
  }
  
//  public static String postFile(String fileName, String userName, String password, String macAddress) throws Exception {
  public static String postFile(byte[] data) throws Exception {
        HttpClient client = new DefaultHttpClient();
	    String url = scriptPath;
        try {
          HttpPost post = new HttpPost(url);

          MultipartEntityBuilder builder = MultipartEntityBuilder.create();
          builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

          builder.addBinaryBody("image", data);

          HttpEntity entity = builder.build();
          post.setEntity(entity);
          consumedData += post.getEntity().getContentLength();
          HttpResponse response = client.execute(post);
          consumedData += response.getEntity().getContentLength();

//          if(getContent(response).length() < 50){
              return getContent(response);
//          }
        } catch (ClientProtocolException e) {
            Log.e("ClientProtocolException : "+e, e.getMessage());
        } catch (IOException e) {
            Log.e("IOException : "+e, e.getMessage());
        } catch(IllegalArgumentException e) {
            Log.e("OcrRecognizeAsyncTask", "Wrong path to script." + e.getMessage());
        }

        return "";
	} 

	public static String getContent(HttpResponse response) throws IOException {
	    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	    String body = "";
	    String content = "";

	    while ((body = rd.readLine()) != null)  {
	        content += body + "\n";
	    }
	    return content.trim();
	}
  
}
