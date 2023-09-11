package com.sapereapi.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.Generate;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Simulation;
import com.sapereapi.model.input.NeighboursUpdateRequest;

import eu.sapere.middleware.node.NodeConfig;

@RestController
@RequestMapping("/config")
public class ConfigController  {
	@Autowired
	private Environment environment;

	@GetMapping(path = "/allNodeConfigs")
	public List<NodeConfig> allNodeConfigs() {
		return Sapere.getInstance().retrieveAllNodeConfigs();
	}

	@GetMapping(path = "/nodeContext")
	public NodeContext nodeContext() {
		return Sapere.getNodeContext();
	}

	@PostMapping(path = "/updateNeighbours")
	public NodeContext updateNeighbours(@RequestBody NeighboursUpdateRequest request) {
		return Sapere.getInstance().updateNeighbours(request);
	}

	@PostMapping(path = "/addNodeConfig")
	public NodeConfig addNodeConfig(@RequestBody NodeConfig nodeConfig) {
		return EnergyDbHelper.registerNodeConfig(nodeConfig);
	}

	@PostMapping(path = "/addServiceSim")
	public String addServiceSim(@RequestBody Simulation simulation) {
		return Sapere.getInstance().updateAgents(simulation);
	}

	@PostMapping(path = "/setnodename")
	public String setnodename(String nodename) {
		return Sapere.getInstance().updateNodename(nodename);
	}

	@PostMapping(path = "/generateSim")
	public String generateSim(@RequestBody Generate generate) {
		return Sapere.getInstance().generateSimulation(generate);
	}
}
