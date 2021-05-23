package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.tree.*;
import org.pokebot.api.Client;
import org.pokebot.api.battle.BattleController;
import org.pokebot.api.battle.BattleDialog;
import org.pokebot.api.battle.BattleManager;
import org.pokebot.api.battle.ClientNetworkMessageHandler;
import org.pokebot.api.screen.MainGameScreen;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.util.ByteCodeUtil;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

@Slf4j
public final class BattlePatch implements PoolModifier
{
	private ClassPool classPool;

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		this.classPool = classPool;

		patchBattleManager();
		patchClientNetworkMessageHandler();
		patchBattleController();

		return this.classPool;
	}

	private void patchBattleController()
	{
		ClassFile battleController = null;

		outerSearch:
		for (val cf : classPool)
		{
			for (val mf : cf.methods)
			{
				for (val af : mf.instructions)
				{
					if (af.getType() != LDC_INSN)
					{
						continue;
					}

					val lin = (LdcInsnNode) af;
					if (lin.cst.equals(" was successfully caught!"))
					{
						battleController = cf;
						ByteCodeUtil.addInterface(cf, BattleController.class);
						break outerSearch;
					}
				}
			}
		}

		val battleManager = classPool.findClassByInterface(BattleManager.class);

		if (battleManager == null || battleController == null)
		{
			log.error("Unable to find the BattleManager or BattleController class.");
			return;
		}

		for (val ff : battleManager.fields)
		{
			if (ff.desc.equals("L" + battleController.name + ";"))
			{
				ByteCodeUtil.addGetter(battleManager, ff, "battleController", BattleController.class);
				break;
			}
		}

		ClassFile battleDialog = null;

		outerLoop:
		for (val cf : classPool)
		{
			for (val mf : cf.methods)
			{
				if (mf.desc.equals("(Lcom/pbo/game/client/c/a/b/Bag;Lcom/badlogic/gdx/utils/Array;Lcom/pbo/game/client/battle/FormChangingUIBattlerKt;Lcom/pbo/game/client/battle/UIBattlerKt;Lcom/pbo/game/client/battle/BattleController;Lcom/badlogic/gdx/graphics/Texture;ZZ)V")
						|| mf.desc.equals("(Lcom/pbo/game/client/c/a/b/Bag;Lcom/badlogic/gdx/utils/Array;Lcom/pbo/game/client/battle/FormChangingUIBattlerKt;Lcom/pbo/game/client/battle/UIBattlerKt;Lcom/pbo/game/client/battle/BattleController;IILcom/badlogic/gdx/graphics/Texture;)V"))
				{
					battleDialog = cf;
					ByteCodeUtil.addInterface(cf, BattleDialog.class);
					break outerLoop;
				}
			}
		}

		if (battleDialog == null)
		{
			log.error("Unable to fin the BattleDialog class.");
			return;
		}

		for (val ff : battleController.fields)
		{
			if (ff.desc.equals("L" + battleDialog.name + ";"))
			{
				ByteCodeUtil.addGetter(battleController, ff, "battleDialog", BattleDialog.class);
				break;
			}
		}
	}

	private void patchClientNetworkMessageHandler()
	{
		val battleManager = classPool.findClassByInterface(BattleManager.class);

		if (battleManager == null)
		{
			log.error("Unable to find the BattleManager class.");
			return;
		}

		ClassFile networkClass = null;

		searchLoop:
		for (val cf : classPool)
		{
			for (val mf : cf.methods)
			{
				for (val af : mf.instructions)
				{
					if (af.getOpcode() == NEW)
					{
						val tin = (TypeInsnNode) af;

						if (tin.desc.equals(battleManager.name))
						{
							networkClass = cf;
							ByteCodeUtil.addInterface(cf, ClientNetworkMessageHandler.class);
							break searchLoop;
						}
					}
				}
			}
		}

		val mainScreen = classPool.findClassByInterface(MainGameScreen.class);

		if (mainScreen == null || networkClass == null)
		{
			log.error("Unable to find the MainScreen class or the ClientNetworkMessageHandler class.");
			return;
		}

		for (val ff : networkClass.fields)
		{
			if (ff.desc.equals("L" + battleManager.name + ";"))
			{
				ByteCodeUtil.addGetter(networkClass, ff, "battleManager", BattleManager.class);
				break;
			}
		}

		for (val ff : mainScreen.fields)
		{
			if (ff.desc.equals("L" + networkClass.name + ";"))
			{
				ByteCodeUtil.addGetter(mainScreen, ff, "networkHandler", ClientNetworkMessageHandler.class);
				ByteCodeUtil.forwardToClient(classPool.findClassByInterface(Client.class), "networkHandler", ClientNetworkMessageHandler.class);
				break;
			}
		}
	}

	private void patchBattleManager()
	{
		searchLoop:
		for (val cf : classPool)
		{
			for (val mf : cf.methods)
			{
				for (val af : mf.instructions)
				{
					if (af.getType() != LDC_INSN)
					{
						continue;
					}

					val lin = (LdcInsnNode) af;

					if (lin.cst.equals("unrecognized_message")
							|| lin.cst.equals("%s HP %d and Server HP %d different. Report to admin to help debugging."))
					{
						ByteCodeUtil.addInterface(cf, BattleManager.class);
						break searchLoop;
					}
				}
			}
		}
	}

	private static InsnList printStack()
	{
		val insnList = new InsnList();

		insnList.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
		insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false));
		insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false));
		insnList.add(new MethodInsnNode(INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false));
		insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));

		return insnList;
	}
}
