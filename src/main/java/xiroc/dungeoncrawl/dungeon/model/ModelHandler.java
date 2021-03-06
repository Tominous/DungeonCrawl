package xiroc.dungeoncrawl.dungeon.model;

/*
 * 
 * DungeonCrawl (C) 2019 - 2020 XYROC (XIROC1337), All Rights Reserved 
 */

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Lists;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xiroc.dungeoncrawl.DungeonCrawl;
import xiroc.dungeoncrawl.dungeon.model.DungeonModel.FeaturePosition;

public class ModelHandler {

	public static void readAndSaveModelToFile(String name, DungeonModel.EntranceType entranceType, World world,
			BlockPos pos, int width, int height, int length, int spawnerType, int chestType) {
		DungeonCrawl.LOGGER.info("Reading and writing {} to disk. Size: {}, {}, {}. Entrance Type: {}", name, width,
				height, length, entranceType);
		DungeonModelBlock[][][] model = new DungeonModelBlock[width][height][length];

		List<FeaturePosition> featurePositions = Lists.newArrayList();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					BlockState state = world
							.getBlockState(new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z));
					if (state.getBlock() == Blocks.AIR) {
						model[x][y][z] = null;
						continue;
					} else if (state.getBlock() == Blocks.JIGSAW) {
						DungeonCrawl.LOGGER.debug("Found a feature position at {} {} {}", x, y, z);
						featurePositions.add(new FeaturePosition(x, y, z, state.get(BlockStateProperties.FACING)));
						continue;
					}
					model[x][y][z] = new DungeonModelBlock(
							DungeonModelBlockType.get(state.getBlock(), spawnerType, chestType))
									.loadDataFromState(state);
				}
			}
		}
		writeModelToFile(
				new DungeonModel(model, entranceType,
						featurePositions.isEmpty() ? null
								: featurePositions.toArray(new FeaturePosition[featurePositions.size()])),
				((ServerWorld) world).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/" + name + ".nbt");
	}

	public static void writeModelToFile(DungeonModel model, String file) {
		try {
			DungeonCrawl.LOGGER.info("Writing a model to disk at {}. ", file);
			if (model.featurePositions != null)
				DungeonCrawl.LOGGER.info("There are {} feature positions.", model.featurePositions.length);
			convertModelToNBT(model).write(new DataOutputStream(new FileOutputStream(file)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static DungeonModel readModelFromFile(File file) {
		DungeonCrawl.LOGGER.info("Loading model from file " + file.getAbsolutePath());
		try {
			FileReader reader = new FileReader(file);
			return DungeonCrawl.GSON.fromJson(reader, DungeonModel.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static DungeonModel readModelFromInputStream(InputStream input) {
		try {
			InputStreamReader reader = new InputStreamReader(input);
			return DungeonCrawl.GSON.fromJson(reader, DungeonModel.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static CompoundNBT convertModelToNBT(DungeonModel model) {
		byte width = (byte) (model.width > 127 ? -(model.width - 127) : model.width),
				length = (byte) (model.length > 127 ? -(model.length - 127) : model.length),
				height = (byte) (model.height > 127 ? -(model.height - 127) : model.height);

		CompoundNBT newModel = new CompoundNBT();
		newModel.putByte("length", length);
		newModel.putByte("height", height);
		newModel.putByte("width", width);

		newModel.putString("entranceType", model.entranceType.toString());

		ListNBT blocks = new ListNBT();

		for (int x = 0; x < model.width; x++) {
			ListNBT blocks2 = new ListNBT();

			for (int y = 0; y < model.height; y++) {
				ListNBT blocks3 = new ListNBT();

				for (int z = 0; z < model.length; z++) {
					if (model.model[x][y][z] != null)
						blocks3.add(model.model[x][y][z].getAsNBT());
					else
						blocks3.add(new CompoundNBT());
				}
				blocks2.add(blocks3);

			}
			blocks.add(blocks2);

		}

		if (model.featurePositions != null && model.featurePositions.length > 0) {
			ListNBT list = new ListNBT();
			CompoundNBT amount = new CompoundNBT();
			amount.putInt("amount", model.featurePositions.length);
			list.add(amount);
			for (FeaturePosition pos : model.featurePositions) {
				CompoundNBT vecCompound = new CompoundNBT();
				vecCompound.putInt("x", pos.position.getX());
				vecCompound.putInt("y", pos.position.getY());
				vecCompound.putInt("z", pos.position.getZ());
				if (pos.facing != null)
					vecCompound.putString("facing", pos.facing.toString());
				list.add(vecCompound);
			}
			newModel.put("featurePositions", list);
		}

		newModel.put("model", blocks);

		return newModel;
	}

	public static DungeonModel getModelFromNBT(CompoundNBT nbt) {
		int width = nbt.getInt("width"), height = nbt.getInt("height"), length = nbt.getInt("length");

		ListNBT blocks = nbt.getList("model", 9);

		DungeonModelBlock[][][] model = new DungeonModelBlock[width][height][length];

		for (int x = 0; x < width; x++) {
			ListNBT blocks2 = blocks.getList(x);
			for (int y = 0; y < height; y++) {
				ListNBT blocks3 = blocks2.getList(y);
				for (int z = 0; z < length; z++)
					model[x][y][z] = DungeonModelBlock.fromNBT(blocks3.getCompound(z));
			}
		}

		FeaturePosition[] featurePositions = null;

		if (nbt.contains("featurePositions", 9)) {
			ListNBT list = nbt.getList("featurePositions", 10);
			int amount = list.getCompound(0).getInt("amount");
			featurePositions = new FeaturePosition[amount];
			for (int i = 1; i < list.size(); i++) {
				CompoundNBT compound = list.getCompound(i);
				if (compound.contains("facing")) {
					featurePositions[i - 1] = new FeaturePosition(compound.getInt("x"), compound.getInt("y"),
							compound.getInt("z"), Direction.valueOf(compound.getString("facing").toUpperCase(Locale.ROOT)));
				} else {
					featurePositions[i - 1] = new FeaturePosition(compound.getInt("x"), compound.getInt("y"),
							compound.getInt("z"));
				}
			}
		}

		return new DungeonModel(model,
				nbt.contains("entranceType") ? DungeonModel.EntranceType.valueOf(nbt.getString("entranceType"))
						: DungeonModel.EntranceType.OPEN,
				featurePositions);
	}

}
