package com.wrlus.jadx.plugins;


import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;

public class PluginMain implements JadxPlugin {
	public static final String PLUGIN_ID = "wrlu-jadx-plugin";

	private final DefaultOptions options = new DefaultOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "wrlu-jadx-plugin", "Jadx plugin of wrlu.");
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		if (options.isEnable()) {
			context.addPass(new AddCommentPass());
		}
	}
}
