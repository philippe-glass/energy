package eu.sapere.middleware.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.SyntheticPropertyName;

/**
 * The abstract class that realize an agent that manages an LSA. The Agent is
 * represented by an implicit LSA, each operation on the LSA is automatically
 * and transparently propagated to the LSA space.
 * 
 */
public abstract class SapereAgent extends LsaAgent {

	private static final long serialVersionUID = 5430383196672604798L;
	private Map<String, List<Lsa>> bondedLsaList;
	//private String[] output;
	//private String[] input;
	//private String url;
	//private String appid;


	/**
	 * Instantiates the Sapere Agent
	 * 
	 * @param name            The name of the Agent
	 * @param authentication 
	 * @param subdescriptions
	 * @param propertiesName
	 * @param activateQoS
	 * @param type
	 */

	/**
	 * Use this method to set the initial content of the LSA managed by this
	 * SapereAgent.
	 */
	public abstract void setInitialLSA();

	public SapereAgent(String name, AgentAuthentication authentication, String[] subdescriptions, String[] propertiesName, LsaType type, boolean activateQoS) {
		super(name, authentication, subdescriptions, propertiesName, type, activateQoS);
		bondedLsaList = new HashMap<String, List<Lsa>>();
	}

	public void addBondedLSA(Lsa bondedLsa) {
		String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
		if (bondedLsaList.get(query) == null) {
			List<Lsa> temp = new ArrayList<Lsa>();
			temp.add(bondedLsa);
			bondedLsaList.put(query, temp);
		} else {
			bondedLsaList.get(query).add(bondedLsa);
		}
	}

	public List<Lsa> getBondedLsaByQuery(String query) {
		return bondedLsaList.get(query);
	}

	public void removeBondedLsasOfQuery(String query) {
		bondedLsaList.remove(query);
	}



	public Map<String, List<Lsa>> getBondedLsaList() {
		return bondedLsaList;
	}

	public void setBondedLsaList(Map<String, List<Lsa>> bondedLsaList) {
		this.bondedLsaList = bondedLsaList;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
