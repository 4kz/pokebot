package org.pokebot.patcher.patch;

import lombok.val;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.pokebot.api.Client;
import org.pokebot.api.screen.Screen;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.util.ByteCodeUtil;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

public final class ClientPatch implements PoolModifier
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
			if (cf.superName != null && cf.superName.equals("com/badlogic/gdx/Game")
					&& cf.visibleAnnotations.stream().anyMatch(it -> it.desc.equals("Ljavax/inject/Singleton;")))
			{
				ByteCodeUtil.addInterface(cf, Client.class);
				ByteCodeUtil.addCastGetter(cf, "getScreen", "()Lcom/badlogic/gdx/Screen;", "currentScreen", Screen.class);

				/*
				 * Add a method that starts the Pokébot module after the screen is set.
				 */
				searchMethod:
				for (val mf : cf.methods)
				{
					if (!(mf.name.equals("create") && mf.desc.equals("()V")))
					{
						continue;
					}

					for (val af : mf.instructions)
					{
						if (af.getType() == METHOD_INSN)
						{
							val min = (MethodInsnNode) af;

							if (min.name.equals("setScreen"))
							{
								mf.instructions.insert(af, pokebotInitInsns());
								break searchMethod;
							}
						}
					}
				}
			}
		}
	}

	private InsnList pokebotInitInsns()
	{
		val insnList = new InsnList();

		Label label0 = new Label();
		insnList.add(new LabelNode(label0));
		insnList.add(new VarInsnNode(ALOAD, 0));
		insnList.add(new MethodInsnNode(INVOKESTATIC, "org/pokebot/Pokebot", "init", "(Lorg/pokebot/api/Client;)V", false));

		return insnList;
	}
}
