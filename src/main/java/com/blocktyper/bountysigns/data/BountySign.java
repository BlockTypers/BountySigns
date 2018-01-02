package com.blocktyper.bountysigns.data;


import com.blocktyper.v1_2_6.serialization.CardboardBox;

public class BountySign {
	private String id;
	private String world;
	private int x;
	private int y;
	private int z;
	private CardboardBox reward;
	private String target;
	private boolean killTarget = false;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getWorld() {
		return world;
	}

	public void setWorld(String world) {
		this.world = world;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	

	public CardboardBox getReward() {
		return reward;
	}

	public void setReward(CardboardBox reward) {
		this.reward = reward;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isKillTarget() {
		return killTarget;
	}

	public void setKillTarget(boolean killTarget) {
		this.killTarget = killTarget;
	}
	
	

}
