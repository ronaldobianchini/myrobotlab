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

package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Incubator;
import org.slf4j.Logger;

public class IncubatorGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(IncubatorGUI.class.getCanonicalName());

	public IncubatorGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void init() {
	}

	public void getState(Incubator template) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Incubator.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Incubator.class);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}
	

}
