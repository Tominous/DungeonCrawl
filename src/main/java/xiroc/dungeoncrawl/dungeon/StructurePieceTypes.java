package xiroc.dungeoncrawl.dungeon;

/*
 * DungeonCrawl (C) 2019 - 2020 XYROC (XIROC1337), All Rights Reserved 
 */

import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import xiroc.dungeoncrawl.DungeonCrawl;
import xiroc.dungeoncrawl.dungeon.piece.DungeonCorridor;
import xiroc.dungeoncrawl.dungeon.piece.DungeonCorridorHole;
import xiroc.dungeoncrawl.dungeon.piece.DungeonCorridorLarge;
import xiroc.dungeoncrawl.dungeon.piece.DungeonEntranceBuilder;
import xiroc.dungeoncrawl.dungeon.piece.DungeonNodeConnector;
import xiroc.dungeoncrawl.dungeon.piece.DungeonStairs;
import xiroc.dungeoncrawl.dungeon.piece.room.DungeonNodeRoom;
import xiroc.dungeoncrawl.dungeon.piece.room.DungeonRoom;
import xiroc.dungeoncrawl.dungeon.piece.room.DungeonSideRoom;

public class StructurePieceTypes {

	public static final IStructurePieceType ENTRANCE_BUILDER = IStructurePieceType.register(DungeonEntranceBuilder::new,
			create("entrance_builder"));
	public static final IStructurePieceType ROOM = IStructurePieceType.register(DungeonRoom::new, create("room"));
	public static final IStructurePieceType CORRIDOR = IStructurePieceType.register(DungeonCorridor::new,
			create("corridor"));
	public static final IStructurePieceType LARGE_CORRIDOR = IStructurePieceType.register(DungeonCorridorLarge::new,
			create("large_corridor"));
	public static final IStructurePieceType STAIRS = IStructurePieceType.register(DungeonStairs::new, create("stairs"));
	public static final IStructurePieceType HOLE = IStructurePieceType.register(DungeonCorridorHole::new,
			create("corridor_hole"));
	public static final IStructurePieceType SIDE_ROOM = IStructurePieceType.register(DungeonSideRoom::new,
			create("side_room"));
	public static final IStructurePieceType NODE_ROOM = IStructurePieceType.register(DungeonNodeRoom::new,
			create("node_room"));
	public static final IStructurePieceType NODE_CONNECTOR = IStructurePieceType.register(DungeonNodeConnector::new,
			create("node_connector"));

	private static String create(String path) {
		return DungeonCrawl.locate(path).toString();
	}

}
