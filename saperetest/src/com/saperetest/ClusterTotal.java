package com.saperetest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SimulatorLogger;
import com.sapereapi.model.energy.node.NodeTotal;
import com.saperetest.model.DeviceItem;
import com.saperetest.model.TransactionItem;

public class ClusterTotal {
	private Date date;
	private Map<String, Double> mapRequested = new HashMap<String, Double>();
	private Map<String, Double> mapProduced = new HashMap<String, Double>();
	private Map<String, Double> mapConsumed = new HashMap<String, Double>();
	private Map<String, Double> mapConsumedLocally = new HashMap<String, Double>();
	private Map<String, Double> mapProvided = new HashMap<String, Double>();
	private Map<String, Double> mapProvidedLocally = new HashMap<String, Double>();
	private Map<String, Double> mapAvailable = new HashMap<String, Double>();
	private Map<String, Double> mapMissing = new HashMap<String, Double>();

	private List<DeviceItem> listRequests = new ArrayList<DeviceItem>();
	private List<DeviceItem> listProductions = new ArrayList<DeviceItem>();

	public ClusterTotal(Date date) {
		super();
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setRequested(String node, Double value) {
		mapRequested.put(node, value);
	}

	public void setProduced(String node, Double value) {
		mapProduced.put(node, value);
	}

	public void setConsumed(String node, Double value) {
		mapConsumed.put(node, value);
	}

	public void setProvided(String node, Double value) {
		mapProvided.put(node, value);
	}

	public void setAvailable(String node, Double value) {
		mapAvailable.put(node, value);
	}

	public void setMissing(String node, Double value) {
		mapMissing.put(node, value);
	}

	public void setProvidedLocally(String node, Double value) {
		mapProvidedLocally.put(node, value);
	}

	public void setpConsumedLocally(String node, Double value) {
		mapConsumedLocally.put(node, value);
	}

	public void addDeviceItem(DeviceItem item) {
		if (item.getIsProducer()) {
			listProductions.add(item);
		} else {
			listRequests.add(item);
		}
	}

	public List<String> getNodes() {
		List<String> result = new ArrayList<String>();
		for (String node : mapRequested.keySet()) {
			result.add(node);
		}
		return result;
	}

	public void resetTransacions() {
		for (String nextNode : getNodes()) {
			mapConsumed.put(nextNode, 0.0);
			mapProvided.put(nextNode, 0.0);
			mapMissing.put(nextNode, mapRequested.get(nextNode));
			mapAvailable.put(nextNode, mapProduced.get(nextNode));
			for (DeviceItem prod : this.listProductions) {
				prod.setPowerProvided(0.0);
				prod.setPowerConsumed(0.0);
			}
			for (DeviceItem req : this.listRequests) {
				req.setPowerProvided(0.0);
				req.setPowerConsumed(0.0);
			}
		}
	}
/*
	public void simulateTransactionsOld() {
		for (String nextNode : mapAvailable.keySet()) {
			double available = mapAvailable.get(nextNode);
			double provided = mapProvided.get(nextNode);
			if (available > 0) {
				Collections.sort(listRequests, new Comparator<DeviceItem>() {
					@Override
					public int compare(DeviceItem item1, DeviceItem item2) {
						int nodeDist1 = item1.getNode().equals(nextNode) ? 0 : 1;
						int nodeDist2 = item2.getNode().equals(nextNode) ? 0 : 1;
						int compareNode = nodeDist1 - nodeDist2;
						if (compareNode != 0) {
							return compareNode;
						}
						double diffPower = 1000 * (item1.getPower() - item2.getPower());
						return (int) diffPower;
					}
				});
				for (DeviceItem reqItem : listRequests) {
					if (!reqItem.isSatisfied()) {
						if (reqItem.getPower() <= available) {
							double powerProvided = reqItem.getPower();
							reqItem.setPowerConsumed(powerProvided);
							available -= powerProvided;
							provided += powerProvided;
						}
					}
				}
				mapAvailable.put(nextNode, available);
				mapProvided.put(nextNode, provided);
			}
		}
		for (DeviceItem reqItem : listRequests) {
			if (reqItem.isSatisfied()) {
				String nextNode = reqItem.getNode();
				double consumed = mapConsumed.get(nextNode) + reqItem.getPower();
				mapConsumed.put(nextNode, consumed);
				double missing = mapMissing.get(nextNode) - reqItem.getPower();
				mapMissing.put(nextNode, missing);
			}
		}
	}
*/

	private List<TransactionItem> auxGenerateTransaction(DeviceItem producer,
			List<DeviceItem> listUnsatisfiedRequests) {
		List<TransactionItem> result = new ArrayList<TransactionItem>();
		String producerNode = producer.getNode();
		Collections.sort(listUnsatisfiedRequests, new Comparator<DeviceItem>() {
			@Override
			public int compare(DeviceItem item1, DeviceItem item2) {
				int nodeDist1 = item1.getNode().equals(producerNode) ? 0 : 1;
				int nodeDist2 = item2.getNode().equals(producerNode) ? 0 : 1;
				int compareNode = nodeDist1 - nodeDist2;
				if (compareNode != 0) {
					return compareNode;
				}
				double diffPower = 1000 * (item1.getPower() - item2.getPower());
				return (int) diffPower;
			}
		});
		for (DeviceItem reqItem : listUnsatisfiedRequests) {
			if (!reqItem.isSatisfied() && producer.getPowerAvailable() > 0) {
				Double providedPower = Math.min(producer.getPowerAvailable(), reqItem.getPowerMissing());
				if (providedPower > 0) {
					TransactionItem transactionItem = new TransactionItem(producer, reqItem, providedPower);
					result.add(transactionItem);
					reqItem.setPowerConsumed(reqItem.getPowerConsumed() + providedPower);
					producer.setPowerProvided(producer.getPowerProvided() + providedPower);
				}
			}
		}
		return result;
	}

	private DeviceItem findConsumer(String node, String name) {
		for (DeviceItem consumer : listRequests) {
			if (node.equalsIgnoreCase(consumer.getNode()) && name.equals(consumer.getName())) {
				return consumer;
			}
		}
		return null;
	}

	public Double getTotalRequested() {
		double result = 0;
		for (Double nextValue : mapRequested.values()) {
			result += nextValue;
		}
		return result;
	}

	public Double getTotaProduced() {
		double result = 0;
		for (Double nextValue : mapProduced.values()) {
			result += nextValue;
		}
		return result;
	}

	// simulate transactions
	public void simulateTransactions() {
		List<DeviceItem> listUnsatisfiedRequests = new ArrayList<DeviceItem>();
		List<TransactionItem> tmpTransactions = new ArrayList<TransactionItem>();
		listUnsatisfiedRequests.addAll(listRequests);
		for (DeviceItem producer : listProductions) {
			List<TransactionItem> newTransactions = auxGenerateTransaction(producer, listUnsatisfiedRequests);
			tmpTransactions.addAll(newTransactions);
			List<DeviceItem> listUnsatisfiedRequestsNew = new ArrayList<DeviceItem>();
			// refresh listUnsatisfiedRequests
			for (DeviceItem nextRequest : listUnsatisfiedRequests) {
				if (!nextRequest.isSatisfied()) {
					listUnsatisfiedRequestsNew.add(nextRequest);
				}
			}
			listUnsatisfiedRequests = listUnsatisfiedRequestsNew;
		}
		// Cancel transaction if the requested is still not satisfied
		List<TransactionItem> transactionConfirmed = new ArrayList<TransactionItem>();
		for (TransactionItem nextTransaction : tmpTransactions) {
			// check if the consumer is satisfied
			DeviceItem servedConsumer = findConsumer(nextTransaction.getConsumerNode(),
					nextTransaction.getConsumerName());
			if (servedConsumer.isSatisfied()) {
				transactionConfirmed.add(nextTransaction);
			} else {
				double totalProduced = getTotaProduced();
				double totalRequested = getTotalRequested();
				if (totalProduced > totalRequested + 0.001) {
					SimulatorLogger.getInstance().info("Partialy not satisfied : " + servedConsumer
							+ ", total requested = " + totalRequested + ", total produced = " + totalProduced);
				}
			}
		}
		for (TransactionItem nextTransaction : transactionConfirmed) {
			double providedPower = nextTransaction.getPower();
			String nodeProvider = nextTransaction.getProducerNode();
			mapProvided.put(nodeProvider, mapProvided.get(nodeProvider) + providedPower);
			mapAvailable.put(nodeProvider, mapAvailable.get(nodeProvider) - providedPower);
			String nodeConsumer = nextTransaction.getConsumerNode();
			mapConsumed.put(nodeConsumer, mapConsumed.get(nodeConsumer) + providedPower);
			mapMissing.put(nodeConsumer, mapMissing.get(nodeConsumer) - providedPower);
		}
		for (String node : getNodes()) {
			if (mapMissing.get(node) < 0 || mapConsumed.get(node) < 0) {
				SimulatorLogger.getInstance().info("simulateTransactions For debug");
			}
		}
	}

	boolean checkUp() {
		for (String node : getNodes()) {
			if (mapMissing.get(node) < -0.0001 || (mapConsumed.get(node) < -0.0001)
					|| mapAvailable.get(node) < -0.0001) {
				SimulatorLogger.getInstance().info("checkUp For debug");
				return false;
			}
		}
		return true;
	}

	public NodeTotal generateNodeTotal(String node) {
		if (mapRequested.containsKey(node)) {
			NodeTotal result = new NodeTotal();
			result.setDate(date);
			result.setRequested(mapRequested.get(node));
			result.setProduced(mapProduced.get(node));
			result.setAvailable(mapAvailable.get(node));
			result.setConsumed(mapConsumed.get(node));
			result.setProvided(mapProvided.get(node));
			result.setMissing(mapMissing.get(node));
			return result;
		}
		return null;
	}
}
