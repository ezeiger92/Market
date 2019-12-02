package com.chromaclypse.market;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class MarketCommand implements TabExecutor {
	private static final List<String> subcommands = Arrays.asList("create", "remove", "info", "reload");
	private static final List<String> emptyList = Arrays.asList();
	
	private final Market handle;
	
	public MarketCommand(Market handle) {
		this.handle = handle;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		switch (args.length) {
		case 0:
			return subcommands;

		case 1:
			return subcommands.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());

		default:
			return emptyList;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length > 0) {
			String arg = args[0].toLowerCase(Locale.ENGLISH);
			
			Player player = sender instanceof Player ? (Player)sender : null;
			
			switch(arg) {
			case "create": {
				Block target = player.getTargetBlock(null, 8);
				handle.createCheckout(target.getLocation());
				break;
			}
				
			case "remove": {
				Block target = player.getTargetBlock(null, 8);
				handle.removeCheckout(target.getLocation());
				break;
			}
				
			case "info":
				Block target = player.getTargetBlock(null, 8);
				Checkout found = handle.getCheckout(target.getLocation());
				
				if(found != null) {
					sender.sendMessage("Checkout sales: " + found.salesToPlayer + "; purchases: " + found.purchasesFromPlayer); 
				}
				else {
					sender.sendMessage("Not a checkout");
				}
				break;
				
			case "reload":
				handle.getPlugin().onDisable();
				handle.getPlugin().onEnable();
				break;
				
			default:
				break;
			}
			
			return true;
		}
		
		return false;
	}
}
