package com.sapereapi.agent.energy.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sapereapi.model.energy.EnergyRequest;

public class RequestSelectionStrategy {

	List<Comparator<EnergyRequest>> listOfComparators;

	public RequestSelectionStrategy(List<Comparator<EnergyRequest>> listOfComparators) {
		super();
		this.listOfComparators = listOfComparators;
	}

	@SafeVarargs
	public RequestSelectionStrategy(Comparator<EnergyRequest>... args) {
		super();
		this.listOfComparators = new ArrayList<>();
		for (Comparator<EnergyRequest> nextComparator : args) {
			if (nextComparator instanceof Comparator<?>) {
				this.listOfComparators.add(nextComparator);
			}
		}
	}

	public List<EnergyRequest> sortList(List<EnergyRequest> requestList) {
		Collections.sort(requestList, new Comparator<EnergyRequest>() {
			public int compare(EnergyRequest req1, EnergyRequest req2) {
				int result = 0;
				for (Comparator<EnergyRequest> nextComparator : listOfComparators) {
					result = nextComparator.compare(req1, req2);
					if (result != 0) {
						return result;
					}
				}
				return result;
			}
		});
		return requestList;
	}
}
