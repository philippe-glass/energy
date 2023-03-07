package com.sapereapi.agent.energy.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sapereapi.model.energy.SingleOffer;

public class OfferSelectionStrategy {

	List<Comparator<SingleOffer>> listOfComparators;

	public OfferSelectionStrategy(List<Comparator<SingleOffer>> listOfComparators) {
		super();
		this.listOfComparators = listOfComparators;
	}

	@SafeVarargs
	public OfferSelectionStrategy(Comparator<SingleOffer>... args) {
		super();
		this.listOfComparators = new ArrayList<>();
		for (Comparator<SingleOffer> nextComparator : args) {
			if (nextComparator instanceof Comparator<?>) {
				this.listOfComparators.add(nextComparator);
			}
		}
	}

	public List<SingleOffer> sortList(List<SingleOffer> offerList) {
		Collections.sort(offerList, new Comparator<SingleOffer>() {
			public int compare(SingleOffer offer1, SingleOffer offer2) {
				int result = 0;
				for (Comparator<SingleOffer> nextComparator : listOfComparators) {
					result = nextComparator.compare(offer1, offer2);
					if (result != 0) {
						return result;
					}
				}
				return result;
			}
		});
		return offerList;
	}
}
