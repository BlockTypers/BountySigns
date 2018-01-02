package com.blocktyper.bountysigns;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.blocktyper.v1_2_5.serialization.CardboardBox;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.bountysigns.data.AcceptedBounty;
import com.blocktyper.bountysigns.data.BountySign;

public class BountySignInteractListener implements Listener {

	private BountySignsPlugin plugin;
	private Random random = new Random();

	public BountySignInteractListener(BountySignsPlugin plugin) {
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
		if (plugin.getPlayerLastBountyTargetCreatedMap().get(player.getName()) == null) {
			plugin.debugInfo("No target");
			createBountySign = false;
		}

		if (plugin.getPlayerLastBountyRewardCreatedMap().get(player.getName()) == null) {
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

		AcceptedBounty existingAcceptedBounty = plugin.getAcceptedBounty(
				plugin.getAcceptedBounties(bountySign.getTarget()), player.getName(), bountySign.getId());

		if (existingAcceptedBounty != null) {
			long dayInMs = 1000 * 1 * 60 * 60 * 24;
			if (existingAcceptedBounty.getCompletedDate() != null) {
				long msSinceAcceptence = (existingAcceptedBounty.getAcceptedDate().getTime() - (new Date()).getTime());
				if (msSinceAcceptence < dayInMs) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(existingAcceptedBounty.getAcceptedDate());
					cal.add(Calendar.DAY_OF_YEAR, 1);
					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					String message = plugin.getLocalizedMessage(LocalizedMessageEnum.CANT_ACCEPT_BOUNTY_YET.getKey(), player);
					message = new MessageFormat(message).format(new Object[]{ChatColor.RED + format1.format(cal.getTime())});
					player.sendMessage((ChatColor.YELLOW + message));
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
		plugin.addAcceptedBounty(acceptedBounty);

		if (bountySign.getReward() != null) {
			ItemStack reward = bountySign.getReward().unbox();
			sendPlayerRewardMessage(reward, player, bountySign.getTarget());
		}

	}

	private void sendPlayerRewardMessage(ItemStack reward, Player player, String target) {
		if (reward != null) {

			String message = plugin.getLocalizedMessage(LocalizedMessageEnum.MISSION_ACCEPTED.getKey(), player);
			message = new MessageFormat(message).format(new Object[] { (ChatColor.RED + target + ChatColor.GREEN) });
			player.sendMessage(ChatColor.GREEN + message);

			message = plugin.getLocalizedMessage(LocalizedMessageEnum.YOU_WILL_BE_REWARDED.getKey(), player);
			message = new MessageFormat(message)
					.format(new Object[] { (ChatColor.GREEN + plugin.getRewardDescription(reward)) });
			player.sendMessage(ChatColor.YELLOW + message);

			if (reward.getItemMeta() != null && reward.getItemMeta().getEnchants() != null
					&& !reward.getItemMeta().getEnchants().isEmpty()) {
				message = plugin.getLocalizedMessage(LocalizedMessageEnum.ENCHANTMENTS.getKey(), player);
				player.sendMessage(ChatColor.GREEN + " " + message + ": ");
				reward.getItemMeta().getEnchants().keySet()
						.forEach(enchantment -> sendPlayerEnchantMessage(enchantment, reward, player));
			}

			if (reward.getItemMeta() != null && reward.getItemMeta().getLore() != null
					&& !reward.getItemMeta().getLore().isEmpty()) {
				message = plugin.getLocalizedMessage(LocalizedMessageEnum.LORE.getKey(), player);
				player.sendMessage(ChatColor.GREEN + " " + message + ": ");
				reward.getItemMeta().getLore().forEach(lore -> player.sendMessage(ChatColor.BLUE + "  -" + lore));
			}
		}
	}

	private void sendPlayerEnchantMessage(Enchantment enchantment, ItemStack item, Player player) {
		String formatOfMessage = "  -{0}[{1}]";
		String message = new MessageFormat(formatOfMessage)
				.format(new Object[] { enchantment.getName(), item.getItemMeta().getEnchants().get(enchantment) });
		player.sendMessage(ChatColor.GOLD + "  -" + message);
	}

	private BountySign getBountySign(Sign sign) {
		plugin.initDimentionItemCount();

		if (plugin.getDimentionItemCount() == null
				|| plugin.getDimentionItemCount().getItemsInDimentionAtValue() == null
				|| plugin.getDimentionItemCount().getItemsInDimentionAtValue().isEmpty()) {
			plugin.debugInfo("no dimention values recorded");
			return null;
		}

		List<String> exactMatches = plugin.getClickedBlockHelper().getMatchesInDimentionItemCount(
				plugin.getDimentionItemCount(), sign.getWorld().getName(), sign.getX(), sign.getY(), sign.getZ());

		if (exactMatches == null || exactMatches.isEmpty()) {
			plugin.debugWarning("No match was found but we made it all the way through processing");
			return null;
		}

		int index = 0;
		if (exactMatches.size() > 1) {
			plugin.debugInfo("There was more than one match found after processing.");
			index = random.nextInt(exactMatches.size());
		}

		String match = exactMatches.get(index);

		plugin.initBountySignRepo();

		if (plugin.getBountySignRepo() == null) {
			plugin.debugWarning("Failed to load bounty-sign repo.");
			return null;
		}

		if (!plugin.getBountySignRepo().getMap().containsKey(match)) {
			plugin.debugWarning("Failed to load sign from bounty-sign repo.");
			return null;
		}

		BountySign bountySign = plugin.getBountySignRepo().getMap().get(match);
		if (bountySign == null) {
			plugin.debugWarning("Failed to load sign from bounty-sign repo.");
			return null;
		}

		return bountySign;
	}

	private void createBountySign(Player player, Sign sign, String id) {
		ItemStack reward = plugin.getPlayerLastBountyRewardCreatedMap().get(player.getName());
		String targetName = plugin.getPlayerLastBountyTargetCreatedMap().get(player.getName());

		sign.setLine(0, ChatColor.RED + (plugin.isKillTarget() ? "Kill" : "Wanted"));
		sign.setLine(1, ChatColor.RED + targetName);
		sign.setLine(2, ChatColor.DARK_GREEN + "Reward");
		if (sign.update()) {
			BountySign bountySign = new BountySign();
			bountySign.setX(sign.getX());
			bountySign.setY(sign.getY());
			bountySign.setZ(sign.getZ());
			bountySign.setTarget(targetName);
			bountySign.setKillTarget(plugin.isKillTarget());

			bountySign.setReward(new CardboardBox(reward));

			bountySign.setWorld(sign.getWorld().getName());
			bountySign.setId(id == null ? UUID.randomUUID().toString() : id);
			plugin.addBountySign(bountySign);
			String message = plugin.getLocalizedMessage(LocalizedMessageEnum.SIGN_CREATED.getKey(), player);
			player.sendMessage(ChatColor.GREEN + message + ". [" + (ChatColor.YELLOW + bountySign.getId())
					+ ChatColor.GREEN + "]");
			plugin.getPlayerLastBountyRewardCreatedMap().put(player.getName(), null);
		} else {
			String message = plugin.getLocalizedMessage(LocalizedMessageEnum.UNKNOWN_ISSUE_CREATING_SIGN.getKey(),
					player);
			player.sendMessage(ChatColor.RED + message);
		}
	}

}
