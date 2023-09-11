package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.LsaForm;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;

import eu.sapere.middleware.node.NodeConfig;

public class SapereHandler extends AbstractHandler {

	public SapereHandler(String uri, NodeConfig nodeConfig, List<NodeConfig> _defaultNeighbours) {
		super();
		this.uri = uri;
		this.nodeConfig = nodeConfig;
		this.handlerTable = new HashMap<>();
		this.defaultNeighbours = _defaultNeighbours;
		initHandlerTable();
		logger.info("end init EnergyHandler");
	}

	@Route(value = "/startSapere")
	public String startsapere() {
		SapereLogger.getInstance().info("SAPERE starting...");
		return Sapere.getInstance().getInfo();
	}

	@Route(value = "/diffuse")
	public String diffuseLsa() {
		String name = (String) httpInput.get("name");
		Integer hops = (Integer) httpInput.get("hops");
		return Sapere.getInstance().diffuseLsa(name, hops);
	}

	@Route(value = "/info")
	public String getInfo() {
		return Sapere.getInstance().getInfo();
	}

	@Route(value = "/lsasList")
	public List<Service> getLsaList() {
		return Sapere.getInstance().getServices();
	}

	@Route(value = "/node")
	public List<String> getNodes() {
		return Sapere.getInstance().getNodes();
	}

	@Route(value = "/lsas")
	public List<String> getLsas() {
		return Sapere.getInstance().getLsa();
	}

	@Route(value = "/lsasObj")
	public List<LsaForm> getLsasObj() {
		return Sapere.getInstance().getLsasObj();
	}
/*
	@Route(value="/qtable/{name}")
    public Map<String, Double[]> getQtable(@PathVariable String name) {
        return Sapere.getInstance().getQtable(name);
    }

    @Route(value="/lsa/{name}")
    public String getLsa(@PathVariable String name) {
        return Sapere.getInstance().getLsa(name);
    }
  */
}
