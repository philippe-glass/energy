package eu.sapere.middleware.lsa;

/**
 * Synthetic Properties
 * 
 */
public enum SyntheticPropertyName {

//	CREATION_TIME("creationTime"), 
//	CREATOR_ID("creatorId"), 
	LAST_SENDING("lastSending"),
	DECAY("decay"),
	QUERY("query"), //Query LSA
	REWARD("reward"), //Reward LSA
	OUTPUT("output"), 
	SOURCE("source"),
	BOND("bond"),
	DESTINATION("destination"),
	PATH("path"),
	SENDINGS("sendings"),
	TYPE("type"),
	STATE("state"),
	DIFFUSE("diffuse"),
	PREVIOUS("previous"),
	GRADIENT_HOP("gradient_hop"),
	AGGREGATION("aggregation"),
	LAST_AGGREGATION("lastAggregation"), 		// added for aggregation
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
