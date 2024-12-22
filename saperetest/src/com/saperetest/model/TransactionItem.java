package com.saperetest.model;

import com.sapereapi.util.UtilDates;

public class TransactionItem {
	String producerName;
	String producerNode;
	String consumerName;
	String consumerNode;
	Double power;

	public TransactionItem(DeviceItem producer, DeviceItem consumer, Double power) {
		super();
		this.producerName = producer.getName();
		this.producerNode = producer.getNode();
		this.consumerName = consumer.getName();
		this.consumerNode = consumer.getNode();
		this.power = power;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
	}

	public String getConsumerName() {
		return consumerName;
	}

	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}

	public String getConsumerNode() {
		return consumerNode;
	}

	public void setConsumerNode(String consumerNode) {
		this.consumerNode = consumerNode;
	}

	public String getProducerName() {
		return producerName;
	}

	public void setProducerName(String producerName) {
		this.producerName = producerName;
	}

	public String getProducerNode() {
		return producerNode;
	}

	public void setProducerNode(String producerNode) {
		this.producerNode = producerNode;
	}

	@Override
	public String toString() {
		return "[" + producerNode + "]" + producerName + " -> " + "[" + this.consumerNode + "]" + this.consumerName
				+ ":" + UtilDates.df3.format(power) + "W";
	}

}
