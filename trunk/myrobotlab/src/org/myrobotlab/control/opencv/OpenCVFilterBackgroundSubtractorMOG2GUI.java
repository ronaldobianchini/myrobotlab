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

package org.myrobotlab.control.opencv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterBackgroundSubtractorMOG2;
import org.myrobotlab.service.GUIService;

public class OpenCVFilterBackgroundSubtractorMOG2GUI extends OpenCVFilterGUI implements ActionListener {

	String watchText = "watch foreground";
	String learnText = "learn background";
	JButton learn = new JButton(watchText);
	
	public OpenCVFilterBackgroundSubtractorMOG2GUI(String boundFilterName, String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);

		display.add(learn);
		learn.addActionListener(this);
	}

	// FIXME - update components :)
	@Override
	public void getFilterState(final FilterWrapper filterWrapper) {
		boundFilter = filterWrapper;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				OpenCVFilterBackgroundSubtractorMOG2 bf = (OpenCVFilterBackgroundSubtractorMOG2)filterWrapper.filter;
				if (bf.learn == -1)
				{
					learn.setText(watchText);
				} else {
					learn.setText(learnText);
				}
			}
		});

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		OpenCVFilterBackgroundSubtractorMOG2 bf = (OpenCVFilterBackgroundSubtractorMOG2) boundFilter.filter;
		if (o == learn)
		{
			if (watchText.equals(learn.getText()))
			{
				learn.setText(learnText);
				bf.learn = 0;
			} else {
				learn.setText(watchText);
				bf.learn = -1;
			}
		}
		
	}

	

}