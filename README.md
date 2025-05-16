# blockfreq
A tool for Minecraft to analyze which blocks appear at which y-level. 
This code is largely untested and some of the first lines of Java I have ever written live in this project, so be warned.

## Filtering
The filtering is done with the files `include.txt` and `exclude.txt`. These two files should contain one Minecraft block ID, per line, to respectively include and exclude. If the files are empty, they will be ignored.

## Prerequisites
* Java 16+

## Usage
1. Download the release on the right.
2. From the terminal, run the jar file: \
   `java -jar build/libs/blockfreq-{yyyy.MM.dd}.jar <input_region_file output_csv>`

## Build
1. Get the repository on your computer: \
`git clone https://github.com/antoj2/blockfreq.git` 
2. Change directory, into blockfreq: \
`cd blockfreq` 
3. Compile into a runnable jar file: \
`./gradlew jar`
4. Run the jar file: \
`java -jar build/libs/blockfreq-{yyyy.MM.dd}.jar <input_region_file output_csv>`