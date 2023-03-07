package com.sapereapi.sapere;

/**
 * Configuration
 */
public class ConfigReader {

	private String nodeName;
	private String localIp;
	private String[] neighs;
	/**
	 * Config path
	 */
	public static final String CONFIG_FILE = "config.txt";

		
	/**
	 * Network configuration
	 */
	public ConfigReader() {
	
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getLocalIp() {
		return localIp;
	}

	public String[] getNeighs() {
		return neighs;
	}
}
