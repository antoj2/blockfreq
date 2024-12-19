package org.example;

import io.github.ensgijs.nbt.mca.McaRegionFile;
import io.github.ensgijs.nbt.mca.TerrainChunk;
import io.github.ensgijs.nbt.mca.TerrainSection;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.util.PalettizedCuboid;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Main {
    static String AIR = "\"minecraft:air\"";

    public static void main(String[] args) throws IOException {
        McaRegionFile mcaWorld = McaFileHelpers.readAuto(new File("/Users/antonjensen/Library/Application Support/minecraft/saves/New World/region/r.0.0.mca"));

        Map<Integer, Map<String, Integer>> yBlockMap = new TreeMap<>();

        Set<String> blockNameSet = new LinkedHashSet<>();

        for (TerrainChunk chunk : mcaWorld) {
            if (chunk == null || chunk.getBlockAt(0, -64, 0).get("Name").valueToString().equals(AIR))
                continue;

            System.out.printf("Processing chunk at: %d, %d\n", chunk.getChunkX() * 16, chunk.getChunkZ() * 16);
            for (TerrainSection section : chunk) {
                PalettizedCuboid<CompoundTag> blockStates = section.getBlockStates();
                if (blockStates == null) continue;

                int yLevel = section.getSectionY() * 16;
                int index = 1;

                for (CompoundTag j : blockStates) {
                    int currentY = yLevel + index / 256;
                    String blockName = j.get("Name").valueToString();

                    blockNameSet.add(blockName);

                    yBlockMap.computeIfAbsent(currentY, k -> new TreeMap<>()).merge(blockName, 1, Integer::sum);

                    index++;
                }

            }
        }

        List<String> blockNames = new ArrayList<>(blockNameSet);
        Collections.sort(blockNames);

        System.out.println(yBlockMap);
        System.out.println(blockNameSet);

        Main.convertToCSV(yBlockMap, blockNames);
    }

    public static void convertToCSV(Map<Integer, Map<String, Integer>> map, List<String> blockNames) throws IOException {
        File csvFile = new File("1.csv");
        boolean f = csvFile.createNewFile();
        if (!f) {
            throw new IOException("1.csv already exists");
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));
        writer.write("Blocks;" + String.join(";", blockNames) + "\n");

        for (Map.Entry<Integer, Map<String, Integer>> l : map.entrySet()) {
            int[] blocks = new int[blockNames.size()];
            for (Map.Entry<String, Integer> e : l.getValue().entrySet()) {
                blocks[blockNames.indexOf(e.getKey())] = e.getValue();
            }
            writer.write(l.getKey() + ";" + String.join(";", Arrays.stream(blocks).mapToObj(String::valueOf).toArray(String[]::new)) + "\n");
        }

        writer.close();
    }
}