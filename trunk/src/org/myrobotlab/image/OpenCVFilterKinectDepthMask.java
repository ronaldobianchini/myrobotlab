/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.image;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_legacy.*;
import static com.googlecode.javacv.cpp.opencv_video.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_legacy.*;
import static com.googlecode.javacv.cpp.opencv_video.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;

public class OpenCVFilterKinectDepthMask extends OpenCVFilter {
	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterKinectDepthMask.class.getCanonicalName());

	IplImage kinectDepth = null;
	IplImage ktemp = null;
	IplImage ktemp2 = null;
	IplImage black = null;
	IplImage itemp = null;
	IplImage itemp2 = null;
	IplImage gray = null;
	IplImage mask = null;
	
	public ArrayList<KinectImageNode> nodes = new ArrayList<KinectImageNode>();
	
	BufferedImage frameBuffer = null;
	CvMemStorage cvStorage = null;

	public boolean drawBoundingBoxes = true;
	public boolean publishNodes = false;
	
	CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);

	// cvDrawRect has to have 2 points - no cvDrawRect can't draw a cvRect ???
	// http://code.google.com/p/opencvx/ - apparently - I'm not the only one who
	// thinks this is silly http://opencvx.googlecode.com/svn/trunk/cvdrawrectangle.h
	
	CvPoint p0 = new CvPoint(0, 0);
	CvPoint p1 = new CvPoint(0, 0);
	
	public OpenCVFilterKinectDepthMask(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		return image.getBufferedImage(); // TODO - ran out of memory here
	}

	@Override
	public String getDescription() {
		return null;
	}

	int useMask = 0;
	
	@Override
	public void loadDefaultConfiguration() {
	}

	
	String imageKey = "kinectDepth";
	
	int mWidth = 0;
	int mHeight = 0;
	int mX = 0;
	int mY = 0;
	
	int scale = 2;
	
	// countours
	CvSeq contourPointer = new CvSeq();

	int minArea = 30;
	int maxArea = 0;
	boolean isMinArea = true;
	boolean isMaxArea = true;
	
	@Override
	public IplImage process(IplImage image) {

		
		/*
		
		0 - is about 23 "
		30000 - is about 6'
		There is a blackzone in between - (sign issue?)
		
		CvScalar min = cvScalar( 30000, 0.0, 0.0, 0.0);
		CvScalar max = cvScalar(100000, 0.0, 0.0, 0.0);
		
		*/
		if (cvStorage == null) {
			cvStorage = cvCreateMemStorage(0);
		}
		
		
		// TODO - clean up - remove input parameters? only use storage? 
		if (imageKey != null)
		{
			kinectDepth = getIplImage(imageKey);
		} else {
			kinectDepth = image;
		}

		// cv Pyramid Down
		
		if (mask == null ) // || image.width() != mask.width()
		{
			mask = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 1);
			ktemp = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 16, 1);
			ktemp2 = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 1);
			black = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 1);
			itemp = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 3);
			itemp2 = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 3);
			gray = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 1);
		}
		cvZero(black); 								
		cvZero(mask); 								
		cvZero(itemp2); 										
		
		cvPyrDown(image, itemp, 7);
		cvPyrDown(kinectDepth, ktemp, 7);
		
		//cvReshape(arg0, arg1, arg2, arg3);
		//cvConvertScale(ktemp, ktemp2, 0.009, 0);

		CvScalar min = cvScalar(0, 0.0, 0.0, 0.0);
		//CvScalar max = cvScalar(30000, 0.0, 0.0, 0.0);
		CvScalar max = cvScalar(10000, 0.0, 0.0, 0.0);
									
		cvInRangeS(ktemp, min, max, mask);
		
		int offsetX = 0;
		int offsetY = 0;
		mWidth = 607/scale - offsetX;
		mHeight = 460/scale - offsetY;
		mX = 25/scale + offsetX;
		mY = 20/scale + offsetY;
		
		// shifting mask 32 down and to the left 25 x 25 y 
		cvSetImageROI(mask, cvRect(mX, 0, mWidth, mHeight)); // 615-8 = to remove right hand band
		cvSetImageROI(black, cvRect(0, mY, mWidth, mHeight)); 
		cvCopy(mask, black);
		cvResetImageROI(mask);
		cvResetImageROI(black);
		cvCopy(itemp, itemp2, black);

		
		
		myService.invoke("publishFrame", "input", itemp.getBufferedImage());
		myService.invoke("publishFrame", "kinectDepth", ktemp.getBufferedImage());
		myService.invoke("publishFrame", "kinectMask", mask.getBufferedImage());

		// TODO - publish KinectImageNode ArrayList
		// find contours ---- begin ------------------------------------
		CvSeq contour = contourPointer;
		int cnt = 0;

		nodes.clear();
		
		// cvFindContours(mask, cvStorage, contourPointer, Loader.sizeof(CvContour.class), 0 ,CV_CHAIN_APPROX_SIMPLE); NOT CORRECTED
		if (itemp2.nChannels()== 3) {
			cvCvtColor(itemp2, gray, CV_BGR2GRAY);
		} else {
			gray = itemp2.clone();
		}

		cvFindContours(gray, cvStorage, contourPointer, Loader.sizeof(CvContour.class), 0 ,CV_CHAIN_APPROX_SIMPLE);
		
		// new cvFindContours(gray, storage, contourPointer, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
		// old cvFindContours(gray, storage, contourPointer, sizeofCvContour, 0 ,CV_CHAIN_APPROX_SIMPLE);

		// LOG.error("getStructure");

		nodes.clear();

		minArea = 1500;
		while (contour != null && !contour.isNull()) {			
			if (contour.elem_size() > 0) { // TODO - limit here for "TOOOO MANY !!!!"

				CvRect rect = cvBoundingRect(contour, 0);

				// size filter
				if (minArea > 0 && (rect.width() * rect.height()) < minArea)
				{
					isMinArea = false;
				}

				if (maxArea > 0)
				{
					isMaxArea = false;
				} 
				
				
				if (isMinArea && isMaxArea)
				{
					CvSeq points = cvApproxPoly(contour,
							Loader.sizeof(CvContour.class), cvStorage, CV_POLY_APPROX_DP,
							cvContourPerimeter(contour) * 0.02, 1);
					KinectImageNode node = new KinectImageNode();
					//node.cameraFrame = image.getBufferedImage(); 
					node.cvCameraFrame = itemp.clone();  // pyramid down version
					node.boudingBox = new CvRect(rect);
					nodes.add(node);
					
					if (drawBoundingBoxes)
					{
						cvPutText(itemp2, " " + points.total() + " "
								+ (rect.x() + rect.width() / 2) + ","
								+ (rect.y() + rect.height() / 2) + " " + rect.width() + "x" + rect.height() + "="
								+ (rect.width() * rect.height()) + " " + " "
								+ cvCheckContourConvexity(points), cvPoint(
								rect.x() + rect.width() / 2, rect.y()), font,
								CvScalar.WHITE);
						p0.x(rect.x());
						p0.y(rect.y());
						p1.x(rect.x() + rect.width());
						p1.y(rect.y() + rect.height());
						cvDrawRect(itemp2, p0, p1, CvScalar.RED, 1, 8, 0);
					}
				}

				isMinArea = true;
				isMaxArea = true;

				++cnt;
			}
			contour = contour.h_next();
		}

		cvClearMemStorage(cvStorage);

		if (publishNodes) { myService.invoke("publish", (Object) nodes); }
		
		// find contours ---- end --------------------------------------
		
		return itemp2;

	}

}
