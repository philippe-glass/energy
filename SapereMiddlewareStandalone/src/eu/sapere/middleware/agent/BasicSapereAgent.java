package eu.sapere.middleware.agent;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.lsaspace.Operation;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;

/**
 * The implementation provided for Data LSA. Once a Data LSA is injected in the
 * Space, it is no longer on control of the entity that injected it, i.e., the
 * events that happens to the LSA are not reported
 * 
 */
public class BasicSapereAgent extends Agent {

	private static final long serialVersionUID = -2557370488147998985L;
	/** The reference to the local node's Operation Manager */
	private OperationManager opMng = null;

	/**
	 * Instantiates a Basic Sapere Agent
	 * 
	 * @param agentName
	 *            The name of the Agent
	 * @param activateQoS
	 * 			Activation of Quality of service
	 */
	public BasicSapereAgent(String agentName, boolean activateQoS) {
		super(agentName, new AgentAuthentication(agentName, "BasicSapereAgent", "", NodeManager.getNodeLocation()), new String[] {""},new String[] {""}, LsaType.System, activateQoS);
		this.opMng = NodeManager.instance().getOperationManager();
	}

	/**
	 * Injects the given LSA in the local Lsa space
	 * 
	 * @param lsa
	 *            The LSA to be injected
	 * @return The id of the injected LSA
	 */
	public void injectOperation(Lsa lsa) {
		Operation op = new Operation().injectOperation(lsa, this);
		opMng.queueOperation(op);
	}

	/**
	 * Updates an LSA
	 * 
	 * @param lsa
	 *            The new LSA
	 * @param lsaId
	 *            The id of the LSA to be updated
	 */
	public void updateOperation(Lsa lsa, String lsaId) {
		Operation op = new Operation().updateOperation(lsa, this);
		opMng.queueOperation(op);
	}
	
	public void rewardOperation(Lsa lsa, String query, int reward, double maxQst1) {
		Operation op = new Operation().rewardOperation(lsa, this, query, reward, maxQst1);
		opMng.queueOperation(op);
	}

	/**
	 * Removes the specified LSA from the local LSA space
	 * 
	 * @param lsa
	 */
	public void removeLsa(Lsa lsa) {
		Operation op = new Operation().removeOperation(lsa, this);
		opMng.queueOperation(op);
	}

	@Override
	public void onNotification(AbstractSapereEvent event) {

	}
}
