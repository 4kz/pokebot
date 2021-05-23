package org.pokebot.patcher;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.deob.KotlinPacketRefactor;
import org.pokebot.patcher.deob.SourceBasedClassRefactor;
import org.pokebot.patcher.patch.*;
import org.pokebot.patcher.util.JarResult;
import org.pokebot.patcher.util.JarUtil;

import java.io.File;
import java.io.IOException;

@Slf4j
public final class Patcher
{
	public static void main(final String[] args)
	{
		if (args.length < 2)
		{
			log.info("Please use the patcher with the arguments: [input jar] [output jar].");
			return;
		}

		long startTime = System.currentTimeMillis();

		val inFile = new File(args[0]);
		val outFile = new File(args[1]);

		if (!inFile.exists())
		{
			log.info("That specified input jar does not exist.");
			return;
		}

		JarResult jarResult;
		try
		{
			jarResult = JarUtil.loadJar(inFile, Constants.PACKAGE_PREFIX);
		}
		catch (IOException e)
		{
			log.error("Error occurred whilst loading the specified jar file.", e);
			return;
		}

		var cp = jarResult.getPackagePool();
		val depPool = jarResult.getDepPool();

		cp = deobfuscate(cp);
		cp = patch(cp);

		try
		{
			JarUtil.saveJar(outFile, new JarResult(cp, depPool));
		}
		catch (IOException e)
		{
			log.error("Error occurred whilst saving the resulting jar.", e);
		}

		val runTime = System.currentTimeMillis() - startTime;

		log.info("The PBO jar has been patched in " + runTime + " ms.");
	}

	private static ClassPool deobfuscate(ClassPool cp)
	{
		if (Constants.DEBUG)
		{
			cp = new KotlinPacketRefactor().run(cp);
			cp = new SourceBasedClassRefactor().run(cp);
		}

		return cp;
	}

	private static ClassPool patch(ClassPool cp)
	{
		/*
		 * API related patches.
		 */
		cp = new AntiBotPatch().run(cp);
		cp = new ClientPatch().run(cp);
		cp = new InitialScreenPatch().run(cp);
		cp = new LoginScreenPatch().run(cp);
		cp = new MainGameScreenPatch().run(cp);
		cp = new PreMainGamePatch().run(cp);
		cp = new MapManagerPatch().run(cp);
		cp = new BattlePatch().run(cp);

		/*
		 * Event related patches.
		 */
		cp = new ScreenChangeEvent().run(cp);

		return cp;
	}
}
