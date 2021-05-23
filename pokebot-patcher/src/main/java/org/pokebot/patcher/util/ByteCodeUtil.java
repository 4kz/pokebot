package org.pokebot.patcher.util;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.pokebot.api.screen.MainGameScreen;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.asm.wrappers.MethodFile;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

@Slf4j
public final class ByteCodeUtil
{
	private static final int[] LOAD_OPERATIONS = new int[]
			{
					ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, IALOAD, LALOAD,
					FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD
			};

	private static final int[] STORE_OPERATIONS = new int[]
			{
					ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, IASTORE, LASTORE,
					FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE
			};

	public static void addInterface(final ClassFile cf, final Class<?> inter)
	{
		cf.interfaces.add(Util.toSlash(inter.getName()));
	}

	// TODO: Make it work for static as well.
	public static Map<Integer, FieldInsnNode> getInitAssignMap(final ClassFile cf, final MethodFile mf)
	{
		val initMap = new HashMap<Integer, FieldInsnNode>();

		val indexMap = getIndexMap(cf, mf);

		var latestAload = -1;

		for (val af : mf.instructions)
		{
			if (af instanceof VarInsnNode)
			{
				val vin = (VarInsnNode) af;

				if (Arrays.stream(LOAD_OPERATIONS).anyMatch(it -> it == vin.getOpcode()))
				{
					latestAload = vin.var;
				}
				/*
				 * Remap the indexMap after the variable is overwritten.
				 * An example is the following bytecode:
				 *
				 * 		ILOAD 9
				 * 		ASTORE 2
				 *
				 * For this example VAR 2 will be removed from the indexMap (value wise)
				 * as it is overwritten by VAR 9. Now VAL 9 will be remapped to VAR 2
				 * so that the parameter index is right again.
				 */
				else if (Arrays.stream(STORE_OPERATIONS).anyMatch(it -> it == vin.getOpcode())
						&& indexMap.containsValue(vin.var)
						&& latestAload != -1)
				{
					for (val entry : indexMap.entrySet())
					{
						if (entry.getValue().equals(vin.var))
						{
							indexMap.remove(entry.getKey(), entry.getValue());
							break;
						}
					}

					for (val entry : indexMap.entrySet())
					{
						if (entry.getValue().equals(latestAload))
						{
							indexMap.put(entry.getKey(), vin.var);
							break;
						}
					}

					latestAload = -1;
				}
				else
				{
					latestAload = -1;
				}

				continue;
			}
			else if (!(af instanceof FieldInsnNode))
			{
				latestAload = -1;
				continue;
			}

			val fin = (FieldInsnNode) af;

			if ((fin.getOpcode() == PUTFIELD)
					&& latestAload != -1
					&& indexMap.containsValue(latestAload))
			{
				var index = -1;

				for (val entry : indexMap.entrySet())
				{
					if (entry.getValue() != latestAload)
					{
						continue;
					}

					index = entry.getKey();
					break;
				}

				if (initMap.containsKey(index))
				{
					latestAload = -1;
					continue;
				}

				initMap.put(index, fin);
			}
			latestAload = -1;
		}

		return initMap;
	}

	public static Map<Integer, Integer> getIndexMap(final ClassFile cf, final MethodFile mf)
	{
		val indexMap = new HashMap<Integer, Integer>();

		var shift = 0;

		if (Modifier.isStatic(mf.access))
		{
			shift += 1; // <init> will reserve 1
		}

		if (cf.superName != null)
		{
			shift++; // the super will reserve 2
		}

		val args = Type.getArgumentTypes(mf.desc);

		for (int i = 0; i < args.length; i++)
		{
			indexMap.put(i, shift);

			val arg = args[i];
			shift += arg.getSize();
		}

		return indexMap;
	}

	/**
	 * Adds a method that gives access to the toForwardClass through
	 * the Client. If the MainGameScreen does not exist,
	 * it will instead return null as the method will be obtained
	 * from the {@link MainGameScreen}. This method is to make
	 * the Client API more semantic.
	 *
	 * @param cf ClassFile to add the method to (which usually is the
	 *           implementation of {@link org.pokebot.api.Client}.
	 * @param toForwardClass The class (interface) to create an API extension for.
	 */
	public static void forwardToClient(final ClassFile cf, final Class<?> toForwardClass)
	{
		forwardToClient(cf, toForwardClass.getSimpleName(), toForwardClass);
	}

	/**
	 * Adds a method that gives access to the toForwardClass through
	 * the Client. If the MainGameScreen does not exist,
	 * it will instead return null as the method will be obtained
	 * from the {@link MainGameScreen}. This method is to make
	 * the Client API more semantic.
	 *
	 * @param cf ClassFile to add the method to (which usually is the
	 *           implementation of {@link org.pokebot.api.Client}.
	 * @param name The property name. E.g. client will become getClient.
	 * @param toForwardClass The class (interface) to create an API extension for.
	 */
	public static void forwardToClient(final ClassFile cf, final String name, final Class<?> toForwardClass)
	{
		val mainGame = Util.toSlash(MainGameScreen.class.getName());
		val toForward = Util.toSlash(toForwardClass.getName());
		val methodName = "get" + Util.upperFirst(name);

		val mv = cf.visitMethod(ACC_PUBLIC, methodName, "()L" + toForward + ";", null, null);
		mv.visitCode();

		Label label0 = new Label();
		mv.visitLabel(label0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, cf.name, "getScreen", "()Lcom/badlogic/gdx/Screen;", false);
		mv.visitTypeInsn(INSTANCEOF, mainGame);

		Label label1 = new Label();
		mv.visitJumpInsn(IFNE, label1);

		Label label2 = new Label();
		mv.visitLabel(label2);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		mv.visitLabel(label1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, cf.name, "getScreen", "()Lcom/badlogic/gdx/Screen;", false);
		mv.visitTypeInsn(CHECKCAST, mainGame);
		mv.visitMethodInsn(INVOKEINTERFACE, mainGame, methodName, "()L" + toForward + ";", true);
		mv.visitInsn(ARETURN);

		Label label3 = new Label();
		mv.visitLabel(label3);
		mv.visitLocalVariable("this", "L" + cf.name + ";", null, label0, label3, 0);
		mv.visitEnd();
	}

	public static void addCastGetter(final ClassFile cf, final MethodFile mf, final String propertyName, final Class<?> cast)
	{
		addCastGetter(cf, mf.name, mf.desc, propertyName, cast);
	}

	// TODO: Make it work for static as well.
	public static void addCastGetter(final ClassFile cf, final String methodName, final String methodDesc, final String propertyName, final Class<?> cast)
	{
		if (propertyName == null || propertyName.length() < 2)
		{
			log.error("Unable to create cast getter for: " + propertyName + " in class: " + cf.name);
			return;
		}

		val name = "get" + Util.upperFirst(propertyName);

		val castName = Util.toSlash(cast.getName());

		val mv = cf.visitMethod(ACC_PUBLIC, name, "()L" + castName + ";", null, null);
		mv.visitCode();

		Label label0 = new Label();
		mv.visitLabel(label0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, cf.name, methodName, methodDesc, false);
		mv.visitTypeInsn(CHECKCAST, castName);
		mv.visitInsn(ARETURN);

		Label label1 = new Label();
		mv.visitLabel(label1);
		mv.visitLocalVariable("this", "L" + cf.name + ";", null, label0, label1, 0);
		mv.visitEnd();
	}

	public static void addGetter(final ClassFile classFile, final FieldNode fieldNode, final String propertyName)
	{
		addGetter(classFile, fieldNode.name, fieldNode.desc, propertyName, fieldNode.desc);
	}

	public static void addGetter(final ClassFile classFile, final FieldInsnNode fieldNode, final String propertyName)
	{
		addGetter(classFile, fieldNode.name, fieldNode.desc, propertyName, fieldNode.desc);
	}

	public static void addGetter(final ClassFile classFile, final FieldInsnNode fieldNode, final String propertyName, final Class<?> returnType)
	{
		addGetter(classFile, fieldNode.name, fieldNode.desc, propertyName, "L" + Util.toSlash(returnType.getName()) + ";");
	}

	public static void addGetter(final ClassFile classFile, final FieldNode fieldNode, final String propertyName, final Class<?> returnType)
	{
		addGetter(classFile, fieldNode.name, fieldNode.desc, propertyName, "L" + Util.toSlash(returnType.getName()) + ";");
	}

	/**
	 * Finds all interfaces by using preorder traversal.
	 * It will only search for interfaces in its own
	 * package pool.
	 *
	 * @param cf ClassFile to find all interfaces for.
	 * @return List of all interfaces.
	 */
	public static List<String> findAllInterfaces(final ClassFile cf)
	{
		val list = MutableList.<String>of();

		recursivelyAddInterfacesPreorder(cf, list);

		return list;
	}

	private static void recursivelyAddInterfacesPreorder(final ClassFile cf, final List<String> list)
	{
		for (val inter : cf.interfaces)
		{
			list.add(inter);
			val impl = cf.getJarResult().getPackagePool().findClass(inter);
			if (impl != null)
			{
				recursivelyAddInterfacesPreorder(impl, list);
			}
		}
	}

	// TODO: Make it work for static as well.
	private static void addGetter(final ClassFile classFile, final String fieldName, final String fieldDesc, final String propertyName, final String returnDesc)
	{
		if (propertyName == null || propertyName.length() < 2)
		{
			log.error("Unable to create getter for: " + propertyName + " in class: " + classFile.name);
			return;
		}

		var prefix = "get";

		if (returnDesc.equals("Z"))
		{
			prefix = "is";
		}

		val name = prefix + Util.upperFirst(propertyName);

		val methodVisitor = classFile.visitMethod(ACC_PUBLIC, name, "()" + returnDesc, null, null);
		methodVisitor.visitCode();

		Label label0 = new Label();
		methodVisitor.visitLabel(label0);
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitFieldInsn(GETFIELD, classFile.name, fieldName, fieldDesc);
		methodVisitor.visitInsn(getReturnOpcode(returnDesc));

		Label label1 = new Label();
		methodVisitor.visitLabel(label1);
		methodVisitor.visitLocalVariable("this", "L" + classFile.name + ";", null, label0, label1, 0);

		methodVisitor.visitEnd();
	}

	private static int getReturnOpcode(String desc)
	{
		switch (desc)
		{
			case "Z":
			case "C":
			case "B":
			case "S":
			case "I":
				return IRETURN;
			case "J":
				return LRETURN;
			case "D":
				return DRETURN;
			case "F":
				return FRETURN;
			default:
				return ARETURN;
		}
	}
}
