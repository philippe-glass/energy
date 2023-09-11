package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;

import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;
import com.sapereapi.util.UtilHttp;

import eu.sapere.middleware.node.NodeConfig;

public class ServiceHandler extends AbstractHandler {

	public ServiceHandler(String uri, NodeConfig nodeConfig, List<NodeConfig> _defaultNeighbours) {
		super();
		this.uri = uri;
		this.nodeConfig = nodeConfig;
		this.handlerTable = new HashMap<>();
		this.defaultNeighbours = _defaultNeighbours;
		initHandlerTable();
		logger.info("end init ConfigHandler");
	}

	@Route(value = "/addService")
	public List<Service> addService() {
		Service service = new Service();
		UtilHttp.fillObject(service, httpMethod, httpInput, logger);
		return Sapere.getInstance().addServiceGeneric(service);
	}

}
