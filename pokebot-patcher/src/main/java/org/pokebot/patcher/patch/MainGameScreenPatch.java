package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.pokebot.api.screen.LoginScreen;
import org.pokebot.api.screen.MainGameScreen;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.util.ByteCodeUtil;

import static org.objectweb.asm.Opcodes.NEW;

@Slf4j
public final class MainGameScreenPatch implements PoolModifier
{
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
			if (!cf.implementsInterface(LoginScreen.class))
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
								log.error("Unable to identify the last field instruction.");
							}
							else
							{
								ByteCodeUtil.addInterface(classPool.findClass(lastInst), MainGameScreen.class);
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
}
