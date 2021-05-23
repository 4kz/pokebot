package org.pokebot.patcher.asm.wrappers;

import org.objectweb.asm.tree.AnnotationNode;
import org.pokebot.patcher.Constants;

/**
 * Annotation wrapper required to parse the class header
 * after the annotation values were visited.
 */
public final class AnnotationFile extends AnnotationNode
{
	private final ClassFile classFile;

	public AnnotationFile(final String descriptor, final ClassFile classFile)
	{
		super(Constants.ASM_VERSION, descriptor);

		this.classFile = classFile;
	}

	@Override
	public void visitEnd()
	{
		super.visitEnd();
		classFile.parseClassHeader(this);
	}
}
