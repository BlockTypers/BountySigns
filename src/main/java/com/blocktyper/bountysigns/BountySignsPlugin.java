package com.blocktyper.bountysigns;

import java.util.ResourceBundle;

import com.blocktyper.plugin.BlockTyperPlugin;

public class BountySignsPlugin extends BlockTyperPlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.bountysigns.resources.BountySignsMessages";

	

	public void onEnable() {
		super.onEnable();
		getServer().getPluginManager().registerEvents(new BountySignsCommand(this), this);
	}

	// begin localization
	private ResourceBundle bundle = null;

	public ResourceBundle getBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(RESOURCE_NAME, locale);
		return bundle;
	}

	

	// end localization
}
