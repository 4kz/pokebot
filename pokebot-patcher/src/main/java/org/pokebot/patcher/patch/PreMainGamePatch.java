package org.pokebot.patcher.patch;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.pokebot.api.ChatDialog;
import org.pokebot.api.Client;
import org.pokebot.api.LocalPlayerManager;
import org.pokebot.api.MapManager;
import org.pokebot.api.entity.LocalPlayer;
import org.pokebot.api.entity.OurPokemon;
import org.pokebot.api.entity.Player;
import org.pokebot.api.entityproperty.PokemonMove;
import org.pokebot.api.playerproperty.AdminLevel;
import org.pokebot.api.playerproperty.Title;
import org.pokebot.api.playerproperty.TitleBenefits;
import org.pokebot.api.screen.LoginScreen;
import org.pokebot.api.screen.MainGameScreen;
import org.pokebot.patcher.Constants;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.asm.wrappers.MethodFile;
import org.pokebot.patcher.util.ByteCodeUtil;
import org.pokebot.patcher.util.Util;

import java.lang.reflect.Modifier;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * Enables core API functionality namely:
 *
 * LocalPlayer, OurPokemon, Player, Pokemon, PokemonMove, Position, Title,
 * TitleBenefits and AdminLevel.
 */
@Slf4j
public final class PreMainGamePatch implements PoolModifier
{
	private ClassPool classPool;

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		this.classPool = classPool;

		applyPatch();
		patchOurPokemon();
		addOurPokemonGetter();
		patchPokemon();
		addBeforeRenderCall();

		return this.classPool;
	}

	private void addBeforeRenderCall()
	{
		for (val cf : classPool)
		{
			if (cf.interfaces.stream().noneMatch(it -> it.equals("com/badlogic/gdx/Screen")))
			{
				continue;
			}

			val streamMf = cf.methods.stream()
					.filter(it -> it.name.equals("render") && it.desc.equals("(F)V"))
					.collect(Collectors.toList());

			if (streamMf.size() != 1)
			{
				log.error("Unable to add the BeforeRender hook as the render method cannot be found.");
				return;
			}

			val mf = streamMf.get(0);

			val insnList = new InsnList();

			Label label0 = new Label();
			insnList.add(new LabelNode(label0));
			insnList.add(new MethodInsnNode(INVOKESTATIC, "org/pokebot/Pokebot", "getInjector", "()Lcom/google/inject/Injector;", false));
			insnList.add(new LdcInsnNode(Type.getType("Lorg/pokebot/Pokebot;")));
			insnList.add(new MethodInsnNode(INVOKEINTERFACE, "com/google/inject/Injector", "getInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", true));
			insnList.add(new TypeInsnNode(CHECKCAST, "org/pokebot/Pokebot"));
			insnList.add(new VarInsnNode(FLOAD, 1));
			insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "org/pokebot/Pokebot", "beforeRender", "(F)V", false));

			mf.instructions.insert(insnList);
		}
	}

	private void addOurPokemonGetter()
	{
		val localPlayer = classPool.findClassByInterface(LocalPlayer.class);

		if (localPlayer == null)
		{
			return;
		}

		val methods = localPlayer.fields.stream().filter(it -> Modifier.isFinal(it.access)
				&& it.desc.equals("Ljava/util/List;")).collect(Collectors.toList());

		if (methods.size() != 1)
		{
			log.error("There are multiple lists or no lists available in OurPlayer.");
			return;
		}

		val field = methods.get(0);
		ByteCodeUtil.addGetter(localPlayer, field, "pokemon");
	}

	private void patchOurPokemon()
	{
		/*
		 * Find OurPokemon class to add the interface.
		 */
		val cf = classPool.findClassByInterface(LoginScreen.class);

		if (cf == null)
		{
			return;
		}

		boolean passedLocalPlayer = false;
		boolean prevWasArrGetter = false;
		String name = null;

		for (val mf : cf.methods)
		{
			if (!mf.name.equals("render"))
			{
				continue;
			}

			for (val af : mf.instructions)
			{
				if (af instanceof TypeInsnNode)
				{
					val tin = (TypeInsnNode) af;

					if (tin.getOpcode() == NEW && tin.desc.equals(classPool.findClassByInterface(LocalPlayer.class).name))
					{
						passedLocalPlayer = true;
					}

					if (prevWasArrGetter)
					{
						name = tin.desc;
						break;
					}

					prevWasArrGetter = false;
				}
				else if (af instanceof MethodInsnNode)
				{
					val min = (MethodInsnNode) af;

					prevWasArrGetter = min.owner.equals("java/util/List") && min.name.equals("get") && passedLocalPlayer;
				}
				else
				{
					prevWasArrGetter = false;
				}
			}
		}

		if (name == null)
		{
			log.error("Unable to find the OurPokemon class.");
			return;
		}

		val ourPokemon = classPool.findClass(name);
		ByteCodeUtil.addInterface(ourPokemon, OurPokemon.class);

		/*
		 * Create getters.
		 */
		val init = ourPokemon.getSingleConstructor();
		val assignMap = ByteCodeUtil.getInitAssignMap(ourPokemon, init);

		val movesField = assignMap.get(4);
		findPokemonMove(ourPokemon, movesField);
		ByteCodeUtil.addGetter(ourPokemon, movesField, "moves");

		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(5), "experience");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(6), "experienceForNextLevel");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(7), "slotNumber");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(8), "evolutionPokedexNumber");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(10), "nature");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(13), "caughtPlace");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(14), "originalOwner");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(16), "ability");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(17), "level");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(18), "currentHp");
		ByteCodeUtil.addGetter(ourPokemon, assignMap.get(19), "happiness");
	}

	private void patchPokemon()
	{
		val ourPokemon = classPool.findClassByInterface(OurPokemon.class);

		if (ourPokemon == null)
		{
			return;
		}

		val pokemon = ourPokemon.getSuper();
		val init = pokemon.getSingleConstructor();
		val assignMap = ByteCodeUtil.getInitAssignMap(pokemon, init);

		ByteCodeUtil.addGetter(pokemon, assignMap.get(0), "id");
		ByteCodeUtil.addGetter(pokemon, assignMap.get(1), "pokedexNumber");
		ByteCodeUtil.addGetter(pokemon, assignMap.get(3), "shiny");

		val nameFilter = pokemon.fields.stream().filter(it -> it.desc.equals("Ljava/lang/String;"))
				.collect(Collectors.toUnmodifiableList());

		if (nameFilter.size() != 1)
		{
			log.error("The Pokemon class got an extra String field and thus the name cannot be obtained.");
		}
		else
		{
			ByteCodeUtil.addGetter(pokemon, nameFilter.get(0), "name");
		}
	}

	private void findPokemonMove(final ClassFile cf, final FieldInsnNode list)
	{
		String className = null;

		for (val mf : cf.methods)
		{
			boolean rightMethod = false;

			for (val af : mf.instructions)
			{
				if (af.getOpcode() == GETFIELD && af instanceof FieldInsnNode)
				{
					val fin = (FieldInsnNode) af;

					if (fin.name.equals(list.name) && mf.desc.startsWith("(I)L" + Constants.OBFUSCATED_PREFIX))
					{
						rightMethod = true;
					}
				}
				else if (rightMethod && af.getOpcode() == CHECKCAST && af instanceof TypeInsnNode)
				{
					val tin = (TypeInsnNode) af;
					className = tin.desc;
					break;
				}
			}
		}

		if (className == null)
		{
			log.error("Unable to obtain the PokemonMoves class.");
			return;
		}

		val pokemonMoves = classPool.findClass(className);
		ByteCodeUtil.addInterface(pokemonMoves, PokemonMove.class);

		val init = pokemonMoves.getSingleConstructor();
		val assignMap = ByteCodeUtil.getInitAssignMap(pokemonMoves, init);
		ByteCodeUtil.addGetter(pokemonMoves, assignMap.get(0), "id");
		ByteCodeUtil.addGetter(pokemonMoves, assignMap.get(1), "pp");
		ByteCodeUtil.addGetter(pokemonMoves, assignMap.get(2), "maxPp");
		ByteCodeUtil.addGetter(pokemonMoves, assignMap.get(3), "name");
		ByteCodeUtil.addGetter(pokemonMoves, assignMap.get(4), "slotNumber");
		ByteCodeUtil.addGetter(pokemonMoves, assignMap.get(5), "maxPpUseCounter");
	}

	private void applyPatch()
	{
		/*
		 * Finds the ChatDialog, OurPlayer, MapManager and OurPlayerManager classes and
		 * adds a getter for the OurPlayer field.
		 */
		for (val cf : classPool)
		{
			if (!cf.implementsInterface(MainGameScreen.class))
			{
				continue;
			}

			ClassFile ourPlayer = null;

			for (val mf : cf.methods)
			{
				if (!mf.name.equals("<init>"))
				{
					continue;
				}

				val patchedForDesc = "(Lcom/pbo/game/client/ui/dialogs/chat/ChatDialog;Lcom/pbo/game/client/c/a/OurPlayer;Lcom/pbo/game/client/f/MapManager;Lcom/pbo/game/client/c/a/c/OurPlayerManager;Lcom/pbo/common/network/message/NetworkProtocol$NetworkMessage$LoginSuccessMessage;ZLcom/badlogic/gdx/utils/viewport/Viewport;Lcom/badlogic/gdx/scenes/scene2d/Stage;)V";

				if (!mf.desc.equals(patchedForDesc))
				{
					log.error("The MainGameScreen class needs a repatch as the descriptor has changed.");
					break;
				}

				val types = Type.getArgumentTypes(mf.desc);

				val chatDialog = classPool.findClass(Util.toSlash(types[0].getClassName()));
				ByteCodeUtil.addInterface(chatDialog, ChatDialog.class);

				ourPlayer = classPool.findClass(Util.toSlash(types[1].getClassName()));
				ByteCodeUtil.addInterface(ourPlayer, LocalPlayer.class);

				ByteCodeUtil.addInterface(ourPlayer.getSuper(), Player.class);

				val mapManager = classPool.findClass(Util.toSlash(types[2].getClassName()));
				ClassFile childManager = null;

				for (val innerCf : classPool)
				{
					if (innerCf.interfaces.stream().noneMatch(it -> it.equals(mapManager.name)))
					{
						continue;
					}

					childManager = innerCf;
					break;
				}

				if (childManager == null)
				{
					log.error("An implementation of MapManager could not be found.");
					return;
				}

				ByteCodeUtil.addInterface(childManager, MapManager.class);

				val ourPlayerManager = classPool.findClass(Util.toSlash(types[3].getClassName()));
				ByteCodeUtil.addInterface(ourPlayerManager, LocalPlayerManager.class);
			}

			if (ourPlayer == null)
			{
				log.error("LocalPlayer / OurPlayer has not been found.");
				break;
			}

			FieldNode ourPlayerField = null;

			for (val ff : cf.fields)
			{
				if (ff.desc.equals("L" + ourPlayer.name + ";"))
				{
					ourPlayerField = ff;
					break;
				}
			}

			if (ourPlayerField == null)
			{
				log.error("Unable to get the LocalPlayer / OurPlayer field.");
				break;
			}

			ByteCodeUtil.addGetter(cf, ourPlayerField, "localPlayer", LocalPlayer.class);
			break;
		}

		/*
		 * Add LocalPlayer getter in the Client.
		 */
		for (val cf : classPool)
		{
			if (!cf.implementsInterface(Client.class))
			{
				continue;
			}

			ByteCodeUtil.forwardToClient(cf, LocalPlayer.class);
			break;
		}

		/*
		 * Adds getters and setters for the Player class.
		 */
		for (val cf : classPool)
		{
			if (!cf.implementsInterface(Player.class))
			{
				continue;
			}

			for (val mf : cf.methods)
			{
				if (!mf.name.equals("<init>"))
				{
					continue;
				}

				val patchedForDesc = "(Ljava/lang/String;Lcom/pbo/common/achievements/Title;JLcom/badlogic/gdx/math/Vector2;ILcom/badlogic/gdx/utils/Queue;Lcom/pbo/game/client/c/a/c/AdminLevel;Z)V";

				if (!mf.desc.equals(patchedForDesc))
				{
					log.error("The Player class needs a repatch as the descriptor has changed.");
					break;
				}

				val initMap = ByteCodeUtil.getInitAssignMap(cf, mf);

				ByteCodeUtil.addGetter(cf, initMap.get(0), "name");

				val title = initMap.get(1);
				val titleClass = classPool.findClass(title.desc);
				ByteCodeUtil.addInterface(titleClass, Title.class);
				ByteCodeUtil.addGetter(cf, title, "title", Title.class);

				val succeededPatch = patchTitle(titleClass);
				if (!succeededPatch)
				{
					break;
				}

				val adminLevel = initMap.get(6);
				ByteCodeUtil.addInterface(classPool.findClass(adminLevel.desc), AdminLevel.class);
				ByteCodeUtil.addGetter(cf, adminLevel, "adminLevel", AdminLevel.class);
				break;
			}
		}
	}

	/**
	 * Patch the Title class and implementations of it
	 * so that it gives access to the benefits and add
	 * interface to the benefitsclass.
	 *
	 * @param titleClass The {@link ClassFile} that is
	 *                   Title interface.
	 * @return True if the patch succeeded.
	 */
	private boolean patchTitle(final ClassFile titleClass)
	{
		MethodFile benefits;
		val benefitsOpt = titleClass.methods.stream().filter(it -> it.name.equals("getBenefits")).findFirst();
		if (benefitsOpt.isPresent())
		{
			benefits = benefitsOpt.get();
		}
		else
		{
			log.error("The Title class might have potentially been obfuscated.");
			return false;
		}

		ByteCodeUtil.addInterface(classPool.findClass(benefits.desc), TitleBenefits.class);

		for (val innerCf : classPool)
		{
			if (!innerCf.implementsInterface(titleClass))
			{
				continue;
			}

			ByteCodeUtil.addCastGetter(innerCf, benefits, "titleBenefits", TitleBenefits.class);
		}

		return true;
	}
}
