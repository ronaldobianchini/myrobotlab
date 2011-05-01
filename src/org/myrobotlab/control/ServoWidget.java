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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.IOData;

// http://www.superrobotica.com/VisualSC2e.htm
//public class ServoWidget extends JPanel {
public class ServoWidget extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(ServoWidget.class
			.getCanonicalName());
	static final long serialVersionUID = 1L;

	JLabel dataLabel = null;

	DigitalButton onOffButton = null;
	JSlider slider = null;

	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	public ServoWidget(String name, GUIService myService) {
		super(name, myService);

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;

		onOffButton = new DigitalButton(this);
		input.add(onOffButton, gc);
		++gc.gridx;

		input.add(getAnalogValue(), gc);
		onOffButton.setText("attach");

		++gc.gridx;
		input.add(new JLabel(" "));
		++gc.gridx;
		dataLabel = new JLabel("90");

		input.add(dataLabel, gc);

		gc.gridwidth = 2;
		gc.gridx = 1;
		++gc.gridy;
		input.add(left, gc);
		++gc.gridx;

		input.add(right, gc);
		++gc.gridx;

		display.add(input);

	}

	private JSlider getAnalogValue() {
		if (slider == null) {
			slider = new JSlider(0, 180, 90);
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					dataLabel.setText("" + slider.getValue());

					if (myService != null) {
						myService.send(boundServiceName, "moveTo", new Integer(
								slider.getValue()));
					} else {
						LOG.error("can not send message myService is null");
					}
				}
			});

		}
		return slider;
	}

	public Service getOperator() {
		return myService;
	}

	public void setService(GUIService myService) {
		this.myService = myService;
	}

	private class DigitalButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 1L;
		ServoWidget pin = null;

		public DigitalButton(ServoWidget pin) {
			super();
			this.pin = pin;
			setText("Off");
			addActionListener(this);
		}

		// TODO - bind unbind
		@Override
		public void actionPerformed(ActionEvent e) {
			if (getText().equals("attach")) {
				setText("detach");
				IOData io = new IOData();
				// io.address.set(pin.getPinNumber()); io.value.set(0); TODO -
				// fix
				pin.getOperator().send(boundServiceName, "attach", io);
			} else {
				setText("attach");
				IOData io = new IOData();
				// io.address.set(pin.getPinNumber()); io.value.set(1);
				pin.getOperator().send(boundServiceName, "detach", io);
			}
		}
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}

}