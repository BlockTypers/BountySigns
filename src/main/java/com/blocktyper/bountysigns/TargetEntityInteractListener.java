package com.blocktyper.bountysigns;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.bountysigns.data.AcceptedBounty;
import com.blocktyper.plugin.IBlockTyperPlugin;

import listeners.AbstractListener;

public class TargetEntityInteractListener extends AbstractListener {

	private BountySignsPlugin bountySignsPlugin;

	public TargetEntityInteractListener(IBlockTyperPlugin plugin) {
		super(plugin);
		bountySignsPlugin = (BountySignsPlugin) plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerClickNPC(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		Entity targetEntity = plugin.getPlayerHelper().getTargetEntity(player);

		if (targetEntity == null) {
			return;
		}

		String targetName = targetEntity.getCustomName() != null ? targetEntity.getCustomName()
				: targetEntity.getName();

		if (targetName == null) {
			return;
		}

		bountySignsPlugin.initAcceptedBountyRepo();

		if (!bountySignsPlugin.getAcceptedBountyRepo().getMap().containsKey(targetName)
				|| bountySignsPlugin.getAcceptedBountyRepo().getMap().get(targetName) == null
				|| bountySignsPlugin.getAcceptedBountyRepo().getMap().get(targetName).isEmpty())
			return;

		int bountiesCollected = 0;
		int bountiesAlreadyCompleted = 0;

		List<AcceptedBounty> acceptedBounties = new ArrayList<AcceptedBounty>();

		for (AcceptedBounty acceptedBounty : bountySignsPlugin.getAcceptedBountyRepo().getMap().get(targetName)) {
			try {
				if (acceptedBounty == null) {
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

				if (reward == null) {
					plugin.warning("NUll reward found while dropping rewards.");
					continue;
				}

				plugin.debugInfo("REWARD DROPPED: " + reward.getType().name());
				player.sendMessage(ChatColor.GREEN + "Reward: " + bountySignsPlugin.getRewardDescription(reward));
				player.getWorld().dropItem(player.getLocation(), reward);
				bountiesCollected++;
				acceptedBounty.setCompletedDate(new Date());
				bountySignsPlugin.addAcceptedBounty(acceptedBounty);
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

}
