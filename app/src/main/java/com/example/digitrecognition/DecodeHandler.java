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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

//import org.opencv.android.Utils;
//import org.opencv.core.Core;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfInt;
//import org.opencv.core.MatOfInt4;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
//import org.opencv.core.Rect;
//import org.opencv.core.RotatedRect;
//import org.opencv.core.Scalar;
//import org.opencv.core.Size;
//import org.opencv.highgui.Highgui;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.objdetect.CascadeClassifier;
//import org.opencv.utils.Converters;


import com.example.digitrecognition.CaptureActivity;
import com.example.digitrecognition.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

/**
 * Class to send bitmap data for OCR.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
final class DecodeHandler extends Handler {

  private final CaptureActivity captureActivity;
  private boolean running = true;
//  private final TessBaseAPI baseApi;
  private Bitmap bitmap;
//  private Bitmap bitmapForRecognition;
  private static boolean isDecodePending;
  private long timeRequired;
  
//  private ImageButton buttonAgain;
//  private ImageButton buttonDone;

  DecodeHandler(CaptureActivity activity) {
	  captureActivity = activity;
//    baseApi = activity.getBaseApi();
//    beepManager = new BeepManager(activity);
//    beepManager.updatePrefs();
    
//    buttonDone = (ImageButton) activity.findViewById(R.id.button_done);
//    buttonAgain = (ImageButton) activity.findViewById(R.id.button_again);
//
//
//    buttonDone.setVisibility(View.VISIBLE);
//    buttonAgain.setVisibility(View.VISIBLE);
//
//    buttonDone.setOnClickListener(new View.OnClickListener() {
//		@Override
//		public void onClick(View v) {
//			captureActivity.finish();
//		}
//	});
//    buttonAgain.setOnClickListener(new View.OnClickListener() {
//		@Override
//		public void onClick(View v) {
//			captureActivity.resetStatusView();
//		}
//	});
  }

  @Override
  public void handleMessage(Message message) {
    if (!running) {
      return;
    }
    switch (message.what) {        
//    case R.id.ocr_continuous_decode:
//      // Only request a decode if a request is not already pending.
//      if (!isDecodePending) {
//        isDecodePending = true;
//        ocrContinuousDecode((byte[]) message.obj, message.arg1, message.arg2);
//      }
//      break;
    case R.id.ocr_decode:
      ocrDecode((byte[]) message.obj, message.arg1, message.arg2);
      break;
    case R.id.quit:
      running = false;
      Looper.myLooper().quit();
      break;
    }
  }

//  static void resetDecodeState() {
//    isDecodePending = false;
//  }

  /**
   *  Launch an AsyncTask to perform an OCR decode for single-shot mode.
   *  
   * @param data Image data
   * @param width Image width
   * @param height Image height
   */
//  private void ocrDecode(byte[] data, int width, int height) {
  private void ocrDecode(byte[] data, int width, int height) {
    //beepManager.playBeepSoundAndVibrate();
	  captureActivity.displayProgressDialog();

    // Launch OCR asynchronously, so we get the dialog box displayed immediately
    new OcrRecognizeAsyncTask(captureActivity, data, width, height).execute();
//	  new OcrRecognizeAsyncTask(captureActivity, bitmap).execute();
  }

  /**
   *  Perform an OCR decode for realtime recognition mode.
   *  
   * @param data Image data
   * @param width Image width
   * @param height Image height
   */
//  private void ocrContinuousDecode(byte[] data, int width, int height) {   
//    PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
//    if (source == null) {
//      sendContinuousOcrFailMessage();
//      return;
//    }
//    bitmap = source.renderCroppedGreyscaleBitmap();
//    
////    preprocessBitmap();
////      sumImages();
//// OR    - for loading the manually thresholded image - the best results - 100%
//    //File imgFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/plynomer_uprava_threshold.jpg");
//    //Bitmap img = BitmapFactory.decodeFile(imgFile.getAbsolutePath());	
//    //bitmapForRecognition = Bitmap.createBitmap(img);
//
//    OcrResult ocrResult = getOcrResult();
//    Handler handler = activity.getHandler();
//    if (handler == null) {
//      return;
//    }
//
//    if (ocrResult == null) {
//      try {
//        sendContinuousOcrFailMessage();
//      } catch (NullPointerException e) {
//        activity.stopHandler();
//      } finally {
//        bitmap.recycle();
////        bitmapForRecognition.recycle();
////        baseApi.clear();
//      }
//      return;
//    }
//
//    try {
//      Message message = Message.obtain(handler, R.id.ocr_continuous_decode_succeeded, ocrResult);
//      message.sendToTarget();
//    } catch (NullPointerException e) {
//      activity.stopHandler();
//    } finally {
////      baseApi.clear();
//    }
//  }

//  private void sumImages() {
//	  bitmapForRecognition = Bitmap.createBitmap(bitmap);
//	  
//	  String text = "";
//	  String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/adaptive_thres_mean_bin_31.png";
//	  Mat img = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);  //src -> 2502x351, 1 channel, depth 0 (8UC1)
//	  Mat src1 = img.clone();
//	  Mat dst = src1.clone(); //new Mat(src.size(), CvType.CV_8UC1);
//	  Mat rslt = dst.clone();
//	  
//	  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/vzorek.jpg";
////	  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/adaptive_thres_mean_bin_2051.png";
//	  img = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);  
//	  
//	  int i = 31;
//	  while(i <= 251) {
//		  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/adaptive_thres_mean_bin_"+ i +".png";
//		  Mat src2 = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
//		  
//// cislo u obrazku znaci maximalni hodnotu i pro while cyklus (pro obrazek result_351_multiplied.png vypadala podminka takto while(i <= 351))
//
////		  Core.add(src1, src2, dst); // prefix obrazku result (napr. result_351_multiplied.png)
////		  Core.add(src1, src2, dst, img, CvType.CV_8UC1); // prefix obrazku options (napr. options_result_2051.png)		  
//		  Core.multiply(src1, src2, dst, 1, CvType.CV_8UC1); // prefix obrazku test + multiply_all (napr. test_2051_multiply_all.png)
//		  
//		  // prefix obrazku test + addweighted + hodnoty parametru (napr. test_251_addweighted_0.5_0.5_0.5_multiply.png)
////		  Core.addWeighted(src1, 0.1, src2, 0.8, 0.1, dst);
//		  
//		  //update
////		  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/adaptive_thres_mean_bin_"+ i +".png";
////		  src1 = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
//		  
//		  //all multiplied
////		  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/adaptive_thres_mean_bin_"+ i +".png";
////		  src1 = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
//		  
//		  if(i <= 41) 
//			  i += 10;
//		  else if(i < 651)
//			  i += 100;
//		  else 
//		  {
//			  if(i == 651) i = 1051;
//			  else i += 500;
//		  }
//			 
//		  src1 = dst;
//		  Log.e("SUM", "cisloI:" + i);
//	  }
//	  
//	  Core.multiply(dst, img, rslt); // multiply/multiplied na konci nazvu obrazku (napr. result_351_multiplied.png)
//
//	  dst = rslt;
//	  
//	  Mat empty = Mat.zeros(img.size(),  CvType.CV_8UC1);
//	  Core.add(img, empty, rslt, dst, CvType.CV_8UC1);
//	  
////	  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/adaptive_thres_mean_bin_"+ 451 +".png";
////	  src1 = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
////	  Core.multiply(dst, src1, dst, 21d, CvType.CV_8UC1);
//	  
//	  
//	  text = "add_masked";
//	  
//	  //final stage - create result 
//	  Bitmap result = Bitmap.createBitmap(rslt.width(), rslt.height(), Bitmap.Config.ARGB_8888);
//	  Utils.matToBitmap(rslt, result);
//	  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/sum/" + text + ".png";
//	  Highgui.imwrite(path, rslt);
//	  // set bitmap for recognition
//	  bitmapForRecognition = result;
//  }
  
  /** Apply some image transformations before running recognition */
//  private void preprocessBitmap() {
//	  bitmapForRecognition = Bitmap.createBitmap(bitmap);
//	  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////     
////Leptonica pro Android
/////////////////////////	  
////	  Pix pixs = ReadFile.readBitmap(bitmap);
//////	  Pix pixs2 = AdaptiveMap.backgroundNormMorph(pixs);
////	  Pix pixs2 = Convert.convertTo8(pixs);
//////	  Pix pixs2 = Binarize.otsuAdaptiveThreshold(pixs);
//////	  Pix pixs2 = Enhance.unsharpMasking(pixs, 2, (float) 0.5);
////	  
//////	  Pix pixs2 = AdaptiveMap.backgroundNormMorph(pixs);
//////	  Pix pixs2 = Enhance.unsharpMasking(pixs, 2, (float) 0.5);
////	  
//////	  Pix pixs2 = Convert.convertTo8(pixs);
//////	  Pix pixs3 = Binarize.otsuAdaptiveThreshold(pixs2);
////	  
//////	  Pix pixs2 = Convert.convertTo8(pixs);
//////	  Pix pixs3 = Binarize.otsuAdaptiveThreshold(pixs2, sizeX, sizeY, smoothX, smoothY, scoreFraction);
////	  Pix pixs3 = Binarize.otsuAdaptiveThreshold(pixs2, 20, 20, 4, 4, 0.0f);
//////	  Pix pixs3 = Binarize.otsuAdaptiveThreshold(pixs2);
//////	  Pix pixs3 = Binarize.sauvolaBinarizeTiled(pixs2, whsize, factor, nx, ny);	
//////	  Pix pixs3 = Binarize.sauvolaBinarizeTiled(pixs2, 20, 0.3f, 1, 1);
////	  bitmapForRecognition = WriteFile.writeBitmap(pixs3);
//
//	  
//	  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////   
////OpenCV for Android
//////////////////////	  
//	  // load a file from the sd card
////	  File imgFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/plynomer_uprava.jpg");
///*	  File imgFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/vzorek.jpg");
//	  Bitmap img = BitmapFactory.decodeFile(imgFile.getAbsolutePath());		  
//	  
//	  // Mat structures
//	  Mat src = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC4);	
//	  Mat dst = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC4);
//	  
//	  Utils.bitmapToMat(img, src);	
//	  // grayscale
//	  Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2GRAY);
//	  Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGB2GRAY); 	
//*/ 
//	  String text = "";
//	  
//	  String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/vzorek.jpg"; 
//	  //String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/test_251_multiply_add_end.png";
//	  //String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/plynomer_uprava_threshold.jpg"; 
//	  
//	  //!!!!!!!!!!!!!
//	  //String path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/sum/test_251_multiply_all.png";
//	  
//	  Mat orig = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_UNCHANGED);
//	  Mat img = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE);  //src -> 2502x351, 1 channel, depth 0 (8UC1)
//	  Mat src = img.clone();
//	  Mat dst = src.clone(); //new Mat(src.size(), CvType.CV_8UC1);
//	  
//	  Mat blur = dst.clone();
//	  Imgproc.medianBlur(src,blur,5);  // or gaussianblur
////////////////////////////////////////////////////////////////////// 
//	  	  
//	  //adaptive threshold only, as used in http://www.bogotobogo.com/python/OpenCV_Python/python_opencv3_Image_Global_Thresholding_Adaptive_Thresholding_Otsus_Binarization_Segmentations.php
//	  Imgproc.adaptiveThreshold(dst, src, 255f, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 2);
////	  dst = src.clone();
////	  text = "adaptive_thres_gauss_bin_31";
//// OR
//	  // classical threshold using otsu algorithm to count threshold's parameters
//	  Mat threshold = blur.clone();
////	  Imgproc.threshold(blur, threshold, 100d, 255d, Imgproc.THRESH_BINARY); //THRESH_BINARY, ...
////	  dst = threshold.clone();
////	  text = "thres_100_255_trunc";
////      
//	  // negative
//	  Mat negative = threshold.clone();
//	  Core.bitwise_not(threshold, negative);
//	  
//	  // detect lines 	  
//	  Mat edges = negative.clone(); 
//	  Imgproc.Canny(negative, edges, 40d, 80d);
//	  
//	  // count the angle and draw lines
////	  MatOfInt4 lines = new MatOfInt4();
////	  Imgproc.HoughLinesP(threshold, lines, 1d, Math.PI / 180d, 100, dst.size().width / 2d, 5d);
////	  double angle = 0f;
////	  int nb_lines = lines.cols();
////	  for (int i = 0; i < nb_lines; ++i) {
////		  double[] vector = lines.get(0,i);
////		  double x1 = vector[0];
////		  double y1 = vector[1];
////		  double x2 = vector[2];
////		  double y2 = vector[3];
////		  Point beg = new Point(x1, y1);
////		  Point end = new Point(2502d, y2);
////		  
////		  angle += Math.atan2(y2 - y1, x2 - x1);
////		  Core.line(orig, beg, end, new Scalar( 255, 0 ,0 ), 3);
////
////		  threshold.submat(new Rect(beg, end)).setTo(new Scalar(255,255,255));
//////		  for (i = 0; i < src.rows(); ++i) {
//////			  for (int j = 0; j < src.cols(); ++j) {
//////				  Point temp = new Point(src.get(i, j));
//////				  if(temp.x > beg.x) {
//////				  }
//////			  }
//////		  }
////	  } 
////	  Imgproc.HoughLinesP(negative, lines, 1d, Math.PI / 180d, 100, dst.size().width / 2d, 10d);
////	  nb_lines = lines.cols();
////	  for (int i = 0; i < nb_lines; ++i) {
////		  double[] vector = lines.get(0,i);
////		  double x1 = vector[0];
////		  double y1 = vector[1];
////		  double x2 = vector[2];
////		  double y2 = vector[3];
////		  
////		  Point beg = new Point(x1, y1);
////		  Point end = new Point(2502d, y2);
////		  
////		  Core.line(orig, beg, end, new Scalar( 255, 0 ,0 ), 3);
////
////		  threshold.submat(new Rect(beg, end)).setTo(new Scalar(255,255,255));
//////		  for (i = 0; i < src.rows(); ++i) {
//////			  for (int j = 0; j < src.cols(); ++j) {
//////				  Point temp = new Point(src.get(i, j));
//////			  }
//////		  }
////	  } 
////	  Core.line(orig, new Point(0d, 0d), new Point(5d, 5d), new Scalar( 255, 0 ,0 ), 10);
////	  angle = angle / nb_lines; // mean angle, in radians.
//////	  dst = threshold.clone();
//////	  text = "adaptive_thres_mean_bin_inv_551_lines";
////	  
////	  // deskew and rotate
////	  RotatedRect box = new RotatedRect(new Point(2502d/2d,351d/2d), dst.size(), angle);
////	  Mat rot_mat = Imgproc.getRotationMatrix2D(box.center, angle, 1);
////	  Mat warp = negative.clone();
////	  Imgproc.warpAffine(negative, warp, rot_mat, dst.size(), Imgproc.INTER_CUBIC);
////	  
////	  Size box_size = box.size;
////	  if (box.angle < -45d) { //swap
////		double temp = box_size.width;
////		box_size.width = box_size.height;
////		box_size.height = temp;
////	  }
////	  Mat sub = warp.clone();
////	  Imgproc.getRectSubPix(warp, box_size, box.center, sub);	
////	  
////	  // dilate and erode
////	  int erosion_size = 6;   
////	  Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, 
////			  			new Size(2 * erosion_size + 1, 2 * erosion_size + 1), 
////			  			new Point(erosion_size, erosion_size) );
////	  Mat erosion = sub.clone();
////	  Imgproc.erode(sub, erosion, element);
////	  Mat dilation = erosion.clone();
////	  Imgproc.dilate(erosion, dilation, element);
////	  
//////	  Imgproc.erode(mInput, mInput, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));  
//////	  Imgproc.dilate(mInput, mInput, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
////	  
////	  
//////	  find countours and view rectangles in the image
//////	  List<MatOfPoint> contours = new ArrayList<MatOfPoint>();    
//////	  Imgproc.findContours(negative, contours, element, Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
//////	  for(int i = 0; i < contours.size(); i++){
//////	      if (Imgproc.contourArea(contours.get(i)) > 50 ){
//////	          Rect rect = Imgproc.boundingRect(contours.get(i));
//////	          if (rect.height > 28){
//////	        	  Core.rectangle(orig, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,255,0));
//////	          }
//////	      }
//////	  }
////	  
//	  dst = img.clone();
//	  text = "vzorek_output";
//	  
//	 // vzorek2.jpg - rozpoznany text: "1 51 75 5  576 53" (nacteno jako grayscale), "3 5 371 53" (nacteno nezmeneno)
//	 // test_251_multiply_add_end.png - rozpoznany text: "1 1  3   1 7 5   95 53" (nacteno jako grayscale), "1 1  3   1 7 5   95 53" (nacteno nezmeneno)
//	  
//
//////////////////////////////////////////////////////////////////////	
//	  //final stage - create result 
//	  Bitmap result = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
//	  Utils.matToBitmap(dst, result);
//   
//	  path = Environment.getExternalStorageDirectory().getPath() + "/Pictures/preprocessing/" + text + ".png";
//	  Highgui.imwrite(path, dst);
//	  
////	  // save the output of the preprocessing onto the sd card
////	  FileOutputStream out = null;
////	  try {
////	      out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Pictures/outputIMG_vzorek_wo_lines.png");
////	      result.compress(Bitmap.CompressFormat.PNG, 100, out); 
////	  } catch (Exception e) {
////	      e.printStackTrace();
////	  } finally {
////	      try {
////	          if (out != null) { out.flush(); out.close(); }
////	      } catch (IOException e) { e.printStackTrace(); }
////	  }
////	
//	  
//	  // set bitmap for recognition
//	  bitmapForRecognition = result;
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
//}

@SuppressWarnings("unused")
	private OcrResult getOcrResult() {
    OcrResult ocrResult;
    String textResult = null;
    long start = System.currentTimeMillis();

    try {     
//      baseApi.setImage(ReadFile.readBitmap(bitmapForRecognition));
//      baseApi.setImage(ReadFile.readBitmap(bitmap));
//      textResult = baseApi.getUTF8Text();
    	
    	textResult = "123456789"; // to be replaced by the line below
        //textResult = webService(bitmap);
      
//      Log.e("RECOGNIZED_TEXT!!!!", textResult);
      
      timeRequired = System.currentTimeMillis() - start;

      // Check for failure to recognize text
      if (textResult == null || textResult.equals("")) {
        return null;
      }
      ocrResult = new OcrResult();
//      ocrResult.setWordConfidences(baseApi.wordConfidences());
//      ocrResult.setMeanConfidence( baseApi.meanConfidence());
//      if (ViewfinderView.DRAW_REGION_BOXES) {
//        ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
//      }
//      if (ViewfinderView.DRAW_TEXTLINE_BOXES) {
//        ocrResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
//      }
//      if (ViewfinderView.DRAW_STRIP_BOXES) {
//        ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
//      }
      
      // Always get the word bounding boxes--we want it for annotating the bitmap after the user
      // presses the shutter button, in addition to maybe wanting to draw boxes/words during the
      // continuous mode recognition.
//      ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
      
//      if (ViewfinderView.DRAW_CHARACTER_BOXES || ViewfinderView.DRAW_CHARACTER_TEXT) {
//        ocrResult.setCharacterBoundingBoxes(baseApi.getCharacters().getBoxRects());
//      }
    } catch (RuntimeException e) {
      Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
      e.printStackTrace();
      try {
//        baseApi.clear();
    	  captureActivity.stopHandler();
      } catch (NullPointerException e1) {
        // Continue
      }
      return null;
    }
    timeRequired = System.currentTimeMillis() - start;
    ocrResult.setBitmap(bitmap);
//    ocrResult.setBitmapForRecognition(bitmapForRecognition);
    ocrResult.setText(textResult);
    ocrResult.setRecognitionTimeRequired(timeRequired);
    return ocrResult;
  }
  
//  private void sendContinuousOcrFailMessage() {
//    Handler handler = activity.getHandler();
//    if (handler != null) {
//      Message message = Message.obtain(handler, R.id.ocr_continuous_decode_failed, new OcrResultFailure(timeRequired));
//      message.sendToTarget();
//    }
//  }

}












