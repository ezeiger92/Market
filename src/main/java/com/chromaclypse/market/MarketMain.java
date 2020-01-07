package com.chromaclypse.market;

import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import com.chromaclypse.api.command.CommandBase;

public class MarketMain extends JavaPlugin {
	private Market impl;

	@Override
	public void onEnable() {
		impl = new Market(this);
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
		
		getServer().getPluginManager().registerEvents(new TransactionListener(impl), this);
	}
	
	@Override
	public void onDisable() {
		impl = null;
	}
}
