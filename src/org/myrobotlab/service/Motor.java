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

package org.myrobotlab.service;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;

// TODO - implements Motor interface
// This mimics a DPDT Motor
public class Motor extends Service {
	/*
	 * TODO - there are only 2 ways to control a simple DC motor - SMOKE and
	 * H-Bridge/DPDT Where DPDT - one digital line is off/on - the other is CW
	 * CCW
	 * 
	 * Pwr Dir D0 D1 0 0 STOP (CCW) 0 1 STOP (CW) 1 0 GO (CCW) 1 1 GO (CW)
	 * 
	 * POWER - PWM is controlled only on the Pwr Line only - 1 PWM line
	 * 
	 * The other is 1 digital line each for each direction and power (SMOKE) if
	 * both are high
	 * 
	 * Pwr Pwr D0 D1 0 0 STOP 0 1 CW 1 0 CCW 1 1 SHORT & BURN - !!!! NA !!!
	 * 
	 * POWER - PWM must be put on both lines - 2 PWM lines
	 */

	public final static Logger LOG = Logger.getLogger(Motor.class.toString());

	boolean isAttached = false;
	
	int PWRPin;
	int DIRPin;
	int powerMultiplier = 255; // default to Arduino analogWrite max
								
	int FORWARD = 1;
	int BACKWARD = 0;

	float power = 0;
	float maxPower = 1;
	
	public boolean inMotion = false;

	boolean locked = false; // for locking the motor in a stopped position
	String controllerName = null; // board name
	
	DurationThread durationThread = null;

	public Motor(String name) {
		super(name, Motor.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public void attach(String controllerName, int PWRPin, int DIRPin) {
		this.controllerName = controllerName;
		this.PWRPin = PWRPin;
		this.DIRPin = DIRPin;
		send(controllerName, "motorAttach", this.name, PWRPin, DIRPin);
	}
	
	public void invertDirection() {
		FORWARD = 0;
		BACKWARD = 1;
	}

	public void incrementPower(float increment) {
		if (power + increment > maxPower || power + increment < -maxPower) 
		{
			LOG.error("power " + power + " out of bounds with increment "+ increment);
			return;
		}
		float newPowerLevel = power + increment;
		move(newPowerLevel);
	}

	// motor primitives begin ------------------------------------
	public void move(float newPowerLevel) {
		if (locked) return;

		// check if the direction has changed - send command if necessary
		if (newPowerLevel > 0 && power <= 0) {
			send(controllerName, DigitalIO.digitalWrite, DIRPin, FORWARD); 
		} else if (newPowerLevel < 0 && power >= 0) {
			send(controllerName, DigitalIO.digitalWrite, DIRPin, BACKWARD); 
		}

		//LOG.error("direction " + ((newPowerLevel > 0) ? "FORWARD" : "BACKWARD"));
		LOG.error(name + " power " + (int) (newPowerLevel * 100) + "% actual " + (int) (newPowerLevel * powerMultiplier));
		send(controllerName, AnalogIO.analogWrite, PWRPin, Math.abs((int) (newPowerLevel * powerMultiplier)));

		power = newPowerLevel;

	}
	
	Object lock = new Object();
	class DurationThread extends Thread
	{
		public float power = 0.0f;
		public int duration = 0;
		
		Motor instance = null;
		
		DurationThread(float power, int duration, Motor instance)
		{
			super (name + "_duration");
			this.power = power;
			this.duration = duration;
			this.instance = instance;
		}
		
		public void run ()
		{
			while (isRunning) // this is from Service - is OK?
			{
				synchronized (lock) {
					try {
						lock.wait();
						
						instance.move(this.power);
						inMotion = true;
						
						Thread.sleep(this.duration);
						
						instance.stop();
						inMotion = false;
						
					} catch (InterruptedException e) {
						LOG.warn("duration thread interrupted");
					}
					
					
					
				}
				
			}
			
		}
	}

	public void moveFor (float power, int duration)
	{
		// default is not to block
		moveFor(power, duration, false);
	}
	
	// TODO - operate from thread pool
	public void moveFor (float power, int duration, boolean block)
	{
		if (!block)
		{
			// non-blocking call to move for a duration
			if (durationThread == null)
			{
				durationThread = new DurationThread(power, duration, this);
				durationThread.start();
			} else {
				if (inMotion)
				{
					LOG.error("duration is busy with another move" + durationThread.duration);
				} else {
				 	synchronized (lock) {
						durationThread.power = power;
						durationThread.duration = duration;
						lock.notifyAll();
					}
				}
			}
		} else {
			// block the calling thread
			move(this.power);
			inMotion = true; 
			
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			stop();
			inMotion = false;

		}
		
		
	}

	public void stop() {
		move(0);
	}

	public void unLock() {
		LOG.info("unLock");
		locked = false;
	}

	public void lock() {
		LOG.info("lock");
		locked = true;
	}

	public void stopAndLock() {
		LOG.info("stopAndLock");
		move(0);
		lock();
	}

	public void setMaxPower(float max) {
		maxPower = max;
	}

	
	@Override
	public String getToolTip() {
		return "general motor service";
	}
	
}

/* TODO - implement in Arduino

	int targetPosition = 0;
	boolean movingToPosition = false;


public void attachEncoder(String encoderName, int pin) // TODO Encoder Interface
{
	this.encoderName = encoderName;
	PinData pd = new PinData();
	pd.pin = pin;
	encoderPin = pin; // TODO - have encoder own pin - send name for event

	// TODO - Make Encoder Interface

	NotifyEntry notifyEntry = new NotifyEntry();

	notifyEntry.name = name;
	notifyEntry.outMethod = "publishPin";
	notifyEntry.inMethod = "incrementPosition";
	notifyEntry.paramType = PinData.class.getCanonicalName();
	send(encoderName, "notify", notifyEntry);

}


	public int getPosition() {
		return position;
	}

	// feedback
	// public static final String FEEDBACK_TIMER = "FEEDBACK_TIMER";
	enum FeedbackType {
		Timer, Digital
	}

	Timer timer = new Timer();
	FeedbackType poistionFeedbackType = FeedbackType.Timer;

	enum BlockingMode {
		Blocking, Staggered, Overlap
	}

	BlockingMode blockingMode = BlockingMode.Blocking;

	
	 TODO - motors should not have any idea as to what their "pins" are - this
	 should be maintained by the controller
	

String encoderName = null; // feedback device
int encoderPin = 0; // TODO - put in Encoder class


	public int incrementPosition(PinData p) {
		if (p.pin != encoderPin) // TODO TODO TODO - this is wrong - should be
									// filtering on the Arduino publish !!!!
			return 0;

		if (direction == FORWARD) {
			position += 1;
			if (movingToPosition && position >= targetPosition) {
				stopMotor();
				movingToPosition = false;
			}

		} else {
			position -= 1;
			if (movingToPosition && position <= targetPosition) {
				stopMotor();
				movingToPosition = false;
			}
		}

		return position;

	}


	// move to relative amount - needs position feedback
	public void move(int amount) // TODO - "amount" should be moveTo
	{
		// setPower(lastPower);
		if (amount == 0) {
			return;
		} else if (direction == FORWARD) {
			direction = FORWARD;
			position += amount;
		} else if (direction == BACKWARD) {
			direction = BACKWARD;
			position -= amount;
		}

		move();
		amount = Math.abs(amount);
		if (poistionFeedbackType == FeedbackType.Timer
				&& blockingMode == BlockingMode.Blocking) {
			try {
				Thread.sleep(amount * positionMultiplier);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// TODO - this is overlapp mode (least useful)
			// timer.schedule(new FeedbackTask(FeedbackTask.stopMotor, amount *
			// positionMultiplier), amount * positionMultiplier);
		}

		stopMotor();
	}

		// TODO - enums pinMode & OUTPUT
		// TODO - abstract "controller" Controller.OUTPUT

		send(controllerName, "pinMode", PWRPin, Arduino.OUTPUT); // TODO THIS IS NOT!!! A FUNCTION OF THE MOTOR - THIS NEEDS TO BE TAKEN CARE OF BY THE BOARD
		send(controllerName, "pinMode", DIRPin, Arduino.OUTPUT);

	public void move(int direction, float power, int amount) {
		setDir(direction);
		setPower(power);
		move(amount);
	}


	public void setDir(int direction) {
		if (locked) return;
		
		this.direction = direction;		
	}

	public void move(int direction, float power) {
		if (locked) return;
		
		setDir(direction);
		setPower(power);
		move();
	}

	public void moveTo(Integer newPos)
	{
		targetPosition = newPos;
		movingToPosition = true;
		if (position - newPos < 0) {
			setDir(FORWARD);
			// move(Math.abs(position - newPos));
			setPower(0.5f);
			move();
		} else if (position - newPos > 0) {
			setDir(BACKWARD);
			// move(Math.abs(position - newPos));
			setPower(0.5f);
			move();
		} else
			return;
	}

	public void moveCW() {
		setDir(FORWARD);
		move();
	}

	public void moveCCW() {
		setDir(BACKWARD);
		move();
	}

	public void setPower(float power) {
		if (locked) return;
		
		if (power > maxPower || power < -maxPower) 
		{
			LOG.error(power + " power out of bounds - max power is "+ maxPower);
			return;
		}
		
		this.power = power;
		move(power);
	}

	int positionMultiplier = 1000;
	boolean useRamping = false;
	public void setUseRamping(boolean ramping) {
		useRamping = ramping;
	}

	// motor primitives end ------------------------------------
	/*
	 * Power and Direction parameters work on the principle that they are values
	 * of a motor, but are not operated upon until a "move" command is issued.
	 * "Move" will direct the motor to move to use the targeted power and
	 * direction.
	 * 
	 * All of the following functions use primitives and are basically composite
	 * functions
	 */

