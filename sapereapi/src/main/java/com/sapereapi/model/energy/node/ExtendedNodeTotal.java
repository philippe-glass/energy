package com.sapereapi.model.energy.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sapereapi.model.energy.MissingRequest;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class ExtendedNodeTotal extends NodeTotal {
	private static final long serialVersionUID = 1L;
	//private double maxWarningDuration;
	private double sumWarningPower;
	private Long nbMissingRequests;
	//private Long nbOffers;
	//private Date dateLast;
	//private Date dateNext;
	private List<MissingRequest> listConsumerMissingRequests;
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
		Collections.sort(listConsumerMissingRequests,  new Comparator<MissingRequest>() {
		    public int compare(MissingRequest mrq1, MissingRequest mrq2) {
		        return -1*(mrq1.compareWarningDurationDescAndPower(mrq2));
		    }
		});
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

	public void computeSumWarningPower() {
		/*
		Collections.sort(listConsumerMissingRequests,  new Comparator<MissingRequest>() {
		    public int compare(MissingRequest mrq1, MissingRequest mrq2) {
		        return -1*(mrq1.compareWarningDurationDescAndPower(mrq2));
		    }
		});
		*/
		sumWarningPower = 0;
		for(MissingRequest missingRequest : listConsumerMissingRequests) {
			if(sumWarningPower + missingRequest.getPower() <= available) {
				sumWarningPower+=missingRequest.getPower();
			}
		}
	}

	public boolean hasChanges(ExtendedNodeTotal lastTotal) {
		if (offers != null && offers.size() > 0) {
			return true;
		}
		return super.hasChanges(lastTotal);
	}

	public static List<ExtendedNodeTotal> filterHasChanged(List<ExtendedNodeTotal> listNodeTotal,
			AbstractLogger logger) {
		List<ExtendedNodeTotal> result = new ArrayList<ExtendedNodeTotal>();
		ExtendedNodeTotal lastTotal = null;
		for (ExtendedNodeTotal nextTotal : listNodeTotal) {
			if (nextTotal.hasChanges(lastTotal)) {
				result.add(nextTotal);
			} else {
				logger.info("filterHasChanged : datesToRemove = " + UtilDates.format_time.format(nextTotal.getDate()));
			}
			lastTotal = nextTotal;
		}
		return result;
	}
}
