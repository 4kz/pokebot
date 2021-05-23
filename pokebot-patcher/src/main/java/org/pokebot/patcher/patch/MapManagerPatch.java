package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.pokebot.api.Client;
import org.pokebot.api.map.Dimension;
import org.pokebot.api.MapManager;
import org.pokebot.api.entity.*;
import org.pokebot.api.entityproperty.Position;
import org.pokebot.api.screen.MainGameScreen;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.asm.wrappers.MethodFile;
import org.pokebot.patcher.util.ByteCodeUtil;
import org.pokebot.patcher.util.Util;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

@Slf4j
public final class MapManagerPatch implements PoolModifier
{
	private ClassPool classPool;

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		this.classPool = classPool;

		addGetterToMainGame();
		addGetterToClient();
		addMapIdGetter();
		addEnemyGetter();
		findBasicPlayer();
		addLayerGetters();
		noClip();
		isBlocked();
		tileExists();
		addMapGetter();
		addDimensionGetter();

		return this.classPool;
	}

	private void addDimensionGetter()
	{
		val mapManager = classPool.findClassByInterface(MapManager.class);
		val name = Util.toSlash(Dimension.class.getName());

		if (mapManager == null)
		{
			return;
		}
		
		val mv = mapManager.visitMethod(ACC_PUBLIC, "getDimension", "(Ljava/lang/Object;)L" + name + ";", null, null);
		mv.visitCode();

		Label label0 = new Label();
		mv.visitLabel(label0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, "com/badlogic/gdx/maps/tiled/TiledMap");
		mv.visitVarInsn(ASTORE, 2);

		Label label1 = new Label();
		mv.visitLabel(label1);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/tiled/TiledMap", "getProperties", "()Lcom/badlogic/gdx/maps/MapProperties;", false);
		mv.visitLdcInsn("width");
		mv.visitLdcInsn(Type.getType("Ljava/lang/Integer;"));
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/MapProperties", "get", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
		mv.visitVarInsn(ASTORE, 3);

		Label label2 = new Label();
		mv.visitLabel(label2);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/tiled/TiledMap", "getProperties", "()Lcom/badlogic/gdx/maps/MapProperties;", false);
		mv.visitLdcInsn("height");
		mv.visitLdcInsn(Type.getType("Ljava/lang/Integer;"));
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/MapProperties", "get", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
		mv.visitVarInsn(ASTORE, 4);

		Label label3 = new Label();
		mv.visitLabel(label3);
		mv.visitTypeInsn(NEW, name);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
		mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "(II)V", false);
		mv.visitInsn(ARETURN);

		Label label4 = new Label();
		mv.visitLabel(label4);
		mv.visitLocalVariable("this", "L" + mapManager.name + ";", null, label0, label4, 0);
		mv.visitLocalVariable("map", "Ljava/lang/Object;", null, label0, label4, 1);
		mv.visitLocalVariable("casted", "Lcom/badlogic/gdx/maps/tiled/TiledMap;", null, label1, label4, 2);
		mv.visitLocalVariable("width", "Ljava/lang/Integer;", null, label2, label4, 3);
		mv.visitLocalVariable("height", "Ljava/lang/Integer;", null, label3, label4, 4);
		mv.visitEnd();
	}
	
	private void addMapGetter()
	{
		val mapManager = classPool.findClassByInterface(MapManager.class);

		if (mapManager == null)
		{
			return;
		}

		var foundLoad = false;
		FieldInsnNode mapField = null;

		val mfFilter = mapManager.methods.stream().filter(it ->
		{
			for (val af : it.instructions)
			{
				if (af.getType() == METHOD_INSN)
				{
					val min = (MethodInsnNode) af;

					if (min.name.equals("getMapId"))
					{
						return true;
					}
				}
			}
			return false;
		}).collect(Collectors.toList());

		outerSearch:
		for (val mf : mfFilter)
		{
			for (val af : mf.instructions)
			{
				if (af.getOpcode() == PUTFIELD && foundLoad)
				{
					mapField = (FieldInsnNode) af;
					break outerSearch;
				}
				else if (af.getType() == METHOD_INSN)
				{
					val min = (MethodInsnNode) af;

					if (min.name.equals("load"))
					{
						foundLoad = true;
					}
				}
			}
		}

		if (mapField == null)
		{
			log.error("Unable to find the map field.");
			return;
		}

		ByteCodeUtil.addGetter(mapManager, mapField, "map", Object.class);
	}

	private void findBasicPlayer()
	{
		val player = classPool.findClassByInterface(Player.class);
		val npc = classPool.findClassByInterface(Npc.class);

		val playerInterfaces = ByteCodeUtil.findAllInterfaces(player);
		val npcInterfaces = ByteCodeUtil.findAllInterfaces(npc);

		String firstMatch = null;

		outerSearch:
		for (val playerInter : playerInterfaces)
		{
			for (val npcInter : npcInterfaces)
			{
				if (playerInter.equals(npcInter))
				{
					firstMatch = playerInter;
					break outerSearch;
				}
			}
		}

		if (firstMatch == null)
		{
			log.error("Unable to find the BasicPlayer class.");
			return;
		}

		val basicPlayer = classPool.findClass(firstMatch);
		ByteCodeUtil.addInterface(basicPlayer, BasicPlayer.class);

		String movable = null;
		MethodFile posMethod = null;
		/*
		 * Add interface to movable as well.
		 */
		for (val interName : basicPlayer.interfaces)
		{
			val inter = classPool.findClass(interName);

			if (inter == null)
			{
				continue;
			}

			for (val mf : inter.methods)
			{
				if (!mf.desc.endsWith("Lcom/badlogic/gdx/math/Vector2;"))
				{
					continue;
				}

				posMethod = mf;
				movable = interName;
				ByteCodeUtil.addInterface(inter, Movable.class);
				break;
			}
		}

		if (movable == null)
		{
			log.error("Unable to find the Movable interface.");
			return;
		}

		/*
		 * Add position getter to all classes that implement Movable.
		 */
		outerLoop:
		for (val cf : classPool)
		{
			if (cf.isInterface())
			{
				continue;
			}

			final String finalMovable = movable;
			if (ByteCodeUtil.findAllInterfaces(cf).stream().anyMatch(it -> it.equals(finalMovable)))
			{
				for (val mf : cf.methods)
				{
					if (mf.name.equals(posMethod.name)
							&& mf.desc.equals(posMethod.desc)
							&& !Modifier.isAbstract(mf.access))
					{
						addPositionGetter(cf, posMethod);
						continue outerLoop;
					}
				}
			}
		}
	}

	/**
	 * Adds a getter to the class by
	 * converting a Vector to a {@link Position}.
	 * This position will be returned.
	 *
	 * @param cf {@link ClassFile} to add the the getter to.
	 * @param mf {@link MethodFile} that belongs to the vector getter.
	 */
	private void addPositionGetter(final ClassFile cf, final MethodFile mf)
	{
		val className = Util.toSlash(Position.class.getName());
		val desc = Type.getConstructorDescriptor(Arrays.stream(Position.class.getConstructors()).findFirst().get());

		val mv = cf.visitMethod(ACC_PUBLIC, "getPosition", "()L" + className + ";", null, null);
		mv.visitCode();

		Label label0 = new Label();
		mv.visitLabel(label0);
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, cf.name, mf.name, mf.desc, false);
		mv.visitFieldInsn(GETFIELD, "com/badlogic/gdx/math/Vector2", "x", "F");
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, cf.name, mf.name, mf.desc, false);
		mv.visitFieldInsn(GETFIELD, "com/badlogic/gdx/math/Vector2", "y", "F");
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", desc, false);
		mv.visitInsn(ARETURN);

		Label label1 = new Label();
		mv.visitLabel(label1);
		mv.visitLocalVariable("this", "L" + cf.name + ";", null, label0, label1, 0);
		mv.visitEnd();
	}

	private void tileExists()
	{
		val cf = classPool.findClassByInterface(MapManager.class);
		assert cf != null;

		val mv = cf.visitMethod(ACC_PUBLIC, "tileExists", "(Ljava/lang/Object;II)Z", null, null);
		mv.visitCode();
		
		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Exception");
		mv.visitLabel(label0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, "com/badlogic/gdx/maps/tiled/TiledMapTileLayer");
		mv.visitVarInsn(ILOAD, 2);
		mv.visitVarInsn(ILOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/tiled/TiledMapTileLayer", "getCell", "(II)Lcom/badlogic/gdx/maps/tiled/TiledMapTileLayer$Cell;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/tiled/TiledMapTileLayer$Cell", "getTile", "()Lcom/badlogic/gdx/maps/tiled/TiledMapTile;", false);

		Label label3 = new Label();
		mv.visitJumpInsn(IFNULL, label3);
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO, label1);
		mv.visitLabel(label3);
		mv.visitFrame(F_SAME, 0, null, 0, null);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(label1);
		mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{INTEGER});
		mv.visitInsn(IRETURN);
		mv.visitLabel(label2);

		mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
		mv.visitVarInsn(ASTORE, 4);

		Label label4 = new Label();
		mv.visitLabel(label4);
		mv.visitInsn(ICONST_0);
		mv.visitInsn(IRETURN);

		Label label5 = new Label();
		mv.visitLabel(label5);
		mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, label4, label5, 4);
		mv.visitLocalVariable("this", "L" + cf.name + ";", null, label0, label5, 0);
		mv.visitLocalVariable("layer", "Ljava/lang/Object;", null, label0, label5, 1);
		mv.visitLocalVariable("xTile", "I", null, label0, label5, 2);
		mv.visitLocalVariable("yTile", "I", null, label0, label5, 3);
		mv.visitEnd();
	}

	private void isBlocked()
	{
		val cf = classPool.findClassByInterface(MapManager.class);
		assert cf != null;

		val mv = cf.visitMethod(ACC_PUBLIC, "isBlocked", "(Ljava/lang/Object;II)Z", null, null);
		mv.visitCode();
		
		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Exception");
		mv.visitLabel(label0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, "com/badlogic/gdx/maps/tiled/TiledMapTileLayer");
		mv.visitVarInsn(ILOAD, 2);
		mv.visitVarInsn(ILOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/tiled/TiledMapTileLayer", "getCell", "(II)Lcom/badlogic/gdx/maps/tiled/TiledMapTileLayer$Cell;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/tiled/TiledMapTileLayer$Cell", "getTile", "()Lcom/badlogic/gdx/maps/tiled/TiledMapTile;", false);
		mv.visitMethodInsn(INVOKEINTERFACE, "com/badlogic/gdx/maps/tiled/TiledMapTile", "getProperties", "()Lcom/badlogic/gdx/maps/MapProperties;", true);
		mv.visitLdcInsn("Blocked");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/maps/MapProperties", "containsKey", "(Ljava/lang/String;)Z", false);
		mv.visitLabel(label1);
		mv.visitInsn(IRETURN);
		mv.visitLabel(label2);
		mv.visitVarInsn(ASTORE, 4);
		
		Label label3 = new Label();
		mv.visitLabel(label3);
		mv.visitInsn(ICONST_0);
		mv.visitInsn(IRETURN);
		Label label4 = new Label();
		
		mv.visitLabel(label4);
		mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, label3, label4, 4);
		mv.visitLocalVariable("this", "L" + cf.name + ";", null, label0, label4, 0);
		mv.visitLocalVariable("layer", "Ljava/lang/Object;", null, label0, label4, 1);
		mv.visitLocalVariable("xTile", "I", null, label0, label4, 2);
		mv.visitLocalVariable("yTile", "I", null, label0, label4, 3);
		mv.visitEnd();
	}
	
	private void noClip()
	{
		val cf = classPool.findClassByInterface(MapManager.class);
		val npcCf = classPool.findClassByInterface(Npc.class);

		assert cf != null && npcCf != null;

		MethodFile clipMethod = null;

		for (val mf : cf.methods)
		{
			if (!mf.desc.equals("(Lcom/badlogic/gdx/math/Vector2;Lcom/pbo/common/network/message/NetworkProtocol$NetworkMessage$Direction;)Z"))
			{
				continue;
			}

			for (val af : mf.instructions)
			{
				if (af.getType() != METHOD_INSN)
				{
					continue;
				}

				val min = (MethodInsnNode) af;

				if (min.owner.equals(npcCf.name))
				{
					clipMethod = mf;
					break;
				}
			}
		}

		if (clipMethod == null)
		{
			log.error("Unable to find the method that handles clipping.");
			return;
		}

		val insnList = new InsnList();

		Label label0 = new Label();
		insnList.add(new LabelNode(label0));
		insnList.add(new FieldInsnNode(GETSTATIC, "org/pokebot/Constants", "ENABLE_NO_CLIP", "Z"));
		
		Label label1 = new Label();
		LabelNode labelNode = new LabelNode(label1);
		insnList.add(new JumpInsnNode(IFEQ, labelNode));
		
		Label label2 = new Label();
		insnList.add(new LabelNode(label2));
		insnList.add(new InsnNode(ICONST_0));
		insnList.add(new InsnNode(IRETURN));
		insnList.add(new LabelNode(label1));

		clipMethod.instructions.insert(insnList);
	}

	/**
	 * Adds getters for all individual layers.
	 */
	private void addLayerGetters()
	{
		val mapManager = classPool.findClassByInterface(MapManager.class);

		if (mapManager == null)
		{
			return;
		}

		boolean prevGetLayers = false;
		HashMap<String, FieldInsnNode> getterMap = new HashMap<>();
		String ldcName = null;

		for (val mf : mapManager.methods)
		{
			for (val af : mf.instructions)
			{
				if (af.getType() == METHOD_INSN)
				{
					val min = (MethodInsnNode) af;

					prevGetLayers = min.name.equals("getLayers");
				}
				else if (af.getType() == LDC_INSN && prevGetLayers)
				{
					val lin = (LdcInsnNode) af;

					if (lin.cst.getClass() != String.class)
					{
						continue;
					}

					val str = (String) lin.cst;

					switch (str)
					{
						case "CollisionMain":
						case "Grass":
						case "Surf":
						case "BridgeEntryLayer":
						case "BridgeExitLayer":
						case "BridgeOverCollisionLayer":
						case "BridgeUnderCollisionLayer":
						case "BridgeLayer":
						case "LedgeLeft":
						case "LedgeRight":
						case "LedgeUp":
						case "LedgeDown":
						case "BikeLedge":
						case "RockClimb":
						case "Waterfall":
							ldcName = str;
							break;
					}
					prevGetLayers = false;
				}
				else if (af.getOpcode() == PUTFIELD && ldcName != null)
				{
					getterMap.put(ldcName, (FieldInsnNode) af);
					ldcName = null;
				}
				else
				{
					prevGetLayers = false;
				}
			}
		}

		for (val entry : getterMap.entrySet())
		{
			ByteCodeUtil.addGetter(mapManager, entry.getValue(), Util.lowerFirst(entry.getKey()), Object.class);
		}
	}

	private void addEnemyGetter()
	{
		val mapManager = classPool.findClassByInterface(MapManager.class);

		if (mapManager == null)
		{
			return;
		}

		MethodFile enemyMethod = null;

		for (val mf : mapManager.methods)
		{
			if (mf.desc.startsWith("(Lcom/pbo/common/network/message/NetworkProtocol$NetworkMessage$EnemyInitMessage;"))
			{
				enemyMethod = mf;
				break;
			}
		}

		if (enemyMethod == null)
		{
			log.error("Unable to find the enemy method in MapManager.");
			return;
		}

		FieldInsnNode latestField = null;
		FieldInsnNode playerField = null;
		FieldInsnNode npcField = null;
		String putOnClass = null;

		for (val af : enemyMethod.instructions)
		{
			if (af.getType() == METHOD_INSN)
			{
				val min = (MethodInsnNode) af;

				if (min.name.equals("put") && min.owner.equals("com/badlogic/gdx/utils/LongMap"))
				{
					if (putOnClass == null)
					{
						continue;
					}

					val owner = classPool.findClass(putOnClass);
					val superClass = owner.getSuper();

					if (latestField == null)
					{
						log.error("Found Npc or Player without the field being called.");
					}
					else if (superClass != null && superClass.implementsInterface(Player.class))
					{
						ByteCodeUtil.addInterface(superClass, EnemyPlayer.class);
						playerField = latestField;
					}
					else if (owner.implementsInterface(Player.class))
					{
						log.error("The put method is talking to the player interface instead of the implementation.");
					}
					else if (Modifier.isInterface(owner.access))
					{
						ByteCodeUtil.addInterface(owner, Npc.class);
						npcField = latestField;
					}
					else
					{
						log.error("Uncaught exception whilst obtaining the EnemyPlayer and Npc class.");
					}
				}
				else
				{
					putOnClass = min.owner;
				}
			}
			else if (af.getType() == FIELD_INSN)
			{
				latestField = (FieldInsnNode) af;
			}
		}

		if (npcField == null || playerField == null)
		{
			return;
		}

		addLongMapGetter(mapManager, playerField, "getPlayers", EnemyPlayer.class);
		addLongMapGetter(mapManager, npcField, "getNpcs", Npc.class);
	}

	private void addMapIdGetter()
	{
		val mapManager = classPool.findClassByInterface(MapManager.class);

		if (mapManager == null)
		{
			return;
		}

		FieldInsnNode fieldNode = null;

		for (val mf : mapManager.methods)
		{
			for (val af : mf.instructions)
			{
				if (af.getType() == METHOD_INSN)
				{
					val min = (MethodInsnNode) af;

					if (min.name.equals("getMapId") && af.getNext().getType() == FIELD_INSN)
					{
						fieldNode = (FieldInsnNode) af.getNext();

						break;
					}
				}
			}
		}

		if (fieldNode == null)
		{
			log.error("Unable to add getter for the map ID as the field node cannot be identified.");
			return;
		}

		ByteCodeUtil.addGetter(mapManager, fieldNode, "mapId");
	}

	private void addGetterToClient()
	{
		val cf = classPool.findClassByInterface(Client.class);

		if (cf == null)
		{
			return;
		}

		ByteCodeUtil.forwardToClient(cf, MapManager.class);
	}

	private void addGetterToMainGame()
	{
		val cf = classPool.findClassByInterface(MainGameScreen.class);

		if (cf == null)
		{
			return;
		}

		val constructor = cf.getSingleConstructor();

		val assignMap = ByteCodeUtil.getInitAssignMap(cf, constructor);
		ByteCodeUtil.addGetter(cf, assignMap.get(2), "mapManager", MapManager.class);
	}

	private void addLongMapGetter(final ClassFile cf, final FieldInsnNode ff, final String name, final Class<?> clazz)
	{
		val castName = Util.toSlash(clazz.getName());

		val varName = Util.lowerFirst(clazz.getSimpleName());
		val mv = cf.visitMethod(ACC_PUBLIC, name, "()Ljava/util/List;", "()Ljava/util/List<L" + castName + ";>;", null);
		mv.visitCode();

		Label label0 = new Label();
		mv.visitLabel(label0);
		mv.visitTypeInsn(NEW, "java/util/ArrayList");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
		mv.visitVarInsn(ASTORE, 1);

		Label label1 = new Label();
		mv.visitLabel(label1);

		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, ff.owner, ff.name, ff.desc);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/utils/LongMap", "values", "()Lcom/badlogic/gdx/utils/LongMap$Values;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/badlogic/gdx/utils/LongMap$Values", "iterator", "()Ljava/util/Iterator;", false);
		mv.visitVarInsn(ASTORE, 2);

		Label label2 = new Label();
		mv.visitLabel(label2);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);

		Label label3 = new Label();
		mv.visitJumpInsn(IFEQ, label3);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
		mv.visitVarInsn(ASTORE, 3);

		Label label4 = new Label();
		mv.visitLabel(label4);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitTypeInsn(CHECKCAST, castName);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
		mv.visitInsn(POP);

		Label label5 = new Label();
		mv.visitLabel(label5);
		mv.visitJumpInsn(GOTO, label2);
		mv.visitLabel(label3);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitInsn(ARETURN);

		Label label6 = new Label();
		mv.visitLabel(label6);
		mv.visitLocalVariable(varName, "Ljava/lang/Object;", null, label4, label5, 3);
		mv.visitLocalVariable("this", "L" + cf.name + ";", null, label0, label6, 0);
		mv.visitLocalVariable("list", "Ljava/util/ArrayList;", "Ljava/util/ArrayList<L" + castName + ";>;", label1, label6, 1);
		mv.visitEnd();
	}
}
