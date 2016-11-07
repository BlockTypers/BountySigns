package com.blocktyper.bountysigns.data;

import java.util.Map;


public class BountySignRepo {
	private Map<String, BountySign> map;

	public Map<String, BountySign> getMap() {
		return map;
	}

	public void setMap(Map<String, BountySign> map) {
		this.map = map;
	}
}
