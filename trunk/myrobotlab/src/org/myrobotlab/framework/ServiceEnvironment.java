package org.myrobotlab.framework;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import org.myrobotlab.service.Runtime;

/**
 * ServiceEnvironment represents a instance of MRL
 * with a null URI it will be the local instance
 * any !null instance represents a foreign instance 
 *
 */
public class ServiceEnvironment implements Serializable {

	private static final long serialVersionUID = 1L;

	// access URL
	public URL accessURL;
	public HashMap<String, ServiceWrapper> serviceDirectory; // TODO make public & concurrent

	/**
	 * platform of the environment - this can be used to implement proxy rules
	 * depending on what Services will work on which platform 
	 */
	public Platform platform;

	public ServiceEnvironment(URL url)
	{
		this.accessURL = url;
		serviceDirectory = new HashMap<String, ServiceWrapper>();
		platform = Runtime.getPlatform();
	}
}
