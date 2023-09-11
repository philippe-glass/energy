package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.Generate;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Simulation;
import com.sapereapi.model.input.NeighboursUpdateRequest;
import com.sapereapi.util.UtilHttp;

import eu.sapere.middleware.node.NodeConfig;

public class ConfigHandler extends AbstractHandler {
	public ConfigHandler(String uri, NodeConfig nodeConfig, List<NodeConfig> _defaultNeighbours) {
		super();
		this.uri = uri;
		this.nodeConfig = nodeConfig;
		this.handlerTable = new HashMap<>();
		this.defaultNeighbours = _defaultNeighbours;
		initHandlerTable();
		logger.info("end init ConfigHandler");
	}

	@Route(value = "/allNodeConfigs")
	public List<NodeConfig> allNodeConfigs() {
		return Sapere.getInstance().retrieveAllNodeConfigs();
	}

	@Route(value = "/nodeContext")
	public NodeContext nodeContext() {
		return Sapere.getNodeContext();
	}

	@Route(value = "/updateNeighbours")
	public NodeContext updateNeighbours() {
		NeighboursUpdateRequest request = new NeighboursUpdateRequest();
		UtilHttp.fillObject(request, httpMethod, httpInput, logger);
		return Sapere.getInstance().updateNeighbours(request);
	}

	@Route(value = "/addNodeConfig")
	public NodeConfig addNodeConfig() {
		NodeConfig aNodeConfig = new NodeConfig();
		UtilHttp.fillObject(aNodeConfig, httpMethod, httpInput, logger);
		return EnergyDbHelper.registerNodeConfig(aNodeConfig);
	}

	@Route(value = "/addServiceSim")
	public String addServiceSim() {
		Simulation simulation = new Simulation();
		UtilHttp.fillObject(simulation, httpMethod, httpInput, logger);
		return Sapere.getInstance().updateAgents(simulation);
	}

	@Route(value = "/setnodename")
	public String setnodename() {
		String nodename = "" + httpInput.get("nodename");
		return Sapere.getInstance().updateNodename(nodename);
	}

	@PostMapping(value = "/generateSim")
	public String generateSim() {
		Generate generate = new Generate();
		UtilHttp.fillObject(generate, httpMethod, httpInput, logger);
		return Sapere.getInstance().generateSimulation(generate);
	}
}
