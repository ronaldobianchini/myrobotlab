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

package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author grperry
 *
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static String BLOCKING = "B";
	public final static String RETURN = "R";
	public final static Logger LOG = Logger.getLogger(Message.class);

	/**
	 * unique identifier for this message - TODO remove
	 */
	public String msgID; 
	/**
	 * datetimestamp when message is created GMT
	 */
	public String timeStamp;
	/**
	 * globally unique name of destination Service.  This will be the Service
	 * endpoint of this Message.
	 */
	public String name;
	/**
	 * name of the sending Service which sent this Message 
	 */
	public String sender; 
	/**
	 * originating source method which generated this Message 
	 */
	public String sendingMethod; 
	/**
	 * history of the message, its routing stops and Services it passed
	 * through.  This is important to prevent endless looping of messages.
	 */
	public ArrayList<RoutingEntry> historyList;
	/**
	 * status is currently not used (for future use)
	 */
	public String status;
	public String msgType; // Broadcast|Blocking|Blocking Return - deprecated
	/**
	 * the method which will be invoked on the destination @see Service 
	 */
	public String method;

	
	/**
	 *  the data which will be sent to the destination method
	 *  data payload - if invoking a service request this
	 *  would be the parameter (list) - this would the
	 *  return type data if the message is outbound
	 */
	public Object[] data; 
	
	public Message() {
		timeStamp = new String(); // FIXME - remove silly assignments !!!
		name = new String();
		sender = new String();
		sendingMethod = new String();
		historyList = new ArrayList<RoutingEntry>();
		status = new String(); // TODO - optimization - leave as null
		msgType = new String();
		method = new String();
	}

	public Message(final Message other) {
		set(other);
	}

	final public void set(final Message other) {
		msgID = other.msgID;
		timeStamp = other.timeStamp;
		name = other.getName();
		sender = other.sender;
		sendingMethod = other.sendingMethod;
		historyList = other.historyList;
		status = other.status;
		msgType = other.msgType;
		method = other.method;
		data = other.data;
	}

	final public String getParameterSignature() {
		if (data == null) {
			return "null";
		}

		String ret = "";
		for (int i = 0; i < data.length; ++i) {
			ret += data[i].getClass().getCanonicalName();
			if (data.length != i + 1) {
				ret += ",";
			}

		}
		return ret;

	}


	final public void setData(Object ... params) {
		this.data = params;
	}
	
	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<Message");
		ret.append("{");
		ret.append("\"MSGID\":" + "\"" + msgID + "\"");
		ret.append("\"timeStamp\":" + "\"" + timeStamp + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"sender\":" + "\"" + sender + "\"");
		ret.append("\"sendingMethod\":" + "\"" + sendingMethod + "\"");
		ret.append("\"historyList\":" + "\"" + historyList.toString() + "\"");
		ret.append("\"status\":" + "\"" + status + "\"");
		ret.append("\"msgType\":" + "\"" + msgType + "\"");
		ret.append("\"method\":" + "\"" + method + "\"");
		ret.append("\"dataClass\":" + "\"" + getParameterSignature() + "\"");
		ret.append("\"data\":" + "\"" + data + "\"");

		// ret.append("</Message>");
		ret.append("}");
		return ret.toString();
	}

	public Object[] getData() {
		return data;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}