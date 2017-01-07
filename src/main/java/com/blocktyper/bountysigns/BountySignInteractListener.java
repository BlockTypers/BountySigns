package com.blocktyper.bountysigns;

import java.text.MessageFormat;
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
import com.blocktyper.plugin.IBlockTyperPlugin;
import com.blocktyper.serialization.CardboardBox;

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
					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					player.sendMessage(
							(ChatColor.YELLOW + "You have colected this bounty and can not repeat until ") + (ChatColor.RED + "["
									+ format1.format(cal.getTime()) + "]."));
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

		if (bountySign.getReward() != null) {
			ItemStack reward = bountySign.getReward().unbox();
			sendPlayerRewardMessage(reward, player, bountySign.getTarget());
		}

	}
	
	private void sendPlayerRewardMessage(ItemStack reward, Player player, String target){
		if (reward != null) {
			
			player.sendMessage((ChatColor.GREEN + "Mission accepted. Find '") + (ChatColor.RED + target) + ChatColor.GREEN + "'.");
			
			player.sendMessage(
					ChatColor.YELLOW + "You will be rewarded: " + (ChatColor.GREEN + bountySignsPlugin.getRewardDescription(reward)));

			if (reward.getItemMeta() != null && reward.getItemMeta().getEnchants() != null
					&& !reward.getItemMeta().getEnchants().isEmpty()) {
				player.sendMessage(ChatColor.GREEN + " Enchanments: ");
				reward.getItemMeta().getEnchants().keySet().forEach(enchantment -> sendPlayerEnchantMessage(enchantment, reward, player) );
			}

			if (reward.getItemMeta() != null && reward.getItemMeta().getLore() != null
					&& !reward.getItemMeta().getLore().isEmpty()) {
				player.sendMessage(ChatColor.GREEN + " Lore: ");
				reward.getItemMeta().getLore().forEach(lore -> player.sendMessage(ChatColor.BLUE + "  -" + lore) );
			}
		}
	}
	
	private void sendPlayerEnchantMessage(Enchantment enchantment, ItemStack item, Player player){
		String formatOfMessage = "  -{0}[{1}]";
		String message = new MessageFormat(formatOfMessage).format(new Object[]{enchantment.getName(), item.getItemMeta().getEnchants().get(enchantment)});
		player.sendMessage(ChatColor.GOLD + "  -" + message);
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
			plugin.debugInfo("There was more than one match found after processing.");
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

		sign.setLine(0, ChatColor.RED + (bountySignsPlugin.isKillTarget() ? "Kill" : "Wanted"));
		sign.setLine(1, ChatColor.RED + targetName);
		sign.setLine(2, ChatColor.DARK_GREEN + "Reward");
		if (sign.update()) {
			BountySign bountySign = new BountySign();
			bountySign.setX(sign.getX());
			bountySign.setY(sign.getY());
			bountySign.setZ(sign.getZ());
			bountySign.setTarget(targetName);
			bountySign.setKillTarget(bountySignsPlugin.isKillTarget());

			bountySign.setReward(new CardboardBox(reward));

			bountySign.setWorld(sign.getWorld().getName());
			bountySign.setId(id == null ? UUID.randomUUID().toString() : id);
			bountySignsPlugin.addBountySign(bountySign);
			player.sendMessage(ChatColor.GREEN + "Bounty sign created. [" + (ChatColor.YELLOW + bountySign.getId()) + ChatColor.GREEN + "]");
			bountySignsPlugin.getPlayerLastBountyRewardCreatedMap().put(player.getName(), null);
		} else {
			player.sendMessage(ChatColor.RED + "Unknown issue creating bounty sign created");
		}
	}
	
}
