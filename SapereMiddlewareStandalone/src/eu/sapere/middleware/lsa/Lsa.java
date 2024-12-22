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
import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;

public class Lsa implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The id of the LSA */
	private AgentAuthentication agentAuthentication = null;
	private List<Property> propertyList;
	protected List<String> subDescription;
	private Map<SyntheticPropertyName, Object> syntheticProperties;
	public static final int PROPERTIESSIZE = 3*10;


	/**
	 * Initializes an empty LSA with the given agentName
	 * 
	 * @param agentAuthentication
	 */
	public Lsa(AgentAuthentication  agentAuthentication) {
		subDescription = new ArrayList<String>();
		syntheticProperties = new HashMap<SyntheticPropertyName, Object>();
		propertyList = new ArrayList<Property>();
		this.agentAuthentication = agentAuthentication;
	}

	/**
	 * @param agentAuthentication
	 * @param properties
	 * @param subDescription
	 * @param syntheticProperties
	 */
	public Lsa(AgentAuthentication  agentAuthentication, List<Property> properties, List<String> subDescription,
			Map<SyntheticPropertyName, Object> syntheticProperties) {
		this.agentAuthentication = agentAuthentication;
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
		return agentAuthentication.getAgentName();
	}


	/**
	 * 
	 * @return
	 */
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

	public boolean isLocal() {
		boolean isLocal = NodeManager.isLocal(agentAuthentication.getNodeLocation());
		return isLocal;
	}

	public boolean isPropagated() {
		return !isLocal();
	}


	/**
	 * Returns the copy of the LSA
	 * 
	 * @return the copy of the LSA
	 */

	public Lsa copy() {
		Lsa copy = new Lsa(agentAuthentication.copy());
		copy.setAgentAuthentication(agentAuthentication.copy());
		List<Property> copyPropertyList = new ArrayList<Property>();
		for (Property property : this.propertyList) {
			copyPropertyList.add(property.copyForLSA());
		}
		copy.propertyList = copyPropertyList;
		copy.subDescription = new ArrayList<String>(this.subDescription);
		copy.syntheticProperties = new HashMap<SyntheticPropertyName, Object>(this.syntheticProperties);
		return copy;
	}

	public void completeInvolvedLocations(Map<String, NodeLocation> mapNodeLocation) {
		for (Property property : propertyList) {
			property.completeInvolvedLocations(this, mapNodeLocation);
		}
	}

	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<NodeLocation>();
		try {
			result.add(agentAuthentication.getNodeLocation());
			for (Property prop : propertyList) {
				Object value = prop.getValue();
				if (value instanceof IPropertyObject) {
					IPropertyObject propObject = (IPropertyObject) value;
					for (NodeLocation nextNodeLocation : propObject.retrieveInvolvedLocations()) {
						if (!result.contains(nextNodeLocation)) {
							result.add(nextNodeLocation);
						}
					}
				}
			}
		} catch (Throwable e) {
			MiddlewareLogger.getInstance().error(e);
		}
		return result;
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
			if (p.getQuery() != null && p.getQuery().equals(prop.getQuery()) && p.getBond().equals(prop.getBond())) {
				exist = true;
				break;
			}
		}
		return exist;

	}

	public void removeAllProperties() {
		while (propertyList.size() > 0) {
			Property p = propertyList.get(0);
			propertyList.remove(p);
		}
	}

	public List<Property> removePropertiesByQueryAndName(String query, String name) {
		List<Property> result = getPropertiesByQueryAndName(query, name);
		for (Property p : result) {
			propertyList.remove(p);
		}
		return result;
	}

	public List<Property> removePropertiesByQueryAndNames(String query, String[] names) {
		List<Property> result = new ArrayList<Property>();
		for (String name : names) {
			result.addAll(removePropertiesByQueryAndName(query, name));
		}
		return result;
	}

	public List<Property> removePropertiesByName(String name) {
		List<Property> result = getPropertiesByName(name);
		for (Property p : result) {
			propertyList.remove(p);
		}
		return result;
	}

	public void replacePropertyWithName(Property prop) {
		String propName = prop.getName();
		if (hasProperty(propName)) {
			removePropertiesByName(propName);
		}
		addProperty(prop);
	}

	public List<Property> removePropertiesByNames(String[] names) {
		List<Property> result = new ArrayList<Property>();
		for (String name : names) {
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
			if (prop.getQuery() != null && prop.getQuery().equals(query) && prop.getName().equals(name)) {
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
			if (prop.getQuery() != null && prop.getQuery().equals(query) && prop.getValue() == null) {
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
			if (prop.getQuery() != null && prop.getQuery().equals(query)) {
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
		String agentName = getAgentName();
		return "<" + agentName + " , " + subDescription.toString() + " , " + propertyList.toString() + " , "
				+ syntheticProperties.toString() + ">";
	}

	public String toReducedString() {
		String agentName = getAgentName();
		List<String> propNames = new ArrayList<String>();
		for (Property prop : propertyList) {
			propNames.add(prop.getName());
		}
		return "<" + agentName + " , " + subDescription.toString() + "," + propNames + " , "
				+ syntheticProperties.toString() + ">";
	}

	public String toReducedString2() {
		String agentName = getAgentName();
		StringBuffer result = new StringBuffer();
		String nodeName = agentAuthentication.getNodeLocation() == null ? ""
				: agentAuthentication.getNodeLocation().getName();
		result.append("<").append(agentName).append(" (node ").append(nodeName).append(")").append(" , path:")
				.append(getSyntheticProperty(SyntheticPropertyName.PATH)).append(", sendings:")
				.append(getSyntheticProperty(SyntheticPropertyName.SENDINGS)).append(">");
		return result.toString();
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
				String agentName = getAgentName();
				for (Property targetProp : targetLsa.getProperties()) { // if there is a property in the target LSA
																		// corresponding to the query
					if (targetProp.getQuery() != null && targetProp.getQuery().equals(agentName)) {
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
			if (p.getQuery() != null && p.getQuery().equals(query)) { // same query
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
			SyntheticPropertyName syntheticPropertyName = SyntheticPropertyName.getByText(fieldName);
			propValue = getSyntheticProperty(syntheticPropertyName);
		} else {
			Property prop = getOnePropertyByName(fieldName);
			if (prop != null) {
				propValue = prop.getValue();
			}
		}
		return propValue;
	}

	public boolean hasAggregatedValue(String fieldName) {
		for(Property property : getPropertiesByName(fieldName)) {
			if(property.getAggregatedValue() != null) {
				return true;
			}
		}
		return false;
	}

	public Object getOneAggregatedValue(String fieldName) {
		for(Property property : getPropertiesByName(fieldName)) {
			if(property.getAggregatedValue() != null) {
				return property.getAggregatedValue();
			}
		}
		return null;
	}

	public void setAggredatedValue(String fieldName, Object aggregatedValue) {
		for(Property property : getPropertiesByName(fieldName)) {
			property.setAggregatedValue(aggregatedValue);
		}
	}

	public void clearAggredatedValue(String fieldName) {
		for(Property property : getPropertiesByName(fieldName)) {
			property.setAggregatedValue(null);
		}
	}

	/**
	 *
	 * @param fieldName
	 * @return
	 */
	public Date getDateValue(String fieldName) {
		Object propValue = getOneValue(fieldName);
		if (propValue != null && propValue instanceof Date) {
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
		if (propValue != null) {
			return new BigDecimal("" + propValue);
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
		if (propValue != null) {
			return Integer.parseInt("" + propValue);
		}
		return null;
	}

	/**
	 * Added for Aggregation ecolaw
	 * 
	 * @return id
	 */
	public String getId() {
		return this.agentAuthentication.getNodeLocation().getName() + "-" + this.agentAuthentication.getAgentName();
	}

	public boolean hasProperty(String query, String propName) {
		List<Property> listProp = this.getPropertiesByQueryAndName(query, propName);
		return (listProp.size() > 0);
	}

	public boolean hasProperty(String propName) {
		List<Property> listProp = this.getPropertiesByName(propName);
		return (listProp.size() > 0);
	}

	public boolean isFrom(String location) {
		return this.agentAuthentication.getNodeLocation().getMainServiceAddress().equals(location);
	}

	public boolean hasAlreadyBeenReceivedIn(String location) {
		List<String> sendings = getPath();
		return sendings.contains(location);
	}

	public boolean hasAlreadyBeenSentTo(String location) {
		List<String> sendings = getSendings();
		return sendings.contains(location);
	}

	public void addLocationInPath(String location) {
		List<String> path = getPath();
		boolean addPath = false;
		if (path.size() > 0) {
			String lastLoaction = path.get(0);
			addPath = !lastLoaction.equals(location);
		} else {
			addPath = true;
		}
		if (addPath) {
			path.add(location);
			this.addSyntheticProperty(SyntheticPropertyName.PATH, path);
		}
	}

	public void addLocationInSending(String location) {
		aux_addStrInProperty(SyntheticPropertyName.SENDINGS, location);
	}

	public void aux_addStrInProperty(SyntheticPropertyName propName, String location) {
		List<String> listStr = aux_getListStr(propName);
		if (!listStr.contains(location)) {
			listStr.add(location);
			this.addSyntheticProperty(propName, listStr);
		}
	}

	public List<String> aux_getListStr(SyntheticPropertyName propName) {
		if (hasSyntheticProperty(propName)) {
			Object oCurrentPath = getSyntheticProperty(propName);
			if (oCurrentPath instanceof List<?>) {
				List<String> currentPath = (List) oCurrentPath;
				return currentPath;
			}
		}
		return new ArrayList<String>();
	}

	public List<String> getPath() {
		return aux_getListStr(SyntheticPropertyName.PATH);
	}

	public List<String> getSendings() {
		return aux_getListStr(SyntheticPropertyName.SENDINGS);
	}

	public int getSourceDistance() {
		List<String> path = getPath();
		return path.size();
	}

	public long getTimeElapsedSinceSendingMS() {
		long result = -1;
		if (hasSyntheticProperty(SyntheticPropertyName.LAST_SENDING)) {
			Object oSendingDate = syntheticProperties.get(SyntheticPropertyName.LAST_SENDING);
			if (oSendingDate instanceof Date) {
				Date sendingDate = (Date) oSendingDate;
				result = new Date().getTime() - sendingDate.getTime();
			}
		}
		return result;
	}

	/**
	 * @return true if the LSA has the Source Property, false otherwise
	 */
	public boolean hasSource() {
		return hasSyntheticProperty(SyntheticPropertyName.SOURCE);
	}

	public boolean hasAggregation() {
		return hasSyntheticProperty(SyntheticPropertyName.AGGREGATION);
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

	public boolean hasPropertiesQueryAndName(String query, String name) {
		for (Property prop : propertyList) {
			if (prop.getQuery() != null && prop.getQuery().equals(query) && prop.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasPropertiesName(String name) {
		for (Property prop : propertyList) {
			if (prop.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasTargetedProperty(Lsa bondedLsa) {
		// Loop on targeted properties
		String agentName = getAgentName();
		for (String nextTargetedProperty : subDescription) {
			if (bondedLsa.hasPropertiesQueryAndName(agentName, nextTargetedProperty)) {
				return true;
			}
			if (bondedLsa.hasPropertiesName(nextTargetedProperty)) {
				return true;
			}
		}
		return false;
	}

	public void updateLsaPropertyTags(String[] lsaInputTags, String[] lsaOutputTags) {
		subDescription.clear();
		this.addSubDescription(lsaInputTags);
		this.addSyntheticProperty(SyntheticPropertyName.OUTPUT, String.join(",", lsaOutputTags));
	}
}
