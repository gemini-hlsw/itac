package edu.gemini.tac.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.util.FileCopyUtils;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The unwrapper supports xml, gz, zip (jar) and tar file formats and any nesting of these types. In order to
 * uncompress und unwrap nested files temporary files are written to the file system. All unwrapped files are
 * written to a temporary location in the file system since the import files can potentilly become pretty big
 * because of the pdf files that come with them. The importer will pick up the xml and pdf files from there
 * and copy the pdfs into a permanent location from where they will be accessible in the web frontend.
 */
public class ProposalUnwrapper {
    private static final Logger LOGGER = Logger.getLogger(ProposalUnwrapper.class.getName());
    
    private List<Entry> finalResult;
    private Map<String, Entry> unwrappedFiles;
    private File tmpFolder;

    public ProposalUnwrapper(final InputStream inputStream, final String fileName) {
        tmpFolder = new File("/tmp/itac-proposal-unwrapper");
        tmpFolder.mkdirs();
        finalResult = new ArrayList<Entry>();
        unwrappedFiles = new HashMap<String, Entry>();

        try {
            unwrapStream(inputStream, fileName);
            createFinalResult();
        } catch (IOException e) {
            // if unwrapping fails throw runtime exception to force transaction rollback
            throw new RuntimeException("unwrapping of file " + fileName + " failed", e);
        }
    }

    public List<Entry> getEntries() {
        return finalResult;
    }
    
    public void cleanup() {
        try {
            FileUtils.deleteDirectory(tmpFolder);
        } catch(IOException e) {
            LOGGER.warn("could not delete temporary folder " + tmpFolder.getAbsolutePath(), e);
        }
    }
    
    private void unwrapStream(InputStream inputStream, String fileName) throws IOException {
        // ignore AppleDouble format files (leading ._ in file name) !!
        if (fileName.startsWith("._") || fileName.contains("/._")) {
            LOGGER.log(Level.DEBUG, "ignoring AppleDouble format file " + fileName);
        }
        // deal with packages / compressed stuff
        else if (fileName.endsWith(".zip")) {
            LOGGER.log(Level.DEBUG, "decompressing zip file " + fileName);
            unzipStream(inputStream);
        }
        else if (fileName.endsWith(".gz")) {
            LOGGER.log(Level.DEBUG, "decompressing gzip file " + fileName);
            ungzipStream(inputStream, fileName);
        }
        else if (fileName.endsWith(".tar")) {
            LOGGER.log(Level.DEBUG, "untaring tar file " + fileName);
            untarStream(inputStream);
        }
        else if (fileName.endsWith(".jar")) {
            LOGGER.log(Level.DEBUG, "decompressing jar file " + fileName);
            unzipStream(inputStream);
        }
        // deal with actual files (xml and pdf)
        else if (fileName.endsWith(".xml") || fileName.endsWith(".pdf")) {
            addUnwrappedFile(fileName, inputStream);
        }
        // ignore everything else
        else {
            LOGGER.log(Level.DEBUG, "ignoring file " + fileName);
        }
    }

    /**
     * Cleans all entries from the unwrapped list for which we did only find a pdf but no xml with the same name
     * in the same folder. This is to protect us from imports where peopole just pack together files in a directory
     * without making sure that there's always a pair of an xml with a pdf. Xml without pdf is accepted, the
     * importer will add a default pdf with an empty page in that case.
     */
    private void createFinalResult() throws IOException {
        for (String key : unwrappedFiles.keySet()) {
            Entry e = unwrappedFiles.get(key);
            if (e.getProposal() != null) {
                // only add xmls with pdfs or single xmls, there's nothing we can do with a lonely pdf
                if (e.getPdf() == null) {
                    File emptyPdf = new File(tmpFolder+File.separator+key+".pdf");
                    FileCopyUtils.copy(getClass().getResourceAsStream("empty.pdf"), new FileOutputStream(emptyPdf));
                    e.addFile("pdf", emptyPdf);
                }

                // this entry is now sound and valid, it's a beautiful pair of an xml and pdf with the same name
                // add it to the final list for further processing
                finalResult.add(e);
            }
        }
    }

    private void addUnwrappedFile(String fileName, InputStream inputStream) throws IOException {
        LOGGER.log(Level.DEBUG, "adding unwrapped file " + fileName);

        // ok, this is a file we are interested in, we copy it from the input stream to
        // the designated temporary location to be able to access it later in the import process
        // (NOTE: The file name can include folders since the tared / zipped files can contain an arbitrary
        // folder structure. Therefore we recreate this here in order not to run into problems. During import
        // all files will go into a flat structure using the proposal id to have guaranteed unique file names)
        final File file = new File(tmpFolder.getAbsolutePath()+File.separatorChar+fileName);
        file.getParentFile().mkdirs();
        final FileOutputStream stream = new FileOutputStream(file);
        try {
            FileCopyUtils.copy(inputStream, stream);
        } finally {
            stream.close();
        }

        // Now add the copied file to the entries with the unwrapped files. xml and pdfs that belong together
        // will be stored in the same entry using the file name WITHOUT file type extension as their common key.
        // The assumption is that files that belong together are in the same folder and have the same name.
        String key = fileName.substring(0, fileName.length() - 4);  // key is name without dot and type
        String type = fileName.substring(fileName.length() - 3);    // type is the last three letters [xml|pdf]
        Entry entry = unwrappedFiles.get(key);
        if (entry == null) {
            entry = new Entry(key + ".xml"); // whichever file we run into first, for displaying we always show the xml
            unwrappedFiles.put(key, entry);
        }
        entry.addFile(type, file);
    }
    
    private void ungzipStream(InputStream inputStream, String fileName) throws IOException {
        File tempFile = new File(tmpFolder.getAbsolutePath() + File.separator + "unzippedStream.tmp");
        GZIPInputStream compressedStream = new GZIPInputStream(inputStream, 1024*1024);
        FileOutputStream tempFileStream = new FileOutputStream(tempFile);
        try {
            // inflate file contents to temporary file
            FileCopyUtils.copy(compressedStream, tempFileStream);

            // determine file name without gz ending - this file name will be used for further processing
            String tempFileName;
            tempFileName = fileName.endsWith(".gz") ? fileName.substring(0, (fileName.length()-3)) : fileName;

            // now finally import that thing...
            LOGGER.log(Level.DEBUG, "stuff is uncompressed, continuing...");
            unwrapStream(new FileInputStream(tempFile), tempFileName);
        } finally {
            compressedStream.close();
            tempFileStream.close();
            // and cleanup
            tempFile.delete();
        }
    }

    private void unzipStream(InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // NOTE: don't use FileCopyUtils.copy, this will close the stream!
                    // NOTE: close on byte array streams has no effect, don't bother with it
                    int size;
                    byte[] buffer = new byte[100*1024];
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(100*1024);
                    while ((size = zipInputStream.read(buffer, 0, buffer.length)) != -1) {
                        outputStream.write(buffer, 0, size);
                    }
                    unwrapStream(new ByteArrayInputStream(outputStream.toByteArray()), entry.getName());
                }
            }
        } finally {
            zipInputStream.close();
        }
    }

    private void untarStream(InputStream inputStream) throws IOException {
        TarArchiveInputStream tarInputStream = new TarArchiveInputStream(inputStream);
        try {
            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                untarEntry(tarInputStream, entry);
            }
        } finally {
            tarInputStream.close();
        }
    }

    private void untarEntry(TarArchiveInputStream tarInputStream, TarArchiveEntry entry) throws IOException {
        if (entry.isDirectory()) {
            for (TarArchiveEntry curEntry : entry.getDirectoryEntries()) {
                untarEntry(tarInputStream, curEntry);
            }
        } else {
            // NOTE: close on byte array streams has no effect, don't bother with it
            byte[] buffer = new byte[(int)entry.getSize()];
            tarInputStream.read(buffer, 0, (int)entry.getSize());
            unwrapStream(new ByteArrayInputStream(buffer), entry.getName());
        }
    }

    public static class Entry {
        private String fileName;
        private Map<String, File> files;

        protected Entry(final String fileName) {
            this.fileName = fileName;
            this.files = new HashMap<String, File>();
        }
        
        protected void addFile(String type, File file) {
            files.put(type, file);
        }
        
        // used to display result information to users
        public String getFileName() {
            return this.fileName;
        }

        public File getProposal() {
            return files.get("xml");
        }

        public File getPdf() {
            return files.get("pdf");
        }

        @Override
        public String toString() {
            return "ImportFile{" +
                    "fileName='" + fileName +
                    '}';
        }
    }

}
