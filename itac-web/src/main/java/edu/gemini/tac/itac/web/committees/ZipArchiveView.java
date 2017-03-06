package edu.gemini.tac.itac.web.committees;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Allows multiple strings (e.g., HTML of Proposal details pages) to be compressed into a Zip, which is then served to the browser
 *
 * Author: lobrien
 * Date: 4/19/11
 */
public class ZipArchiveView extends AbstractView {
    private static final Logger LOGGER = Logger.getLogger(ZipArchiveView.class);

    ByteArrayOutputStream out;
    ZipOutputStream zip;

    void addToArchive(String fileName, byte[] content){
        initializeOutputStreams();
        enzippen(sanitize(fileName), content, zip);
    }

    void addToArchive(String fileName, String content){
        initializeOutputStreams();
        enzippen(sanitize(fileName), content, zip);
    }

    private void initializeOutputStreams() {
        if (out == null) {
            out = new ByteArrayOutputStream();
        }
        if (zip == null){
            zip = new ZipOutputStream(out);
            zip.setMethod(ZipOutputStream.DEFLATED);
            zip.setLevel(1);
        }
    }

    private String sanitize(String fileName){
        String f = fileName.replaceAll("/", "_");
        f = f.replaceAll("\\\\", "_");
        f = f.replaceAll(":", "_");
        return f;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> stringObjectMap, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        LOGGER.log(Level.DEBUG, "renderMergedOutputModel");

        /*
        Map<String, String> pages = (Map<String, String>) stringObjectMap.get("pages");
        Validate.notNull(pages);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(out);
        zip.setMethod(ZipOutputStream.DEFLATED);
        for (String identifier : pages.keySet()) {
            String page = pages.get(identifier);
            enzippen(identifier, page, zip);
        }
        */
        zip.flush();
        out.flush();
        zip.close();
        out.close();
        final byte[] bytes = out.toByteArray();

        ServletOutputStream sos = httpServletResponse.getOutputStream();
        httpServletResponse.setContentLength(bytes.length);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"export.zip\"");
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setHeader("Content-Transfer-Encoding", "binary");
        httpServletResponse.setHeader("Content-Description", "File Transfer");

        sos.write(bytes);
        sos.flush();

        LOGGER.log(Level.DEBUG, "finished()");
        out = new ByteArrayOutputStream();
        // Racy.
        zip = new ZipOutputStream(out);
        zip.setMethod(ZipOutputStream.DEFLATED);
    }

    /**
     *
     * @param proposalName name of file, if it does not end with pdf, then html will be added
     * @param s data to write to archive
     * @param zos
     */
    private void enzippen(String proposalName, String s, ZipOutputStream zos) {
        try {
            if (proposalName.endsWith(".pdf"))
                zos.putNextEntry(new ZipEntry(proposalName));
            else if (proposalName.endsWith(".xml"))
                zos.putNextEntry(new ZipEntry(proposalName));
            else
                zos.putNextEntry(new ZipEntry(proposalName + ".html"));
            zos.write(s.getBytes(), 0, s.getBytes().length);
            zos.closeEntry();
            LOGGER.log(Level.DEBUG, "Wrote " + s.getBytes().length + " to zip file " + proposalName);
        } catch (Exception x) {
            LOGGER.log(Level.ERROR, x);
        }
    }

    private void enzippen(String proposalName, byte[] content, ZipOutputStream zos) {
        try {
            if (proposalName.endsWith(".pdf"))
                zos.putNextEntry(new ZipEntry(proposalName));
            else if (proposalName.endsWith(".xml"))
                zos.putNextEntry(new ZipEntry(proposalName));
            else
                zos.putNextEntry(new ZipEntry(proposalName + ".html"));
            zos.write(content, 0, content.length);
            zos.closeEntry();
            LOGGER.log(Level.DEBUG, "Wrote " + content.length + " to zip file " + proposalName);
        } catch (Exception x) {
            LOGGER.log(Level.ERROR, x);
        }
    }
}
