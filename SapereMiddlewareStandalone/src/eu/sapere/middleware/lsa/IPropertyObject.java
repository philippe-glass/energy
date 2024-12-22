package eu.sapere.middleware.lsa;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeLocation;

public interface IPropertyObject extends Serializable {
	public IPropertyObject copyForLSA(AbstractLogger logger);
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger);
	public List<NodeLocation> retrieveInvolvedLocations();
}
