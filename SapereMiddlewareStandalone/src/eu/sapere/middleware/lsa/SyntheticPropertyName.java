package eu.sapere.middleware.lsa;


/**
 * Synthetic Properties
 * 
 */
public enum SyntheticPropertyName {

//	CREATION_TIME("creationTime"), 
//	CREATOR_ID("creatorId"), 
	LAST_MODIFIED("lastModified"), 		// added for aggregation
	DECAY("decay"),
	QUERY("query"), //Query LSA
	REWARD("reward"), //Reward LSA
	OUTPUT("output"), 
	SOURCE("source"),
	BOND("bond"),
	DESTINATION("destination"),
	TYPE("type"),
	STATE("state"),
	DIFFUSE("diffuse"),
	PREVIOUS("previous"),
	GRADIENT_HOP("gradient_hop"),
	AGGREGATION_STANDARD_OP("aggregation_std_op"),	// added for aggregation
	AGGREGATION_CUSTOM_OP("aggregation_cust_op"),	// added for aggregation
	AGGREGATION_ALLNODES("aggregation_all_nodes"),	// added for aggregation
	AGGREGATION_BY("aggregation_by"),	// added for aggregation
	//QOS("qos"),
	LOCATION("location");

	private SyntheticPropertyName(final String text) {
		this.text = text;
	}

	private final String text;

	@Override
	public String toString() {
		return text;
	}

	public String getText() {
		return text;
	}

	public static SyntheticPropertyName getByText(String text) {
		String text2 = (text == null) ? "" : text;
		for (SyntheticPropertyName pLevel : SyntheticPropertyName.values()) {
			if (pLevel.getText().equals(text2)) {
				return pLevel;
			}
		}
		return null;
	}

	public static boolean isSyntheticProperty(String fieldName) {
		SyntheticPropertyName syntheticPropertyName = getByText(fieldName);
		return (syntheticPropertyName != null);
	}
}
