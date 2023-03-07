package eu.sapere.middleware.lsa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.values.AbstractAggregationOperator;
import eu.sapere.middleware.lsa.values.CustomizedAggregationOperator;
import eu.sapere.middleware.lsa.values.MapStandardOperators;
import eu.sapere.middleware.lsa.values.StandardAggregationOperator;

public class Lsa implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The id of the LSA */
	private String agentName;
	private AgentAuthentication agentAuthentication = null;
	private List<Property> propertyList;
	protected List<String> subDescription;
	private Map<SyntheticPropertyName, Object> syntheticProperties;
	public static final int PROPERTIESSIZE = 10;

	/**
	 * Initializes an empty LSA with the given agentName
	 * 
	 * @param agentName
	 */
	public Lsa(String agentName) {
		this.agentName = agentName;
		subDescription = new ArrayList<String>();
		syntheticProperties = new HashMap<SyntheticPropertyName, Object>();
		propertyList = new ArrayList<Property>();
	}

	/**
	 * @param agentName
	 * @param properties
	 * @param subDescription
	 * @param syntheticProperties
	 */
	public Lsa(String agentName, List<Property> properties, List<String> subDescription,
			Map<SyntheticPropertyName, Object> syntheticProperties) {
		this.agentName = agentName;
		this.subDescription = subDescription;
		this.syntheticProperties = syntheticProperties;
		this.propertyList = properties;

	}

	/**
	 * Retrieves the id of the LSA
	 * 
	 * @return the id of the LSA
	 */
	public String getAgentName() {
		return agentName;
	}

	/**
	 * Sets the id of the LSA
	 * 
	 * @param agentName The agentName of the LSA
	 */
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}


	public AgentAuthentication getAgentAuthentication() {
		return agentAuthentication;
	}

	public void setAgentAuthentication(AgentAuthentication agentAuthentication) {
		this.agentAuthentication = agentAuthentication;
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return propertyList == null && subDescription == null && syntheticProperties == null;
	}

	/**
	 * Returns the copy of the LSA
	 * 
	 * @return the copy of the LSA
	 */

	public Lsa getCopy() {
		Lsa copy = new Lsa("");
		copy.setAgentName(agentName);
		copy.setAgentAuthentication(agentAuthentication);
		copy.propertyList = new ArrayList<Property>(this.propertyList);
		copy.subDescription = new ArrayList<String>(this.subDescription);
		copy.syntheticProperties = new HashMap<SyntheticPropertyName, Object>(this.syntheticProperties);
		return copy;
	}

	/**
	 * Adds the given Property to the LSA - FIFO
	 * 
	 * @param prop
	 * @return
	 */
	public Lsa addProperty(Property prop) {
		if (propertyList.size() < PROPERTIESSIZE)
			propertyList.add(prop);
		else {
			propertyList.remove(0);
			propertyList.add(prop);
		}
		return this;
	}

	public Boolean contains(Property prop) {
		Boolean exist = false;
		for (Property p : propertyList) {
			if (p.getQuery() !=null && p.getQuery().equals(prop.getQuery()) && p.getBond().equals(prop.getBond())) {
				exist = true;
				break;
			}
		}
		return exist;

	}

	public void removeAllProperties() {
		while(propertyList.size()>0) {
			Property p = propertyList.get(0);
			propertyList.remove(p);
		}
	}

	public List<Property> removePropertiesByQueryAndName(String query, String name) {
		List<Property> result = getPropertiesByQueryAndName(query, name);
		for(Property p : result) {
			propertyList.remove(p);
		}
		return result;
	}

	public List<Property> removePropertiesByQueryAndNames(String query, String[] names) {
		List<Property> result = new ArrayList<Property>();
		for(String name : names) {
			result.addAll(removePropertiesByQueryAndName(query, name));
		}
		return result;
	}

	public List<Property> removePropertiesByName(String name) {
		List<Property> result = getPropertiesByName(name);
		for(Property p : result) {
			propertyList.remove(p);
		}
		return result;
	}

	public void replacePropertyWithName(Property prop) {
		String propName = prop.getName();
		if(hasProperty(propName)) {
			removePropertiesByName(propName);
		}
		addProperty(prop);
	}

	public List<Property> removePropertiesByNames(String[] names) {
		List<Property> result = new ArrayList<Property>();
		for(String name : names) {
			result.addAll(removePropertiesByName(name));
		}
		return result;
	}

	/**
	 * get properties by query and name
	 * 
	 * @param query
	 * @param name
	 * @return
	 * 
	 */
	public List<Property> getPropertiesByQueryAndName(String query, String name) {
		List<Property> props = new ArrayList<Property>();
		for (Property prop : propertyList) {
			if (prop.getQuery()!= null && prop.getQuery().equals(query) && prop.getName().equals(name)) {
				props.add(prop);
			}
		}
		return props;
	}

	/**
	 * get properties by name
	 * 
	 * @param query
	 * @param name
	 * @return
	 * 
	 */
	public List<Property> getPropertiesByName(String name) {
		List<Property> props = new ArrayList<Property>();
		for (Property prop : propertyList) {
			if (prop.getName().equals(name)) {
				props.add(prop);
			}
		}
		return props;
	}

	public Property getOnePropertyByName(String propName) {
		List<Property> listProp = getPropertiesByName(propName);
		if (listProp.size() > 0) {
			Collections.shuffle(listProp);
			Property valueProp = listProp.get(0);
			return valueProp;
		}
		return null;
	}

	public Property getOnePropertyByQueryAndName(String query, String propName) {
		List<Property> listProp = this.getPropertiesByQueryAndName(query, propName);
		if (listProp.size() > 0) {
			Collections.shuffle(listProp);
			Property valueProp = listProp.get(0);
			return valueProp;
		}
		return null;
	}

	public Object getOnePropertyValueByName(String propName) {
		List<Property> listProp = this.getPropertiesByName(propName);
		if (listProp.size() > 0) {
			Collections.shuffle(listProp);
			Property valueProp = listProp.get(0);
			return valueProp.getValue();
		}
		return null;
	}

	public Object getOnePropertyValueByQueryAndName(String query, String propName) {
		List<Property> listProp = this.getPropertiesByQueryAndName(query, propName);
		if (listProp.size() > 0) {
			Collections.shuffle(listProp);
			Property valueProp = listProp.get(0);
			return valueProp.getValue();
		}
		return null;
	}

	public boolean checkNullPropertiesByQuery(String query) {
		boolean exist = false;
		for (Property prop : propertyList) {
			if (prop.getQuery() !=null && prop.getQuery().equals(query) && prop.getValue() == null) {
				exist = true;
				break;
			}
		}
		return exist;
	}

	/**
	 * get properties by query
	 * 
	 * @param query
	 * @return
	 * 
	 */
	public List<Property> getPropertiesByQuery(String query) {
		List<Property> props = new ArrayList<Property>();
		for (Property prop : propertyList) {
			if (prop.getQuery()!=null && prop.getQuery().equals(query)) {
				props.add(prop);
			}
		}
		return props;
	}

	/**
	 * Adds the given subDescription to the LSA
	 * 
	 * @param name
	 * @return
	 */
	public Lsa addSubDescription(String[] name) {
		for (String s : name)
			subDescription.add(s);
		return this;
	}

	/**
	 * Adds the given syntheticProperty to the LSA
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public Lsa addSyntheticProperty(SyntheticPropertyName name, Object value) {
		syntheticProperties.put(name, value);
		return this;
	}

	/**
	 * Removes a subDescription from the LSA
	 * 
	 * @param name
	 * @return
	 */
	public Lsa removeSubdescription(String name) {
		subDescription.remove(name);
		return this;
	}

	/**
	 * Removes a Synthetic Property from the LSA
	 * 
	 * @param syntheticPropertyName
	 * @return
	 */
	public Lsa removeSyntheticProperty(SyntheticPropertyName syntheticPropertyName) {
		syntheticProperties.remove(syntheticPropertyName);
		return this;
	}

	/**
	 * Removes a Synthetic Property from the LSA
	 * 
	 * @param syntheticPropertyName
	 * @return
	 */
	public Lsa removeContent() {
		propertyList.clear();
		subDescription.clear();
		syntheticProperties.clear();
		return this;
	}

	/**
	 * Returns true if the LSA has the specified Synthetic Property
	 * 
	 * @param name The name of the Synthetic Property
	 * @return true if the LSA has at least a Synthetic Property, false otherwise
	 */
	public boolean hasSubdescription(String name) {
		return subDescription.contains(name);
	}

	public boolean isSubdescriptionEmpty() {
		return subDescription.isEmpty();
	}

	/**
	 * Returns true if the LSA has the specified Synthetic Property
	 * 
	 * @param name The name of the Synthetic Property
	 * @return true if the LSA has at least a Synthetic Property, false otherwise
	 */
	public boolean hasSyntheticProperty(SyntheticPropertyName name) {
		return syntheticProperties.containsKey(name);
	}

	public Object getSyntheticProperty(SyntheticPropertyName name) {
		if (this.hasSyntheticProperty(name))
			return syntheticProperties.get(name);
		else
			return "";
	}

	/**
	 * Returns a string representation of this LSA
	 * 
	 * @return the String representation of the LSA
	 */
	public String toVisualString() {
		return "<" + agentName + " , " + subDescription.toString() + " , " + propertyList.toString() + " , "
				+ syntheticProperties.toString() + ">";
	}

	public List<String> getSubDescription() {
		return subDescription;
	}

	public List<Property> getProperties() {
		return propertyList;
	}

	public boolean shouldBound(Lsa targetLsa) {
		Boolean bond = false;
		if (!targetLsa.getAgentName().equals(this.getAgentName())) { // not the same agent
			if (this.getProperties().isEmpty()) {
				bond = true;
			} else if (getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Query)) {
				for (Property targetProp : targetLsa.getProperties()) { // if there is a property in the target LSA
																		// corresponding to the query
					if (targetProp.getQuery()!=null && targetProp.getQuery().equals(agentName)) {
						bond = true;
						break;
					}
				}
			} else if (getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Service)) {
				bond = true;
//				for (Property prop : getProperties()) {
//					if (prop.getQuery().equals(targetLsa.getSyntheticProperty(SyntheticPropertyName.QUERY))
//					if (!targetLsa.getPropertiesByQuery(prop.getQuery()).isEmpty()
//							&& Arrays.asList(prop.getBond().split(",")).contains(targetLsa.getAgentName())) { // check query & bond
//						bond = false; // find a prop corresponding to the query from this agent
//						break;
//					}
//				}
				// hasBondedBefore(targetLsa.getAgentName(),targetLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString());
			}
		}
		return bond;
	}

	public Boolean hasBondedBefore(String bondedAgentName, String query) {
		Boolean hasBonded = false;
		for (Property p : getProperties()) {
			if (p.getQuery()!=null && p.getQuery().equals(query)) { // same query
				for (String s : p.getBond().split(",")) {

					if (s.equals(bondedAgentName)) {
						hasBonded = true;
						break;
					}
				}
			}
		}
		return hasBonded;
	}


	/**
	 *
	 * @param fieldName
	 * @return
	 */
	public Object getOneValue(String fieldName) {
		Object propValue = null;
		if (SyntheticPropertyName.isSyntheticProperty(fieldName)) {
			SyntheticPropertyName  syntheticPropertyName = SyntheticPropertyName.getByText(fieldName);
			propValue = getSyntheticProperty(syntheticPropertyName);
		} else {
			Property prop = getOnePropertyByName(fieldName);
			if (prop != null) {
				propValue = prop.getValue();
			}
		}
		return propValue;
	}

	/**
	 *
	 * @param fieldName
	 * @return
	 */
	public Date getDateValue(String fieldName) {
		Object propValue = getOneValue(fieldName);
		if(propValue != null && propValue instanceof Date) {
			return (Date) propValue;
		}
		return null;
	}

	/**
	 *
	 * @param fieldName
	 * @return
	 */
	public BigDecimal getBigDecimalValue(String fieldName) {
		Object propValue = getOneValue(fieldName);
		if(propValue != null) {
			return new BigDecimal(""+ propValue);
		}
		return null;
	}

	/**
	 *
	 * @param fieldName
	 * @return
	 */
	public Integer getIntegerValue(String fieldName) {
		Object propValue = getOneValue(fieldName);
		if(propValue != null) {
			return Integer.parseInt(""+ propValue);
		}
		return null;
	}

	/**
	 * Added for Aggregation ecolaw
	 * @return  id
	 */
	public String getId() {
		return this.agentAuthentication.getAgentNode()+ "-" + this.agentAuthentication.getAgentName();
	}

	public boolean hasProperty(String query, String propName) {
		List<Property> listProp = this.getPropertiesByQueryAndName(query, propName);
		return (listProp.size() > 0);
	}

	public boolean hasProperty(String propName) {
		List<Property> listProp = this.getPropertiesByName(propName);
		return (listProp.size() > 0);
	}

	/**
	 *
	 * @return true if the LSA has the Aggregation Operator Property, false
	 *         otherwise
	 */
	public boolean hasAggregationOp() {
		return hasSyntheticProperty(SyntheticPropertyName.AGGREGATION_STANDARD_OP)
			|| hasSyntheticProperty(SyntheticPropertyName.AGGREGATION_CUSTOM_OP);
	}

	/**
	 *
	 * @return the value of the Aggregation Operator Property
	 */
	public String getStandardAggregationOp() {
		if(hasSyntheticProperty(SyntheticPropertyName.AGGREGATION_STANDARD_OP)) {
			return "" + getSyntheticProperty(SyntheticPropertyName.AGGREGATION_STANDARD_OP);
		}
		return null;
	}

	public String getCustomizedAggregationOp() {
		if(hasSyntheticProperty(SyntheticPropertyName.AGGREGATION_CUSTOM_OP)) {
			return "" + getSyntheticProperty(SyntheticPropertyName.AGGREGATION_CUSTOM_OP);
		}
		return null;
	}

	/**
	 *
	 * @return
	 */
	public AbstractAggregationOperator getAggregationOperator() {
		if(hasAggregationBy()) {
			String customizeOpName = getCustomizedAggregationOp();
			if(customizeOpName != null) {
				return new CustomizedAggregationOperator();
			} else {
				String aggregationOpName = getStandardAggregationOp();
				StandardAggregationOperator aggregationOp = MapStandardOperators.getOperator(aggregationOpName);
				//AggregationOperator aggregationOp = AggregationOperator.getByLabel(aggregationOp);
				return aggregationOp;
			}
		}
		return null;
	}

	/**
	 * @return true if the LSA is subject to self Aggregation
	 */
	public boolean explicitAggregationApplies() {
		return (hasAggregationOp() && hasAggregationBy() && hasSource());
	}

	/**
	 * @return true if the LSA has the Property AGGREGATION_BY Value, false otherwise
	 */
	public boolean hasAggregationBy() {
		return hasSyntheticProperty(SyntheticPropertyName.AGGREGATION_BY);
	}

	/**
	 * @return the value of the Field Value Property
	 */
	public String getAggregationBy() {
		return "" + getSyntheticProperty(SyntheticPropertyName.AGGREGATION_BY);
	}

	public boolean getAggregationAllNodes() {
		String aggregationAllNodes = "" + getSyntheticProperty(SyntheticPropertyName.AGGREGATION_ALLNODES);
		return "1".equals(aggregationAllNodes);
	}

	/**
	 * @return true if the LSA has the Source Property, false otherwise
	 */
	public boolean hasSource() {
		return hasSyntheticProperty(SyntheticPropertyName.SOURCE);
	}

	/**
	 * @return true if the LSA has the Aggregation source Property
	 */
	public String getAggregationSource() {
		if (hasSyntheticProperty(SyntheticPropertyName.SOURCE)) {
			return getSyntheticProperty(SyntheticPropertyName.SOURCE).toString();
		}
		return null;
	}

	/**
	 * @return true is the LSA is subject to other Aggregation
	 */
	public boolean requestedAggregationApplies() {
		return (hasAggregationOp() && hasAggregationBy() && !hasSource());
	}

	/**
	 * @return true if the LSA is subject to the Aggregation eco-law, false
	 *         otherwise
	 */
	public boolean aggregationApplies() {
		return ((hasAggregationOp() && hasAggregationBy() && hasSource()) || (hasAggregationOp() && hasAggregationBy()));
	}
}
