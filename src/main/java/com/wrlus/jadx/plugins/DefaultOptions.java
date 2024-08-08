package com.wrlus.jadx.plugins;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class DefaultOptions extends BasePluginOptionsBuilder {

	private boolean enable;

	@Override
	public void registerOptions() {
		boolOption(PluginMain.PLUGIN_ID + ".enable")
				.description("Enable Jadx plugin of wrlu.")
				.defaultValue(true)
				.setter(v -> enable = v);
	}

	public boolean isEnable() {
		return enable;
	}
}
