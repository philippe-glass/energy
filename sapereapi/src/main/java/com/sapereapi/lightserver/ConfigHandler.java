package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.Generate;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.model.Simulation;
import com.sapereapi.model.input.NeighboursUpdateRequest;
import com.sapereapi.util.UtilHttp;

import eu.sapere.middleware.node.NodeLocation;

public class ConfigHandler extends AbstractHandler {
	public ConfigHandler(String uri, ServerConfig _serverConfig) {
		super();
		this.uri = uri;
		this.handlerTable = new HashMap<>();
		this.serverConfig = _serverConfig;
		initHandlerTable();
		logger.info("end init ConfigHandler");
	}

	@Route(value = "/allNodeLocations")
	public List<NodeLocation> allNodeLocations() throws HandlingException {
		return Sapere.getInstance().retrieveAllNodeLocations();
	}

	@Route(value = "/nodeContext")
	public NodeContext nodeContext() {
		return Sapere.getNodeContext();
	}

	@Route(value = "/updateNeighbours")
	public NodeContext updateNeighbours() throws HandlingException {
		NeighboursUpdateRequest request = new NeighboursUpdateRequest();
		UtilHttp.fillObject(request, httpMethod, httpInput, logger);
		return Sapere.getInstance().updateNeighbours(request);
	}

	@Route(value = "/addNodeLocation")
	public NodeLocation addNodeLocation() throws HandlingException {
		NodeLocation aNodeLocation = new NodeLocation();
		UtilHttp.fillObject(aNodeLocation, httpMethod, httpInput, logger);
		return EnergyDbHelper.registerNodeLocation(aNodeLocation);
	}

	@Route(value = "/addServiceSim")
	public String addServiceSim() {
		Simulation simulation = new Simulation();
		UtilHttp.fillObject(simulation, httpMethod, httpInput, logger);
		return Sapere.getInstance().updateAgents(simulation);
	}

	@Route(value = "/setnodename")
	public String setnodename() throws HandlingException {
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
