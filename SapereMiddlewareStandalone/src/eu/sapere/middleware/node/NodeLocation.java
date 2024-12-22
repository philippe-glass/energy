package eu.sapere.middleware.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.IPropertyObject;
import eu.sapere.middleware.lsa.Lsa;


public class NodeLocation implements Serializable, IPropertyObject {
	private static final long serialVersionUID = 1L;
	private String name;
	private String host;
	private Integer restPort;
	private Integer mainPort;
	//private Long id;
	// private boolean isStarted;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return "http://" + host + ":" + restPort + "/energy/";
	}

	public String geHost() {
		return host;
	}

	public void setHost(String _host) {
		this.host = _host;
	}

	public Integer getRestPort() {
		return restPort;
	}

	public void setRestPort(Integer restPort) {
		this.restPort = restPort;
	}

	public Integer getMainPort() {
		return mainPort;
	}

	public void setMainPort(Integer mainPort) {
		this.mainPort = mainPort;
	}

	public NodeLocation() {
		super();
	}

	public NodeLocation(String _name, String _host, Integer _mainPort, Integer _restPort) {
		super();
		this.name = _name;
		this.host = _host;
		this.mainPort = _mainPort;
		this.restPort = _restPort;
		// this.url = "http://" + localip + ":" + localport + "/energy/";
	}

	public String getMainServiceAddress() {
		if (mainPort == null || host == null) {
			return null;
		}
		return host + ":" + mainPort;
	}

	public String getRestServiceAddress() {
		return host + ":" + restPort;
	}

	public String getHost() {
		return host;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeLocation) {
			NodeLocation otherNodeLocation = (NodeLocation) obj;
			return host.equals(otherNodeLocation.getHost()) && mainPort.equals(otherNodeLocation.getMainPort());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public NodeLocation clone() {
		return copy();
	}
	
	@Override
	public NodeLocation copyForLSA(AbstractLogger logger) {
		return copy();
	}

	/**
	 *  (for spreading)
	 * @param copyId
	 * @return
	 */
	public NodeLocation copy() {
		NodeLocation copy = new NodeLocation(name, host, mainPort, restPort);
		return copy;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<NodeLocation>();
		result.add(this.clone());
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("{");
		/*
		if(id != null) {
			result.append("(id:").append(id).append(")");
		}*/
		result.append("").append(this.name).append(" on ").append(this.host).append(":").append(this.mainPort)
				.append("(restPort:").append(restPort).append(")}");
		return result.toString();
	}
}
