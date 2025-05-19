package com.github.antoj2.blockfreq;

import picocli.CommandLine;

import java.io.File;

public class ExistingFileConverter implements CommandLine.ITypeConverter<File> {
    @Override
    public File convert(String value) throws Exception {
        File f = new File(value);
        if (!f.exists()) {
            throw new CommandLine.ParameterException(null, "File does not exist: " + f);
        }
        return f;
    }
}
