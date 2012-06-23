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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.myrobotlab.arduino.gui.Base;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.GUI;

/**
 * Arduino Diecimila http://www.arduino.cc/en/Main/ArduinoBoardDiecimila Serial:
 * 0 (RX) and 1 (TX). Used to receive (RX) and transmit (TX) TTL serial data.
 * These pins are connected to the corresponding pins of the FTDI USB-to-TTL
 * Serial chip. External Interrupts: 2 and 3. These pins can be configured to
 * trigger an interrupt on a low value, a rising or falling edge, or a change in
 * value. See the attachInterrupt() function for details. PWM: 3, 5, 6, 9, 10,
 * and 11. Provide 8-bit PWM output with the analogWrite() function. SPI: 10
 * (SS), 11 (MOSI), 12 (MISO), 13 (SCK). These pins support SPI communication,
 * which, although provided by the underlying hardware, is not currently
 * included in the Arduino language. LED: 13. There is a built-in LED connected
 * to digital pin 13. When the pin is HIGH value, the LED is on, when the pin is
 * LOW, it's off.
 * 
 * TODO - log serial data window
 * 
 */
public class ArduinoGUI extends ServiceGUI implements ItemListener, ActionListener {

	public ArduinoGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	/**
	 * component array - to access all components by name
	 */
	public Arduino myArduino;
	HashMap<String, Component> components = new HashMap<String, Component>();

	static final long serialVersionUID = 1L;

	JTabbedPane tabs = new JTabbedPane();

	/*
	 * ---------- Pins begin -------------------------
	 */

	JLayeredPane imageMap;
	/*
	 * ---------- Pins end -------------------------
	 */

	/*
	 * ---------- Config begin -------------------------
	 */
	ArrayList<Pin> pinList = null;
	//JComboBox boardType = new JComboBox(new String[] { "Duemilanove", "Mega" });
	JComboBox serialDevice = new JComboBox(new String[] { "" });
	JComboBox baudRate = new JComboBox(
			new Integer[] { 300, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 57600, 115200 });
	/**
	 * for pins 6 and 5 1kHz default
	 */
	JComboBox PWMRate1 = new JComboBox(new String[] { "62", "250", "1000", "8000", "64000" });
	/**
	 * for pins 9 and 10 500 hz default
	 */
	JComboBox PWMRate2 = new JComboBox(new String[] { "31", "125", "500", "4000", "32000" });
	/**
	 * for pins 3 and 111 500 hz default
	 */
	JComboBox PWMRate3 = new JComboBox(new String[] { "31", "125", "500", "4000", "32000" });

	JIntegerField rawReadMsgLength = new JIntegerField(4);
	JCheckBox rawReadMessage = new JCheckBox();
	/*
	 * ---------- Config end -------------------------
	 */

	/*
	 * ---------- Oscope begin -------------------------
	 */
	SerializableImage sensorImage = null;
	Graphics g = null;
	VideoWidget oscope = null;
	/*
	 * ---------- Oscope end -------------------------
	 */

	/*
	 * ---------- Editor begin -------------------------
	 */
	Base arduinoIDE; 
	// DigitalButton fullscreenButton = null;
	DigitalButton uploadButton = null;
	JPanel editorPanel = null;
	GridBagConstraints epgc = new GridBagConstraints();
	Dimension size = new Dimension(620, 442);

	/*
	 * ---------- Editor begin -------------------------
	 */

	public void init() {

		// ---------------- tabs begin ----------------------
		tabs.setTabPlacement(JTabbedPane.RIGHT);

		// ---------------- tabs begin ----------------------
		// --------- configPanel begin ----------------------
		JPanel configPanel = new JPanel(new GridBagLayout());
		GridBagConstraints cpgc = new GridBagConstraints();

		cpgc.anchor = GridBagConstraints.WEST;

		cpgc.gridx = 0;
		cpgc.gridy = 0;

		configPanel.add(new JLabel("type : "), cpgc);
		++cpgc.gridx;
		//configPanel.add(boardType, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" pwm 5 6 : "), cpgc);
		++cpgc.gridx;
		PWMRate1.setSelectedIndex(2);
		configPanel.add(PWMRate1, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" pwm 9 10 : "), cpgc);
		++cpgc.gridx;
		PWMRate2.setSelectedIndex(2);
		configPanel.add(PWMRate2, cpgc);

		cpgc.gridx = 0;
		++cpgc.gridy;
		configPanel.add(new JLabel("port : "), cpgc);
		++cpgc.gridx;
		configPanel.add(serialDevice, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" serial rate : "), cpgc);
		++cpgc.gridx;
		configPanel.add(baudRate, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" pwm 3 11 : "), cpgc);
		++cpgc.gridx;
		PWMRate3.setSelectedIndex(2);
		configPanel.add(PWMRate3, cpgc);

		++cpgc.gridy;
		cpgc.gridx = 0;

		serialDevice.setName("serialDevice");
		//boardType.setName("boardType");
		baudRate.setName("baudRate");
		PWMRate1.setName("PWMRate1");
		PWMRate2.setName("PWMRate2");
		PWMRate3.setName("PWMRate3");

		PWMRate1.addActionListener(this);
		PWMRate2.addActionListener(this);
		PWMRate3.addActionListener(this);

		//boardType.addActionListener(this);
		serialDevice.addActionListener(this);
		baudRate.addActionListener(this);

		// --------- configPanel end ----------------------
		// --------- pinPanel begin -----------------------
		arduinoIDE = Base.getBase(this, myService.getFrame());
		arduinoIDE.handleActivated(arduinoIDE.editor);

		Map<String, String> boardPreferences = Base.getBoardPreferences();
		String boardName = boardPreferences.get("name");

		getPinPanel(); // FIXME - need preferences ?!?! 
		// ------digital pins tab end ------------

		// ------oscope tab begin ----------------
		JPanel oscopePanel = new JPanel(new GridBagLayout());
		GridBagConstraints opgc = new GridBagConstraints();

		JPanel tracePanel = new JPanel(new GridBagLayout());

		opgc.fill = GridBagConstraints.HORIZONTAL;
		opgc.gridx = 0;
		opgc.gridy = 0;
		// opgc.anchor = GridBagConstraints.WEST;

		// Color gradient = new Color();
		int red = 0x00;
		int gre = 0x16;
		int blu = 0x16;

		// pinList.size() mega 60 deuo 20
		for (int i = 0; i < 20; ++i) {
			Pin p = pinList.get(i);
			if (i < 14) { // digital pins -----------------
				p.trace.setText("D " + (i));
				p.trace.onText = "D " + (i);
				p.trace.offText = "D " + (i);
			} else {
				// analog pins ------------------
				p.trace.setText("A " + (i - 14));
				p.trace.onText = "A " + (i - 14);
				p.trace.offText = "A " + (i - 14);
			}
			tracePanel.add(p.trace, opgc);
			p.trace.setBackground(new Color(red, gre, blu));
			p.trace.offBGColor = new Color(red, gre, blu);
			gre += 12;
			blu += 12;
			++opgc.gridy;
		}

		opgc.gridx = 0;
		opgc.gridy = 0;

		oscope = new VideoWidget(boundServiceName, myService, false);
		oscope.init();
		sensorImage = new SerializableImage(new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB),
				"output");
		g = sensorImage.getImage().getGraphics();
		oscope.displayFrame(sensorImage);

		oscopePanel.add(tracePanel, opgc);
		++opgc.gridx;
		oscopePanel.add(oscope.display, opgc);

		// ------oscope tab end ----------------
		// ------editor tab begin ----------------
		editorPanel = new JPanel(new GridBagLayout());
		epgc = new GridBagConstraints();
		epgc.gridx = 0;
		epgc.gridy = 0;
		epgc.anchor = GridBagConstraints.WEST;
		epgc.gridx = 0;
		epgc.gridy = 0;
		// ------editor tab end ----------------

		JFrame top = myService.getFrame();
		tabs.addTab("pins", imageMap);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top, tabs, imageMap, boundServiceName, "pins"));
		tabs.addTab("config", configPanel);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top, tabs, configPanel, boundServiceName,
				"config"));
		tabs.addTab("oscope", oscopePanel);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top, tabs, oscopePanel, boundServiceName,
				"oscope"));
		tabs.addTab("editor", arduinoIDE.editor);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top, tabs, arduinoIDE.editor, boundServiceName,
				"editor"));

		display.add(tabs);
		
		// arduinoIDE editor status setup FIXME - lame
		arduinoIDE.editor.status.setup();

	}
	
	public JLayeredPane getPinPanel()
	{
		Map<String, String> boardPreferences = Base.getBoardPreferences();
		String boardName = boardPreferences.get("name");

		if (boardName.contains("Mega"))
		{
			return getMegaPanel();
		} else {
			return getDuemilanovePanel();
		}
		
	}

	/**
	 * FIXME - needs to add a route on AttachGUI and publish from the Arduino
	 * service when applicable (when polling)
	 * 
	 * @param p
	 *            - PinData from serial reads
	 */
	public void readData(PinData p) {
		log.info("ArduinoGUI setDataLabel " + p);
		Pin pin = pinList.get(p.pin);
		pin.data.setText(new Integer(p.value).toString());
		Integer d = Integer.parseInt(pin.counter.getText());
		d++;
		pin.counter.setText((d).toString());
	}

	public void getState(Arduino data) {
		if (data != null) {
			myArduino = data;
			setPorts(myArduino.portNames);
			
			serialDevice.removeActionListener(this);
			serialDevice.setSelectedItem(myArduino.getPortName());
			serialDevice.addActionListener(this);
			
			baudRate.removeActionListener(this);
			baudRate.setSelectedItem(myArduino.getBaudRate());
			baudRate.addActionListener(this);
			
			// TODO - iterate through others and unselect them
			// FIXME  - do we need to remove action listeners?
			log.info(arduinoIDE.editor.serialCheckBoxMenuItems.get(myArduino.getPortName()).isSelected());
			arduinoIDE.editor.serialCheckBoxMenuItems.get(myArduino.getPortName()).setSelected(true);
			arduinoIDE.editor.serialCheckBoxMenuItems.get(myArduino.getPortName()).doClick();
			
		}

	}
	
	public void openSketchInGUI(String path)
	{
		arduinoIDE.handleOpenReplace(path);
	}
	

	/**
	 * 
	 * FIXME - should be called "displayPorts" or "refreshSystemPorts"
	 * 
	 * setPorts is called by getState - which is called when the Arduino changes
	 * port state is NOT called by the GUI component
	 * 
	 * @param p
	 */
	public void setPorts(ArrayList<String> p) {
		// serialDevice.removeAllItems();
		// serialDevice.addItem(""); // the null port
		// serialDevice.removeAllItems();
		for (int i = 0; i < p.size(); ++i) {
			String n = p.get(i);
			log.info(n);
			serialDevice.addItem(n);
		}

	}
	public void uploadSketchFromGUI (Boolean b)
	{
		//arduinoIDE.editor.handleUpload(b);
		arduinoIDE.editor.handleBlockingUpload(b);
	}

	@Override
	public void attachGUI() {
		subscribe("publishPin", "publishPin", PinData.class); 
		subscribe("publishState", "getState", Arduino.class);
		subscribe("openSketchInGUI", "openSketchInGUI", String.class);
		subscribe("uploadSketchFromGUI", "uploadSketchFromGUI", Boolean.class);
		
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishPin", "publishPin", PinData.class); 
		unsubscribe("publishState", "getState", Arduino.class);
		unsubscribe("openSketchInGUI", "openSketchInGUI", String.class);
		unsubscribe("uploadSketchFromGUI", "uploadSketchFromGUI", Boolean.class);
	}

	@Override
	public void itemStateChanged(ItemEvent item) {
		{
			// called when the button is pressed
			JCheckBox cb = (JCheckBox) item.getSource();
			// Determine status
			boolean isSel = cb.isSelected();
			if (isSel) {
				myService.send(boundServiceName, "setRawReadMsg", true);
				myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(false);
			} else {
				myService.send(boundServiceName, "setRawReadMsg", false);
				myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(true);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		String cmd = e.getActionCommand();
		Component c = (Component) e.getSource();

		// buttons
		if (DigitalButton.class == o.getClass()) {
			DigitalButton b = (DigitalButton) o;
			/*
			 * if (fullscreenButton == c) { if ("fullscreen".equals(cmd)) {
			 * JFrame full = new JFrame(); // icon URL url =
			 * getClass().getResource("/resource/mrl_logo_36_36.png"); Toolkit
			 * kit = Toolkit.getDefaultToolkit(); Image img =
			 * kit.createImage(url); full.setIconImage(img);
			 * 
			 * full.setExtendedState(JFrame.MAXIMIZED_BOTH);
			 * 
			 * editorPanel.remove(editorScrollPane);
			 * full.getContentPane().add(editorScrollPane); //full.pack();
			 * full.setVisible(true); full.addWindowListener(this);
			 * myService.pack(); return; } }
			 */
			if (uploadButton == c) {
				uploadButton.toggle();
				return;
			}

			IOData io = new IOData();
			Pin pin = null;

			if (b.parent != null) {
				io.address = ((Pin) b.parent).pinNumber; // TODO - optimize
				pin = ((Pin) b.parent);
			}

			if (b.type == Pin.TYPE_ONOFF) {
				if ("off".equals(cmd)) {
					// now on
					io.value = Pin.HIGH;
					myService.send(boundServiceName, "digitalWrite", io);
					b.toggle();
				} else {
					// now off
					io.value = Pin.LOW;
					myService.send(boundServiceName, "digitalWrite", io);
					b.toggle();
				}

			} else if (b.type == Pin.TYPE_INOUT) {
				if ("OUTPUT".equals(cmd)) {
					// is now input
					io.value = Pin.INPUT;
					myService.send(boundServiceName, "pinMode", io);
					myService.send(boundServiceName, "digitalReadPollStart", io.address);
					b.toggle();
				} else {
					// is now output
					io.value = Pin.OUTPUT;
					myService.send(boundServiceName, "pinMode", io);
					myService.send(boundServiceName, "digitalReadPollStop", io.address);
					b.toggle();
				}
			} else if (b.type == Pin.TYPE_ACTIVEINACTIVE) {
				if ("active".equals(cmd)) {
					// now inactive
					myService.send(boundServiceName, "analogReadPollingStop", io.address);
					b.toggle();
				} else {
					// now active
					myService.send(boundServiceName, "analogReadPollingStart", io.address);
					b.toggle();
				}
			} else if (b.type == Pin.TYPE_TRACE) {
				if (b.isOn) {
					// now off

					// if pin is analog && off - switch it on
					if (pin.isAnalog) {
						if (pin.activeInActive.isOn) {
							myService.send(boundServiceName, "analogReadPollingStop", io.address);
							pin.activeInActive.setOff();
						}

					}

					b.toggle();
				} else {

					// if pin is digital and on - turn off
					if (pin.onOff.isOn && !pin.isAnalog) {
						io.value = Pin.LOW;
						myService.send(boundServiceName, "digitalWrite", io);
						pin.onOff.toggle();
					}

					// if pin is digital - make sure pinmode is input
					if (!pin.inOut.isOn && !pin.isAnalog) {
						io.value = Pin.INPUT;
						myService.send(boundServiceName, "pinMode", io);
						myService.send(boundServiceName, "digitalReadPollStart", io.address);
						pin.inOut.toggle();
					}

					// if pin is analog && off - switch it on
					if (pin.isAnalog) {
						if (!pin.activeInActive.isOn) {
							myService.send(boundServiceName, "analogReadPollingStart", io.address);
							pin.activeInActive.setOn();
						}

					}

					// myService.send(boundServiceName,
					// "analogReadPollingStart", io.address);
					b.toggle();
				}
			} else {
				log.error("unknown pin type " + b.type);
			}

			log.info("DigitalButton");
		}

		// ports & timers
		if (c == serialDevice) {
			JComboBox cb = (JComboBox) c;
			String newPort = (String) cb.getSelectedItem();
			myService.send(boundServiceName, "setPort", newPort);
		} else if (c == baudRate) {
			JComboBox cb = (JComboBox) c;
			Integer newBaud = (Integer) cb.getSelectedItem();
			myService.send(boundServiceName, "setBaud", newBaud);
		} else if (c == PWMRate1 || c == PWMRate2 || c == PWMRate3) {
			JComboBox cb = (JComboBox) e.getSource();
			Integer newFrequency = Integer.parseInt((String) cb.getSelectedItem());
			IOData io = new IOData();
			int timerAddr = (c == PWMRate1) ? Arduino.TCCR0B : ((c == PWMRate2) ? Arduino.TCCR0B : Arduino.TCCR2B);
			io.address = timerAddr;
			io.value = newFrequency;
			myService.send(boundServiceName, "setPWMFrequency", io);
		} 
/*		
		else if (c == boardType) {
			log.info("type change");
			JComboBox cb = (JComboBox) e.getSource();
			String newType = (String) cb.getSelectedItem();
			getPinPanel();
			// ----------- TODO ---------------------
			// MEGA Type switching

		}
*/		

	}

	public void closeSerialDevice()
	{
		myService.send(boundServiceName, "closeSerialDevice");
	}
	
	class TraceData {
		Color color = null;
		String label;
		String controllerName;
		int pin;
		int data[] = new int[DATA_WIDTH];
		int index = 0;
		int total = 0;
		int max = 0;
		int min = 1024; // TODO - user input on min/max
		int sum = 0;
		int mean = 0;
	}

	int DATA_WIDTH = 480; // TODO - sync with size
	int DATA_HEIGHT = 380;
	HashMap<Integer, TraceData> traceData = new HashMap<Integer, TraceData>();
	int clearX = 0;

	public void publishPin(PinData pin) {
		if (!traceData.containsKey(pin.pin)) {
			TraceData td = new TraceData();
			// td.color = Color.decode("0x0f7391");
			td.color = pinList.get(pin.pin).trace.offBGColor;
			traceData.put(pin.pin, td);
		}

		TraceData t = traceData.get(pin.pin);
		t.index++;
		t.data[t.index] = pin.value;
		++t.total;
		t.sum += pin.value;
		t.mean = t.sum / t.total;

		g.setColor(t.color);
		// g.drawRect(20, t.pin * 15 + 5, 200, 15);
		g.drawLine(t.index, DATA_HEIGHT - t.data[t.index - 1] / 2, t.index, DATA_HEIGHT - pin.value / 2);

		// computer min max and mean
		// if different then blank & post to screen
		if (pin.value > t.max)
			t.max = pin.value;
		if (pin.value < t.min)
			t.min = pin.value;

		if (t.index < DATA_WIDTH - 1) {
			clearX = t.index + 1;
			// g.drawLine(clearX, DATA_HEIGHT - t.data[t.index-1]/2, clearX,
			// DATA_HEIGHT - t.data[clearX]/2);
		} else {
			t.index = 0;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, DATA_WIDTH, DATA_HEIGHT);
			g.setColor(Color.GRAY);
			g.drawLine(0, DATA_HEIGHT - 25, DATA_WIDTH - 1, DATA_HEIGHT - 25);
			g.drawString("50", 10, DATA_HEIGHT - 25);
			g.drawLine(0, DATA_HEIGHT - 50, DATA_WIDTH - 1, DATA_HEIGHT - 50);
			g.drawString("100", 10, DATA_HEIGHT - 50);
			g.drawLine(0, DATA_HEIGHT - 100, DATA_WIDTH - 1, DATA_HEIGHT - 100);
			g.drawString("200", 10, DATA_HEIGHT - 100);
			g.drawLine(0, DATA_HEIGHT - 200, DATA_WIDTH - 1, DATA_HEIGHT - 200);
			g.drawString("400", 10, DATA_HEIGHT - 200);
			g.drawLine(0, DATA_HEIGHT - 300, DATA_WIDTH - 1, DATA_HEIGHT - 300);
			g.drawString("600", 10, DATA_HEIGHT - 300);
			g.drawLine(0, DATA_HEIGHT - 400, DATA_WIDTH - 1, DATA_HEIGHT - 400);
			g.drawString("800", 10, DATA_HEIGHT - 400);

			g.setColor(Color.BLACK);
			g.fillRect(20, t.pin * 15 + 5, 200, 15);
			g.setColor(t.color);
			g.drawString(" min " + t.min + " max " + t.max + " mean " + t.mean + " total " + t.total + " sum " + t.sum,
					20, t.pin * 15 + 20);

		}

		oscope.displayFrame(sensorImage);
	}

	/**
	 * Spew the contents of a String object out to a file.
	 */
	static public void saveFile(String str, File file) throws IOException {
		File temp = File.createTempFile(file.getName(), null, file.getParentFile());
		// PApplet.saveStrings(temp, new String[] { str }); FIXME
		if (file.exists()) {
			boolean result = file.delete();
			if (!result) {
				throw new IOException("Could not remove old version of " + file.getAbsolutePath());
			}
		}
		boolean result = temp.renameTo(file);
		if (!result) {
			throw new IOException("Could not replace " + file.getAbsolutePath());
		}
	}

	/**
	 * Get the number of lines in a file by counting the number of newline
	 * characters inside a String (and adding 1).
	 */
	static public int countLines(String what) {
		int count = 1;
		for (char c : what.toCharArray()) {
			if (c == '\n')
				count++;
		}
		return count;
	}

	public JLayeredPane getMegaPanel() {
		
		if (imageMap != null){
			tabs.remove(imageMap);
		}
		
		pinList = new ArrayList<Pin>();
		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(size);

		// set correct arduino image
		JLabel image = new JLabel();

		ImageIcon dPic = Util.getImageIcon("images/service/Arduino/mega.200.pins.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));

		for (int i = 0; i < 70; ++i) {

			Pin p = null;
	
			if (i > 1 && i < 14) { // pwm pins -----------------
				p = new Pin(myService, boundServiceName, i, true, false, true);
				int xOffSet = 0;
				if (i > 7)
					xOffSet = 18; // skip pin
				p.inOut.setBounds(252 - 18 * i - xOffSet, 30, 15, 30);
				imageMap.add(p.inOut, new Integer(2));
				p.onOff.setBounds(252 - 18 * i - xOffSet, 0, 15, 30);
				// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
				imageMap.add(p.onOff, new Integer(2));

				if (p.isPWM) {
					p.pwmSlider.setBounds(252 - 18 * i - xOffSet, 75, 15, 90);
					imageMap.add(p.pwmSlider, new Integer(2));
					p.data.setBounds(252  - 18 * i - xOffSet, 180, 32, 15);
					p.data.setForeground(Color.white);
					p.data.setBackground(Color.decode("0x0f7391"));
					p.data.setOpaque(true);
					imageMap.add(p.data, new Integer(2));
				}
			} else if (i < 54 && i > 21) {
				// digital pin racks
				p = new Pin(myService, boundServiceName, i, false, false, false);

				if (i != 23 && i != 25 && i != 27 && i != 29)
				{
					if ((i%2 == 0))
					{
						// first rack of digital pins
						p.inOut.setBounds(472, 55 + 9 * (i - 21), 30, 15);
						imageMap.add(p.inOut, new Integer(2));
						p.onOff.setBounds(502, 55 + 9 * (i - 21), 30, 15);
						// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
						imageMap.add(p.onOff, new Integer(2));
					} else {
						// second rack of digital pins
						p.inOut.setBounds(567, 45 + 9 * (i - 21), 30, 15);
						imageMap.add(p.inOut, new Integer(2));
						p.onOff.setBounds(597, 45 + 9 * (i - 21), 30, 15);
						// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
						imageMap.add(p.onOff, new Integer(2));					
					}
				}

			} else if (i > 53) {
				p = new Pin(myService, boundServiceName, i, false, true, true);
				// analog pins -----------------
				int xOffSet = 0;
				if (i > 61)
					xOffSet = 18; // skip pin
				p.activeInActive.setBounds(128 + 18 * (i - 52) + xOffSet, 392, 15, 48);
				imageMap.add(p.activeInActive, new Integer(2));
				/* bag data at the moment - go look at the Oscope
				p.data.setBounds(208 + 18 * (i - 52) + xOffSet, 260, 32, 18);
				p.data.setForeground(Color.white);
				p.data.setBackground(Color.decode("0x0f7391"));
				p.data.setOpaque(true);
				imageMap.add(p.data, new Integer(2));
				*/
			} else {
				p = new Pin(myService, boundServiceName, i, false, false, false);
			}
			
			// set up the listeners
			p.onOff.addActionListener(this);
			p.inOut.addActionListener(this);
			p.activeInActive.addActionListener(this);
			p.trace.addActionListener(this);
			// p.inOut2.addActionListener(this);

			pinList.add(p);

		}

		JFrame top = myService.getFrame();
		tabs.insertTab("pins", null, imageMap, "pin panel", 0);
		tabs.setTabComponentAt(0, new TabControl(top, tabs, imageMap, boundServiceName, "pins"));

		return imageMap;
	}


	public JLayeredPane getDuemilanovePanel() {

		if (imageMap != null){
			tabs.remove(imageMap);
		}
		
		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(size);
		pinList = new ArrayList<Pin>();

		// set correct arduino image
		JLabel image = new JLabel();

		ImageIcon dPic = Util.getImageIcon("images/service/Arduino/arduino.duemilanove.200.pins.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));

		for (int i = 0; i < 20; ++i) {

			Pin p = null;
			if (i < 14) {
				if (((i == 3) || (i == 5) || (i == 6) || (i == 9) || (i == 10) || (i == 11))) {
					p = new Pin(myService, boundServiceName, i, true, false, false);
				} else {
					p = new Pin(myService, boundServiceName, i, false, false, false);
				}
			} else {
				p = new Pin(myService, boundServiceName, i, false, true, false);
			}

			// set up the listeners
			p.onOff.addActionListener(this);
			p.inOut.addActionListener(this);
			p.activeInActive.addActionListener(this);
			p.trace.addActionListener(this);
			// p.inOut2.addActionListener(this);

			pinList.add(p);

			if (i < 2) {
				continue;
			}
			if (i < 14) { // digital pins -----------------
				int yOffSet = 0;
				if (i > 7)
					yOffSet = 18; // skip pin
				p.inOut.setBounds(406, 297 - 18 * i - yOffSet, 30, 15);
				imageMap.add(p.inOut, new Integer(2));
				p.onOff.setBounds(436, 297 - 18 * i - yOffSet, 30, 15);
				// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
				imageMap.add(p.onOff, new Integer(2));

				if (p.isPWM) {
					p.pwmSlider.setBounds(256, 297 - 18 * i - yOffSet, 90, 15);
					imageMap.add(p.pwmSlider, new Integer(2));
					p.data.setBounds(232, 297 - 18 * i - yOffSet, 32, 15);
					p.data.setForeground(Color.white);
					p.data.setBackground(Color.decode("0x0f7391"));
					p.data.setOpaque(true);
					imageMap.add(p.data, new Integer(2));
				}
			} else {
				// analog pins -----------------
				p.activeInActive.setBounds(11, 208 - 18 * (14 - i), 48, 15);
				imageMap.add(p.activeInActive, new Integer(2));
				p.data.setBounds(116, 205 - 18 * (14 - i), 32, 18);
				p.data.setForeground(Color.white);
				p.data.setBackground(Color.decode("0x0f7391"));
				p.data.setOpaque(true);
				imageMap.add(p.data, new Integer(2));
			}
		}
		
		JFrame top = myService.getFrame();
		tabs.insertTab("pins", null, imageMap, "pin panel", 0);
		tabs.setTabComponentAt(0, new TabControl(top, tabs, imageMap, boundServiceName, "pins"));
		return imageMap;
	}

}