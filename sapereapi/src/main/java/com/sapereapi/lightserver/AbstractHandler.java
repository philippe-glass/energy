package com.sapereapi.lightserver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeConfig;

public abstract class AbstractHandler {
	protected AbstractLogger logger = SapereLogger.getInstance();
	protected String uri = null;
	protected NodeConfig nodeConfig = null;
	protected List<NodeConfig> defaultNeighbours = null;
	protected URI requestedUri = null;
	protected String httpMethod = null;
	protected Map<String, Object> httpInput = new HashMap<>();
	protected Map<String, Method> handlerTable = new HashMap<>();
	protected int debugLevel = 1;

	public String getHttpMethod() {
		return httpMethod;
	}

	public Map<String, Object> getHttpInput() {
		return httpInput;
	}

	protected void initHandlerTable() {
		this.handlerTable = new HashMap<>();
		Class<?> handlerClass = this.getClass();
		Method[] methods = handlerClass.getDeclaredMethods();
		for(Method nextMethod : methods) {
			if(nextMethod.getParameterCount() == 0) {
				String uril = "/"+ nextMethod.getName();	// URI by default
				if(nextMethod.isAnnotationPresent(Route.class)) {
					for(Annotation annotation : nextMethod.getAnnotations()) {
						if(Route.class.equals(annotation.annotationType())) {
							Route aRoute = (Route) annotation;
							logger.info("Route annontation found " + aRoute);
							uril = aRoute.value();
						}
					}
				}
				this.handlerTable.put(uril, nextMethod);
			}
		}
	}

	public String getUri() {
		return uri;
	}

	public Object callService(String requestedServcice, String _httpMethod, Map<String, Object> _httpInput, boolean useStressStest) {
		Object result = null;
		if(handlerTable.containsKey(requestedServcice)) {
			this.httpMethod = _httpMethod;
			this.httpInput = _httpInput;
			Method handlerMethod = handlerTable.get(requestedServcice);
			try {
				result = handlerMethod.invoke(this);
				/*
				if(false) {
					for (int i=0; i<10000;i++) {
						result = handlerMethod.invoke(this);
					}
				}*/
			} catch (Throwable e) {
				logger.error(e);
			}
		} else {
			logger.error(requestedServcice + " service not implemented in " + this.uri + " handler");
		}
		return result;
	}

}
