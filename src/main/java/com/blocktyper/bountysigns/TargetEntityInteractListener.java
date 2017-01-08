package com.blocktyper.bountysigns;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.bountysigns.data.AcceptedBounty;
import com.blocktyper.bountysigns.data.BountySign;

public class TargetEntityInteractListener implements Listener {

	private BountySignsPlugin plugin;

	public TargetEntityInteractListener(BountySignsPlugin plugin) {
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerDamageNPC(EntityDamageByEntityEvent event) {

		if (!(event.getDamager() instanceof Player))
			return;

		plugin.debugInfo("onPlayerDamageNPC");

		if (event.getEntity() instanceof Damageable) {

		}

		Damageable damageable = (Damageable) event.getEntity();
		Double remainingHealth = damageable.getHealth();

		if (event.getFinalDamage() >= damageable.getHealth()) {
			remainingHealth = 0.0;
		} else {
			remainingHealth = damageable.getHealth() - event.getFinalDamage();
		}

		onPlayerClickOrDamageNPC(event, event.getEntity(), (Player) event.getDamager(), true, remainingHealth);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerClickNPC(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		Entity targetEntity = plugin.getPlayerHelper().getTargetEntity(player);

		if (targetEntity != null) {
			plugin.debugInfo("onPlayerClickNPC");
			onPlayerClickOrDamageNPC(event, targetEntity, player, false, null);
		}
	}

	private void onPlayerClickOrDamageNPC(Event event, Entity targetEntity, Player player, boolean isDamage,
			Double remainingHealth) {
		if (targetEntity == null || player == null) {
			return;
		}

		String targetName = targetEntity.getCustomName() != null ? targetEntity.getCustomName()
				: targetEntity.getName();

		if (targetName == null) {
			return;
		}

		plugin.initAcceptedBountyRepo();

		if (!plugin.getAcceptedBountyRepo().getMap().containsKey(targetName)
				|| plugin.getAcceptedBountyRepo().getMap().get(targetName) == null
				|| plugin.getAcceptedBountyRepo().getMap().get(targetName).isEmpty()) {
			plugin.debugInfo("target not recognized as bounty");
			return;
		}

		List<AcceptedBounty> acceptedBounties = plugin.getAcceptedBountyRepo().getMap().get(targetName).parallelStream()
				.filter(acceptedBounty -> acceptedBounty != null && acceptedBounty.getPlayer().equals(player.getName()))
				.collect(Collectors.toList());

		if (acceptedBounties == null || acceptedBounties.isEmpty()) {
			plugin.debugInfo("No bounties for this player and NPC combination.");
			return;
		}

		plugin.debugInfo("dropRewardsForBounties");
		dropRewardsForBounties(player, acceptedBounties, remainingHealth);
	}

	private void dropRewardsForBounties(Player player, List<AcceptedBounty> acceptedBounties, Double remainingHealth) {
		if (acceptedBounties == null || player == null)
			return;

		int bountiesCollected = 0;
		int bountiesAlreadyCompleted = 0;

		for (AcceptedBounty acceptedBounty : acceptedBounties) {

			if (acceptedBounty == null) {
				plugin.warning("NUll acceptedBounty found while dropping rewards.");
				continue;
			}

			try {
				if (acceptedBounty.getCompletedDate() != null) {
					bountiesAlreadyCompleted++;
					continue;
				}

				BountySign bountySign = plugin.getBountySignRepo().getMap().get(acceptedBounty.getBountySignId());

				if (bountySign == null) {
					plugin.warning(
							"Bounty sign not found while dropping rewards. ID: " + acceptedBounty.getBountySignId());
					continue;
				}

				if (bountySign.isKillTarget() && (remainingHealth == null || remainingHealth > 0)) {
					if (remainingHealth == null) {
						String message = plugin
								.getLocalizedMessage(LocalizedMessageEnum.PLAYER_MUST_DAMAGE_TARGET.getKey(), player);
						player.sendMessage(ChatColor.RED + message);
					} else {
						String message = plugin.getLocalizedMessage(LocalizedMessageEnum.REMAINING_HEALTH.getKey(),
								player);
						player.sendMessage(ChatColor.RED + message + ": " + ChatColor.GREEN + +remainingHealth);
					}
					continue;
				}

				if (dropRewardForBounty(player, acceptedBounty))
					bountiesCollected++;
			} catch (Exception e) {
				plugin.warning("Error while dropping rewards: " + e.getMessage());
			}

		}

		if (bountiesCollected > 0) {
			LocalizedMessageEnum localizedMessageEnum = bountiesCollected > 1 ? LocalizedMessageEnum.BOUNTIES_COLLECTED
					: LocalizedMessageEnum.BOUNTY_COLLECTED;
			String message = plugin.getLocalizedMessage(localizedMessageEnum.getKey(), player);
			player.sendMessage(ChatColor.GREEN + (bountiesCollected + " " + message));
		} else if (bountiesAlreadyCompleted > 0) {
			String message = plugin.getLocalizedMessage(LocalizedMessageEnum.BOUNTY_ALREADY_COLLECTED.getKey(), player);
			player.sendMessage(ChatColor.YELLOW + message);
		}
	}

	private boolean dropRewardForBounty(Player player, AcceptedBounty acceptedBounty) {
		if (acceptedBounty == null || player == null)
			return false;

		ItemStack reward = acceptedBounty.getReward().unbox();

		if (reward == null) {
			plugin.warning("NUll reward found while dropping rewards.");
			return false;
		}

		plugin.debugInfo("REWARD DROPPED: " + reward.getType().name());
		String message = plugin.getLocalizedMessage(LocalizedMessageEnum.REWARD.getKey(), player);
		player.sendMessage(ChatColor.GREEN + message + ": " + ChatColor.GOLD + plugin.getRewardDescription(reward));
		player.getWorld().dropItem(player.getLocation(), reward);
		acceptedBounty.setCompletedDate(new Date());
		plugin.addAcceptedBounty(acceptedBounty);

		return true;
	}

}
