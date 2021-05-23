package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.pokebot.api.Client;
import org.pokebot.api.screen.LoadingScreen;
import org.pokebot.api.screen.ShutdownScreen;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.asm.wrappers.MethodFile;
import org.pokebot.patcher.util.ByteCodeUtil;

import static org.objectweb.asm.Opcodes.NEW;

/**
 * Finds and patches the ShutdownScreen and LoadingScreen.
 */
@Slf4j
public final class InitialScreenPatch implements PoolModifier
{
	private static final String GAME_ANALYTICS = "de/golfgl/gdxgameanalytics/GameAnalytics";

	private ClassPool classPool;

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		this.classPool = classPool;

		applyPatch();

		return this.classPool;
	}

	private void applyPatch()
	{
		for (val cf : classPool)
		{
			if (!cf.implementsInterface(Client.class))
			{
				continue;
			}

			for (val mf : cf.methods)
			{
				String lastInst = null;

				for (val af : mf.instructions)
				{
					if (af instanceof MethodInsnNode)
					{
						val min = (MethodInsnNode) af;
						if (min.name.equals("setScreen"))
						{
							if (lastInst == null)
							{
								log.error("Unable to identify the last NEW instruction.");
							}
							else if (isLoadingScreen(mf))
							{
								ByteCodeUtil.addInterface(classPool.findClass(lastInst), LoadingScreen.class);
							}
							else
							{
								ByteCodeUtil.addInterface(classPool.findClass(lastInst), ShutdownScreen.class);
							}
						}
					}
					else if (af instanceof TypeInsnNode)
					{
						val tin = (TypeInsnNode) af;
						if (tin.getOpcode() == NEW)
						{
							lastInst = tin.desc;
						}
					}
				}
			}
		}
	}

	private boolean isLoadingScreen(final MethodFile mf)
	{
		for (val af : mf.instructions)
		{
			if (af instanceof FieldInsnNode)
			{
				val fin = (FieldInsnNode) af;
				if (fin.desc.equals("L" + GAME_ANALYTICS + ";"))
				{
					return true;
				}
			}
			else if (af instanceof MethodInsnNode)
			{
				val min  = (MethodInsnNode) af;
				if (min.owner.equals(GAME_ANALYTICS))
				{
					return true;
				}
			}
		}
		return false;
	}
}
