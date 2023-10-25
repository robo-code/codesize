package codesize;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Codesize is a tool for calculating the bytecode size of a Java class file, ZIP/JAR archive.
 *
 * @author Christian D. Schnell (original) - for Java 1.3
 * @author Flemming N. Larsen (contributor) - Updated for Java 1.4, 5, 6, 7, 8 and 9
 * @author Michael Jung - Release of version 1.1 to the Maven Repository
 */
public class Codesize {
    private final static int DEFAULT_BUFFER_SIZE = 512 * 1024; // 512 KB

    private static boolean verbose;

    private Codesize() {
    }

    /**
     * Container which keeps information extracted by Codesize.
     *
     * @author Christian D. Schnell
     * @see Codesize#processClassFile(File)
     * @see Codesize#processDirectory(File)
     * @see Codesize#processZipFile(File)
     * @see Codesize#processZipFile(File, ZipInputStream)
     */
    public static class Item implements Comparable<Item> {
        private final File location;
        private final int nClassFiles, ttlClassSize, ttlCodeSize;

        Item(File location, int nClassFiles, int ttlClassSize, int ttlCodeSize) {
            this.location = location;
            this.nClassFiles = nClassFiles;
            this.ttlClassSize = ttlClassSize;
            this.ttlCodeSize = ttlCodeSize;
        }

        /**
         * Returns the file location of the item.
         * @return the file location of the item.
         */
        public File getLocation() {
            return location;
        }

        /**
         * Returns the number of found class files.
         * @return the number of found class files.
         */
        public int getNClassFiles() {
            return nClassFiles;
        }

        /**
         * Returns the total size of all found class files.
         * @return the total size of all found class files.
         */
        public int getClassSize() {
            return ttlClassSize;
        }

        /**
         * Returns the total code size of all found class files.
         * @return the total code size of all found class files.
         */
        public int getCodeSize() {
            return ttlCodeSize;
        }

        /**
         * Compares this item with another item based on their code sizes.
         *
         * @param item the item to be compared
         * @return a negative integer, zero, or a positive integer as this item is less than, equal to, or greater
         * than the specified item.
         */
        @Override
        public int compareTo(Item item) {
            return ttlCodeSize - item.ttlCodeSize;
        }
    }

    /**
     * Processes the arguments given from a command line.
     *
     * @param args the arguments given from the command line
     * @return a list of retrieved Codesize items
     */
    private static List<Item> processCmdLine(String[] args) {
        List<Item> result = new ArrayList<>();

        File file;
        Item item;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-r")) {
                File repository = new File(args[++i]);
                String[] files = repository.list();
                if (files == null) continue;

                for (String s : files) {
                    file = new File(repository, s);
                    if (s.toLowerCase().endsWith(".class")) {
                        item = processClassFile(file);
                    } else {
                        item = processZipFile(file);
                    }
                    if (item != null) {
                        result.add(item);
                    }
                }
            } else {
                file = new File(args[i]);
                if (file.isDirectory()) {
                    item = processDirectory(file);
                } else if (args[i].toLowerCase().endsWith(".class")) {
                    item = processClassFile(file);
                } else {
                    item = processZipFile(file);
                }
                if (item != null) {
                    result.add(item);
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Adds all class files that exists under the specified directory and all it's
     * subdirectories to the specified list of class files.
     *
     * @param directory the directory containing the class files to add
     * @param result    the list to add all found class files to
     */
    private static void deepListClassFiles(File directory, List<File> result) {
        String[] files = directory.list();
        if (files == null) return;

        for (String s : files) {
            File file = new File(directory, s);
            if (file.isDirectory()) {
                deepListClassFiles(file, result);
            } else if (s.toLowerCase().endsWith(".class")) {
                result.add(file);
            }
        }
    }

    /**
     * Returns the filename of the specified file.
     *
     * @param file the file to extract the filename from
     */
    private static String stripFilename(File file) {
        String result = file.toString();

        if (result.contains(File.separator)) {
            result = result.substring(result.lastIndexOf(File.separator) + 1);
        }
        return result;
    }

    /**
     * Prints out the help information for Codesize.
     */
    private static void help() {
        Package p = Codesize.class.getPackage();
        String title = p.getImplementationTitle();
        String version = p.getImplementationVersion();

        String text = title +
                " version " +
                version +
                " - https://github.com/robo-code/codesize\n" +
                "SYNTAX:\n\n" +
                "  codesize [-v] [<class-file> | <zip-file> | <directory> | -r <repository>]+\n\n" +
                "- <class-file> is a single .class file\n" +
                "- <zip-file> is a zip compressed file (or a .jar file)\n" +
                "- <directory> is treated like an uncompressed <zip-file>,\n" +
                "  recursively processing any subdirectories\n" +
                "- <repository> is a directory like '<robocode>/robots':\n" +
                "  - any class file in it is treated like a <class-file>\n" +
                "  - any zip file in it is treated like a <zip-file>\n" +
                "  - any subdirectory is ignored (can't distinguish different robots here)\n" +
                "- specify -v for verbose output";

        System.out.println(text);
    }

    /**
     * Returns the code size of a class from an InputStream.
     *
     * @param inputStream the input stream of the class
     * @param filename    the filename of the class
     * @throws IOException when an I/O error occurs.
     */
    private static int processClassInputStream(InputStream inputStream, String filename) throws IOException {
        int result = 0;

        ClassParser classParser = new ClassParser(inputStream, filename);
        Method[] methods = classParser.parse().getMethods();

        for (Method method : methods) {
            Code code = method.getCode();

            if (code != null) {
                result += code.getCode().length;
            }
        }

        if (verbose) {
            System.out.println(filename + " code size: " + result);
        }

        return result;
    }

    /**
     * Extracts code size information for a class file.
     *
     * @param classFile the filename of the class file
     * @return the extracted Codesize information for the class file
     */
    private static Item processClassFile(File classFile) {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(classFile.toPath()))) {
            return new Item(classFile, 1, (int) classFile.length(),
                    processClassInputStream(inputStream, classFile.getName()));
        } catch (IOException e) {
            System.err.println("Ignoring " + stripFilename(classFile) + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Extracts code size information for a directory.
     *
     * @param directory the filename of the directory
     * @return the extracted Codesize information about the directory
     */
    public static Item processDirectory(File directory) {
        int ttlClassSize = 0, ttlCodeSize = 0;

        List<File> classFiles = new ArrayList<>();

        deepListClassFiles(directory, classFiles);

        for (File classFile : classFiles) {
            Item item = processClassFile(classFile);
            if (item == null) continue;

            ttlClassSize += item.ttlClassSize;
            ttlCodeSize += item.ttlCodeSize;
        }
        return new Item(directory, classFiles.size(), ttlClassSize, ttlCodeSize);
    }

    /**
     * Extracts code size information for a zip file.
     *
     * @param zipFile the filename of the zip file
     * @return the extracted Codesize information for the zip file
     */
    public static Item processZipFile(File zipFile) {
        if (verbose) {
            System.out.println("Processing zip file " + zipFile.getName());
        }
        try {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(Files.newInputStream(zipFile.toPath())))) {
                return processZipFile(zipFile, inputStream);
            }
        } catch (IOException e) {
            System.err.println("Ignoring " + stripFilename(zipFile) + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Extracts code size information for a zip file given a ZipInputStream.
     *
     * @param zipFile     the filename of the zip file
     * @param inputStream the input stream of the zip file
     * @return the extracted Codesize information for the zip file
     * @throws IOException when an I/O error occurs.
     */
    public static Item processZipFile(File zipFile, ZipInputStream inputStream) throws IOException {
        int nClassFiles = 0, ttlClassSize = 0, ttlCodeSize = 0;

        ZipEntry zipEntry;

        while ((zipEntry = inputStream.getNextEntry()) != null) {
            String lcName = zipEntry.getName().toLowerCase();

            if (lcName.endsWith(".class")) {
                ByteArrayOutputStream baos = getByteArrayOutputStream(inputStream, (int) zipFile.length());

                ttlCodeSize += processClassInputStream(new ByteArrayInputStream(baos.toByteArray()), zipEntry.getName());
                ttlClassSize += baos.size();
                nClassFiles++;
            } else if (lcName.endsWith(".jar")) {
                ByteArrayOutputStream baos = getByteArrayOutputStream(inputStream, (int) zipFile.length());

                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                    Item item = processZipFile(zipFile, zis);

                    ttlCodeSize += item.ttlCodeSize;
                    ttlClassSize += item.ttlClassSize;
                }
            }
        }
        if (ttlCodeSize == 0) {
            throw new IOException("total code size is 0");
        }
        return new Item(zipFile, nClassFiles, ttlClassSize, ttlCodeSize);
    }

    /**
     * Reads all bytes from ZipInputStream and returns a ByteArrayOutputStream
     * containing all read bytes that can be read.
     *
     * @param zis    the ZipInputStream to read from
     * @param length the length of the ZipInputStream, or -1 if the length is unknown
     * @return a ByteArrayOutputStream containing all bytes from the
     * ZipInputStream
     * @throws IOException when an I/O error occurs.
     */
    private static ByteArrayOutputStream getByteArrayOutputStream(ZipInputStream zis, int length) throws IOException {
        if (length < 0) {
            length = DEFAULT_BUFFER_SIZE;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int nRead;

        byte[] buf = new byte[length];
        while ((nRead = zis.read(buf, 0, length)) > -1) {
            baos.write(buf, 0, nRead);
        }
        return baos;
    }

    /**
     * Dumps a list of Codesize items to the specified PrintStream.
     *
     * @param items  the list of items to print out
     * @param target the PrintStream to print the items to
     */
    public static void dump(List<Item> items, PrintStream target) {
        target.println("\tCode\tClass\tClass");
        target.println("Nr\tsize\tsize\tfiles\tLocation");
        target.println("--------------------------------------------------------------------");

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);

            target.println(
                    (i + 1) + "\t" + item.ttlCodeSize + "\t" + item.ttlClassSize + "\t" + item.nClassFiles + "\t"
                            + stripFilename(item.location));
        }
    }

    /**
     * The main entry for running the Codesize tool from the command line.
     *
     * @param args the arguments given from the command line
     */
    public static void main(String[] args) {
        List<Item> items = processCmdLine(args);
        if (items.isEmpty()) {
            help();
        } else {
            dump(items, System.out);
        }
    }
}
