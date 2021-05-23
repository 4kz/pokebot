package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.asm.wrappers.MethodFile;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.LDC_INSN;

@Slf4j
public final class AntiBotPatch implements PoolModifier
{
	private ClassPool classPool;

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		this.classPool = classPool;

		interceptProcesses();
		interceptProgramFiles();
		interceptMacAddress();

		return this.classPool;
	}

	private void interceptMacAddress()
	{
		MethodFile macMethod = null;

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

					if (lin.cst.equals("%02X%s"))
					{
						macMethod = mf;
						break searchLoop;
					}
				}
			}
		}

		if (macMethod == null)
		{
			log.error("Unable to find the method that sends your MAC-address to the server.");
			return;
		}

		for (val af : macMethod.instructions)
		{
			if (af.getOpcode() == ARETURN)
			{
				macMethod.instructions.insertBefore(af, new MethodInsnNode(INVOKESTATIC, "org/pokebot/safety/Interceptors", "interceptMac", "(Ljava/lang/String;)Ljava/lang/String;", false));
			}
		}
	}

	private void interceptProgramFiles()
	{
		MethodFile pfMethod = null;

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

					if (lin.cst.equals("ProgramFiles") || lin.cst.equals("%programfiles% (x86)"))
					{
						pfMethod = mf;
						break searchLoop;
					}
				}
			}
		}

		if (pfMethod == null)
		{
			log.error("Unable to find the method that sends the program files to the server.");
			return;
		}

		boolean nextAloadIsList = false;

		for (val af : pfMethod.instructions)
		{
			if (af.getType() == FIELD_INSN)
			{
				val fin = (FieldInsnNode) af;

				if (fin.name.equals("PROGRAM_LIST"))
				{
					nextAloadIsList = true;
				}
			}
			else if (nextAloadIsList && af.getOpcode() == ALOAD)
			{
				pfMethod.instructions.insert(af, new MethodInsnNode(INVOKESTATIC, "org/pokebot/safety/Interceptors", "interceptProgramFiles", "(Ljava/util/List;)Ljava/util/List;", false));
			}
		}
	}

	private void interceptProcesses()
	{
		MethodFile processMethod = null;

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

					if (lin.cst.equals("ps -e"))
					{
						processMethod = mf;
						break searchLoop;
					}
				}
			}
		}

		if (processMethod == null)
		{
			log.error("Unable to find the method that sends processes to the server.");
			return;
		}

		for (val af : processMethod.instructions)
		{
			if (af.getOpcode() == ARETURN)
			{
				processMethod.instructions.insertBefore(af, new MethodInsnNode(INVOKESTATIC, "org/pokebot/safety/Interceptors", "interceptProcesses", "(Ljava/util/List;)Ljava/util/List;", false));
			}
		}
	}
}
