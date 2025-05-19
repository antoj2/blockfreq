package com.github.antoj2.blockfreq;

import io.github.ensgijs.nbt.mca.McaRegionFile;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import picocli.CommandLine;

import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(name = "BlockFreq", version = "BlockFreq 2025.05.18", mixinStandardHelpOptions = true)
public class BlockFreq implements Runnable {
    @CommandLine.Option(names = {"-I", "--include"}, description = "File to use for include filtering", converter = ExistingFileConverter.class)
    File includeFile;

    @CommandLine.Option(names = {"-E", "--exclude"}, description = "File to use for exclude filtering", converter = ExistingFileConverter.class)
    File excludeFile;

    @CommandLine.Option(names = "-b", description = "Create multiple csv files for different biomes")
    boolean biome;

    @CommandLine.Option(names = {"-o", "--output"},
            description = "Filename to call the output csv file. \nWill be used as a format if -b is specified, replacing {} with the biome name")
    File output;

    @CommandLine.Parameters(arity = "1..*", paramLabel = "<input>", description = "Files or directories to analyze")
    File[] inputs;

    public record FilterGroups(HashSet<String> include, HashSet<String> exclude, boolean includeEmpty,
                               boolean excludeEmpty) {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BlockFreq()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        FilterGroups filter;
        try {
            filter = this.computeFilters();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<File> flattened;
        try {
            flattened = flattenInput(inputs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (biome) {
            McaProcessorBiomed.ChunkResultBiomed collected = flattened.parallelStream().map(file -> {
                try {
                    McaRegionFile mcaFile = McaFileHelpers.readAuto(file);
                    McaProcessorBiomed mcaProcessor = new McaProcessorBiomed();
                    return mcaProcessor.processRegion(mcaFile, filter);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading " + file);
                }
            }).reduce(McaProcessorBiomed.ChunkResultBiomed::merge).orElseGet(McaProcessorBiomed.ChunkResultBiomed::new);

            for (Map.Entry<String, McaProcessor.ChunkResult> biome : collected.biomes().entrySet()) {
                List<String> blockNames = new ArrayList<>(biome.getValue().blockNameSet());
                Collections.sort(blockNames);

                System.out.println(biome.getValue().yBlockMap());

                if (output != null) {
                    try {
                        BlockFreq.convertToCSV(biome.getValue().yBlockMap(), blockNames, output.getName().replace("{}", biome.getKey()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            McaProcessor.ChunkResult collected = flattened.parallelStream().map(file -> {
                try {
                    McaRegionFile mcaFile = McaFileHelpers.readAuto(file);
                    McaProcessor mcaProcessor = new McaProcessor();
                    return mcaProcessor.processRegion(mcaFile, filter);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading " + file);
                }
            }).reduce(McaProcessor.ChunkResult::merge).orElseGet(McaProcessor.ChunkResult::new);

            List<String> blockNames = new ArrayList<>(collected.blockNameSet());
            Collections.sort(blockNames);

            System.out.println(collected.yBlockMap());

            if (output != null) {
                try {
                    BlockFreq.convertToCSV(collected.yBlockMap(), blockNames, output.getName());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private FilterGroups computeFilters() throws IOException {
        HashSet<String> includes = new HashSet<>();
        if (includeFile != null) {
            includes = makeFilterSet(includeFile);
        }
        HashSet<String> excludes = new HashSet<>();
        if (excludeFile != null) {
            excludes = makeFilterSet(excludeFile);
        }

        return new FilterGroups(includes, excludes, includes.isEmpty(), excludes.isEmpty());
    }

    private ArrayList<File> flattenInput(File[] inputs) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        for (File file : inputs) {
            File[] listFiles = file.listFiles(McaFileHelpers::isValidMcaFileName);
            if (listFiles != null) {
                // file is a directory
                files.addAll(List.of(listFiles));
            } else {
                files.add(file);
            }
        }
        return files;
    }

    private static HashSet<String> makeFilterSet(File filterFile) throws IOException {
        HashSet<String> set = new HashSet<>();
        Path path = filterFile.toPath();
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
