package com.blocktyper.bountysigns.data;

import java.util.List;
import java.util.Map;


public class AcceptedBountyRepo {
	private Map<String, List<AcceptedBounty>> map;

	public Map<String, List<AcceptedBounty>> getMap() {
		return map;
	}

	public void setMap(Map<String, List<AcceptedBounty>> map) {
		this.map = map;
	}
}
