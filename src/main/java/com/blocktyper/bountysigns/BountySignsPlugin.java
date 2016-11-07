package com.blocktyper.bountysigns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.bukkit.inventory.ItemStack;

import com.blocktyper.bountysigns.data.AcceptedBounty;
import com.blocktyper.bountysigns.data.AcceptedBountyRepo;
import com.blocktyper.bountysigns.data.BountySign;
import com.blocktyper.bountysigns.data.BountySignRepo;
import com.blocktyper.helpers.DimentionItemCount;
import com.blocktyper.plugin.BlockTyperPlugin;

public class BountySignsPlugin extends BlockTyperPlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.bountysigns.resources.BountySignsMessages";

	public static final String DATA_KEY_BOUNTY_SIGNS = "bounty-signs";
	public static final String DATA_KEY_ACCEPTED_BOUNTIES = "accepted-bounties";
	public static final String DATA_KEY_BOUNTY_SIGN_DIMENTION_MAP = "bounty-sign-dimiention-item-count";

	private Map<String, String> playerLastBountyTargetCreatedMap = new HashMap<String, String>();
	private Map<String, ItemStack> playerLastBountyRewardCreatedMap = new HashMap<String, ItemStack>();

	private BountySignRepo bountySignRepo = null;
	private AcceptedBountyRepo acceptedBountyRepo = null;
	private DimentionItemCount dimentionItemCount = null;

	public void onEnable() {
		super.onEnable();
		new BountySignsCommand(this);
		new BountySignInteractListener(this);
		new TargetEntityInteractListener(this);
	}

	// begin localization
	private ResourceBundle bundle = null;

	public ResourceBundle getBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(RESOURCE_NAME, locale);
		return bundle;
	}
	// end localization

	public Map<String, String> getPlayerLastBountyTargetCreatedMap() {
		return playerLastBountyTargetCreatedMap;
	}

	public void setPlayerLastBountyTargetCreatedMap(Map<String, String> playerLastBountyTargetCreatedMap) {
		this.playerLastBountyTargetCreatedMap = playerLastBountyTargetCreatedMap;
	}

	public Map<String, ItemStack> getPlayerLastBountyRewardCreatedMap() {
		return playerLastBountyRewardCreatedMap;
	}

	public void setPlayerLastBountyRewardCreatedMap(Map<String, ItemStack> playerLastBountyRewardCreatedMap) {
		this.playerLastBountyRewardCreatedMap = playerLastBountyRewardCreatedMap;
	}

	public BountySignRepo getBountySignRepo() {
		return bountySignRepo;
	}

	public void setBountySignRepo(BountySignRepo bountySignRepo) {
		this.bountySignRepo = bountySignRepo;
	}

	public AcceptedBountyRepo getAcceptedBountyRepo() {
		return acceptedBountyRepo;
	}

	public void setAcceptedBountyRepo(AcceptedBountyRepo acceptedBountyRepo) {
		this.acceptedBountyRepo = acceptedBountyRepo;
	}

	public DimentionItemCount getDimentionItemCount() {
		return dimentionItemCount;
	}

	public void setDimentionItemCount(DimentionItemCount dimentionItemCount) {
		this.dimentionItemCount = dimentionItemCount;
	}

	public void removeIdFromDimentionItemCount(String idToRemove) {

		initDimentionItemCount();

		DimentionItemCount dimentionItemCount = getDimentionItemCount();

		dimentionItemCount = getClickedBlockHelper().removeIdFromDimentionItemCount(idToRemove, dimentionItemCount);

		setDimentionItemCount(dimentionItemCount);

		updateDimentionItemCount();
	}

	public void initBountySignRepo() {
		if (bountySignRepo == null) {
			bountySignRepo = getTypeData(DATA_KEY_BOUNTY_SIGNS, BountySignRepo.class);

			if (bountySignRepo == null || bountySignRepo.getMap() == null) {
				bountySignRepo = new BountySignRepo();
				bountySignRepo.setMap(new HashMap<String, BountySign>());
				updateBountySignRepo();
			}
		}
	}

	public void initAcceptedBountyRepo() {
		if (acceptedBountyRepo == null) {
			acceptedBountyRepo = getTypeData(DATA_KEY_ACCEPTED_BOUNTIES, AcceptedBountyRepo.class);

			if (acceptedBountyRepo == null || acceptedBountyRepo.getMap() == null) {
				acceptedBountyRepo = new AcceptedBountyRepo();
				acceptedBountyRepo.setMap(new HashMap<String, List<AcceptedBounty>>());
				updateAcceptedBountyRepo();
			}
		}
	}

	public void updateAcceptedBountyRepo() {
		setData(DATA_KEY_ACCEPTED_BOUNTIES, acceptedBountyRepo);
	}

	public List<AcceptedBounty> getAcceptedBounties(String target) {
		initAcceptedBountyRepo();
		return acceptedBountyRepo.getMap().get(target);
	}

	public AcceptedBounty getAcceptedBounty(List<AcceptedBounty> acceptedBounties, String playerName,
			String bountySignId) {
		initAcceptedBountyRepo();

		if (acceptedBounties == null) {
			acceptedBounties = new ArrayList<AcceptedBounty>();
		}

		for (AcceptedBounty existing : acceptedBounties) {
			if (!existing.getPlayer().equals(playerName))
				continue;
			if (!existing.getBountySignId().equals(bountySignId))
				continue;

			return existing;
		}

		return null;
	}

	public boolean addAcceptedBounty(AcceptedBounty acceptedBounty) {
		try {

			initAcceptedBountyRepo();

			List<AcceptedBounty> acceptedBounties = getAcceptedBounties(acceptedBounty.getTarget());

			if (acceptedBounties == null) {
				acceptedBounties = new ArrayList<AcceptedBounty>();
			}

			AcceptedBounty bountyToRemove = getAcceptedBounty(acceptedBounties, acceptedBounty.getPlayer(),
					acceptedBounty.getBountySignId());

			if (bountyToRemove != null)
				acceptedBounties.remove(bountyToRemove);

			acceptedBounties.add(acceptedBounty);

			acceptedBountyRepo.getMap().put(acceptedBounty.getTarget(), acceptedBounties);

			updateAcceptedBountyRepo();
		} catch (Exception e) {
			warning("Unexpected error while saving door: " + e.getMessage());
			return false;
		}

		return true;
	}

	public void updateBountySignRepo() {
		setData(DATA_KEY_BOUNTY_SIGNS, bountySignRepo);
	}

	public void initDimentionItemCount() {
		if (dimentionItemCount == null) {
			dimentionItemCount = getTypeData(DATA_KEY_BOUNTY_SIGN_DIMENTION_MAP, DimentionItemCount.class);
			if (dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null) {
				dimentionItemCount = new DimentionItemCount();
				dimentionItemCount.setItemsInDimentionAtValue(new HashMap<String, Map<Integer, Set<String>>>());
				updateDimentionItemCount();
			}
		}
	}

	public void updateDimentionItemCount() {
		setData(DATA_KEY_BOUNTY_SIGN_DIMENTION_MAP, dimentionItemCount, true);
	}

	public List<String> getDimentionList() {
		List<String> dimentions = new ArrayList<String>();
		dimentions.add("x");
		dimentions.add("y");
		dimentions.add("z");
		return dimentions;
	}

	public boolean addBountySign(BountySign bountySign) {
		try {

			initBountySignRepo();
			initDimentionItemCount();

			for (String dimention : getDimentionList()) {
				if (dimentionItemCount.getItemsInDimentionAtValue().get(dimention) == null) {
					dimentionItemCount.getItemsInDimentionAtValue().put(dimention, new HashMap<Integer, Set<String>>());
				}

				int value = dimention.equals("x") ? bountySign.getX()
						: (dimention.equals("y") ? bountySign.getY() : bountySign.getZ());

				if (dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(value) == null) {
					dimentionItemCount.getItemsInDimentionAtValue().get(dimention).put(value, new HashSet<String>());
				}
				dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(value).add(bountySign.getId());
			}
			bountySignRepo.getMap().put(bountySign.getId(), bountySign);
			updateDimentionItemCount();
			updateBountySignRepo();
		} catch (Exception e) {
			warning("Unexpected error while saving door: " + e.getMessage());
			return false;
		}

		return true;
	}

	public String getRewardDescription(ItemStack itemStack) {
		String desc = itemStack.getType().name()
				+ (itemStack.getItemMeta() != null && itemStack.getItemMeta().getDisplayName() != null
						? " - " + itemStack.getItemMeta().getDisplayName() : "")
				+ " [" + itemStack.getAmount() + "]";
		return desc;
	}
}
