package com.sapereapi.model.energy.input;

import java.util.Date;

public class OfferFilter {
	private Date dateMin;
	private Date dateMax;
	private String consumerFilter;
	private String producerFilter;
	private String additionalFilter;

	public Date getDateMin() {
		return dateMin;
	}

	public void setDateMin(Date dateMin) {
		this.dateMin = dateMin;
	}

	public Date getDateMax() {
		return dateMax;
	}

	public void setDateMax(Date dateMax) {
		this.dateMax = dateMax;
	}

	public String getConsumerFilter() {
		return consumerFilter;
	}

	public void setConsumerFilter(String consumerFilter) {
		this.consumerFilter = consumerFilter;
	}

	public String getProducerFilter() {
		return producerFilter;
	}

	public void setProducerFilter(String producerFilter) {
		this.producerFilter = producerFilter;
	}

	public String getAdditionalFilter() {
		return additionalFilter;
	}

	public void setAdditionalFilter(String additionalFilter) {
		this.additionalFilter = additionalFilter;
	}

}
