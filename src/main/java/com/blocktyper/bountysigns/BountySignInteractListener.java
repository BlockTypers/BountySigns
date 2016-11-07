package com.blocktyper.bountysigns;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.bountysigns.data.AcceptedBounty;
import com.blocktyper.bountysigns.data.BountySign;
import com.blocktyper.bountysigns.data.CardboardBox;
import com.blocktyper.plugin.IBlockTyperPlugin;

import listeners.AbstractListener;

public class BountySignInteractListener extends AbstractListener{

	private BountySignsPlugin bountySignsPlugin;
	private Random random = new Random();
	
	public BountySignInteractListener(IBlockTyperPlugin plugin) {
		super(plugin);
		bountySignsPlugin = (BountySignsPlugin)plugin;
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) {
			bountySignsPlugin.debugInfo("no block clicked");
			return;
		}

		if (!event.getClickedBlock().getType().equals(Material.SIGN)
				&& !event.getClickedBlock().getType().equals(Material.SIGN_POST)) {
			bountySignsPlugin.debugInfo("not a sign");
			return;
		}

		Sign sign = null;

		try {
			sign = (Sign) event.getClickedBlock().getState();
		} catch (Exception e) {
			bountySignsPlugin.warning("Issue casting sign: " + e.getMessage());
			return;
		}

		if (sign == null) {
			bountySignsPlugin.warning("Sign was null after casting");
			return;
		}

		boolean createBountySign = true;
		Player player = event.getPlayer();
		if (bountySignsPlugin.getPlayerLastBountyTargetCreatedMap().get(player.getName()) == null) {
			bountySignsPlugin.debugInfo("No target");
			createBountySign = false;
		}

		if (bountySignsPlugin.getPlayerLastBountyRewardCreatedMap().get(player.getName()) == null) {
			bountySignsPlugin.debugInfo("no reward");
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

		AcceptedBounty existingAcceptedBounty = bountySignsPlugin.getAcceptedBounty(
				bountySignsPlugin.getAcceptedBounties(bountySign.getTarget()), player.getName(), bountySign.getId());

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
		bountySignsPlugin.addAcceptedBounty(acceptedBounty);

		ItemStack reward = bountySign.getReward().unbox();
		player.sendMessage(ChatColor.GREEN + "Mission accepted. Find '" + bountySign.getTarget() + "'.");

		if (bountySign.getReward() != null) {
			player.sendMessage(
					ChatColor.GREEN + "You will be rewarded: " + bountySignsPlugin.getRewardDescription(bountySign.getReward().unbox()));

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
	
	
	
	private BountySign getBountySign(Sign sign) {
		bountySignsPlugin.initDimentionItemCount();

		if (bountySignsPlugin.getDimentionItemCount() == null
				|| bountySignsPlugin.getDimentionItemCount().getItemsInDimentionAtValue() == null
				|| bountySignsPlugin.getDimentionItemCount().getItemsInDimentionAtValue().isEmpty()) {
			plugin.debugInfo("no dimention values recorded");
			return null;
		}

		List<String> exactMatches = plugin.getClickedBlockHelper()
				.getMatchesInDimentionItemCount(bountySignsPlugin.getDimentionItemCount(), sign.getWorld().getName(), sign.getX(), sign.getY(), sign.getZ());

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

		bountySignsPlugin.initBountySignRepo();

		if (bountySignsPlugin.getBountySignRepo() == null) {
			plugin.debugWarning("Failed to load bounty-sign repo.");
			return null;
		}

		if (!bountySignsPlugin.getBountySignRepo().getMap().containsKey(match)) {
			plugin.debugWarning("Failed to load sign from bounty-sign repo.");
			return null;
		}

		BountySign bountySign = bountySignsPlugin.getBountySignRepo().getMap().get(match);
		if (bountySign == null) {
			plugin.debugWarning("Failed to load sign from bounty-sign repo.");
			return null;
		}

		return bountySign;
	}

	private void createBountySign(Player player, Sign sign, String id) {
		ItemStack reward = bountySignsPlugin.getPlayerLastBountyRewardCreatedMap().get(player.getName());
		String targetName = bountySignsPlugin.getPlayerLastBountyTargetCreatedMap().get(player.getName());

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
			bountySignsPlugin.addBountySign(bountySign);
			player.sendMessage(ChatColor.GREEN + "Bounty sign created. [" + bountySign.getId() + "]");
			bountySignsPlugin.getPlayerLastBountyRewardCreatedMap().put(player.getName(), null);
			player.sendMessage(ChatColor.YELLOW + "Reward cleared");
		} else {
			player.sendMessage(ChatColor.RED + "Unknown issue creating bounty sign created");
		}
	}
	
}
