package techguns.world;

import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.Tags;

/**
 * A 1.12 scanned structure (WorldgenStructure + BlockUtils.placeScannedStructure), converted to an NBT template by
 * tools/port/port_structures.py. Placed at a random rotation in the corner of its chunk:
 * <ul>
 * <li>{@code surface}: on dry land whose height varies at most {@code height_diff}, template floor at the top block + {@code y_offset};</li>
 * <li>{@code water}: floating at sea level - 3 (the aircraft carrier);</li>
 * <li>{@code nether}: on a cave floor with 10 free blocks above; {@code foundation} copies the bottom layer down that far.</li>
 * </ul>
 * The 1.12 random block choices are data structure blocks whose metadata is a JSON node, resolved by {@link Piece#resolve}.
 */
public class TemplateStructure extends Structure {
	public enum Placement implements StringRepresentable {
		SURFACE, WATER, NETHER;

		public static final Codec<Placement> CODEC = StringRepresentable.fromEnum(Placement::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public static final MapCodec<TemplateStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
			ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
			Placement.CODEC.optionalFieldOf("placement", Placement.SURFACE).forGetter(s -> s.placement),
			Codec.INT.optionalFieldOf("y_offset", 0).forGetter(s -> s.yOffset),
			Codec.INT.optionalFieldOf("height_diff", 3).forGetter(s -> s.heightDiff),
			Codec.INT.optionalFieldOf("foundation", 0).forGetter(s -> s.foundation)).apply(i, TemplateStructure::new));

	final ResourceLocation template;
	final Placement placement;
	final int yOffset, heightDiff, foundation;

	public TemplateStructure(StructureSettings settings, ResourceLocation template, Placement placement, int yOffset, int heightDiff, int foundation) {
		super(settings);
		this.template = template;
		this.placement = placement;
		this.yOffset = yOffset;
		this.heightDiff = heightDiff;
		this.foundation = foundation;
	}

	@Override
	public StructureType<?> type() {
		return TGWorldgen.TEMPLATE.get();
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		Rotation rotation = Rotation.getRandom(context.random());
		StructureTemplate tpl = context.structureTemplateManager().getOrCreate(template);
		BlockPos corner = context.chunkPos().getWorldPosition();
		// shift the template so its rotated box starts at the chunk corner
		BoundingBox box = tpl.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), corner);
		BlockPos origin = corner.offset(corner.getX() - box.minX(), 0, corner.getZ() - box.minZ());
		int y = switch (placement) {
			case SURFACE -> surfaceY(context, corner, box.getXSpan(), box.getZSpan());
			case WATER -> waterY(context, corner, box.getXSpan(), box.getZSpan());
			case NETHER -> netherY(context, corner, box.getXSpan(), box.getZSpan());
		};
		if (y == Integer.MIN_VALUE) return Optional.empty();
		BlockPos pos = origin.atY(y + yOffset);
		return Optional.of(new GenerationStub(pos, builder -> builder.addPiece(
				new Piece(context.structureTemplateManager(), template, pos, rotation, foundation))));
	}

	/** 1.12 BlockUtils.getValidSpawnYArea: average top block on a grid of step 4, no water, at most heightDiff apart. */
	private int surfaceY(GenerationContext context, BlockPos corner, int sizeX, int sizeZ) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, count = 0;
		for (int x = 0; x <= sizeX; x = x < sizeX && x + 4 > sizeX ? sizeX : x + 4) {
			for (int z = 0; z <= sizeZ; z = z < sizeZ && z + 4 > sizeZ ? sizeZ : z + 4) {
				int h = context.chunkGenerator().getFirstOccupiedHeight(corner.getX() + x, corner.getZ() + z, Heightmap.Types.OCEAN_FLOOR_WG,
						context.heightAccessor(), context.randomState());
				int top = context.chunkGenerator().getFirstOccupiedHeight(corner.getX() + x, corner.getZ() + z, Heightmap.Types.WORLD_SURFACE_WG,
						context.heightAccessor(), context.randomState());
				if (top != h) return Integer.MIN_VALUE;
				min = Math.min(min, h);
				max = Math.max(max, h);
				sum += h;
				count++;
				if (z == sizeZ) break;
			}
			if (x == sizeX) break;
		}
		return max - min > heightDiff ? Integer.MIN_VALUE : sum / count;
	}

	/** On open water at all four corners. */
	private int waterY(GenerationContext context, BlockPos corner, int sizeX, int sizeZ) {
		int sea = context.chunkGenerator().getSeaLevel();
		for (int i = 0; i < 4; i++) {
			int x = corner.getX() + (i & 1) * sizeX, z = corner.getZ() + (i >> 1) * sizeZ;
			if (context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState())
					> sea - 8) return Integer.MIN_VALUE;
		}
		return sea - 3;
	}

	/** 1.12 BlockUtils.getCaveHeight: a floor between y 20 and 100 with 10 air above, at all four corners, at most 10 apart. */
	private int netherY(GenerationContext context, BlockPos corner, int sizeX, int sizeZ) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0;
		for (int i = 0; i < 4; i++) {
			NoiseColumn column = context.chunkGenerator().getBaseColumn(corner.getX() + (i & 1) * sizeX, corner.getZ() + (i >> 1) * sizeZ,
					context.heightAccessor(), context.randomState());
			int floor = Integer.MIN_VALUE, air = 0;
			for (int y = 100; y >= 20; y--) {
				BlockState state = column.getBlock(y);
				if (state.isAir()) air++;
				else if (air >= 10 && state.getFluidState().isEmpty()) { floor = y; break; }
				else air = 0;
			}
			if (floor == Integer.MIN_VALUE) return floor;
			min = Math.min(min, floor);
			max = Math.max(max, floor);
			sum += floor;
		}
		return max - min > 10 ? Integer.MIN_VALUE : sum / 4;
	}

	public static class Piece extends TemplateStructurePiece {
		private final int foundation;

		public Piece(StructureTemplateManager manager, ResourceLocation template, BlockPos pos, Rotation rotation, int foundation) {
			super(TGWorldgen.TEMPLATE_PIECE.get(), 0, manager, template, template.toString(), new StructurePlaceSettings().setRotation(rotation), pos);
			this.foundation = foundation;
		}

		public Piece(StructurePieceSerializationContext context, CompoundTag tag) {
			super(TGWorldgen.TEMPLATE_PIECE.get(), tag, context.structureTemplateManager(),
					id -> new StructurePlaceSettings().setRotation(Rotation.valueOf(tag.getString("Rot"))));
			foundation = tag.getInt("Foundation");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.putString("Rot", placeSettings.getRotation().name());
			tag.putInt("Foundation", foundation);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
				ChunkPos chunk, BlockPos pivot) {
			super.postProcess(level, structures, generator, random, box, chunk, pivot);
			if (foundation <= 0) return;
			// 1.12 BlockUtils.placeFoundation: the bottom layer continues down through air and lava
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = Math.max(box.minX(), boundingBox.minX()); x <= Math.min(box.maxX(), boundingBox.maxX()); x++) {
				for (int z = Math.max(box.minZ(), boundingBox.minZ()); z <= Math.min(box.maxZ(), boundingBox.maxZ()); z++) {
					BlockState bottom = level.getBlockState(pos.set(x, boundingBox.minY(), z));
					if (bottom.isAir()) continue;
					for (int i = 1; i <= foundation && level.getBlockState(pos.setY(boundingBox.minY() - i)).canBeReplaced(); i++) {
						level.setBlock(pos, bottom, 2);
					}
				}
			}
		}

		@Override
		protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
			if (!box.isInside(pos)) return;
			BlockState state = Blocks.AIR.defaultBlockState();
			try {
				String id = resolve(JsonParser.parseString(marker), pos, level);
				state = BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), id, false).blockState();
			} catch (CommandSyntaxException | RuntimeException e) {
				TGWorldgen.LOGGER.error("Bad structure marker {} in {}", marker, templateName, e);
			}
			level.setBlock(pos, Block.updateFromNeighbourShapes(state.rotate(level, pos, placeSettings.getRotation()), level, pos), 2);
		}

		/**
		 * A block state string from a marker node: a state string; {"random": [[node, weight]...]} picked per block;
		 * {"roll": group, "of": [[node, weight]...]} picked once per structure and group (the 1.12 cluster type); {"chance": p, "then": node,
		 * "else": node}; {"biome": [woodland, snow, desert, nether]} (1.12 BiomeColorType).
		 */
		public String resolve(JsonElement node, BlockPos pos, ServerLevelAccessor level) {
			if (node.isJsonPrimitive()) return node.getAsString();
			JsonObject o = node.getAsJsonObject();
			if (o.has("random")) return resolve(pick(o.getAsJsonArray("random"), RandomSource.create(Mth.getSeed(pos))), pos, level);
			if (o.has("roll")) {
				RandomSource rnd = RandomSource.create(Mth.getSeed(templatePosition) ^ o.get("roll").getAsString().hashCode());
				return resolve(pick(o.getAsJsonArray("of"), rnd), pos, level);
			}
			if (o.has("chance")) {
				boolean hit = RandomSource.create(Mth.getSeed(pos) * 31).nextFloat() < o.get("chance").getAsFloat();
				return resolve(o.get(hit ? "then" : "else"), pos, level);
			}
			Holder<Biome> biome = level.getBiome(pos);
			int type = biome.is(BiomeTags.IS_NETHER) ? 3 : biome.is(Tags.Biomes.IS_SNOWY) || biome.is(Tags.Biomes.IS_COLD) ? 1
					: biome.is(Tags.Biomes.IS_SANDY) ? 2 : 0;
			return resolve(o.getAsJsonArray("biome").get(type), pos, level);
		}

		private static JsonElement pick(JsonArray choices, RandomSource rnd) {
			int total = 0;
			for (JsonElement c : choices) total += c.getAsJsonArray().get(1).getAsInt();
			int roll = rnd.nextInt(total);
			for (JsonElement c : choices) if ((roll -= c.getAsJsonArray().get(1).getAsInt()) < 0) return c.getAsJsonArray().get(0);
			throw new IllegalStateException();
		}
	}
}
