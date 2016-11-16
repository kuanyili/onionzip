# Onionzip - Universal Unzip

List and extract ZIP archive with arbitrary filename encoding. No more mojibake in decompressed filenames!

## Installation

### Requirements

- JDK 7 or up
- Gradle

### Install on Unix-Like Systems

1. Clone the Onionzip repository
   ```bash
   $ git clone https://github.com/kuanyili/onionzip.git
   ```

2. Compile and install
   ```bash
   $ cd onionzip
   $ gradle installDist
   ```

3. Add `REPO_DIR/build/install/onionzip/bin` to `PATH` environment variable

## Usage

```
usage: onionzip [OPTIONS] ZIP_FILE
 -c,--charset <arg>        charset of filenames in the archive (detected
                           if not given)
 -h,--help                 print this message
 -l,--list                 list contents of archive
 -s,--supported-charsets   list supported charsets
```

## Note

Password-protected archive is not yet supported.

## License

Apache License, Version 2.0
