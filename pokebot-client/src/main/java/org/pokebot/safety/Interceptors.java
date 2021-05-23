package org.pokebot.safety;

import java.util.List;

public final class Interceptors
{
	private static String macAddress = null;

	public static List<String> interceptProcesses(final List<String> incomingList)
	{
		System.out.println("Someone tried to access your processes, but it got spoofed instead.");

		return Providers.processesProvider();
	}

	public static List<String> interceptProgramFiles(final List<String> incomingList)
	{
		System.out.println("Someone tried to access your program files, but it got spoofed instead.");

		return Providers.programFilesProvider();
	}

	public static String interceptMac(final String incomingMac)
	{
		if (macAddress == null)
		{
			macAddress = Providers.macAddressProvider();
		}

		System.out.println("Spoofed MAC-address to: " + macAddress);

		return macAddress;
	}
}
