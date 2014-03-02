package org.myrobotlab.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.pickToLight.Controller;
import org.myrobotlab.pickToLight.KitRequest;
import org.myrobotlab.pickToLight.Module;
import org.myrobotlab.pickToLight.ModuleList;
import org.myrobotlab.pickToLight.ModuleRequest;
import org.myrobotlab.pickToLight.SOAPResponse;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

// bin calls
// moduleList calls
// setAllBoxesLEDs (on off)
// setBoxesOn(String list)
// setBoxesOff(String list)
// getBesSwitchState()
// displayString(boxlist, str)
// ZOD
// update uri
// blinkOff

/*
 * 
 * DEPRECATE Worker .. maybe
 */

// TODO - automated registration
// Polling / Sensor - important - check sensor state

// - read config in /boot/  - registration url including password - proxy ? 

/**
 * @author GroG
 * 
 *         C:\mrl\myrobotlab>xjc -d src -p org.myrobotlab.pickToLight
 *         PickToLightTypes.xsd
 * 
 */
public class PickToLight extends Service implements GpioPinListenerDigital {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(PickToLight.class);

	transient public RasPi raspi;
	transient public WebGUI webgui;
	transient public Worker worker;

	// static HashMap<String, Module> modules = new HashMap<String, Module>();
	ConcurrentHashMap<String, Module> modules = new ConcurrentHashMap<String, Module>();
	// transient HashMap<String, Worker> workers = new HashMap<String,
	// Worker>();

	public final static String MODE_KITTING = "kitting";
	public final static String MODE_LEARNING = "learning";
	public final static String MODE_INSTALLING = "installing";
	public final static String MODE_STARTING = "starting";

	String mode = "kitting";
	String messageGoodPick = "GOOD";

	private int rasPiBus = 1;
	// FIXME - who will update me ?
	private String updateURL;
	private int blinkNumber = 5;
	private int blinkDelay = 300;

	transient Timer timer;

	KitRequest lastKitRequest = null;
	Date lastKitRequestDate;
	int kitRequestCount;
	Properties properties = new Properties();

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("raspi", "RasPi", "raspi");
		peers.put("webgui", "WebGUI", "web server interface");
		return peers;
	}

	// FIXME push Task add/remove/repeat into Service
	class Task extends TimerTask {

		Message msg;
		int interval = 0;

		public Task(Task s) {
			this.msg = s.msg;
			this.interval = s.interval;
		}

		public Task(int interval, String name, String method) {
			this(interval, name, method, (Object[]) null);
		}

		public Task(int interval, String name, String method, Object... data) {
			this.msg = createMessage(name, method, data);
			this.interval = interval;
		}

		@Override
		public void run() {

			getInbox().add(msg);

			if (interval > 0) {
				Task t = new Task(this);
				// clear history list - becomes "new" message
				t.msg.historyList.clear();
				timer.schedule(t, interval);
			}
		}
	}

	/**
	 * Worker is a PickToLight level thread which operates over (potentially)
	 * all of the service modules. Displays have their own
	 * 
	 */
	public class Worker extends Thread {

		public boolean isWorking = false;

		String task;
		Object[] data;

		public Worker(String task, Object... data) {
			super(task);
			this.task = task;
			this.data = data;
		}

		public void run() {
			try {

				isWorking = true;
				while (isWorking) {

					switch (task) {

					case "pollAll":
						for (Map.Entry<String, Module> o : modules.entrySet()) {
							Module m = o.getValue();
							// if display
							// read pause
							// sleep(30);
							m.display(Integer.toHexString(m.readSensor()));
						}

						// poll pause
						sleep(300);
						break;

					case "pollSet":
						log.info("Worker - pollSet");
						ArrayList<Module> list = ((ModuleList) data[0]).list;
						
						Iterator<Module> iter = list.iterator();
						while (iter.hasNext()) {
							Module m = iter.next();
							log.info("sensor {} value {} ", m.getI2CAddress(), m.readSensor());
							if (m.readSensor() == 1){ // FIXME !!!! BITMASK READ !!! (m.readSensor() == 3
								blinkOff(m.getI2CAddress());
								iter.remove();
							}
						}

						// poll pause
						sleep(300);
						break;

					case "cycleAll":

						// TreeMap<String, Module> sorted = new TreeMap<String,
						// Module>(modules);
						/*
						 * for (Map.Entry<String, Module> o :
						 * modules.entrySet()) { o.getValue().cycle((String)
						 * data.get("msg")); } isWorking = false;
						 */
						break;

					default:
						log.error(String.format("don't know how to handle task %s", task));
						break;
					}

				}
				log.info("leaving Worker");
			} catch (Exception e) {
				isWorking = false;
			}
		}
	}

	public PickToLight(String n) {
		super(n);
		webgui = (WebGUI) createPeer("webgui");
		webgui.autoStartBrowser(false);
		webgui.useLocalResources(true);
		raspi = (RasPi) createPeer("raspi");
		loadProperties();
	}

	public Properties loadProperties() {
		InputStream input = null;

		try {

			log.info("loading default properties");
			properties.load(PickToLight.class.getResourceAsStream("/resource/PickToLight/pickToLight.properties"));

			log.info("loading mes properties");
			input = new FileInputStream("/boot/pickToLight.properties");
			properties.load(input);

		} catch (Exception ex) {
			Logging.logException(ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// dont' care
				}
			}
		}

		return properties;
	}

	public String getVersion() {
		return Runtime.getVersion();
	}

	@Override
	public String getDescription() {
		return "Pick to light system";
	}

	public void createModules() {

		Integer[] devices = scanI2CDevices();

		// rather heavy handed no?
		modules.clear();

		log.info(String.format("found %d devices", devices.length));

		for (int i = 0; i < devices.length; ++i) {
			int deviceAddress = devices[i];
			// FIXME - kludge to work with our prototype
			// addresses of displays are above 100
			/*
			 * if (deviceAddress > 100) { createModule(rasPiBus, deviceAddress);
			 * }
			 */

			createModule(rasPiBus, deviceAddress);
		}
	}

	public boolean createModule(int bus, int address) {
		String key = makeKey(address);
		log.info(String.format("create module key %s (bus %d address %d)", key, bus, address));
		Module box = new Module(bus, address);
		modules.put(key, box);
		return true;
	}

	public void ledOn(Integer address) {
		String key = makeKey(address);
		log.info("ledOn address {}", key);
		if (modules.containsKey(key)) {
			modules.get(key).ledOn();
		} else {
			// FIXME - Service Error Cache !!!! - IN GLOBAL getModule !
			error("ledOn could not find module %d", key);
		}
	}

	public void ledOff(Integer address) {
		String key = makeKey(address);
		log.info("ledOff address {}", key);
		if (modules.containsKey(key)) {
			modules.get(key).ledOff();
		} else {
			// FIXME - Service Error Cache !!!! - IN GLOBAL getModule !
			error("ledOff could not find module %d", key);
		}
	}

	public void ledsAllOn() {
		TreeMap<String, Module> sorted = new TreeMap<String, Module>(modules);
		for (Map.Entry<String, Module> o : sorted.entrySet()) {
			o.getValue().ledOn();
		}
	}

	public void ledsAllOff() {
		TreeMap<String, Module> sorted = new TreeMap<String, Module>(modules);
		for (Map.Entry<String, Module> o : sorted.entrySet()) {
			o.getValue().ledOff();
		}
	}

	public String display(Integer address, String msg) {
		String key = makeKey(address);
		if (modules.containsKey(key)) {
			modules.get(key).display(msg);
			return msg;
		} else {
			String err = String.format("display could not find module %d", key);
			log.error(err);
			return err;
		}
	}

	// public final
	// IN MEMORY ERROR LIST !!!!!!
	// getErrors( Error - key - detail - time )

	public boolean cycleIPAddress() {
		Controller c = getController();
		String ip = c.getIpAddress();
		if (ip == null || ip.length() == 0) {
			error("could not get ip");
			return false;
		}
		cycleAll(ip);
		return true;
	}

	public String displayAll(String msg) {

		for (Map.Entry<String, Module> o : modules.entrySet()) {
			Module mc = o.getValue();
			mc.display(msg);
		}

		return msg;
	}

	// FIXME normalize splitting code
	public String display(String moduleList, String value) {
		if (moduleList == null) {
			log.error("box list is null");
			return "box list is null";
		}
		String[] list = moduleList.split(" ");
		for (int i = 0; i < list.length; ++i) {
			try {
				String strKey = list[i].trim();
				if (strKey.length() > 0) {

					String key = makeKey(Integer.parseInt(strKey));
					if (modules.containsKey(key)) {
						modules.get(key).display(value);
					} else {
						log.error(String.format("display could not find module %s", strKey));
					}
				}
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
		return moduleList;
	}

	/**
	 * single location for key generation - in case other parts are add in a
	 * composite key
	 * 
	 * @param address
	 * @return
	 */
	public String makeKey(Integer address) {
		return makeKey(rasPiBus, address);
	}

	public String makeKey(Integer bus, Integer address) {
		// return String.format("%d.%d", bus, address);
		return String.format("%d", address);
	}

	// DEPRECATE ???
	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		// display pin state on console

		System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " [" + event.getPin().getName() + "]" + " = " + event.getState());
		GpioPin pin = event.getPin();

		/*
		 * if (pin.getName().equals("GPIO 0")) {
		 * modules.get("01").blinkOff("ok"); } else if
		 * (pin.getName().equals("GPIO 1")) { modules.get("02").blinkOff("ok");
		 * } else if (pin.getName().equals("GPIO 2")) {
		 * modules.get("03").blinkOff("ok"); } else if
		 * (pin.getName().equals("GPIO 3")) { modules.get("04").blinkOff("ok");
		 * }
		 */

		// if (pin.getName().equals(anObject))
	}

	public Integer[] scanI2CDevices() {
		ArrayList<Integer> ret = new ArrayList<Integer>();

		// our modules don't have addresses above 56
		Integer[] all = raspi.scanI2CDevices(rasPiBus);
		for (int i = 0; i < all.length; ++i) {
			Integer address = all[i];
			if (address > 56) {
				continue;
			}

			ret.add(all[i]);
		}

		return ret.toArray(new Integer[ret.size()]);
	}

	public void startService() {
		super.startService();

		raspi.startService();
		webgui.startService();
		createModules();
		
		systemCheck();

		// TODO - SystemCheck - cycle ip, cycle mac, all leds, all displays 8888
		// - display all sensor states S1 S2 S3 S4
		// TODO - blinkAllOn(Msg)
		/*
		 * String result = register(); cycleAll(result);
		 */

		//
		// blinkAllOn("S001");
		// sleep(5000);
		// autoRegister(10);
		// register();
	}

	public void systemCheck() {
		// TODO put into state mode - system check
		// check current mode see if its possible

		blinkAllOn("test");
		Controller c = getController();
		sleep(3000);

		// ip address
		displayAll("ip  ");
		sleep(1000);
		cycleAll(c.getIpAddress());
		sleep(5000);
		cycleAllStop();

		// mac address
		displayAll("mac ");
		sleep(1000);
		cycleAll(c.getMacAddress());
		sleep(5000);
		cycleAllStop();

		// i2c
		displayAll("i2c ");
		sleep(1000);
		displayAll(String.format("%d", modules.size()));
		sleep(3000);
		displayI2CAddresses();
		sleep(4000);

		// conn address
		displayAll("conn");
		sleep(1000);
		SOAPResponse sr = register();
		if (sr.isError()) {
			blinkAllOn(sr.getError());
		} else {
			displayAll("good");
		}

		sleep(5000);

		displayAll("done");

		sleep(5000);
		clearAll();
	}

	public Controller getController() {

		try {

			// WARNING WARNING WARNING WARNING WARNING WARNING WARNING
			// register calls getController at regular interval
			// so modules are re-recated at that interval
			// createModules();

			Controller controller = new Controller();

			controller.setVersion(Runtime.getVersion());
			controller.setName(getName());

			String ip = "";
			String mac = "";

			ArrayList<String> addresses = Runtime.getLocalAddresses();
			if (addresses.size() != 1) {
				log.error(String.format("incorrect number of ip addresses %d", addresses.size()));
			}

			if (!addresses.isEmpty()) {
				ip = addresses.get(0);
			}

			ArrayList<String> macs = Runtime.getLocalHardwareAddresses();
			if (macs.size() != 1) {
				log.error(String.format("incorrect number of mac addresses %d", addresses.size()));
			}
			if (!macs.isEmpty()) {
				mac = macs.get(0);
			}

			controller.setIpAddress(ip);
			controller.setMacAddress(mac);

			controller.setModules(modules);

			return controller;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	// ------------ TODO - IMPLEMENT - BEGIN ----------------------
	public String update(String url) {
		// TODO - auto-update
		return "TODO - auto update";
	}

	public String update() {
		return update(updateURL);
	}

	public void drawColon(Integer bus, Integer address, boolean draw) {

	}

	public int setBrightness(Integer address, Integer level) {
		return level;
	}

	public Module getModule(Integer address) {
		return getModule(rasPiBus, address);
	}

	public Module getModule(Integer bus, Integer address) {
		String key = makeKey(bus, address);
		if (!modules.containsKey(key)) {
			log.error(String.format("get module - could not find module with key %s", key));
			return null;
		}
		return modules.get(key);
	}

	// ---- cycling message on individual module begin ----
	public void cycle(Integer address, String msg) {
		cycle(address, msg, 300);
	}

	public void cycle(Integer address, String msg, Integer delay) {
		getModule(address).cycle(msg, delay);
	}

	public void cycleStop(Integer address) {
		getModule(address).cycleStop();
	}

	public void cycleAll(String msg) {
		cycleAll(msg, 300);
	}

	public void cycleAll(String msg, int delay) {
		TreeMap<String, Module> sorted = new TreeMap<String, Module>(modules);
		for (Map.Entry<String, Module> o : sorted.entrySet()) {
			o.getValue().cycle(msg, delay);
		}
	}

	public void cycleAllStop() {
		TreeMap<String, Module> sorted = new TreeMap<String, Module>(modules);
		for (Map.Entry<String, Module> o : sorted.entrySet()) {
			o.getValue().cycleStop();
		}
	}

	public void clearAll() {
		for (Map.Entry<String, Module> o : modules.entrySet()) {
			o.getValue().clear();
		}
	}

	public void displayI2CAddresses() {
		for (Map.Entry<String, Module> o : modules.entrySet()) {
			o.getValue().display(o.getKey());
		}
	}

	public void blinkAllOn(String msg) {
		for (Map.Entry<String, Module> o : modules.entrySet()) {
			blinkOn(o.getValue().getI2CAddress(), msg);
		}
	}

	public void blinkOn(Integer address, String msg) {
		blinkOn(address, msg, blinkNumber, blinkDelay);
	}

	public void blinkOn(Integer address, String msg, int blinkNumber, int blinkDelay) {
		getModule(address).blinkOn(msg, blinkNumber, blinkDelay);
	}
	
	public void blinkOff(Integer address){
		blinkOff(address, null);
	}

	public void blinkOff(Integer address, String msg) {
		blinkOff(address, msg, blinkNumber, blinkDelay);
	}

	public void blinkOff(Integer address, String msg, int blinkNumber, int blinkDelay) {
		getModule(address).blinkOff(msg, blinkNumber, blinkDelay);
	}

	/*
	 * public void startWorker(String key) { stopWorker(key); }
	 * 
	 * public void stopWorker(String key) { if (workers.containsKey("cycleAll"))
	 * { if (worker != null) { worker.isWorking = false; worker.interrupt();
	 * worker = null; } } }
	 */

	public void autoRefreshI2CDisplay(int seconds) {
		addLocalTask(seconds * 1000, "refreshI2CDisplay");
	}

	public void autoRegister(int seconds) {
		addLocalTask(seconds * 1000, "register");
	}

	public void autoCheckForUpdates(int seconds) {
		addLocalTask(seconds * 1000, "checkForUpdates");
	}

	public void refreshI2CDisplay() {
		createModules();
		displayI2CAddresses();
	}

	// ---- cycling message on individual module end ----

	// need a complex type
	public KitRequest kitToLight(KitRequest kit) {
		log.info("KitRequest");
		clearAll();
		if (kit == null) {
			error("kitToLight - kit is null");
			return null;
		}

		lastKitRequest = kit;
		lastKitRequestDate = new Date();
		++kitRequestCount;

		info("kitToLight vin %s kit %s", kit.vin, kit.kitId);

		if (kit.list == null) {
			error("kit list is null");
		}

		log.info("found {} ModuleRequests", kit.list.length);

		ModuleList pollingList = new ModuleList();
		for (int i = 0; i < kit.list.length; ++i) {
			ModuleRequest mr = kit.list[i];
			String key = makeKey(mr.i2c);
			if (modules.containsKey(key)) {
				Module m = modules.get(key);

				m.display(mr.quantity);
				m.ledOn();
				
				pollingList.list.add(m);

			} else {
				error("could not find i2c address %d for vin %s kitId %s", mr.i2c, kit.vin, kit.kitId);
			}
		}
		
		pollSet(pollingList);

		return kit;
	}

	public KitRequest createKitRequest() {
		KitRequest kit = new KitRequest();
		kit.vin = "8374756";
		kit.kitId = "324";

		kit.list = new ModuleRequest[modules.size()];
		int i = 0;
		for (Map.Entry<String, Module> o : modules.entrySet()) {

			ModuleRequest mr = new ModuleRequest();
			mr.i2c = o.getValue().getI2CAddress();
			mr.quantity = "" + (int) ((Math.random() * 5) + 1);

			kit.list[i] = mr;
			++i;
		}
		return kit;
	}

	public void writeToDisplay(int address, byte b0, byte b1, byte b2, byte b3) {
		try {

			I2CBus i2cbus = I2CFactory.getInstance(rasPiBus);
			I2CDevice device = i2cbus.getDevice(address);
			device.write(address, (byte) 0x80);

			I2CDevice display = i2cbus.getDevice(0x38);
			display.write(new byte[] { 0, 0x17, b0, b1, b2, b3 }, 0, 6);

			device.write(address, (byte) 0x83);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		if (MODE_LEARNING.equalsIgnoreCase(mode)) {
			ledsAllOn();
			displayI2CAddresses();
		}
		this.mode = mode;
	}

	final public static String soapRegisterTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://tempuri.org/\"><soapenv:Header/><soapenv:Body><tem:RegisterController><tem:Name>%s</tem:Name><tem:MACAddress>%s</tem:MACAddress><tem:IPAddress>%s</tem:IPAddress><tem:I2CAddresses></tem:I2CAddresses></tem:RegisterController></soapenv:Body></soapenv:Envelope>";
	final public static String soapEventTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://tempuri.org/\"><soapenv:Header/><soapenv:Body><tem:PickToLightEvent><tem:MACAddress>%s</tem:MACAddress><tem:IPAddress>%s</tem:IPAddress><tem:EventType>%s</tem:EventType><tem:Data>%s</tem:Data></tem:PickToLightEvent></soapenv:Body></soapenv:Envelope>";

	public final static String ERROR_CONNECTION_REFUSED = "E001";
	public final static String ERROR_CONNECTION_RESET = "E002";
	public final static String ERROR_NO_RESPONSE = "E003";

	public SOAPResponse register() {
		Controller controller = getController();
		String body = String.format(soapRegisterTemplate, "name", controller.getMacAddress(), controller.getIpAddress());
		String soapResponse = sendSoap("http://tempuri.org/SoapService/RegisterController", body);

		SOAPResponse ret = new SOAPResponse();

		if (soapResponse == null || soapResponse.length() == 0) {
			ret.setError(ERROR_NO_RESPONSE);
		} else if (soapResponse.contains("reset")) {
			ret.setError(ERROR_CONNECTION_RESET);
		} else if (soapResponse.contains("refused")) {
			ret.setError(ERROR_CONNECTION_RESET);
		}

		log.info(soapResponse);

		return ret;
	}

	public String sendEvent(String eventType, Object data) {
		Controller controller = getController();
		String body = String.format(soapRegisterTemplate, "name", controller.getMacAddress(), controller.getIpAddress());
		return sendSoap("http://tempuri.org/SoapService/PickToLightEvent", body);
	}

	public String sendSoap(String soapAction, String soapEnv) {
		String mesEndpoint = properties.getProperty("mes.endpoint");
		String mesUser = properties.getProperty("mes.user");
		String mesDomain = properties.getProperty("mes.domain");
		String mesPassword = properties.getProperty("mes.password");

		String ret = "";

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			List<String> authpref = new ArrayList<String>();
			authpref.add(AuthPolicy.NTLM);
			httpclient.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
			NTCredentials creds = new NTCredentials(mesUser, mesPassword, "", mesDomain);
			httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

			HttpContext localContext = new BasicHttpContext();
			HttpPost post = new HttpPost(mesEndpoint);

			// ,"utf-8"
			StringEntity stringentity = new StringEntity(soapEnv);
			stringentity.setChunked(true);
			post.setEntity(stringentity);
			post.addHeader("Accept", "text/xml");
			post.addHeader("SOAPAction", soapAction);
			post.addHeader("Content-Type", "text/xml; charset=utf-8");

			HttpResponse response = httpclient.execute(post, localContext);
			HttpEntity entity = response.getEntity();
			ret = EntityUtils.toString(entity);

			// parse the response - check
		} catch (Exception e) {
			error("endpoint %s user %s domain %s password %s", mesEndpoint, mesUser, mesDomain, mesPassword);
			Logging.logException(e);
			ret = e.getMessage();
		}

		log.info(ret);
		return ret;

	}

	public void purgeAllTasks() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	public void addLocalTask(int interval, String method) {
		if (timer == null) {
			timer = new Timer(String.format("%s.timer", getName()));
		}

		Task task = new Task(interval, getName(), method);
		timer.schedule(task, 0);
	}

	public void addLocalTaskWithCallback(int interval, String method) {
		if (timer == null) {
			timer = new Timer(String.format("%s.timer", getName()));
		}

		Task task = new Task(interval, getName(), method);
		timer.schedule(task, 0);
	}

	public void pollAll() {
		log.info("pollAll");
		stopPolling();

		worker = new Worker("pollAll");
		worker.start();
	}
	
	public void pollSet(ModuleList moduleList) {
		log.info("pollSet");
		stopPolling();

		worker = new Worker("pollSet", moduleList);
		worker.start();
	}

	public void stopPolling() {
		if (worker != null) {
			worker.interrupt();
			worker.isWorking = false;
			worker = null;
		}
	}

	// ------------ TODO - IMPLEMENT - END ----------------------

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		PickToLight pick = new PickToLight("pick.1");
		pick.startService();
		pick.systemCheck();

		boolean stopHere = true;
		if (stopHere) {
			return;
		}

		pick.register();

		pick.autoRefreshI2CDisplay(1);

		boolean ret = true;
		if (ret) {
			return;
		}

		pick.register();
		pick.createModules();

		Controller controller = pick.getController();
		pick.startService();

		int selector = 0x83; // IR selected - LED OFF

		/*
		 * int MASK_DISPLAY = 0x01; int MASK_LED = 0x02; int MASK_SENSOR = 0x80;
		 */

		log.info(String.format("0x%s", Integer.toHexString(selector)));
		selector &= ~Module.MASK_LED; // LED ON
		log.info(String.format("0x%s", Integer.toHexString(selector)));
		selector |= Module.MASK_LED; // LED OFF
		log.info(String.format("0x%s", Integer.toHexString(selector)));

		ArrayList<String> ips = Runtime.getLocalAddresses();

		for (int i = 0; i < ips.size(); ++i) {
			log.info(ips.get(i));
		}

		ips = Runtime.getLocalHardwareAddresses();

		for (int i = 0; i < ips.size(); ++i) {
			log.info(ips.get(i));
		}

		// Controller2 c = pick.getController();
		// log.info("{}", c);

		// String binList = pickToLight.getBoxList();
		// pickToLight.display(binList, "helo");
		/*
		 * pickToLight.display("01", "1234"); pickToLight.display(" 01 02 03 ",
		 * "1234  1"); pickToLight.display("01 03", " 1234"); //
		 * pickToLight.display(binList, "1234 ");
		 */

		// Runtime.createAndStart("web", "WebGUI");

		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */

	}

}
