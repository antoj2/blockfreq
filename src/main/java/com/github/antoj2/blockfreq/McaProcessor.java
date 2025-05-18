package com.github.antoj2.blockfreq;

import io.github.ensgijs.nbt.mca.McaRegionFile;
import io.github.ensgijs.nbt.mca.TerrainChunk;
import io.github.ensgijs.nbt.mca.TerrainSection;
import io.github.ensgijs.nbt.mca.util.PalettizedCuboid;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.StringTag;

import java.util.*;

public class McaProcessor {
    public record ChunkResult(
            Map<Integer, Map<String, Integer>> yBlockMap,
            Set<String> blockNameSet
    ) {
    }

    private final Map<Integer, Map<String, Integer>> yBlockMap = new TreeMap<>();
    private final Set<String> blockNameSet = new LinkedHashSet<>();

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

                int currentY = yLevel + index / 256;

                this.blockNameSet.add(blockName);

                this.yBlockMap.computeIfAbsent(currentY, k -> new TreeMap<>()).merge(blockName, 1, Integer::sum);

                index++;
            }
        }
    }

    public ChunkResult processRegion(McaRegionFile mcaFile, BlockFreq.FilterGroups filter) {
        for (TerrainChunk chunk : mcaFile) {
            this.processChunk(chunk, filter);
        }

        return new ChunkResult(this.yBlockMap, this.blockNameSet);
    }
}