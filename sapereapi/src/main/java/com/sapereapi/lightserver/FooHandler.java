package com.sapereapi.lightserver;

public class FooHandler {
	public static StringBuffer generateResponse(int testCounter) {
		StringBuffer response = new StringBuffer();
		response.append("<HTML>");
		response.append("<HEAD></HEAD>");
		response.append("<BODY>");
		response.append("<H1>This is a foo-handler test for com.sapereapi.lightserver</H1>");
		response.append("<H2>testCounter = " + testCounter + "</H2>");
		response.append("<table>");
		response.append(" <caption>");
		response.append("    Color names and values");
		response.append("  </caption>");
		response.append("  <tbody>");
		for (int i = 0; i < 100; i++) {
			response.append("    <tr>");
			response.append("      <th scope='col'>Name</th>");
			response.append("      <th scope='col'>HEX</th>");
			response.append("      <th scope='col'>HSLa</th>");
			response.append("      <th scope='col'>RGBa</th>");
			response.append("    </tr>");
			response.append("    <tr>");
			response.append("      <th scope='row'>Teal</th>");
			response.append("      <td><code>#51F6F6</code></td>");
			response.append("      <td><code>hsl(180 90% 64% / 1)</code></td>");
			response.append("      <td><code>rgb(81 246 246 / 1)</code></td>");
			response.append("    </tr>");
			response.append("    <tr>");
			response.append("      <th scope='row'>Goldenrod</th>");
			response.append("      <td><code>#F6BC57</code></td>");
			response.append("      <td><code>hsl(38 90% 65% / 1)</code></td>");
			response.append("      <td><code>rgba(246 188 87 / 1)</code></td>");
			response.append("    </tr>");
		}
		response.append("  </tbody>");
		response.append("</table>");
		response.append("</BODY>");
		response.append("</HTML>");
		return response;
	}

	public static StringBuffer generateJsonReponse() {
		StringBuffer rep = new StringBuffer();
		rep.append(
				"{\"consumerNames\":[\"Consumer_N1_1\",\"Consumer_N1_2\",\"Consumer_N1_3\",\"Consumer_N1_4\",\"Consumer_N1_5\",\"Consumer_N1_6\",\"Consumer_N1_7\",\"Consumer_N1_8\",\"Consumer_N1_9\",\"Consumer_N1_10\",\"Consumer_N1_11\"]");
		rep.append(
				",\"listPriorityLevel\":[{\"label\":\"\",\"value\":\"UNKNOWN\"},{\"label\":\"Low\",\"value\":\"LOW\"},{\"label\":\"Medium\",\"value\":\"MEDIUM\"},{\"label\":\"High\",\"value\":\"HIGH\"}]");
		rep.append(
				",\"listYesNo\":[{\"label\":\" \",\"value\":\"\"},{\"label\":\"Yes\",\"value\":\"YES\"},{\"label\":\"No\",\"value\":\"NO\"}]");
		rep.append(",\"warnings\":[]");
		rep.append(",\"currentDate\":\"2022-12-15T19:41:51\"");
		rep.append(",\"timeShiftMS\":0");
		// rep.append(",\"mapRunningAgents\":{\"Entré du tableau
		// TSC.01\":{\"availablePower\":0.0,\"electricalPanel\":\"\",\"prosumerRole\":\"CONSUMER\",\"distance\":0,\"endDate\":\"2023-12-15T19:41:02\",\"ongoingContractsTotal\":{\"current\":1375.82,\"margin\":0.0,\"min\":1255.9189999999999,\"max\":1388.121,\"intervalLength\":132.20200000000023},\"currentDate\":\"2023-06-20T17:41:51\",\"startInFutur\":false,\"disabledPower\":0.0,\"offersRepartition\":{},\"ongoingContractsRepartition\":{\"Prod_N1_2\":{\"current\":1375.82,\"margin\":0.0,\"min\":1255.9189999999999,\"max\":1388.121,\"intervalLength\":132.20200000000023}},\"deviceName\":\"Entré
		// du tableau
		// TSC.01\",\"isInSpace\":true,\"local\":true,\"running\":true,\"duration\":525599.9818833333,\"timeShiftMS\":0,\"priorityLevel\":\"Low\",\"energyRequest\":{\"issuerLocal\":true,\"maxBeginDate\":\"2023-12-15T19:41:03\",\"endDate\":\"2023-12-15T19:41:02\",\"totalDurationSec\":31535998,\"timeSlot\":{\"beginDate\":\"2022-12-15T19:41:03\",\"endDate\":\"2023-12-15T19:41:02\"},\"currentDate\":\"2023-06-20T17:41:51\",\"main\":true,\"startInFutur\":false,\"complementary\":false,\"issuerLocation\":{\"restPort\":9191,\"mainPort\":10001,\"restServiceAddress\":\"localhost:9191\",\"name\":\"N1\",\"host\":\"localhost\",\"mainServiceAddress\":\"localhost:10001\",\"id\":1,\"url\":\"http://localhost:9191/energy/\"},\"issuer\":\"Consumer_N1_2\",\"duration\":525599.9818833333,\"priorityLevel\":\"LOW\",\"timeShiftMS\":0,\"powerMargin\":0.0,\"pricingTable\":{\"ratesTable\":{},\"timeShiftMS\":0,\"timeSlots\":[],\"mapPrices\":{}},\"disabled\":false,\"power\":1375.82,\"refreshDate\":\"2023-06-20T17:41:51\",\"powerMin\":1375.82,\"isMain\":true,\"totalDurationMS\":31535998913,\"active\":true,\"delayToleranceMinutes\":525600.0,\"issuerDistance\":0,\"wH\":1.2050807379999999E7,\"beginDate\":\"2022-12-15T19:41:03","warningDurationSec":0,"powerMax":1375.82,"isComplementary":false,"deviceProperties":{"threePhases":false,"singlePhases":false,"electricalPanel":"","priorityLevel":0,"environmentalImpactLevel":3,"environmentalImpact":"MEDIUM","name":"Entré
		// du tableau
		// TSC.01","producer":false,"sensorNumber":"SE05000238","location":"Parascolaire","category":"ELECTRICAL_PANEL","phases":[]},"aux_expiryDate":"2023-06-20T17:41:51","powerSlot":{"current":1375.82,"margin":0.0,"min":1375.82,"max":1375.82,"intervalLength":0.0},"kWH":12050.807379999998},"energySupply":{"issuerLocal":true,"endDate":"2023-12-15T19:41:02","totalDurationSec":31535998,"timeSlot":{"beginDate":"2022-12-15T19:41:03","endDate":"2023-12-15T19:41:02"},"currentDate":"2023-06-20T17:41:51","main":true,"startInFutur":false,"complementary":false,"issuerLocation":{"restPort":9191,"mainPort":10001,"restServiceAddress":"localhost:9191","name":"N1","host":"localhost","mainServiceAddress":"localhost:10001","id":1,"url":"http://localhost:9191/energy/"},"issuer":"Consumer_N1_2","duration":525599.9818833333,"timeShiftMS":0,"powerMargin":0.0,"pricingTable":{"ratesTable":{},"timeShiftMS":0,"timeSlots":[],"mapPrices":{}},"disabled":false,"power":1375.82,"powerMin":1375.82,"isMain":true,"totalDurationMS":31535998913,"active":true,"issuerDistance":0,"wH":1.2050807379999999E7,"beginDate":"2022-12-15T19:41:03","powerMax":1375.82,"isComplementary":false,"deviceProperties":{"threePhases":false,"singlePhases":false,"electricalPanel":"","priorityLevel":0,"environmentalImpactLevel":3,"environmentalImpact":"MEDIUM","name":"Entré
		// du tableau
		// TSC.01","producer":false,"sensorNumber":"SE05000238","location":"Parascolaire","category":"ELECTRICAL_PANEL","phases":[]},"powerSlot":{"current":1375.82,"margin":0.0,"min":1375.82,"max":1375.82,"intervalLength":0.0},"kWH":12050.807379999998},"ongoingContractsTotalLocal":{"current":1375.82,"margin":0.0,"min":1255.9189999999999,"max":1388.121,"intervalLength":132.20200000000023},"sensorNumber":"SE05000238","waitingContractsConsumers":[],"power":1375.82,"delayToleranceRatio":1.0,"id":2,"isDisabled":false,"consumer":true,"agentName":"Consumer_N1_2","delayToleranceMinutes":525600.0,"missingPower":0.0,"url":"Consumer_N1_2","waitingContractsPower":{"current":0.0,"margin":0.0,"min":0.0,"max":0.0,"intervalLength":0.0},"beginDate":"2022-12-15T19:41:03","hasExpired":false,"offersTotal":0.0,"warningDurationSec":0,"deviceCategory":{"label":"Electrical
		// panel","value":"ELECTRICAL_PANEL"},"linkedAgents":["Prod_N1_2"],"environmentalImpact":3,"producer":false,"isSatisfied":true,"location":{"restPort":9191,"mainPort":10001,"restServiceAddress":"localhost:9191","name":"N1","host":"localhost","mainServiceAddress":"localhost:10001","id":1,"url":"http://localhost:9191/energy/"},"deviceLocation":"Parascolaire"},"Entré
		// du tableau
		// TSG.03":{"availablePower":0.0,"electricalPanel":"","prosumerRole":"CONSUMER","distance":0,"endDate":"2023-12-15T19:41:04","ongoingContractsTotal":{"current":2717.56,"margin":0.0,"min":2581.6820000000002,"max":2853.438,"intervalLength":271.75599999999986},"currentDate":"2023-06-20T17:41:51","startInFutur":false,"disabledPower":0.0,"offersRepartition":{},"ongoingContractsRepartition":{"Prod_N1_2":{"current":2717.56,"margin":0.0,"min":2581.6820000000002,"max":2853.438,"intervalLength":271.75599999999986}},"deviceName":"Entré
		// du tableau
		// TSG.03","isInSpace":true,"local":true,"running":true,"duration":525599.9799833334,"timeShiftMS":0,"priorityLevel":"Low","energyRequest":{"issuerLocal":true,"maxBeginDate":"2023-12-15T19:41:05","endDate":"2023-12-15T19:41:04","totalDurationSec":31535998,"timeSlot":{"beginDate":"2022-12-15T19:41:05","endDate":"2023-12-15T19:41:04"},"currentDate":"2023-06-20T17:41:51","main":true,"startInFutur":false,"complementary":false,"issuerLocation":{"restPort":9191,"mainPort":10001,"restServiceAddress":"localhost:9191","name":"N1","host":"localhost","mainServiceAddress":"localhost:10001","id":1,"url":"http://localhost:9191/energy/"},"issuer":"Consumer_N1_3","duration":525599.9799833334,"priorityLevel":"LOW","timeShiftMS":0,"powerMargin":0.0,"pricingTable":{"ratesTable":{},"timeShiftMS":0,"timeSlots":[],"mapPrices":{}},"disabled":false,"power":2717.56,"refreshDate":"2023-06-20T17:41:51","powerMin":2717.56,"isMain":true,"totalDurationMS":31535998799,"active":true,"delayToleranceMinutes":525600.0,"issuerDistance":0,"wH":2.380310804E7,"beginDate":"2022-12-15T19:41:05","warningDurationSec":0,"powerMax":2717.56,"isComplementary":false,"deviceProperties":{"threePhases":false,"singlePhases":false,"electricalPanel":"","priorityLevel":0,"environmentalImpactLevel":3,"environmentalImpact":"MEDIUM","name":"Entré
		// du tableau
		// TSG.03","producer":false,"sensorNumber":"SE05000283","location":"Gymnase","category":"ELECTRICAL_PANEL","phases":[]},"aux_expiryDate":"2023-06-20T17:41:51","powerSlot":{"current":2717.56,"margin":0.0,"min":2717.56,"max":2717.56,"intervalLength":0.0},"kWH":23803.10804},"energySupply":{"issuerLocal":true,"endDate":"2023-12-15T19:41:04","totalDurationSec":31535998,"timeSlot":{"beginDate":"2022-12-15T19:41:05","endDate":"2023-12-15T19:41:04"},"currentDate":"2023-06-20T17:41:51","main":true,"startInFutur":false,"complementary":false,"issuerLocation":{"restPort":9191,"mainPort":10001,"restServiceAddress":"localhost:9191","name":"N1","host":"localhost","mainServiceAddress":"localhost:10001","id":1,"url":"http://localhost:9191/energy/"},"issuer":"Consumer_N1_3","duration":525599.9799833334,"timeShiftMS":0,"powerMargin":0.0,"pricingTable":{"ratesTable":{},"timeShiftMS":0,"timeSlots":[],"mapPrices":{}},"disabled":false,"power":2717.56,"powerMin":2717.56,"isMain":true,"totalDurationMS":31535998799,"active":true,"issuerDistance":0,"wH":2.380310804E7,"beginDate":"2022-12-15T19:41:05","powerMax":2717.56,"isComplementary":false,"deviceProperties":{"threePhases":false,"singlePhases":false,"electricalPanel":"","priorityLevel":0,"environmentalImpactLevel":3,"environmentalImpact":"MEDIUM","name":"Entré
		// du tableau
		// TSG.03","producer":false,"sensorNumber":"SE05000283","location":"Gymnase","category":"ELECTRICAL_PANEL","phases":[]},"powerSlot":{"current":2717.56,"margin":0.0,"min":2717.56,"max":2717.56,"intervalLength":0.0},"kWH":23803.10804},"ongoingContractsTotalLocal":{"current":2717.56,"margin":0.0,"min":2581.6820000000002,"max":2853.438,"intervalLength":271.75599999999986},"sensorNumber":"SE05000283","waitingContractsConsumers":[],"power":2717.56,"delayToleranceRatio":1.0,"id":3,"isDisabled":false,"consumer":true,"agentName":"Consumer_N1_3","delayToleranceMinutes":525600.0,"missingPower":0.0,"url":"Consumer_N1_3","waitingContractsPower":{"current":0.0,"margin":0.0,"min":0.0,"max":0.0,"intervalLength":0.0},"beginDate":"2022-12-15T19:41:05","hasExpired":false,"offersTotal":0.0,"warningDurationSec":0,"deviceCategory":{"label":"Electrical
		// panel","value":"ELECTRICAL_PANEL"},"linkedAgents":["Prod_N1_2"],"environmentalImpact":3,"producer":false,"isSatisfied":true,"location":{"restPort":9191,"mainPort":10001,"restServiceAddress":"localhost:9191","name":"N1","host":"localhost","mainServiceAddress":"localhost:10001","id":1,"url":"http://localhost:9191/energy/"},"deviceLocation":"Gymnase"},"ventil.
		// Extraction WC Filles
		// (marron)":{"availablePower":0.0,"electricalPanel":"220F1","prosumerRole":"CONSUMER","distance":0,"endDate":"2023-12-15T19:40:39","ongoingContractsTotal":{"current":0.51,"margin":0.0,"min":0.48450000000000004,"max":0.5355,"intervalLength":0.050999999999999934},"currentDate":"2023-06-20T17:41:51","startInFutur":false,"disabledPower":0.0,"offersRepartition":{},"ongoingContractsRepartition":{"Prod_N1_2":{"current":0.51,"margin":0.0,"min":0.48450000000000004,"max":0.5355,"intervalLength":0.050999999999999934}},"deviceName":"ventil.
		// Extraction WC Filles
		// (marron)","isInSpace":true,"local":true,"running":true,"duration":525599.9950333333,"timeShiftMS":0,"pr...
		rep.append("}");
		return rep;

	}
}
