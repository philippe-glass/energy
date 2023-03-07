package eu.sapere.middleware.node.lsaspace;

import java.io.Serializable;

/**
 * Enumeration for the possible type of Operation over a space
 * 
 */

public enum OperationType implements Serializable {
	/**
	 * Inject Operation
	 */
	INJECT,
	
	/**
	 * Remove Operation
	 */
	REMOVE,

	/**
	 * Update Operation
	 */
	UPDATE,
	
	/**
	 * Reward Operation
	 */
	REWARD

}
