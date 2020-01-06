package com.chromaclypse.market;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.chromaclypse.api.command.CommandBase;

public class MarketMain extends JavaPlugin {
	private Market impl;
	private int task_id;

	@Override
	public void onEnable() {
		impl = new Market(this);
		new TransactionListener(impl);
		MarketCommand commandImpl = new MarketCommand(impl);
		
		TabExecutor t = new CommandBase()
			.with().arg("version").calls(CommandBase::pluginVersion)
			.with().arg("reload").calls(commandImpl::reload)
			.with().arg("create").calls(commandImpl::create)
			.with().arg("remove").calls(commandImpl::remove)
			.with().arg("info").calls(commandImpl::info)
			.getCommand();

		getCommand("market").setExecutor(t);
		getCommand("market").setTabCompleter(t);
		
		long hour = 20 * 60 * 60;
		task_id = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			
			for(Map.Entry<Location, Checkout> entry : impl.getCheckouts().entrySet()) {
				impl.updatePrice(entry.getKey(), entry.getValue());
			}
			impl.save();
		}, hour, hour);
		
		getServer().getPluginManager().registerEvents(new TransactionListener(impl), this);
	}
	
	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
		getServer().getScheduler().cancelTask(task_id);
		impl = null;
	}
}
