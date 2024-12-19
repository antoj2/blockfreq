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

        ArrayList<String> blockNames = new ArrayList<>();

        for (TerrainChunk chunk : mcaWorld) {
            if (chunk == null || chunk.getBlockAt(0, -64, 0).get("Name").valueToString().equals(AIR))
                continue;

            System.out.printf("Processing chunk at: %d, %d\n", chunk.getChunkX() * 16, chunk.getChunkZ() * 16);
            for (TerrainSection section : chunk) {
                PalettizedCuboid<CompoundTag> blockStates = section.getBlockStates();
                if (blockStates == null) continue;

                int yLevel = section.getSectionY() * 16;
                int counter = 0;

                for (CompoundTag j : blockStates) {
                    counter++;
                    int finalYLevel = yLevel;
                    String blockName = j.get("Name").valueToString().replace("\"", "");
                    if (!blockNames.contains(blockName)) {
                        blockNames.add(blockName);
                    }
                    yBlockMap.compute(finalYLevel, (_, value) -> {
                        if (value == null) {
                            value = new TreeMap<>();
                        }

                        value.compute(blockName, (_, value1) -> (value1 == null) ? 1 : value1 + 1);
                        return value;
                    });
                    if (counter >= 256) {
                        yLevel++;
                        counter = 0;
                    }
                }

            }
        }

        blockNames.sort(Comparator.naturalOrder());

        System.out.println(yBlockMap);
        System.out.println(blockNames);

        Main.convertToCSV(yBlockMap, blockNames);
    }

    public static void convertToCSV(Map<Integer, Map<String, Integer>> map, ArrayList<String> blockNames) throws IOException {
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