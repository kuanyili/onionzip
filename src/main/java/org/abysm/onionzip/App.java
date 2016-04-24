/*
 * Copyright 2016 Kuan-Yi Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.abysm.onionzip;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.mozilla.universalchardet.UniversalDetector;

public class App {
    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addOption("c", "charset", true, "charset for filenames in the archive (detected if not given)");
        options.addOption("h", "help", false, "print this message");
        options.addOption("l", "list", false, "list archive files");
        options.addOption("s", "supported-charsets", false, "list supported charsets");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("supported-charsets")) {
            for (String charsetName : Charset.availableCharsets().keySet()) {
                System.out.println(charsetName);
            }
            return;
        }

        if (cmd.hasOption("help") || cmd.getArgs().length != 1) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("onionzip [OPTIONS] ZIP_FILE", options);
            return;
        }

        String zipFilename = cmd.getArgs()[0];

        String charset;
        if (cmd.hasOption("charset")) {
            charset = cmd.getOptionValue("charset");
        } else {
            charset = detectZipFileCharset(zipFilename);
            System.err.format("Detected charset: %s\n", charset);
        }

        if (cmd.hasOption("list")) {
            listZipFile(zipFilename, charset);
        } else {
            extractZipFile(zipFilename, charset);
        }
    }

    private static String detectZipFileCharset(String name) throws IOException {
        UniversalDetector filenameCharsetDetector = new UniversalDetector(null);
        ZipFile zipFile = new ZipFile(name);
        try {
            for (Enumeration<ZipArchiveEntry> e = zipFile.getEntries(); e.hasMoreElements(); ) {
                ZipArchiveEntry entry = e.nextElement();
                if (!entry.getGeneralPurposeBit().usesUTF8ForNames()) {
                    byte[] filename = entry.getRawName();
                    filenameCharsetDetector.handleData(filename, 0, filename.length);
                }
            }
        } finally {
            ZipFile.closeQuietly(zipFile);
        }
        filenameCharsetDetector.dataEnd();
        return filenameCharsetDetector.getDetectedCharset();
    }

    private static void listZipFile(String name, String encoding) throws IOException {
        ZipFile zipFile = new ZipFile(name, encoding);
        try {
            System.out.println("Length\tDatetime\tName\tEFS\tUnix Mode");
            for (Enumeration<ZipArchiveEntry> e = zipFile.getEntries(); e.hasMoreElements(); ) {
                ZipArchiveEntry entry = e.nextElement();
                System.out.format(
                        "%d\t%s\t%s\t%b\t%o\n",
                        entry.getSize(),
                        entry.getLastModifiedDate().toString(),
                        entry.getName(),
                        entry.getGeneralPurposeBit().usesUTF8ForNames(),
                        entry.getUnixMode()
                );
            }
        } finally {
            ZipFile.closeQuietly(zipFile);
        }
    }

    private static void extractZipFile(String name, String encoding) throws IOException {
        ZipFile zipFile = new ZipFile(name, encoding);
        try {
            for (Enumeration<ZipArchiveEntry> e = zipFile.getEntries(); e.hasMoreElements(); ) {
                ZipArchiveEntry entry = e.nextElement();
                System.out.println(entry.getName());
                if (entry.isDirectory()) {
                    Path directory = Paths.get(entry.getName());
                    Files.createDirectories(directory);
                } else if (entry.isUnixSymlink()) {
                    Path symlink = Paths.get(entry.getName());
                    Path parentDirectory = symlink.getParent();
                    Path target = Paths.get(zipFile.getUnixSymlink(entry));
                    if (parentDirectory != null) {
                        Files.createDirectories(parentDirectory);
                    }
                    Files.createSymbolicLink(symlink, target);
                } else {
                    Path file = Paths.get(entry.getName());
                    Path parentDirectory = file.getParent();
                    if (parentDirectory != null) {
                        Files.createDirectories(parentDirectory);
                    }
                    InputStream contentInputStream = zipFile.getInputStream(entry);
                    FileOutputStream extractedFileOutputStream = new FileOutputStream(entry.getName());
                    try {
                        IOUtils.copy(contentInputStream, extractedFileOutputStream);
                    } finally {
                        IOUtils.closeQuietly(contentInputStream);
                        IOUtils.closeQuietly(extractedFileOutputStream);
                    }
                    FileTime fileTime = FileTime.fromMillis(entry.getLastModifiedDate().getTime());
                    Files.setLastModifiedTime(file, fileTime);
                }
            }
        } finally {
            ZipFile.closeQuietly(zipFile);
        }
    }
}
