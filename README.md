# blockfreq
A command-line tool for analyzing block distribution by Y-level in Minecraft region files.
This project is one of my first in Java, and while it works for my use cases, it hasn't been thoroughly tested. Use with caution.

## Prerequisites
* Java 16+

## Usage
Download the release on the right.

Run normally, no filtering, single region file:
```
java -jar blockfreq.jar -o "output.csv" "r.x.z.mca"
```
If `-o` is not provided, the data will just be printed to standard output.

### Notice: 
The output parameter (`-o`) has been omitted in a lot of the commands in the next section for brevity.

### Filtering
Filtering out, or only including specific blocks, can be done by creating a text file (in this case `include.txt` and `exclude.txt`) with one block ID per line.

These files are specified by the parameters `-I, --include` and `-E, --exclude`, respectively including and excluding.
```
java -jar blockfreq.jar -I="include.txt" "r.x.z.mca"
```
```
java -jar blockfreq.jar -E="exclude.txt" "r.x.z.mca"
```

### Multiple region files
You can analyze multiple region files by listing them out individually or by specifying a directory.
```
java -jar blockfreq.jar "r.0.0.mca" "r.0.1.mca" "r.1.0.mca"
```
```
java -jar blockfreq.jar "path/to/world/region"
```
You can also combine files and directories:
```
java -jar blockfreq.jar "path/to/world/region" "r.0.0.mca"
```
Or multiple directories:
```
java -jar blockfreq.jar "path/to/world/region" "path/to/another/world/region"
```

### Creating multiple csv files for different biomes
This is done with the `-b` parameter.
```
java -jar blockfreq.jar -b "path/to/world/region"
```
When using `-b`, the `-o` output becomes a template: `{}` will be replaced with each biome name in the file names.

That means, this:
```
java -jar blockfreq.jar -b -o "cool_biome_{}.csv" "r.x.z.mca"
```
Will save files as, e.g. "cool_biome_minecraft:plains.csv", "cool_biome_minecraft:dark_forest.csv"

### Usage message:
```console
$ java -jar blockfreq.jar -h
Usage: BlockFreq [-bhV] [-E=<excludeFile>] [-I=<includeFile>] [-o=<output>]
                 <input>...
      <input>...          Files or directories to analyze
  -b                      Create multiple csv files for different biomes
  -E, --exclude=<excludeFile>
                          File to use for exclude filtering
  -h, --help              Show this help message and exit.
  -I, --include=<includeFile>
                          File to use for include filtering
  -o, --output=<output>   Filename to call the output csv file.
                          Will be used as a format if -b is specified,
                            replacing {} with the biome name
  -V, --version           Print version information and exit.
```

## Build
1. Clone the repository: \
`git clone https://github.com/antoj2/blockfreq.git` 
2. Change directory: \
`cd blockfreq` 
3. Build the jar file: \
`./gradlew jar`
4. Run the jar file (replace `{date}` with the build date in `YYYY.MM.DD` format, e.g. `2025.05.19`): \
`java -jar build/libs/blockfreq-{date}.jar [-bhV] [-E=<excludeFile>] [-I=<includeFile>] [-o=<output>] <input>...`
