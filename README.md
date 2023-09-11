To start the project:
$ docker-compose up


##SAPERE middleware

*	Agent : {String agentName, Map<String, int[]> R, Map<String, Double[]> Q}
*	Space : {Map<String, Lsa> spaceMap, INotifier notifier}
*	EcoLawsEngine : {List<IEcoLaw> myEcoLaws: Decay, Bonding, Propagation}
*	LSA : {String agentName, List<Property> propertyList, Set<String> subDescription, Map<SyntheticPropertyName, Object> syntheticProperties}
*	Property : {String query, String bond, Object value, String type, String state, String ip, Boolean chosen}
*	OperationType : INJECT, REMOVE, UPDATE, REWARD
*	SyntheticPropertyName : 
	*	DECAY("decay") : decremental number
	*	QUERY("query") : query id
	*	REWARD("reward") : reward
	*	OUTPUT("output") : service output type
	*	SOURCE("source") : lsa source node
	*	BOND("bond") : last bonded agent
	*	DESTINATION("destination") : destination node
	*	TYPE("type") : service, query, reward
	*	STATE("state") : current composition state
	*	DIFFUSE("diffuse") : 0 to not diffuse, 1 to diffuse, 2 if error occurs
	*	PREVIOUS("previous") : previous node
	*	GRADIENT_HOP("gradient_hop") : hops number to reach
	*	LOCATION("location") : host node name
						

