package com.blocktyper.bountysigns;

public enum LocalizedMessageEnum {

	MISSION_ACCEPTED("bountysigns.mission.accepted"),
	YOU_WILL_BE_REWARDED("bountysigns.you.will.be.rewarded"),
	ENCHANTMENTS("bountysigns.enchantments"),
	LORE("bountysigns.lore"),
	SIGN_CREATED("bountysigns.sign.created"),
	UNKNOWN_ISSUE_CREATING_SIGN("bountysigns.unknown.issue.creating.sign"),
	PLAYER_MUST_DAMAGE_TARGET("bountysigns.player.must.damage.target"),
	REMAINING_HEALTH("bountysigns.remaining.health"),
	BOUNTY_COLLECTED("bountysigns.bounty.collected"),
	BOUNTIES_COLLECTED("bountysigns.bounties.collected"),
	BOUNTY_ALREADY_COLLECTED("bountysigns.bounty.already.collected"),
	CANT_ACCEPT_BOUNTY_YET("bountysigns.cant.accept.mission.yet"),
	REWARD("bountysigns.rewarded")
	;

	private String key;

	private LocalizedMessageEnum(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
