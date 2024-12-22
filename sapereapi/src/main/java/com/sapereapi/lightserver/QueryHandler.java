package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;

import com.sapereapi.agent.QueryAgent;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.model.Service;
import com.sapereapi.model.input.Query;
import com.sapereapi.util.UtilHttp;

public class QueryHandler extends AbstractHandler {

	public QueryHandler(String uri, ServerConfig _serverConfig) {
		super();
		this.uri = uri;
		this.handlerTable = new HashMap<>();
		this.serverConfig = _serverConfig;
		initHandlerTable();
		logger.info("end init ConfigHandler");
	}

	@Route(value = "/addQuery")
	public List<Service> addQuery() {
		Query request = new Query();
		UtilHttp.fillObject(request, httpMethod, httpInput, logger);
		return Sapere.getInstance().addQuery(request);
	}

	@Route(value = "/getResult")
	public String getQueryResultByName() {
		String query = "" + httpInput.get("query");
		QueryAgent answer = Sapere.getInstance().getQueryByName(query);
		return answer != null ? answer.getSelectedResult() : "No result";
	}

	@Route(value = "/reward")
	public String rewardQueryByName() {
		String name = "" + httpInput.get("name");
		String sreward = "" + httpInput.get("reward");
		Integer reward = Integer.valueOf(sreward);
		QueryAgent answer = Sapere.getInstance().getQueryByName(name);
		return answer.rewardLsaFromApi(reward) ? "Rewarded " : "Not rewarded";
	}

}
