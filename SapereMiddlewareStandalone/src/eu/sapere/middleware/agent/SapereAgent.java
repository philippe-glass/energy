package eu.sapere.middleware.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.lsa.values.StandardAggregationOperator;
import eu.sapere.middleware.node.NodeManager;

/**
 * The abstract class that realize an agent that manages an LSA. The Agent is
 * represented by an implicit LSA, each operation on the LSA is automatically
 * and transparently propagated to the LSA space.
 * 
 */
public abstract class SapereAgent extends LsaAgent {

	private static final long serialVersionUID = 5430383196672604798L;
	private Map<String, List<Lsa>> bondedLsaList;
	private String[] output;
	private String[] input;
	private String url;
	private String appid;


	/**
	 * Instantiates the Sapere Agent
	 * 
	 * @param name            The name of the Agent
	 * @param authentication 
	 * @param subdescriptions
	 * @param propertiesName
	 * @param type
	 */
	public SapereAgent(String name, AgentAuthentication authentication, String[] subdescriptions, String[] propertiesName, LsaType type) {
		super(name, authentication, subdescriptions, propertiesName, type);
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

	/**
	 * Use this method to set the initial content of the LSA managed by this
	 * SapereAgent.
	 */
	public abstract void setInitialLSA();

	/**
	 * @param nHop The number of hops
	 */
	public void addGradient(int nHop) {
		lsa.addSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP, nHop + "");
		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocation());
	}

	/**
	 * @param lsa
	 * @param ip
	 */
	public void sendTo(Lsa lsa, String ip) {
		lsa.addSyntheticProperty(SyntheticPropertyName.DESTINATION, ip);
		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocation());
	}

	/**
	 * Adds a decay property
	 * 
	 * @param decayValue The initial decay value
	 */
	public void addDecay(int decayValue) {
		lsa.addSyntheticProperty(SyntheticPropertyName.DECAY, decayValue + "");
	}

	public void addStandardAggregation(StandardAggregationOperator aggregationOperator, String fieldName, boolean aggregateAllNodes) {
		lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_STANDARD_OP, aggregationOperator.getName());
		lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_BY, fieldName);
		if(aggregateAllNodes) {
			lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_ALLNODES, "1");
		}
	}

	public void addCustomizedAggregation(String customizedAggregationOp, String fieldName, boolean aggregateAllNodes) {
		//lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_OP, new CustomizedAggregationOperator());
		lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_CUSTOM_OP, customizedAggregationOp);
		lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_BY, fieldName);
		if(aggregateAllNodes) {
			lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION_ALLNODES, "1");
		}
	}

	public String fusionStates(String[] state1, String[] state2) {
		String result = "";
		for (int i = 0; i < state1.length; i++) {
			if (state2.length > i && state1[i].equals(state2[i])) {
				result += state1[i] + ",";
			} else if (state2.length > i && !state1[i].equals(state2[i])) {
				for (int j = i; j < state1.length; j++) {
					result += state1[j] + ",";
				}
				for (int j = i; j < state2.length; j++) {
					result += state2[j] + ",";
				}
				break;
			} else if (state2.length <= i) {
				for (int j = i; j < state1.length; j++) {
					result += state1[j] + ",";
				}
				break;
			}
			if (i == state1.length - 1 && state2.length > i) {
				for (int j = i + 1; j < state2.length; j++) {
					result += state2[j] + ",";
				}
				break;
			}
		}
		return result.substring(0, result.length() - 1);
	}

	public String getPreviousState(String newState, String[] output) {
		String[] state = newState.split(",");
		String[] newArray = Arrays.copyOfRange(state, 0, state.length - output.length);
		return String.join(",", newArray);
	}

	public Map<String, List<Lsa>> getBondedLsaList() {
		return bondedLsaList;
	}

	public void setBondedLsaList(Map<String, List<Lsa>> bondedLsaList) {
		this.bondedLsaList = bondedLsaList;
	}

	public String[] getOutput() {
		return output;
	}

	public void setOutput(String[] output) {
		this.output = output;
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}
	
	
	
}
