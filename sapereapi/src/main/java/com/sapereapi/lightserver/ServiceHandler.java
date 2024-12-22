package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;

import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.model.Service;
import com.sapereapi.util.UtilHttp;

public class ServiceHandler extends AbstractHandler {

	public ServiceHandler(String uri, ServerConfig _serverConfig) {
		super();
		this.uri = uri;
		this.handlerTable = new HashMap<>();
		this.serverConfig = _serverConfig;
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
