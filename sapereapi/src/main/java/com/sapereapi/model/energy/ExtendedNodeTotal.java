package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExtendedNodeTotal extends NodeTotal {
	private static final long serialVersionUID = 1L;
	//private double maxWarningDuration;
	private double sumWarningPower;
	private Long nbMissingRequests;
	private Long nbOffers;
	private Date dateLast;
	private Date dateNext;
	private List<MissingRequest> listConsumerMissingRequests;
	//private List<String> consumersMissingRequests;
	//private List<String> consumersWarningRequests;
	private String contractDoublons;
	private List<SingleOffer> offers;

	public Long getNbMissingRequests() {
		return nbMissingRequests;
	}

	public void setNbMissingRequests(Long nbMissingRequests) {
		this.nbMissingRequests = nbMissingRequests;
	}

	public String getContractDoublons() {
		return contractDoublons;
	}

	public void setContractDoublons(String contractDoublons) {
		this.contractDoublons = contractDoublons;
	}

	public Long getNbOffers() {
		return nbOffers;
	}

	public void setNbOffers(Long nbOffers) {
		this.nbOffers = nbOffers;
	}

	public Date getDateLast() {
		return dateLast;
	}

	public void setDateLast(Date dateLast) {
		this.dateLast = dateLast;
	}

	public Date getDateNext() {
		return dateNext;
	}

	public void setDateNext(Date dateNext) {
		this.dateNext = dateNext;
	}

	public double getSumWarningPower() {
		return sumWarningPower;
	}

	public void setSumWarningPower(double _sumWarningPower) {
		this.sumWarningPower = _sumWarningPower;
	}

	public List<SingleOffer> getOffers() {
		return offers;
	}

	public void setOffers(List<SingleOffer> offers) {
		this.offers = offers;
	}

	public void addOffer(SingleOffer offer) {
		if(this.offers == null){
			this.offers = new ArrayList<SingleOffer>();
			offers.add(offer);
		}
	}

	public void addMissingRequest(MissingRequest missingRequest) {
		listConsumerMissingRequests.add(missingRequest);
	}

	public List<MissingRequest> getListConsumerMissingRequests() {
		return listConsumerMissingRequests;
	}

	public void setListConsumerMissingRequests(List<MissingRequest> listConsumerMissingRequests) {
		this.listConsumerMissingRequests = listConsumerMissingRequests;
	}

	public List<MissingRequest> getConsumersWarningRequests() {
		List<MissingRequest> result = new ArrayList<MissingRequest>();
		for(MissingRequest req : listConsumerMissingRequests) {
			if(req.getHasWarning()) {
				result.add(req);
			}
		}
		return result;
	}

	public MissingRequest getFirstWarningRequest() {
		 List<MissingRequest> listWarningReq = getConsumersWarningRequests();
		 if(listWarningReq.size()>0) {
			 return listWarningReq.get(0);
		 }
		 return null;
	}
}
