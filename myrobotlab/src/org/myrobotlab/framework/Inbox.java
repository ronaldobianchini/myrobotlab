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
import java.util.HashMap;
import java.util.LinkedList;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Inbox implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Inbox.class.getCanonicalName());

	String name;
	LinkedList<Message> msgBox = new LinkedList<Message>();
	boolean isRunning = false;
	boolean bufferOverrun = false;
	boolean blocking = false;
	int maxQueue = 100; // will need to adjust unit test if you change this value

	HashMap<Long, Object[]> blockingList = new HashMap<Long, Object[]>();

	public Inbox() {
		this("Inbox");
	}

	public Inbox(String name) {
		this.name = name;
	}

	/**
	 * Blocks and waits on a message put on the queue of the InBox. Service
	 * default behavior will wait on getMsg for a message, when they recieve a
	 * message they invoke it.
	 * 
	 * @return the Message on the queue
	 * @see Message
	 */
	public Message getMsg() throws InterruptedException {
		/*
		 * TODO - remove below - Inbox will call switchboards
		 * serializer/deserializer & communicator send/recieve interface
		 * switchboard has references to serializer and communicator - also all
		 * configuration needed At this level ALL details on where the Message /
		 * Message came from should be hidden and interfaces should be exposed
		 * only-
		 */

		Message msg = null;
		// too chatty log.debug("inbox getMsg just before synchronized");
		synchronized (msgBox) {

			while (msg == null) { // while no messages && no messages that are
									// blocking
				if (msgBox.size() == 0) {
					// log.debug("Inbox WAITING " + name);
					msgBox.wait(); // must own the lock
				} else {
					msg = msgBox.removeLast();
					log.debug(String.format("%s.msgBox -1 %d", name, msgBox.size()));

					// --- sendBlocking support begin --------------------
					// TODO - possible safety check msg.status == Message.RETURN
					// &&
					if (blockingList.containsKey(msg.msgID)) {
						Object[] returnContainer = blockingList.get(msg.msgID);
						if (msg.data == null) // TODO - don't know if this is
												// correct but this works for
												// null data now
						{
							returnContainer[0] = null;
						} else {
							returnContainer[0] = msg.data[0]; // transferring
																// return data !
						}
						synchronized (returnContainer) {
							blockingList.remove(msg.msgID);
							returnContainer.notify(); // addListener sender
						}
						msg = null; // do not invoke this msg - sendBlocking has
									// been notified data returned
					}
					// --- sendBlocking support end --------------------

				}
			}
			msgBox.notifyAll();
		}

		// chase network bugs
		// log.error(String.format("%s.inbox.getMsg() %s.%s() from %s.%s", name,
		// msg.name, msg.method, msg.sender, msg.sendingMethod));
		return msg;
	}

	// FIXME - implement with HashSet or HashMap !!!!
	// ******* TEST WITHOUT DUPE CHECKING *********
	public boolean duplicateMsg(ArrayList<RoutingEntry> history) {

		for (int i = 0; i < history.size(); ++i) {
			if (history.get(i).name.equals(name)) {
				log.error("dupe message {} {}", name, history);

				return true;
			}
		}

		return false;
	}

	public void add(Message msg) {
		// FIXME - implement as HashSet<>
		// chase network bugs
		// log.error(String.format("%s.inbox.add(msg) %s.%s <-- %s.%s", name,
		// msg.name, msg.method, msg.sender, msg.sendingMethod));

		if ((msg.historyList.size() > 0) && (duplicateMsg(msg.historyList))) {
			log.error("*dumping duplicate message msgid " + name + "." + msg.method + " " + msg.msgID);
			log.error("history list {}", msg.historyList);
			return;
		}

		RoutingEntry re = new RoutingEntry(); // TODO - constructor which takes
												// all fields
		re.name = name;
		msg.historyList.add(re);

		synchronized (msgBox) {
			while (blocking && msgBox.size() == maxQueue) // queue "full"
			{
				try {
					// log.warn("inbox enque msg WAITING since inbox " +
					// maxQueue + " is full" + name);
					msgBox.wait(); // Limit the size
				} catch (InterruptedException ex) {
					log.debug("inbox enque msg INTERRUPTED " + name);
				}
			}

			if (msgBox.size() > maxQueue) {
				bufferOverrun = true;
				log.warn(name + " inbox BUFFER OVERRUN dumping msg " + msgBox.size());
			} else {
				msgBox.addFirst(msg);
				log.debug(name + ".msgBox +1 = " + msgBox.size());
				msgBox.notifyAll(); // must own the lock
			}
		}

	}

	public void setBlocking(boolean toBlock) {
		blocking = toBlock;
	}

	public void clear() {
		msgBox.clear();
	}

	public boolean isBufferOverrun() {
		return bufferOverrun;
	}

	public int size() {
		return msgBox.size();
	}

}
