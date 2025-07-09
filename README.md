To start the project:
$ docker-compose up


Java projects:
There are 3 different Java projects (one for each software component). They can be easily imported into the Eclipse Integrated Development Environment.
%
 . sapereMiddlewareStandalone: This project contains the source code for the middleware library derived from SAPERE. It generates the SapereV1.jar library, which defines the tuple space and coordination mechanisms. This library is not directly executable.
 . sapereapi: This project contains the source code for the coordination platform service and the Digital Twins. It imports the SapereV1.jar library and generates the executable library coordination_platform.jar which corresponds to the light server (its main class is $LightServer$ class). This service can also run using a Spring server (in this case, we launch the $SapereAPIApplication$ class). For this reason, the sapereapi project is a 'Spring-boot' project and uses the Spring and Maven libraries. It should be noted that the Spring server is no longer regularly used, as the Spring server consumes more memory than the 'light' server.
 . saperetest: This project contains the test simulators: there is one main class per simulator. This project imports the coordiantion_plarform.jar library because the simulators use certain classes defined in the coordination platform, such as the structures of the various objects exchanged with the different REST services (input and output objects).
%


Python projects:
%
These are two independent python projects, both of which can be imported into the PyCharm integrated development environment:
 . download_gcp_file}: This project includes scripts for importing smart-meter measurement data from the Exoscale cloud made available by the school HES-SO Geneva. This measurement data is extracted from files and stored in the database. The download_gcp_file project is independent of other projects and only imports a specific library to query the Exoscale cloud server.
 . lstm: This is the source code for the machine learning service, which currently only implements the LSTM model. This project uses the Keras and TensorFlow external libraries for machine learning and the Flask library, which defines a REST server model. The executable file (app.py) corresponds to the source of the Flask REST server.
%
\subsection{Angular JS projects}
%
% \textcolor{red}{to be completed}
%
This is the \textbf{sapereangular} project, which can be imported into the Visual Studio Code integrated development environment. This project contains the source code of the front web server. There is one instance of the front-end web server running per platform coordination. The project is broken down into one module per web page, which is accessed as a tab, and each web page contains 3 types of source file: the HTML template, the controller that interacts with the REST server (.ts file) and the css style sheet.



##SAPERE middleware

*	Agent : {String agentName, Lsa lsa, List<Property> waitingProperties, AgentLearningData learningData}
*	Space : {Map<String, Lsa> spaceMap, INotifier notifier}
*	EcoLawsEngine : {List<IEcoLaw> myEcoLaws: Decay, Bonding, Propagation}
*	LSA : {String agentName, List<Property> propertyList, Set<String> subDescription, Map<SyntheticPropertyName, Object> syntheticProperties}
*	Property : {String query, String bond, Object value, String type, String state, String ip, Boolean chosen, String aggregatedValue}
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
	*	AGGREGATION("aggregation") : Contains the aggregation to be applied for each LSA property identified by its name. This structure allows several aggregations to be applied independently to different LSA properties.
	*	LAST_AGGREGATION("lastAggregation") Last time of aggregation
	*	LOCATION("location") : host node name
	*	PATH("path") : List of the node addresses that have been reached by the LSA (do not include the source address) This information is updated at reception.
	*	SENDINGS("sendings") : List of node addresses where a same LSA has been sent. This information is updated before the LSA is sent : if sending fails, the address will be present in SEDINGS but not in PATH.
	* 	LAST_SENDING("lastSending") : Time elapsed (in MS) since the last sending

