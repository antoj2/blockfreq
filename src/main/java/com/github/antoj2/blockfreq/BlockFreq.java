package com.github.antoj2.blockfreq;

import io.github.ensgijs.nbt.mca.McaRegionFile;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class BlockFreq {
    static String INCLUDEFILE = "include.txt";
    static String EXCLUDEFILE = "exclude.txt";

    public record FilterGroups(HashSet<String> include, HashSet<String> exclude, boolean includeEmpty,
                               boolean excludeEmpty) {
    }

    static FilterGroups filter;

    static {
        try {
            filter = computeFilters();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        String currentJar = new File(BlockFreq.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();

        if (args.length == 0) {
            System.out.println("You need to provide the right amount of parameters");
            System.out.println("Help: `java -jar " + currentJar + " (-h) <input_region_file output_csv>");
            System.exit(1);
        }
        if (args[0].equals("-h")) {
            System.out.println("Help: `java -jar " + currentJar + " (-h) <input_region_file.mca output_csv.csv>");
            System.exit(0);
        }
        if (!args[0].endsWith(".mca") || !args[1].endsWith(".csv")) {
            System.out.println("Invalid file extension");
            System.exit(1);
        }

        // I think we can assume args[0] and args[1] is the region file and csv file respectively

        String input = args[0];
        String output = args[1];

        File csvFile = new File(output);
        if (csvFile.exists()) {
            if (!askForConfirmation(String.format("%s already exists. Do you wish to continue? (Y/n)", output)))
                System.exit(0);
        }

        McaRegionFile mcaWorld = McaFileHelpers.readAuto(new File(input));

        System.out.println(Arrays.toString(args));

        McaProcessorBiomed mcaProcessor = new McaProcessorBiomed();
        McaProcessorBiomed.ChunkResultBiomed regionResult = mcaProcessor.processRegion(mcaWorld, filter);

        System.out.println("Finished processing chunks");
        System.out.println("regionResult = " + regionResult);

        for (Map.Entry<String, McaProcessor.ChunkResult> biome : regionResult.biomes().entrySet()) {
            List<String> blockNames = new ArrayList<>(biome.getValue().blockNameSet());
            Collections.sort(blockNames);

            System.out.println(biome.getValue().yBlockMap());

            BlockFreq.convertToCSV(biome.getValue().yBlockMap(), blockNames, biome.getKey() + ".csv");
        }

//        List<String> blockNames = new ArrayList<>(regionResult.blockNameSet());
//        Collections.sort(blockNames);
//
//        System.out.println(regionResult.yBlockMap());
//
//        BlockFreq.convertToCSV(regionResult.yBlockMap(), blockNames, output);
    }

    private static FilterGroups computeFilters() throws IOException {
        HashSet<String> includes = makeFilterSet(INCLUDEFILE);
        HashSet<String> excludes = makeFilterSet(EXCLUDEFILE);

        return new FilterGroups(includes, excludes, includes.isEmpty(), excludes.isEmpty());
    }

    private static HashSet<String> makeFilterSet(String filterFile) throws IOException {
        HashSet<String> set = new HashSet<>();
        Path path = Paths.get(filterFile);
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                reader.lines().filter(line -> !line.trim().isEmpty()).forEach(set::add);
            }
        }
        return set;
    }

    public static void convertToCSV(Map<Integer, Map<String, Integer>> map, List<String> blockNames, String fileName) throws IOException {
        File csvFile = new File(fileName);
        csvFile.createNewFile();

        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < blockNames.size(); i++) {
            nameToIndex.put(blockNames.get(i), i);
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, false));
        writer.write("Y-level," + String.join(",", blockNames) + "\n");

        for (Map.Entry<Integer, Map<String, Integer>> l : map.entrySet()) {
            int[] blocks = new int[blockNames.size()];
            for (Map.Entry<String, Integer> e : l.getValue().entrySet()) {
                blocks[nameToIndex.get(e.getKey())] = e.getValue();
            }
            writer.write(l.getKey() + "," + String.join(",", Arrays.stream(blocks).mapToObj(String::valueOf).toArray(String[]::new)) + "\n");
        }

        writer.close();
    }

    public static boolean askForConfirmation(String prompt) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;

        while (true) {
            System.out.println(prompt);
            input = reader.readLine().trim();

            // default to yes
            if (input.isEmpty()) {
                return true;
            }

            if (input.equalsIgnoreCase("y")) return true;
            if (input.equalsIgnoreCase("n")) return false;
        }
    }
}
