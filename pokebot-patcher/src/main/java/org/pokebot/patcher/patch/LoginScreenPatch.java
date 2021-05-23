package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.pokebot.api.screen.LoadingScreen;
import org.pokebot.api.screen.LoginScreen;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.util.ByteCodeUtil;

@Slf4j
public final class LoginScreenPatch implements PoolModifier
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
			if (!cf.implementsInterface(LoadingScreen.class))
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
								ByteCodeUtil.addInterface(classPool.findClass(lastInst), LoginScreen.class);
							}
						}
					}
					else if (af instanceof FieldInsnNode)
					{
						val fin = (FieldInsnNode) af;

						if (!fin.owner.equals(cf.name))
						{
							lastInst = fin.owner;
						}
					}
				}
			}
		}
	}
}
