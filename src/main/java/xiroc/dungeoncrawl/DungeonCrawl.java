package xiroc.dungeoncrawl;

/*
GENRERAL LICENSE FOR DungeonCrawl v1.0

(1) DungeonCrawl is the intellectual property of XYROC (otherwise known as XIROC1337). Distribution of the compiled mod on any other site than curseforge.com, minecraft.curseforge.com, xiroc.ovh or minecraftforum.net is strictly forbidden. Further, all sites included in the following list are NOT allowed to redistribute the mod or profit from it in any way:
https://raw.githubusercontent.com/StopModReposts/Illegal-Mod-Sites/master/SITES.md
Redistributing this mod on the above mentioned illegal sites is a violation of copyright.

(2) Modpack creators are only allowed to include this mod in FREE-TO-PLAY modpacks. Including this mod in modpacks that require payment to become playable or accessible for the user is forbidden.

(3) You are allowed to read, use and share the Source Code of this mod, for example to create similar projects. However, completely copying the mod or copying large parts of the source (= more than 20%) without the explicit approval of the mod author is forbidden.

DungeonCrawl (C) 2019 XYROC (XIROC1337), All Rights Reserved
 */

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.placement.NoPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import xiroc.dungeoncrawl.config.Config;
import xiroc.dungeoncrawl.config.JsonConfig;
import xiroc.dungeoncrawl.dungeon.Dungeon;
import xiroc.dungeoncrawl.dungeon.segment.DungeonSegmentModelBlock;
import xiroc.dungeoncrawl.dungeon.segment.DungeonSegmentModelRegistry;
import xiroc.dungeoncrawl.dungeon.treasure.Treasure;
import xiroc.dungeoncrawl.module.BOPCompatModule;
import xiroc.dungeoncrawl.module.ModuleManager;
import xiroc.dungeoncrawl.part.block.BlockRegistry;
import xiroc.dungeoncrawl.util.EventManager;
import xiroc.dungeoncrawl.util.IBlockPlacementHandler;

@Mod(DungeonCrawl.MODID)
public class DungeonCrawl {

	public static final String MODID = "dungeoncrawl";
	public static final String NAME = "Dungeon Crawl";
	public static final String VERSION = "1.6.0";

	public static final Logger LOGGER = LogManager.getLogger(NAME);

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static IEventBus EVENT_BUS;

	public DungeonCrawl() {
		LOGGER.info("Here we go! Launching Dungeon Crawl {}...", VERSION);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new EventManager());
//		MinecraftForge.EVENT_BUS.register(new Tools());
		ForgeRegistries.FEATURES
				.register(Dungeon.DUNGEON.setRegistryName(new ResourceLocation(Dungeon.NAME.toLowerCase())));
		Treasure.init();
		EVENT_BUS = Bus.MOD.bus().get();

		ModuleManager.registerModule(new BOPCompatModule());
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		LOGGER.info("Common Setup");
		ModLoadingContext.get().registerConfig(Type.COMMON, Config.CONFIG);
		Config.load(FMLPaths.CONFIGDIR.get().resolve("dungeon_crawl.toml"));

		DungeonSegmentModelBlock.load();
		IBlockPlacementHandler.load();
		BlockRegistry.load();

		DungeonCrawl.LOGGER.info("Adding features and structures");
		for (Biome biome : ForgeRegistries.BIOMES) {
			if (!JsonConfig.BIOME_BLACKLIST.contains(biome.getRegistryName().toString())) {
				DungeonCrawl.LOGGER.debug("Biome >> " + biome.getRegistryName());
				biome.addFeature(Decoration.UNDERGROUND_STRUCTURES, Biome.createDecoratedFeature(Dungeon.DUNGEON,
						NoFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, NoPlacementConfig.NO_PLACEMENT_CONFIG));
				if (!JsonConfig.BIOME_OVERWORLD_BLACKLIST.contains(biome.getRegistryName().toString())) {
					DungeonCrawl.LOGGER.debug("Generation Biome >> " + biome.getRegistryName());
					biome.addStructure(Dungeon.DUNGEON, NoFeatureConfig.NO_FEATURE_CONFIG);
				}
			}
		}

		ModuleManager.load();
	}

	public static String getDate() {
		return new SimpleDateFormat().format(new Date());
	}

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (event.getWorld().isRemote())
			return;
		ServerWorld server = (ServerWorld) event.getWorld();
		DungeonSegmentModelRegistry.load(server.getServer().getResourceManager());
	}

	public static ResourceLocation locate(String path) {
		return new ResourceLocation(MODID, path);
	}

}
