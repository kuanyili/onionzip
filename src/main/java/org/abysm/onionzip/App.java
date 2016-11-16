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

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {
    public static void main(String[] args) throws IOException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("c", "charset", true, "charset for filenames in the archive (detected if not given)");
        options.addOption("h", "help", false, "print this message");
        options.addOption("l", "list", false, "list archive files");
        options.addOption("s", "supported-charsets", false, "list supported charsets");

        try {
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
            ZipFileExtractHelper zipFileHandler = new ZipFileExtractHelper(zipFilename);

            if (cmd.hasOption("charset")) {
                zipFileHandler.setEncoding(cmd.getOptionValue("charset"));
            } else {
                zipFileHandler.setEncodingByDetection();
                System.err.format("Detected charset: %s\n", zipFileHandler.getEncoding());
            }

            if (cmd.hasOption("list")) {
                zipFileHandler.list();
            } else {
                zipFileHandler.extract();
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
    }
}
