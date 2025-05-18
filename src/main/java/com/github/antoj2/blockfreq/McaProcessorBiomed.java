package com.github.antoj2.blockfreq;

import io.github.ensgijs.nbt.mca.McaRegionFile;
import io.github.ensgijs.nbt.mca.TerrainChunk;
import io.github.ensgijs.nbt.mca.TerrainSection;
import io.github.ensgijs.nbt.mca.util.PalettizedCuboid;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.StringTag;

import java.util.*;
import java.util.stream.Collectors;

public class McaProcessorBiomed {
    public record ChunkResultBiomed(Map<String, McaProcessor.ChunkResult> biomes) {
    }

    private final Map<String, Map<Integer, Map<String, Integer>>> biomeYBlockMap = new TreeMap<>();
    private final Map<String, Set<String>> biomeBlockNameSet = new TreeMap<>();

    public void processChunk(TerrainChunk chunk, BlockFreq.FilterGroups filter) {
        if (chunk == null || !chunk.getStatus().equals("minecraft:full")) return;


        System.out.printf("Processing chunk at: %d, %d\n", chunk.getChunkX() * 16, chunk.getChunkZ() * 16);
        for (TerrainSection section : chunk) {
            PalettizedCuboid<CompoundTag> blockStates = section.getBlockStates();
            if (blockStates == null) continue;

            List<CompoundTag> paletteTag = blockStates.getPalette();
            PalettizedCuboid<StringTag> biomes = section.getBiomes();

            Set<String> paletteBlockNames = new HashSet<>();
            for (CompoundTag blockTag : paletteTag) {
                String blockName = ((StringTag) blockTag.get("Name")).getValue();
                paletteBlockNames.add(blockName);
            }

            if (!filter.includeEmpty() && Collections.disjoint(filter.include(), paletteBlockNames)) continue;
            if (!filter.excludeEmpty() && filter.exclude().containsAll(paletteBlockNames)) continue;

            int yLevel = section.getSectionY() * 16;
            int index = 0;

            for (CompoundTag blockTag : blockStates) {
                String blockName = ((StringTag) blockTag.get("Name")).getValue();
                if ((!filter.includeEmpty() && !filter.include().contains(blockName)) || (!filter.excludeEmpty() && filter.exclude().contains(blockName))) {
                    index++;
                    continue;
                }

                int relX = index % 16;
                int relZ = (index / 16) % 16;
                int relY = (index / 256); // modulus not needed here, since 4095 / 256 < 16

                int currentY = yLevel + relY;

                String biome = biomes.getByRef(relX / 4, relY / 4, relZ / 4).getValue();

                this.biomeBlockNameSet.computeIfAbsent(biome, k -> new HashSet<>()).add(blockName);

                this.biomeYBlockMap.computeIfAbsent(biome, k -> new TreeMap<>()).computeIfAbsent(currentY, k -> new TreeMap<>()).merge(blockName, 1, Integer::sum);

                index++;
            }
        }
    }

    public ChunkResultBiomed processRegion(McaRegionFile mcaFile, BlockFreq.FilterGroups filter) {
        for (TerrainChunk chunk : mcaFile) {
            this.processChunk(chunk, filter);
        }

        return new ChunkResultBiomed(biomeYBlockMap.keySet().stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> new McaProcessor.ChunkResult(biomeYBlockMap.get(key), biomeBlockNameSet.get(key))
                ))
        );
    }
}
