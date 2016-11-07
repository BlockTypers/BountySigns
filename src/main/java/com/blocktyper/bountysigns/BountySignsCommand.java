package com.blocktyper.bountysigns;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

import com.blocktyper.bountysigns.data.AcceptedBounty;
import com.blocktyper.bountysigns.data.AcceptedBountyRepo;
import com.blocktyper.bountysigns.data.BountySign;
import com.blocktyper.bountysigns.data.BountySignRepo;
import com.blocktyper.bountysigns.data.CardboardBox;
import com.blocktyper.bountysigns.data.DimentionItemCount;

public class BountySignsCommand implements CommandExecutor, Listener {
	public static String COMMAND_BOUNTY_SIGN = "bounty-signs";

	public static String ARG_BOUNTY_SIGN_TARGET = "target";
	public static String ARG_BOUNTY_SIGN_REWARD = "reward";

	public static String ARG_BOUNTY_SIGN_TARGET_CLEAR = "bounty-clear";
	public static String ARG_BOUNTY_SIGN_REWARD_CLEAR = "reward-clear";

	private String DATA_KEY_BOUNTY_SIGNS = "bounty-signs";
	private String DATA_KEY_ACCEPTED_BOUNTIES = "accepted-bounties";
	private String DATA_KEY_BOUNTY_SIGN_DIMENTION_MAP = "bounty-sign-dimiention-item-count";

	private BountySignsPlugin plugin;
	private Random random = new Random();

	private Map<String, String> playerLastBountyTargetCreatedMap = new HashMap<String, String>();
	private Map<String, ItemStack> playerLastBountyRewardCreatedMap = new HashMap<String, ItemStack>();

	private BountySignRepo bountySignRepo = null;
	private AcceptedBountyRepo acceptedBountyRepo = null;
	private DimentionItemCount dimentionItemCount = null;

	public BountySignsCommand(BountySignsPlugin plugin) {
		this.plugin = plugin;
		plugin.getCommand(COMMAND_BOUNTY_SIGN).setExecutor(this);
		plugin.info("'/" + COMMAND_BOUNTY_SIGN + "' registered");

		initBountySignRepo();
		initAcceptedBountyRepo();
		initDimentionItemCount();
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if (!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;

			if (args != null && args.length > 0 && args[0] != null) {

				String firstArg = args[0];

				if (firstArg.equals(ARG_BOUNTY_SIGN_TARGET)) {
					setBountyCreationTarget(player, args);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_TARGET_CLEAR)) {
					playerLastBountyTargetCreatedMap.put(player.getName(), null);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_REWARD)) {
					setBountyCreationReward(player, args);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_REWARD_CLEAR)) {
					playerLastBountyRewardCreatedMap.put(player.getName(), null);
					return true;
				} else {
					player.sendMessage(ChatColor.RED + "Unknown command[" + firstArg + "]");
					return false;
				}

			} else {
				return handleNoArgs(player);
			}
		} catch (Exception e) {
			plugin.info("error running '" + COMMAND_BOUNTY_SIGN + "':  " + e.getMessage());
			return false;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerClickNPC(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		Entity targetEntity = getTargetEntity(player);

		if (targetEntity == null) {
			return;
		}

		String targetName = targetEntity.getCustomName() != null ? targetEntity.getCustomName()
				: targetEntity.getName();

		if (targetName == null) {
			return;
		}

		initAcceptedBountyRepo();

		if (!acceptedBountyRepo.getMap().containsKey(targetName) || acceptedBountyRepo.getMap().get(targetName) == null
				|| acceptedBountyRepo.getMap().get(targetName).isEmpty())
			return;

		int bountiesCollected = 0;
		int bountiesAlreadyCompleted = 0;

		List<AcceptedBounty> acceptedBounties = new ArrayList<AcceptedBounty>();

		for (AcceptedBounty acceptedBounty : acceptedBountyRepo.getMap().get(targetName)) {
			try {
				if(acceptedBounty == null){
					plugin.warning("NUll bounty found while adding rewards to drop list");
					continue;
				}
				
				if (!acceptedBounty.getPlayer().equals(player.getName()))
					continue;

				acceptedBounties.add(acceptedBounty);

			} catch (Exception e) {
				plugin.warning("Error while adding rewards to drop list: " + e.getMessage());
			}
		}

		if (acceptedBounties.isEmpty()) {
			plugin.debugInfo("No bounties for this player and NPC combination.");
			return;
		}

		for (AcceptedBounty acceptedBounty : acceptedBounties) {

			try {

				if (acceptedBounty.getCompletedDate() != null) {
					bountiesAlreadyCompleted++;
					continue;
				}

				ItemStack reward = acceptedBounty.getReward().unbox();
				
				if(reward == null){
					plugin.warning("NUll reward found while dropping rewards.");
					continue;
				}
				
				plugin.debugInfo("REWARD DROPPED: " + reward.getType().name());
				player.sendMessage(ChatColor.GREEN + "Reward: " + getRewardDescription(reward));
				player.getWorld().dropItem(player.getLocation(), reward);
				bountiesCollected++;
				acceptedBounty.setCompletedDate(new Date());
				addAcceptedBounty(acceptedBounty);
			} catch (Exception e) {
				plugin.warning("Error while dropping rewards: " + e.getMessage());
			}
			
		}

		if (bountiesCollected > 0) {
			player.sendMessage(ChatColor.GREEN
					+ (bountiesCollected + " " + (bountiesCollected > 1 ? "bounties" : "bounty") + " collected."));
		} else if (bountiesAlreadyCompleted > 0) {
			player.sendMessage(ChatColor.YELLOW
					+ "You have already collected this bounty and must re-accept the mission at the sign to complete again.");
		}

	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) {
			plugin.debugInfo("no block clicked");
			return;
		}

		if (!event.getClickedBlock().getType().equals(Material.SIGN)
				&& !event.getClickedBlock().getType().equals(Material.SIGN_POST)) {
			plugin.debugInfo("not a sign");
			return;
		}

		Sign sign = null;

		try {
			sign = (Sign) event.getClickedBlock().getState();
		} catch (Exception e) {
			plugin.warning("Issue casting sign: " + e.getMessage());
			return;
		}

		if (sign == null) {
			plugin.warning("Sign was null after casting");
			return;
		}

		boolean createBountySign = true;
		Player player = event.getPlayer();
		if (playerLastBountyTargetCreatedMap.get(player.getName()) == null) {
			plugin.debugInfo("No target");
			createBountySign = false;
		}

		if (playerLastBountyRewardCreatedMap.get(player.getName()) == null) {
			plugin.debugInfo("no reward");
			createBountySign = false;
		}

		BountySign bountySign = getBountySign(sign);

		if (createBountySign) {
			createBountySign(player, sign, bountySign != null ? bountySign.getId() : null);
			return;
		}

		if (bountySign == null) {
			return;
		}

		AcceptedBounty existingAcceptedBounty = getAcceptedBounty(getAcceptedBounties(bountySign.getTarget()),
				player.getName(), bountySign.getId());

		if (existingAcceptedBounty != null) {
			long dayInMs = 1000 * 1 * 60 * 60 * 24;
			if (existingAcceptedBounty.getCompletedDate() != null) {
				long msSinceAcceptence = (existingAcceptedBounty.getAcceptedDate().getTime() - (new Date()).getTime());
				if (msSinceAcceptence < dayInMs) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(existingAcceptedBounty.getAcceptedDate());
					cal.add(Calendar.DAY_OF_YEAR, 1);
					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
					player.sendMessage(
							ChatColor.YELLOW + "You have colected this bounty and can not repeat for another 24 hours ["
									+ format1.format(cal.getTime()) + "].");
					return;
				}
			}
		}

		AcceptedBounty acceptedBounty = new AcceptedBounty();
		acceptedBounty.setAcceptedDate(new Date());
		acceptedBounty.setBountySignId(bountySign.getId());
		acceptedBounty.setReward(bountySign.getReward());
		acceptedBounty.setPlayer(player.getName());
		acceptedBounty.setTarget(bountySign.getTarget());
		addAcceptedBounty(acceptedBounty);

		ItemStack reward = bountySign.getReward().unbox();
		player.sendMessage(ChatColor.GREEN + "Mission accepted. Find '" + bountySign.getTarget() + "'.");

		if (bountySign.getReward() != null) {
			player.sendMessage(
					ChatColor.GREEN + "You will be rewarded: " + getRewardDescription(bountySign.getReward().unbox()));

			if (reward.getItemMeta() != null && reward.getItemMeta().getEnchants() != null
					&& !reward.getItemMeta().getEnchants().isEmpty()) {
				player.sendMessage(ChatColor.GREEN + " Enchanments: ");
				for (Enchantment enchantment : reward.getItemMeta().getEnchants().keySet()) {
					player.sendMessage(ChatColor.GREEN + "  -" + enchantment.getName() + "["
							+ reward.getItemMeta().getEnchants().get(enchantment) + "]");
				}
			}

			if (reward.getItemMeta() != null && reward.getItemMeta().getLore() != null
					&& !reward.getItemMeta().getLore().isEmpty()) {
				player.sendMessage(ChatColor.GREEN + " Lore: ");
				for (String lore : reward.getItemMeta().getLore()) {
					player.sendMessage(ChatColor.GREEN + "  -" + lore);
				}
			}
		}

	}

	private String getRewardDescription(ItemStack itemStack) {
		String desc = itemStack.getType().name()
				+ (itemStack.getItemMeta() != null && itemStack.getItemMeta().getDisplayName() != null
						? " - " + itemStack.getItemMeta().getDisplayName() : "")
				+ " [" + itemStack.getAmount() + "]";
		return desc;
	}

	private BountySign getBountySign(Sign sign) {
		initDimentionItemCount();

		if (dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null
				|| dimentionItemCount.getItemsInDimentionAtValue().isEmpty()) {
			plugin.debugInfo("no dimention values recorded");
			return null;
		}

		Map<String, Set<String>> matchesMap = new HashMap<String, Set<String>>();

		String lastDimention = null;
		for (String dimention : getDimentionList()) {
			if (!dimentionItemCount.getItemsInDimentionAtValue().containsKey(dimention)
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention) == null
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention).isEmpty()) {
				plugin.debugInfo("no " + dimention + " values recorded");
				return null;
			}

			int coordValue = dimention.equals("x") ? sign.getX() : (dimention.equals("y") ? sign.getY() : sign.getZ());

			if (!dimentionItemCount.getItemsInDimentionAtValue().get(dimention).containsKey(coordValue)
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(coordValue).isEmpty()) {
				plugin.debugInfo("no matching " + dimention + " value");
				return null;
			} else {

				Set<String> newMatchesList = new HashSet<String>();
				if (lastDimention == null || matchesMap.containsKey(lastDimention)) {
					for (String uuid : dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(coordValue)) {
						if (lastDimention == null || matchesMap.get(lastDimention).contains(uuid)) {
							newMatchesList.add(uuid);
						}
					}
				}

				matchesMap.put(dimention, newMatchesList);
			}
			lastDimention = dimention;
		}

		List<String> exactMatches = null;
		if (lastDimention != null && matchesMap.containsKey(lastDimention)) {
			exactMatches = new ArrayList<String>(matchesMap.get(lastDimention));
		}

		if (exactMatches == null || exactMatches.isEmpty()) {
			plugin.debugWarning("No match was found but we made it all the way through processing");
			return null;
		}

		int index = 0;
		if (exactMatches.size() > 1) {
			plugin.info("There was more than one match found after processing");
			index = random.nextInt(exactMatches.size());
		}

		String match = exactMatches.get(index);

		initBountySignRepo();

		if (bountySignRepo == null) {
			plugin.debugWarning("Failed to load bounty-sign repo.");
			return null;
		}

		if (!bountySignRepo.getMap().containsKey(match)) {
			plugin.debugWarning("Failed to load sign from bounty-sign repo.");
			return null;
		}

		BountySign bountySign = bountySignRepo.getMap().get(match);
		if (bountySign == null) {
			plugin.debugWarning("Failed to load sign from bounty-sign repo.");
			return null;
		}

		return bountySign;
	}

	private void createBountySign(Player player, Sign sign, String id) {
		ItemStack reward = playerLastBountyRewardCreatedMap.get(player.getName());
		String targetName = playerLastBountyTargetCreatedMap.get(player.getName());

		sign.setLine(0, "Wanted");
		sign.setLine(1, targetName);
		sign.setLine(2, ChatColor.DARK_GREEN + "Reward");
		if (sign.update()) {
			BountySign bountySign = new BountySign();
			bountySign.setX(sign.getX());
			bountySign.setY(sign.getY());
			bountySign.setZ(sign.getZ());
			bountySign.setTarget(targetName);

			bountySign.setReward(new CardboardBox(reward));

			bountySign.setWorld(sign.getWorld().getName());
			bountySign.setId(id == null ? UUID.randomUUID().toString() : id);
			addBountySign(bountySign);
			player.sendMessage(ChatColor.GREEN + "Bounty sign created. [" + bountySign.getId() + "]");
			playerLastBountyRewardCreatedMap.put(player.getName(), null);
			player.sendMessage(ChatColor.YELLOW + "Reward cleared");
		} else {
			player.sendMessage(ChatColor.RED + "Unknown issue creating bounty sign created");
		}
	}

	private void setBountyCreationReward(Player player, String[] args) {

		ItemStack itemInHand = plugin.getPlayerHelper().getItemInHand(player);

		if (itemInHand == null) {
			player.sendMessage(ChatColor.RED + "You must have and reward item in your hand.");
			return;
		}

		int amount = itemInHand.getAmount();

		if (args.length > 1) {
			String amountString = args[1];

			try {
				amount = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				player.sendMessage(ChatColor.RED + "amount [" + amountString + "] not recognized as a number.");
				return;
			}
		}

		if (amount > itemInHand.getType().getMaxStackSize()) {
			player.sendMessage(ChatColor.RED + "amount [" + amount + "] exceeds max stack size ["
					+ itemInHand.getType().getMaxStackSize() + "].");
			return;
		}

		ItemStack reward = new ItemStack(itemInHand.getType());
		reward.setAmount(amount);
		reward.setItemMeta(itemInHand.getItemMeta());

		playerLastBountyRewardCreatedMap.put(player.getName(), reward);

		player.sendMessage(
				ChatColor.GREEN + "Bounty creation reward set: " + (reward.getItemMeta().getDisplayName() != null
						? reward.getItemMeta().getDisplayName() : reward.getType().name()));

		if (playerLastBountyTargetCreatedMap.get(player.getName()) != null) {
			player.sendMessage(ChatColor.GREEN + "You are ready to go right-click a sign");
		}
	}

	private void setBountyCreationTarget(Player player, String[] args) {

		List<String> permissions = new ArrayList<String>();
		permissions.add("bountysigns.add.new.bounty.sign");

		if (!playerCanDoAction(player, true, permissions))
			return;

		String targetName = null;

		if (args.length > 1)
			targetName = args[1];

		if (targetName == null) {
			Entity targetEntity = getTargetEntity(player);

			if (targetEntity == null) {
				player.sendMessage(ChatColor.RED
						+ "You must be looking at an entity to set as the bounty target or you must provide the name as the 2nd argument.");
				return;
			}

			plugin.debugInfo("Bounty sign entity type: " + targetEntity.getType().name());
			plugin.debugInfo("Bounty sign entity id: " + targetEntity.getEntityId());
			plugin.debugInfo("Bounty sign entity uuid: " + targetEntity.getUniqueId());

			targetName = targetEntity.getCustomName() != null ? targetEntity.getCustomName() : targetEntity.getName();
		}

		if (targetName == null) {
			player.sendMessage(ChatColor.RED + "Unknown issue determining the bounty target name.");
			return;
		}

		playerLastBountyTargetCreatedMap.put(player.getName(), targetName);
		player.sendMessage(ChatColor.GREEN + "Bounty creation target set: " + targetName);

		if (playerLastBountyRewardCreatedMap.get(player.getName()) != null) {
			player.sendMessage(ChatColor.GREEN + "You are ready to go right-click a sign");
		}

	}

	private boolean playerCanDoAction(Player player, boolean sendMessage, List<String> permissions) {
		if (player.isOp() || permissions == null || permissions.isEmpty()) {
			return true;
		}

		for (String permission : permissions) {
			if (player.hasPermission(permission)) {
				return true;
			}
		}

		if (sendMessage) {
			String message = "You do not have persmission to execute this command";// plugin.getLocalizedMessage("bountysigns.player.cant.do.action");
			player.sendMessage(ChatColor.RED
					+ new MessageFormat(message).format(new Object[] { StringUtils.join(permissions, ",") }));
		}

		return false;
	}

	private boolean handleNoArgs(Player player) {
		player.sendMessage(ChatColor.GREEN + "/ym help ");
		return true;
	}

	private Entity getTargetEntity(final Player player) {

		BlockIterator iterator = new BlockIterator(player.getWorld(), player.getLocation().toVector(),
				player.getEyeLocation().getDirection(), 0, 100);
		Entity target = null;
		while (iterator.hasNext()) {
			Block item = iterator.next();
			for (Entity entity : player.getNearbyEntities(100, 100, 100)) {
				int acc = 2;
				for (int x = -acc; x < acc; x++)
					for (int z = -acc; z < acc; z++)
						for (int y = -acc; y < acc; y++)
							if (entity.getLocation().getBlock().getRelative(x, y, z).equals(item)) {
								return target = entity;
							}
			}
		}
		return target;
	}

	private void initBountySignRepo() {
		if (bountySignRepo == null) {
			bountySignRepo = plugin.getTypeData(DATA_KEY_BOUNTY_SIGNS, BountySignRepo.class);

			if (bountySignRepo == null || bountySignRepo.getMap() == null) {
				bountySignRepo = new BountySignRepo();
				bountySignRepo.setMap(new HashMap<String, BountySign>());
				updateBountySignRepo();
			}
		}
	}

	private void initAcceptedBountyRepo() {
		if (acceptedBountyRepo == null) {
			acceptedBountyRepo = plugin.getTypeData(DATA_KEY_ACCEPTED_BOUNTIES, AcceptedBountyRepo.class);

			if (acceptedBountyRepo == null || acceptedBountyRepo.getMap() == null) {
				acceptedBountyRepo = new AcceptedBountyRepo();
				acceptedBountyRepo.setMap(new HashMap<String, List<AcceptedBounty>>());
				updateAcceptedBountyRepo();
			}
		}
	}

	private void updateAcceptedBountyRepo() {
		plugin.setData(DATA_KEY_ACCEPTED_BOUNTIES, acceptedBountyRepo);
	}

	private List<AcceptedBounty> getAcceptedBounties(String target) {
		initAcceptedBountyRepo();
		return acceptedBountyRepo.getMap().get(target);
	}

	private AcceptedBounty getAcceptedBounty(List<AcceptedBounty> acceptedBounties, String playerName,
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

	private boolean addAcceptedBounty(AcceptedBounty acceptedBounty) {
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
			plugin.warning("Unexpected error while saving door: " + e.getMessage());
			return false;
		}

		return true;
	}

	private void updateBountySignRepo() {
		plugin.setData(DATA_KEY_BOUNTY_SIGNS, bountySignRepo);
	}

	private void initDimentionItemCount() {
		if (dimentionItemCount == null) {
			dimentionItemCount = plugin.getTypeData(DATA_KEY_BOUNTY_SIGN_DIMENTION_MAP, DimentionItemCount.class);
			if (dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null) {
				dimentionItemCount = new DimentionItemCount();
				dimentionItemCount.setItemsInDimentionAtValue(new HashMap<String, Map<Integer, Set<String>>>());
				updateDimentionItemCount();
			}
		}
	}

	private void updateDimentionItemCount() {
		plugin.setData(DATA_KEY_BOUNTY_SIGN_DIMENTION_MAP, dimentionItemCount, true);
	}

	private List<String> getDimentionList() {
		List<String> dimentions = new ArrayList<String>();
		dimentions.add("x");
		dimentions.add("y");
		dimentions.add("z");
		return dimentions;
	}

	private boolean addBountySign(BountySign bountySign) {
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
			plugin.warning("Unexpected error while saving door: " + e.getMessage());
			return false;
		}

		return true;
	}

}
