package org.pokebot.patcher.asm.wrappers;

import kotlinx.metadata.jvm.KotlinClassHeader;
import kotlinx.metadata.jvm.KotlinClassMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.pokebot.patcher.Constants;
import org.pokebot.patcher.util.JarResult;
import org.pokebot.patcher.util.MutableList;
import org.pokebot.patcher.util.Util;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An extension of {@link ClassNode} that stores additional
 * data and has utility functions.
 */
@Slf4j
public final class ClassFile extends ClassNode
{
	/**
	 * The Kotlin metadata that has been parsed from the header.
	 */
	@Getter
	private KotlinClassMetadata kotlinMetadata;

	/**
	 * The pool that this {@link ClassFile} belongs to.
	 */
	@Getter
	private final JarResult jarResult;

	public final List<MethodFile> methods = MutableList.of();

	public ClassFile(final JarResult jarResult)
	{
		super(Constants.ASM_VERSION);

		this.jarResult = jarResult;
	}

	public boolean implementsInterface(final ClassFile cf)
	{
		return interfaces.stream().anyMatch(it -> it.equals(cf.name));
	}

	public boolean implementsInterface(final Class<?> clazz)
	{
		return interfaces.stream().anyMatch(it -> it.equals(Util.toSlash(clazz.getName())));
	}

	public MethodFile getSingleConstructor()
	{
		val constructors = methods.stream().filter(it -> it.name.equals("<init>")).collect(Collectors.toList());

		if (constructors.size() != 1)
		{
			log.error("Class with name: " + name + " contains multiple constructors.");
			return null;
		}

		return constructors.get(0);
	}

	/**
	 * Tells if the this class file is present in the obfuscated
	 * package.
	 *
	 * @return True if the class file is in the obfuscated package.
	 */
	public boolean inObfuscatedPackage()
	{
		return name.startsWith(Constants.OBFUSCATED_PREFIX);
	}

	/**
	 * Appends '.class' the name.
	 *
	 * @return String that is conform the
	 * naming of a {@link java.util.jar.JarEntry}.
	 */
	public String getJarName()
	{
		return name + ".class";
	}

	/**
	 * Checks if the access modifier for the interface flag.
	 *
	 * @return Boolean that tells if this {@link ClassFile}
	 * as an interface.
	 */
	public boolean isInterface()
	{
		return Modifier.isInterface(access);
	}

	public boolean isAbstract()
	{
		return Modifier.isAbstract(access);
	}

	/**
	 * Finds the {@link ClassFile} from the {@link JarResult}
	 * that matches the super name.
	 *
	 * @return A {@link ClassFile} that matches the name
	 * of the super class.
	 */
	public ClassFile getSuper()
	{
		return jarResult.findClass(superName);
	}

	@Override
	public MethodVisitor visitMethod(
			final int access,
			final String name,
			final String descriptor,
			final String signature,
			final String[] exceptions)
	{
		MethodFile method = new MethodFile(access, name, descriptor, signature, exceptions);
		methods.add(method);
		return method;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible)
	{
		AnnotationFile annotation = new AnnotationFile(descriptor, this);
		if (visible)
		{
			visibleAnnotations = Util.add(visibleAnnotations, annotation);
		}
		else
		{
			invisibleAnnotations = Util.add(invisibleAnnotations, annotation);
		}
		return annotation;
	}

	/**
	 * Parses the header into a {@link KotlinClassMetadata} file.
	 *
	 * @param annotationNode Annotation to obtain the metadata from.
	 */
	public void parseClassHeader(final AnnotationNode annotationNode)
	{
		if (!annotationNode.desc.equals("Lkotlin/Metadata;"))
		{
			return;
		}

		val values = annotationNode.values;

		if (values == null)
		{
			return;
		}

		val headerMap = new HashMap<>();
		Object key = null;
		for (int i = 0; i < values.size(); i++)
		{
			val isKey = i % 2 == 0;
			if (isKey)
			{
				key = values.get(i);
			}
			else
			{
				headerMap.put(key, values.get(i));
			}
		}

		val kind = (int) headerMap.get("k");
		val metadataVersion = Util.listObjectToIntArray(headerMap.get("mv"));
		val bytecodeVersion = Util.listObjectToIntArray(headerMap.get("bv"));
		val data1 = Util.listObjectToArray(headerMap.get("d1"), new String[0]);
		val data2 = Util.listObjectToArray(headerMap.get("d2"), new String[0]);

		val header = new KotlinClassHeader(kind, metadataVersion, bytecodeVersion, data1, data2, null, null, null);

		kotlinMetadata = KotlinClassMetadata.read(header);
	}

	/**
	 * Copy from {@link ClassVisitor} to work with {@link MethodFile}
	 * instead of {@link MethodNode}. The {@link MethodFile} allows
	 * more flexibility for an extensive API or caching.
	 *
	 * @param classVisitor The class visitor that will be accepted.
	 */
	@Override
	public void accept(final ClassVisitor classVisitor)
	{
		// Visit the header.
		String[] interfacesArray = new String[this.interfaces.size()];
		this.interfaces.toArray(interfacesArray);
		classVisitor.visit(version, access, name, signature, superName, interfacesArray);
		// Visit the source.
		if (sourceFile != null || sourceDebug != null)
			classVisitor.visitSource(sourceFile, sourceDebug);
		// Visit the module.
		if (module != null)
			module.accept(classVisitor);
		// Visit the nest host class.
		if (nestHostClass != null)
			classVisitor.visitNestHost(nestHostClass);
		// Visit the outer class.
		if (outerClass != null)
			classVisitor.visitOuterClass(outerClass, outerMethod, outerMethodDesc);
		// Visit the annotations.
		if (visibleAnnotations != null)
			for (AnnotationNode annotation : visibleAnnotations)
				annotation.accept(classVisitor.visitAnnotation(annotation.desc, true));
		if (invisibleAnnotations != null)
			for (AnnotationNode annotation : invisibleAnnotations)
				annotation.accept(classVisitor.visitAnnotation(annotation.desc, false));
		if (visibleTypeAnnotations != null)
			for (TypeAnnotationNode typeAnnotation : visibleTypeAnnotations)
				typeAnnotation.accept(
						classVisitor.visitTypeAnnotation(
								typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
		if (invisibleTypeAnnotations != null)
			for (TypeAnnotationNode typeAnnotation : invisibleTypeAnnotations)
				typeAnnotation.accept(
						classVisitor.visitTypeAnnotation(
								typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
		// Visit the non standard attributes.
		if (attrs != null)
			for (org.objectweb.asm.Attribute attr : attrs)
				classVisitor.visitAttribute(attr);
		// Visit the nest members.
		if (nestMembers != null)
			for (String nestMember : nestMembers) classVisitor.visitNestMember(nestMember);
		// Visit the permitted subclasses.
		if (permittedSubclasses != null)
			for (String permittedSubclass : permittedSubclasses) classVisitor.visitPermittedSubclass(permittedSubclass);
		// Visit the inner classes.
		for (org.objectweb.asm.tree.InnerClassNode innerClass : innerClasses) innerClass.accept(classVisitor);
		// Visit the record components.
		if (recordComponents != null)
			for (org.objectweb.asm.tree.RecordComponentNode recordComponent : recordComponents)
				recordComponent.accept(classVisitor);
		// Visit the fields.
		for (org.objectweb.asm.tree.FieldNode field : fields)
			field.accept(classVisitor);
		// Visit the methods.
		for (MethodNode method : methods)
			method.accept(classVisitor);
		classVisitor.visitEnd();
	}
}
