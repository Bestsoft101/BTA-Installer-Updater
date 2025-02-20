package net.minecraft.client;

import java.io.File;

import b100.installer.updater.Updater;

public class Minecraft {
	
	/** This field gets set to the instance directory by MultiMC */
	private static File multiMcDirectory;
	
	public static File getMultiMcDirectory() {
		return multiMcDirectory;
	}
	
	public static void main(String[] args) {
		Updater.main(args);
	}
	
}
