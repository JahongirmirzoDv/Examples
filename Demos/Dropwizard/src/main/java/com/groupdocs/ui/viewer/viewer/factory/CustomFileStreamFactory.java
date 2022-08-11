package com.groupdocs.ui.viewer.viewer.factory;

import com.groupdocs.ui.viewer.cache.ViewerCache;
import com.groupdocs.ui.viewer.exception.ReadWriteException;
import com.groupdocs.viewer.interfaces.FileStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomFileStreamFactory implements FileStreamFactory {
    private static final Logger logger = LoggerFactory.getLogger(CustomFileStreamFactory.class);
    private final String extension;
    private final Path tempDirectory;
    private final ViewerCache cache;

    public CustomFileStreamFactory(ViewerCache cache, String extension) {
        this.cache = cache;
        this.extension = extension;

        try {
            this.tempDirectory = Files.createTempDirectory("gd_sp_psf_");
        } catch (IOException e) {
            logger.warn("Can't create temp directory. Can't create PageStreamFactory object.", e);
            throw new ReadWriteException(e);
        }
    }

    @Override
    public OutputStream createFileStream() {
        String cacheKey = createCacheKey();
        final Path resourcePath = tempDirectory.resolve(cacheKey);

        try {
            return new FileOutputStream(resourcePath.toFile());
        } catch (FileNotFoundException e) {
            logger.warn("Can't create temp file '" + resourcePath + "'. Can't create file stream.", e);
            throw new ReadWriteException(e);
        }
    }

    @Override
    public void closeFileStream(OutputStream outputStream) {
        final String cacheKey = createCacheKey();
        final Path resourcePath = tempDirectory.resolve(cacheKey);

        try {
            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(resourcePath))) {
                this.cache.set(cacheKey, inputStream);
            } catch (IOException e) {
                logger.warn("Can't copy temp file '" + resourcePath + "' to cache.", e);
                throw new RuntimeException(e);
            }

            outputStream.close();
        } catch (IOException e) {
            logger.warn("Can't close file stream or move data to cache.", e);
        } finally {
            try {
                if (!Files.deleteIfExists(resourcePath)) {
                    Files.delete(resourcePath);
                }
            } catch (IOException e) {
                logger.warn("Can't delete temp file stream file.", e);
            }
        }
    }

    private String createCacheKey() {
        return "f" + extension;
    }
}
