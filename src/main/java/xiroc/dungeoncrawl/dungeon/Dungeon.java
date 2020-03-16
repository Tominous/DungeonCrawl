package xiroc.dungeoncrawl.dungeon;

/*
 * DungeonCrawl (C) 2019 - 2020 XYROC (XIROC1337), All Rights Reserved 
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.Random;
import java.util.function.Function;

import com.mojang.datafixers.Dynamic;

import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import xiroc.dungeoncrawl.DungeonCrawl;
import xiroc.dungeoncrawl.api.event.DungeonPlacementCheckEvent;
import xiroc.dungeoncrawl.config.Config;
import xiroc.dungeoncrawl.config.ObfuscationValues;

public class Dungeon extends Structure<NoFeatureConfig> {

	public static final String NAME = DungeonCrawl.MODID + ":dungeon";
	public static final Dungeon DUNGEON = new Dungeon(NoFeatureConfig::deserialize);

	public static int SIZE = 16;

	public Dungeon(Function<Dynamic<?>, ? extends NoFeatureConfig> p_i51427_1_) {
		super(p_i51427_1_);
	}

	public ChunkPos getStartPositionForPosition(ChunkGenerator<?> chunkGenerator, Random random, int x, int z,
			int spacingOffsetsX, int spacingOffsetsZ) {
		int i = 15; // 15
		int j = i - 5; // 10
		int k = x + i * spacingOffsetsX;
		int l = z + i * spacingOffsetsZ;
		int i1 = k < 0 ? k - i + 1 : k;
		int j1 = l < 0 ? l - i + 1 : l;
		int k1 = i1 / i;
		int l1 = j1 / i;
		((SharedSeedRandom) random).setLargeFeatureSeedWithSalt(chunkGenerator.getSeed(), k1, l1, 10387319);
		k1 = k1 * i;
		l1 = l1 * i;
		k1 = k1 + (random.nextInt(i - j) + random.nextInt(i - j)) / 2;
		l1 = l1 + (random.nextInt(i - j) + random.nextInt(i - j)) / 2;
		return new ChunkPos(k1, l1);
	}

	@Override
	public boolean hasStartAt(ChunkGenerator<?> chunkGen, Random rand, int chunkPosX, int chunkPosZ) {
		ChunkPos chunkpos = this.getStartPositionForPosition(chunkGen, rand, chunkPosX, chunkPosZ, 0, 0);
		if (chunkPosX == chunkpos.x && chunkPosZ == chunkpos.z) {
			for (Biome biome : chunkGen.getBiomeProvider().getBiomesInSquare(chunkPosX * 16 + SIZE / 2 * 8,
					chunkPosZ * 16 + SIZE / 2 * 8, 8 * SIZE)) {
				if (!Config.IGNORE_OVERWORLD_BLACKLIST.get() && !chunkGen.hasStructure(biome, DUNGEON))
					return false;
			}
			return rand.nextFloat() < Config.DUNGEON_PROBABLILITY.get();
		} else {
			return false;
		}
	}

	@Override
	public IStartFactory getStartFactory() {
		return Dungeon.Start::new;
	}

	@Override
	public String getStructureName() {
		return NAME;
	}

	@Override
	public int getSize() {
		return 8;
	}

	public static class Start extends StructureStart {

		public Start(Structure<?> p_i51341_1_, int chunkX, int chunkZ, Biome biomeIn, MutableBoundingBox boundsIn,
				int referenceIn, long seed) {
			super(p_i51341_1_, chunkX, chunkZ, biomeIn, boundsIn, referenceIn, seed);
		}

		@Override
		public void init(ChunkGenerator<?> generator, TemplateManager templateManagerIn, int chunkX, int chunkZ,
				Biome biomeIn) {
			/*
			 * Some Reflection stuff. I dont like this but it is the only way I know
			 * currently.
			 */
			try {
				Field world = ChunkGenerator.class.getDeclaredField(ObfuscationValues.CHUNKGEN_WORLD);

				world.setAccessible(true);

				Field modifierField = Field.class.getDeclaredField("modifiers");
				modifierField.setAccessible(true);
				modifierField.setInt(world, world.getModifiers() & ~Modifier.FINAL);

				DungeonCrawl.LOGGER.debug("Checking [{}, {}]", chunkX, chunkZ);

				IWorld iWorld = (IWorld) world.get(generator);

				if (!(iWorld instanceof ServerWorld))
					return;

				ServerWorld serverWorld = (ServerWorld) iWorld;
				BlockPos spawn = serverWorld.getSpawnPoint();

				int spawnChunkX = spawn.getX() % 16, spawnChunkZ = spawn.getZ() % 16, chunkSize = SIZE / 2;

				if (serverWorld.getDimension().getType() != DimensionType.OVERWORLD
						|| DungeonCrawl.EVENT_BUS
								.post(new DungeonPlacementCheckEvent(serverWorld, biomeIn, chunkX, chunkZ))
						|| spawnChunkX - chunkX < chunkSize && spawnChunkX - chunkX > -chunkSize
						|| spawnChunkZ - chunkZ < chunkSize / 2 && spawnChunkZ - chunkZ > -chunkSize)
					return;

				/* Undoing everything */

				modifierField.setInt(world, Modifier.PRIVATE | Modifier.FINAL); // TODO Does this work as intended?
				modifierField.setAccessible(false);
				world.setAccessible(false);

			} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
				DungeonCrawl.LOGGER.error(
						"Failed to access the chunkGen world through reflection. This might result in dungeons getting generated near the spawn chunk.");
				e.printStackTrace();
			}

			ChunkPos chunkpos = new ChunkPos(chunkX, chunkZ);
			long now = System.currentTimeMillis();
			DungeonBuilder builder = new DungeonBuilder(generator, chunkpos, rand);
			this.components.addAll(builder.build());
			this.recalculateStructureSize();
			DungeonCrawl.LOGGER.info("Created dungeon layout for [{}, {}] ({} ms) ({} pieces). BoundingBox: ({}, {}, {})",
					chunkX, chunkZ, (System.currentTimeMillis() - now), this.components.size(),
					bounds.maxX - bounds.minX, bounds.maxY - bounds.minY, bounds.maxZ - bounds.minZ);
		}

		@Override
		public void generateStructure(IWorld worldIn, Random rand, MutableBoundingBox structurebb, ChunkPos pos) {
			if (!Config.IGNORE_DIMENSION.get() && !(worldIn.getDimension().getType() == DimensionType.OVERWORLD)) {
				DungeonCrawl.LOGGER.warn("Refusing to generate a placed Dungeon in {} because it is not in OVERWORLD.",
						worldIn.getDimension().getType());
				return;
			}
//			DungeonSegmentModelRegistry.load(((ServerWorld) worldIn.getWorld()).getServer().getResourceManager());
			super.generateStructure(worldIn, rand, structurebb, pos);
		}

	}

}