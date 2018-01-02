package com.blocktyper.bountysigns.data;

import com.blocktyper.v1_2_6.serialization.CardboardBox;

import java.util.Date;


public class AcceptedBounty {
	private String player;
	private String target;
	private Date acceptedDate;
	private Date completedDate;
	private String bountySignId;
	private CardboardBox reward;
	
	
	
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getPlayer() {
		return player;
	}
	public void setPlayer(String player) {
		this.player = player;
	}
	
	public Date getAcceptedDate() {
		return acceptedDate;
	}
	public void setAcceptedDate(Date acceptedDate) {
		this.acceptedDate = acceptedDate;
	}
	public Date getCompletedDate() {
		return completedDate;
	}
	public void setCompletedDate(Date completedDate) {
		this.completedDate = completedDate;
	}
	public String getBountySignId() {
		return bountySignId;
	}
	public void setBountySignId(String bountyId) {
		this.bountySignId = bountyId;
	}
	public CardboardBox getReward() {
		return reward;
	}
	public void setReward(CardboardBox reward) {
		this.reward = reward;
	}
	
	
}
