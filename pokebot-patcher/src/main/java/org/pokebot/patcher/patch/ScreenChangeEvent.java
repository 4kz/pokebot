package org.pokebot.patcher.patch;

import lombok.val;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;

import static org.objectweb.asm.Opcodes.*;

/**
 * Post an EventBus event everytime the setScreen method is called.
 */
public final class ScreenChangeEvent implements PoolModifier
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
			if (!cf.inObfuscatedPackage())
			{
				continue;
			}

			for (val mf : cf.methods)
			{
				for (val af : mf.instructions)
				{
					if (af instanceof MethodInsnNode)
					{
						val min = (MethodInsnNode) af;
						if (min.name.equals("setScreen"))
						{
							var it = (AbstractInsnNode) min;
							while ((it = it.getNext()) != null)
							{
								if (it instanceof LabelNode && !mf.name.equals("create"))
								{
									mf.instructions.insert(it, eventBusInsn());
									break;
								}
								// Exception for the 'create' method where Pokébot is not initiated yet.
								else if (mf.name.equals("create") && it instanceof MethodInsnNode)
								{
									val innerMin = (MethodInsnNode) it;
									if (innerMin.name.equals("init"))
									{
										mf.instructions.insert(it, eventBusInsn());
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private InsnList eventBusInsn()
	{
		val il = new InsnList();

		Label label0 = new Label();
		il.add(new LabelNode(label0));
		il.add(new MethodInsnNode(INVOKESTATIC, "org/pokebot/Pokebot", "getInjector", "()Lcom/google/inject/Injector;", false));
		il.add(new LdcInsnNode(Type.getType("Lcom/google/common/eventbus/EventBus;")));
		il.add(new MethodInsnNode(INVOKEINTERFACE, "com/google/inject/Injector", "getInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", true));
		il.add(new TypeInsnNode(CHECKCAST, "com/google/common/eventbus/EventBus"));
		il.add(new TypeInsnNode(NEW, "org/pokebot/api/event/GameScreenChanged"));
		il.add(new InsnNode(DUP));
		il.add(new MethodInsnNode(INVOKESTATIC, "org/pokebot/Pokebot", "getInjector", "()Lcom/google/inject/Injector;", false));
		il.add(new LdcInsnNode(Type.getType("Lorg/pokebot/api/Client;")));
		il.add(new MethodInsnNode(INVOKEINTERFACE, "com/google/inject/Injector", "getInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", true));
		il.add(new TypeInsnNode(CHECKCAST, "org/pokebot/api/Client"));
		il.add(new MethodInsnNode(INVOKEINTERFACE, "org/pokebot/api/Client", "getCurrentScreen", "()Lorg/pokebot/api/screen/Screen;", true));
		il.add(new MethodInsnNode(INVOKESPECIAL, "org/pokebot/api/event/GameScreenChanged", "<init>", "(Lorg/pokebot/api/screen/Screen;)V", false));
		il.add(new MethodInsnNode(INVOKEVIRTUAL, "com/google/common/eventbus/EventBus", "post", "(Ljava/lang/Object;)V", false));

		return il;
	}
}
