package com.blocktyper.bountysigns;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.bountysigns.data.BountySign;

public class BountySignsCommand implements CommandExecutor {

	private static String COMMAND_BOUNTY_SIGNS = "bounty-signs";
	private static String COMMAND_BOUNTY_SIGNS_SHORT = "bnty";

	private static final String ARG_BOUNTY_SIGN_TARGET = "target";
	private static final String ARG_BOUNTY_SIGN_KILL_TARGET = "kill-target";
	private static final String ARG_BOUNTY_SIGN_REWARD = "reward";
	private static final String ARG_BOUNTY_SIGN_RESET = "reset";
	private static final String ARG_BOUNTY_SIGN_END_ID = "clear-id";
	private static final String ARG_BOUNTY_SIGN_END_TARGET = "clear-target";
	private static final String ARG_BOUNTY_SIGN_END_ALL = "clear-all";
	private static final String ARG_BOUNTY_SIGN_LIST = "list";

	private static final Map<String, String> COMMAND_ARG_MAP;
	static {
		Map<String, String> commandArgMap = new HashMap<String, String>();
		commandArgMap.put(ARG_BOUNTY_SIGN_TARGET,
				"Run while looking at an NPC to set the current bounty target.  Alternately, use a 3rd argument of NPC's name instead of looking at an NPC.");
		commandArgMap.put(ARG_BOUNTY_SIGN_KILL_TARGET,
				"Run while looking at an NPC to set the current bounty kill target.  Alternately, use a 3rd argument of NPC's name instead of looking at an NPC.");
		commandArgMap.put(ARG_BOUNTY_SIGN_REWARD,
				"Run while holding an item to set the current bounty reward. Add an amount argument to ignore the stack size of in your hand.");
		commandArgMap.put(ARG_BOUNTY_SIGN_RESET,
				"Resets the current target NPC and reward so that clicking on signs will not turn them into bounty signs.");
		commandArgMap.put(ARG_BOUNTY_SIGN_END_ID, "Clears the bounty sign with the given ID.");
		commandArgMap.put(ARG_BOUNTY_SIGN_END_TARGET, "Clears the bounty signs for the given target NPC.");
		commandArgMap.put(ARG_BOUNTY_SIGN_END_ALL, "Clears all bounty signs.");
		commandArgMap.put(ARG_BOUNTY_SIGN_LIST,
				"List all active bounty signs. Supply an optional NPC name to show bounties just for that NPC.");
		COMMAND_ARG_MAP = Collections.unmodifiableMap(commandArgMap);
	}

	private static final Map<String, String> COMMAND_ARG_MAP_EXAMPLES;
	static {
		Map<String, String> commandArgMap = new HashMap<String, String>();
		commandArgMap.put(ARG_BOUNTY_SIGN_TARGET, "[\"/{0} {1}\", \"/{0} {1} Notch\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_KILL_TARGET, "[\"/{0} {1}\", \"/{0} {1} Notch\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_REWARD, "[\"/{0} {1}\", \"/{0} {1} 3\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_RESET, "[\"/{0} {1}\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_END_ID, "[\"/{0} {1} 991d8d91-b7d2-4c56-8ebd-41059c5b43eb\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_END_TARGET, "[\"/{0} {1} Notch\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_END_ALL, "[\"/{0} {1}\"]");
		commandArgMap.put(ARG_BOUNTY_SIGN_LIST, "[\"/{0} {1}\", \"/{0} {1} Notch\"]");
		COMMAND_ARG_MAP_EXAMPLES = Collections.unmodifiableMap(commandArgMap);
	}

	private BountySignsPlugin plugin;

	public BountySignsCommand(BountySignsPlugin plugin) {
		this.plugin = plugin;
		plugin.getCommand(COMMAND_BOUNTY_SIGNS).setExecutor(this);
		plugin.info("'/" + COMMAND_BOUNTY_SIGNS + "' registered");

		plugin.getCommand(COMMAND_BOUNTY_SIGNS_SHORT).setExecutor(this);
		plugin.info("'/" + COMMAND_BOUNTY_SIGNS_SHORT + "' registered");

		plugin.initBountySignRepo();
		plugin.initAcceptedBountyRepo();
		plugin.initDimentionItemCount();
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if (!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;

			if (!plugin.getPlayerHelper().playerCanDoAction(player, BountySignsPlugin.PERMISSIONS)) {
				player.sendMessage(ChatColor.RED + "You do not have persmission to execute this command");
				return false;
			}

			if (args != null && args.length > 0 && args[0] != null) {

				String firstArg = args[0];

				if (firstArg.equals(ARG_BOUNTY_SIGN_TARGET) || firstArg.equals(ARG_BOUNTY_SIGN_KILL_TARGET)) {
					setBountyCreationTarget(player, args, firstArg.equals(ARG_BOUNTY_SIGN_KILL_TARGET));
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_RESET)) {
					plugin.getPlayerLastBountyTargetCreatedMap().put(player.getName(), null);
					plugin.getPlayerLastBountyRewardCreatedMap().put(player.getName(), null);
					player.sendMessage(ChatColor.GREEN + "Target and reward reset");
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_REWARD)) {
					setBountyCreationReward(player, args);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_END_ID)) {
					endBountyById(player, args);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_END_TARGET)) {
					endAllBountiesForTarget(player, args);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_END_ALL)) {
					endAllBounties(player);
					return true;
				} else if (firstArg.equals(ARG_BOUNTY_SIGN_LIST)) {
					listAllBounties(player, args.length > 1 ? args[1] : null);
					return true;
				} else {
					player.sendMessage(ChatColor.RED + "Unknown command[" + firstArg + "]");
					showUsage(player, label);
					return false;
				}

			} else {
				showUsage(player, label);
				return true;
			}
		} catch (Exception e) {
			plugin.warning("error running '" + label + "':  " + e.getMessage());
			return false;
		}
	}

	private void listAllBounties(Player player, String target) {
		Map<String, BountySign> allSignsMap = plugin.getBountySignRepo().getMap();

		List<BountySign> bountySigns = null;
		if (allSignsMap != null) {

			bountySigns = allSignsMap.values().parallelStream().filter(
					bountySign -> bountySign != null && (target == null || target.equals(bountySign.getTarget())))
					.collect(Collectors.toList());

			bountySigns.forEach(bountySign -> player.sendMessage((ChatColor.DARK_PURPLE + bountySign.getTarget())
					+ (ChatColor.WHITE + " - ") + (ChatColor.YELLOW + bountySign.getId()) + (ChatColor.WHITE + " - ")
					+ (ChatColor.GREEN + plugin.getRewardDescription(bountySign.getReward().unbox()))
					+ (ChatColor.WHITE + " - ") + (ChatColor.GOLD + "{" + bountySign.getX() + "," + bountySign.getY()
							+ "," + bountySign.getZ() + "}")));
		}

		long bountySignsFound = bountySigns != null ? bountySigns.parallelStream().count() : 0;
		player.sendMessage(ChatColor.AQUA + "Bounties found: " + bountySignsFound);
	}

	private void endBountyById(Player player, String[] args) {
		if (args.length < 2) {
			player.sendMessage(ChatColor.RED + "You must supply an id.");
			return;
		}

		endBountyById(player, args[1]);
	}

	private void endBountyById(Player player, String id) {
		
		BountySign bountySign = plugin.getBountySignRepo().getMap().remove(id);

		if (bountySign == null) {
			player.sendMessage("No Bounty with id '" + id + "' was found.");
			return;
		}else{
			plugin.debugInfo("removing bounty : " + bountySign.getId());
		}

		plugin.updateBountySignRepo();
		player.sendMessage(ChatColor.GREEN + "Bounty sign removed. ID: " + ChatColor.YELLOW + id);

		plugin.removeIdFromDimentionItemCount(bountySign.getId());

		// bountySign.getWorld()
		World world = plugin.getServer().getWorld(bountySign.getWorld());

		if (world != null) {
			Block block = world.getBlockAt(bountySign.getX(), bountySign.getY(), bountySign.getZ());

			if (block != null) {
				try {
					Sign sign = (Sign) block.getState();
					if (sign != null) {
						sign.setLine(0, "");
						sign.setLine(1, "");
						sign.setLine(2, "");
						sign.update();
						player.sendMessage(ChatColor.DARK_PURPLE + "Sign cleared at location: [" + ChatColor.YELLOW
								+ bountySign.getWorld() + ChatColor.DARK_PURPLE + "](" + ChatColor.GOLD
								+ bountySign.getX() + "," + bountySign.getY() + "," + bountySign.getZ()
								+ ChatColor.DARK_PURPLE + ")");
					}
				} catch (IndexOutOfBoundsException e) {
					player.sendMessage(ChatColor.RED + "Sign not found at location: [" + ChatColor.YELLOW
							+ bountySign.getWorld() + ChatColor.RED + "](" + ChatColor.GOLD + bountySign.getX() + ","
							+ bountySign.getY() + "," + bountySign.getZ() + ChatColor.RED + ")");
				}
			} else {
				player.sendMessage(ChatColor.RED + "No block found at sign location: [" + ChatColor.YELLOW
						+ bountySign.getWorld() + ChatColor.RED + "](" + ChatColor.GOLD + bountySign.getX() + ","
						+ bountySign.getY() + "," + bountySign.getZ() + ChatColor.RED + ")");
			}
		} else {
			player.sendMessage(ChatColor.RED + "Could not find world '" + ChatColor.YELLOW + bountySign.getWorld()
					+ ChatColor.RED + "' to remove sign at location: (" + ChatColor.GOLD + bountySign.getX() + ","
					+ bountySign.getY() + "," + bountySign.getZ() + ChatColor.RED + ")");
		}

	}

	private void endAllBountiesForTarget(Player player, String[] args) {
		if (args.length < 2 || args[1] == null) {
			player.sendMessage(ChatColor.RED + "You must supply a target entity name.");
			return;
		}
		endAllBountiesOptionalTarget(player, args[1]);
	}

	private void endAllBounties(Player player) {
		endAllBountiesOptionalTarget(player, null);
	}
	
	private void endAllBountiesOptionalTarget(Player player, String target) {

		Map<String, BountySign> allSignsMap = plugin.getBountySignRepo().getMap();

		if (allSignsMap == null || allSignsMap.isEmpty()) {
			player.sendMessage(ChatColor.RED + "There are no signs to remove.");
			return;
		}

		
		List<BountySign> bountySignsToRemove = allSignsMap.values().parallelStream()
				.filter(bountySign -> bountySign != null && (target == null || bountySign.getTarget().equals(target)))
				.collect(Collectors.toList());

		if (bountySignsToRemove != null && !bountySignsToRemove.isEmpty()) {
			bountySignsToRemove.forEach(bountySign -> endBountyById(player, bountySign.getId()));
		}
		
		long signsRemoved = bountySignsToRemove != null ? bountySignsToRemove.parallelStream().count() : 0;

		if (signsRemoved == 0) {
			if(target == null){
				player.sendMessage(ChatColor.RED + "There are no signs to remove.");
			}else{
				player.sendMessage(ChatColor.RED + "There are no signs for that target to remove.");
			}
			
		} else {
			if(target == null){
				player.sendMessage(ChatColor.RED + (signsRemoved + " bounties cleared."));
			}else{
				player.sendMessage(ChatColor.YELLOW
						+ ((signsRemoved + "") + ChatColor.RED + " bounties cleared for target '" + target + "'."));
			}
			
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

		ItemStack reward = itemInHand.clone();
		reward.setAmount(amount);

		plugin.getPlayerLastBountyRewardCreatedMap().put(player.getName(), reward);

		player.sendMessage(
				ChatColor.GREEN + "Bounty creation reward set: " + (reward.getItemMeta().getDisplayName() != null
						? reward.getItemMeta().getDisplayName() : reward.getType().name()));

		if (plugin.getPlayerLastBountyTargetCreatedMap().get(player.getName()) != null) {
			player.sendMessage(ChatColor.GREEN + "You are ready to go right-click a sign");
		}
	}

	private void setBountyCreationTarget(Player player, String[] args, boolean killTarget) {

		String targetName = null;

		if (args.length > 1)
			targetName = args[1];

		if (targetName == null) {
			Entity targetEntity = plugin.getPlayerHelper().getTargetEntity(player);

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

		plugin.setKillTarget(killTarget);
		plugin.getPlayerLastBountyTargetCreatedMap().put(player.getName(), targetName);
		player.sendMessage(ChatColor.GREEN + "Bounty creation target set: " + targetName);

		if (plugin.getPlayerLastBountyRewardCreatedMap().get(player.getName()) != null) {
			player.sendMessage(ChatColor.GREEN + "You are ready to go right-click a sign");
		}

	}

	private void showUsage(Player player, String label) {
		player.sendMessage(ChatColor.GREEN + "Commands: ");
		
		COMMAND_ARG_MAP.keySet().parallelStream().forEach(arg -> 
			sendCommandArgMessage(arg, player, label));
	}
	
	private void sendCommandArgMessage(String arg, Player player, String label){
		player.sendMessage((ChatColor.DARK_PURPLE + "  /" + label + " " + arg)
				+ (ChatColor.GREEN + " (" + COMMAND_ARG_MAP.get(arg) + "}"));
		player.sendMessage(ChatColor.YELLOW + "    "
				+ (new MessageFormat(COMMAND_ARG_MAP_EXAMPLES.get(arg)).format(new Object[] { label, arg })));
	}

}
