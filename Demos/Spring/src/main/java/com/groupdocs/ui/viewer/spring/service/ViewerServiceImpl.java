package com.groupdocs.ui.viewer.spring.service;

import com.groupdocs.ui.viewer.spring.cache.FileViewerCache;
import com.groupdocs.ui.viewer.spring.cache.ViewerCache;
import com.groupdocs.ui.viewer.spring.common.config.GlobalConfiguration;
import com.groupdocs.ui.viewer.spring.common.entity.web.FileDescriptionEntity;
import com.groupdocs.ui.viewer.spring.common.entity.web.LoadDocumentEntity;
import com.groupdocs.ui.viewer.spring.common.entity.web.PageDescriptionEntity;
import com.groupdocs.ui.viewer.spring.common.entity.web.request.LoadDocumentPageRequest;
import com.groupdocs.ui.viewer.spring.common.entity.web.request.LoadDocumentRequest;
import com.groupdocs.ui.viewer.spring.common.entity.web.request.RotateDocumentPagesRequest;
import com.groupdocs.ui.viewer.spring.common.exception.DiskAccessException;
import com.groupdocs.ui.viewer.spring.common.exception.EvaluationModeException;
import com.groupdocs.ui.viewer.spring.common.exception.ReadWriteException;
import com.groupdocs.ui.viewer.spring.common.exception.TotalGroupDocsException;
import com.groupdocs.ui.viewer.spring.common.util.PagesInfoStorage;
import com.groupdocs.ui.viewer.spring.common.util.Utils;
import com.groupdocs.ui.viewer.spring.config.DefaultDirectories;
import com.groupdocs.ui.viewer.spring.config.ViewerConfiguration;
import com.groupdocs.ui.viewer.spring.impls.CustomViewer;
import com.groupdocs.ui.viewer.spring.impls.HtmlViewer;
import com.groupdocs.ui.viewer.spring.impls.PngViewer;
import com.groupdocs.viewer.License;
import com.groupdocs.viewer.exception.IncorrectPasswordException;
import com.groupdocs.viewer.exception.PasswordRequiredException;
import com.groupdocs.viewer.fonts.FolderFontSource;
import com.groupdocs.viewer.fonts.FontSettings;
import com.groupdocs.viewer.fonts.FontSource;
import com.groupdocs.viewer.fonts.SearchOption;
import com.groupdocs.viewer.options.LoadOptions;
import com.groupdocs.viewer.results.Page;
import com.groupdocs.viewer.results.ViewInfo;
import com.groupdocs.viewer.utils.PathUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Service
public class ViewerServiceImpl implements ViewerService {
    private static final Logger logger = LoggerFactory.getLogger(ViewerServiceImpl.class);
    private static final AtomicBoolean isCacheDirectoryNotExist = new AtomicBoolean(false);
    @Autowired
    private GlobalConfiguration globalConfiguration;
    @Autowired
    private ViewerConfiguration viewerConfiguration;
    private boolean isViewerLicenseSet = false;

    private static LoadOptions createLoadOptions(String password) {
        final LoadOptions loadOptions = new LoadOptions();
        loadOptions.setResourceLoadingTimeout(500);
        if (password != null && !Strings.isEmpty(password)) {
            loadOptions.setPassword(password);
        }
        return loadOptions;
    }

    private static int mergeAngles(int currentAngle, int angle) {
        switch (currentAngle) {
            case 0:
                return (angle == 90 ? 90 : 270);
            case 90:
                return (angle == 90 ? 180 : 0);
            case 180:
                return (angle == 90 ? 270 : 90);
            case 270:
                return (angle == 90 ? 0 : 180);
        }
        return 0;
    }

    private static PageDescriptionEntity getPageInfo(Page page, Path cacheDocumentDirectoryPath) {

        int currentAngle = PagesInfoStorage.loadPageAngle(cacheDocumentDirectoryPath, page.getNumber());

        PageDescriptionEntity pageDescriptionEntity = new PageDescriptionEntity();

        pageDescriptionEntity.setNumber(page.getNumber());

        // we intentionally use the 0 here because we plan to rotate only the page background using height/width
        pageDescriptionEntity.setAngle(0);
        pageDescriptionEntity.setHeight(currentAngle == 0 || currentAngle == 180 ? page.getHeight() : page.getWidth());
        pageDescriptionEntity.setWidth(currentAngle == 0 || currentAngle == 180 ? page.getWidth() : page.getHeight());

        return pageDescriptionEntity;
    }

    /**
     * Initializing fields after creating configuration objects
     */
    @PostConstruct
    public void init() {
        setLicense();
        // Register custom fonts
        if (!StringUtils.isEmpty(viewerConfiguration.getFontsDirectory())) {
            FontSource fontSource = new FolderFontSource(viewerConfiguration.getFontsDirectory(), SearchOption.TOP_FOLDER_ONLY);
            FontSettings.setFontSources(fontSource);
        }
        //

        HtmlViewer.setViewerConfiguration(viewerConfiguration);
        PngViewer.setViewerConfiguration(viewerConfiguration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileDescriptionEntity> getFileList(String path) {
        final String filesDirectory = viewerConfiguration.getFilesDirectory();
        final File filesSubDirectory = new File(PathUtils.combine(filesDirectory, path));

        List<FileDescriptionEntity> filesList = new ArrayList<>();
        try {
            final File[] files = filesSubDirectory.listFiles();
            if (files == null) {
                throw new TotalGroupDocsException("Can't list files");
            }

            for (File file : files) {
                // check if current file/folder is hidden
                if (!(file.getName().equals(viewerConfiguration.getCacheFolderName())) && !file.getName().startsWith(".") && !file.isHidden()) {
                    FileDescriptionEntity fileDescription = new FileDescriptionEntity();
                    fileDescription.setGuid(Utils.normalizePathToGuid(filesDirectory, file.getCanonicalFile().getAbsolutePath()));
                    fileDescription.setName(file.getName());
                    // set is directory true/false
                    fileDescription.setIsDirectory(file.isDirectory());

                    // set file size
                    if (!fileDescription.isIsDirectory()) {
                        fileDescription.setSize(file.length());
                    }

                    // add object to array list
                    filesList.add(fileDescription);
                }
            }

            // Sort by name and to make directories to be before files
            Collections.sort(filesList, new Comparator<FileDescriptionEntity>() {
                @Override
                public int compare(FileDescriptionEntity o1, FileDescriptionEntity o2) {
                    if (o1.isIsDirectory() && !o2.isIsDirectory()) {
                        return -1;
                    }
                    if (!o1.isIsDirectory() && o2.isIsDirectory()) {
                        return 1;
                    }
                    return o1.getName().compareTo(o2.getName());
                }
            });
        } catch (IOException e) {
            logger.error("Exception in getting file list", e);
            throw new TotalGroupDocsException(e.getMessage(), e);
        }
        return filesList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadDocumentEntity loadDocument(LoadDocumentRequest loadDocumentRequest, boolean loadAllPages, boolean printVersion) {
        // set request parameters
        final String documentGuid = loadDocumentRequest.getGuid();
        String documentPath = PathUtils.combine(viewerConfiguration.getFilesDirectory(), Utils.normalizeGuidToPath(documentGuid));
        String password = loadDocumentRequest.getPassword();
        password = org.apache.commons.lang3.StringUtils.isEmpty(password) ? null : password;
        LoadDocumentEntity loadDocumentEntity;
        CustomViewer<?> customViewer = null;
        try {
            final Path fileCachePath = createCacheDocumentDirectoryPath(documentPath);
            ViewerCache cache = new FileViewerCache(fileCachePath);

            if (viewerConfiguration.isHtmlMode()) {
                customViewer = new HtmlViewer(documentPath, cache, createLoadOptions(password));
            } else {
                customViewer = new PngViewer(documentPath, cache, createLoadOptions(password));
            }
            loadDocumentEntity = getLoadDocumentEntity(loadAllPages, documentPath, customViewer, printVersion);
            loadDocumentEntity.setShowGridLines(viewerConfiguration.getShowGridLines());
            loadDocumentEntity.setPrintAllowed(viewerConfiguration.getPrintAllowed());
        } catch (IncorrectPasswordException | PasswordRequiredException e) {
            logger.error("Exception that is connected to password", e);
            throw new TotalGroupDocsException(Utils.getExceptionMessage(password), e);
        } finally {
            if (customViewer != null) {
                customViewer.close();
            }
        }
        return loadDocumentEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageDescriptionEntity loadDocumentPage(LoadDocumentPageRequest loadDocumentPageRequest) {
        final String documentGuid = loadDocumentPageRequest.getGuid();
        String documentPath = PathUtils.combine(viewerConfiguration.getFilesDirectory(), Utils.normalizeGuidToPath(documentGuid));
        Integer pageNumber = loadDocumentPageRequest.getPage();
        String password = loadDocumentPageRequest.getPassword();
        password = org.apache.commons.lang3.StringUtils.isEmpty(password) ? null : password;
        CustomViewer<?> customViewer = null;
        try {
            final Path cacheDocumentDirectoryPath = createCacheDocumentDirectoryPath(documentPath);
            ViewerCache cache = new FileViewerCache(cacheDocumentDirectoryPath);

            if (viewerConfiguration.isHtmlMode()) {
                customViewer = new HtmlViewer(documentPath, cache, createLoadOptions(password));
            } else {
                customViewer = new PngViewer(documentPath, cache, createLoadOptions(password));
            }
            return getPageDescriptionEntity(customViewer, cacheDocumentDirectoryPath, pageNumber);
        } catch (Exception ex) {
            logger.error("Exception in loading document page", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        } finally {
            if (customViewer != null) {
                customViewer.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageDescriptionEntity rotateDocumentPages(RotateDocumentPagesRequest rotateDocumentPagesRequest) {
        // set request parameters
        final String documentGuid = rotateDocumentPagesRequest.getGuid();
        String documentPath = PathUtils.combine(viewerConfiguration.getFilesDirectory(), Utils.normalizeGuidToPath(documentGuid));
        List<Integer> pages = rotateDocumentPagesRequest.getPages();
        String password = rotateDocumentPagesRequest.getPassword();
        Integer angle = rotateDocumentPagesRequest.getAngle();
        int pageNumber = pages.get(0);
        CustomViewer<?> customViewer = null;

        try {
            final Path cacheDocumentDirectoryPath = createCacheDocumentDirectoryPath(documentPath);

            // Delete cache files connected to the page
            for (File file : cacheDocumentDirectoryPath.toFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("p" + pageNumber);
                }
            })) {
                if (!file.delete()) {
                    file.deleteOnExit();
                    logger.error("Exception in deleting cache path");
                    throw new DiskAccessException("delete cache file", file);
                }
            }
            // Getting new rotation angle value.
            int currentAngle = PagesInfoStorage.loadPageAngle(cacheDocumentDirectoryPath, pageNumber);
            int newAngle = mergeAngles(currentAngle, angle);
            PagesInfoStorage.savePageAngle(cacheDocumentDirectoryPath, pageNumber, newAngle);
            // Generate new cache
            ViewerCache cache = new FileViewerCache(cacheDocumentDirectoryPath);
            if (viewerConfiguration.isHtmlMode()) {
                customViewer = new HtmlViewer(documentPath, cache, createLoadOptions(password), pageNumber, newAngle);
            } else {
                customViewer = new PngViewer(documentPath, cache, createLoadOptions(password), pageNumber, newAngle);
            }
            return getPageDescriptionEntity(customViewer, cacheDocumentDirectoryPath, pageNumber);

        } catch (Exception ex) {
            logger.error("Exception in rotating document page", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        } finally {
            if (customViewer != null) {
                customViewer.close();
            }
        }
    }

    @Override
    public ResponseEntity<byte[]> getResource(String guid, String resourceName) {
        try {
            if (!org.apache.commons.lang3.StringUtils.isEmpty(guid)) {
                String path = PathUtils.combine(viewerConfiguration.getFilesDirectory(), viewerConfiguration.getCacheFolderName(), guid, resourceName);
                File resourceFile = new File(path);

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(Utils.detectMediaType(resourceFile.getName()));
                responseHeaders.setContentDisposition(
                        ContentDisposition.builder("inline")
                                .filename(resourceFile.getAbsolutePath())
                                .build()
                );

                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .body(FileUtils.readFileToByteArray(resourceFile));
            }
        } catch (IOException e) {
            logger.error("Exception in loading resource", e);
            throw new TotalGroupDocsException(e.getMessage(), e);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewerConfiguration getViewerConfiguration() {
        return viewerConfiguration;
    }

    @Override
    public InputStream getPdf(LoadDocumentRequest loadDocumentRequest) {
        final String documentGuid = loadDocumentRequest.getGuid();
        String documentPath = PathUtils.combine(viewerConfiguration.getFilesDirectory(), Utils.normalizeGuidToPath(documentGuid));
        String password = loadDocumentRequest.getPassword();
        password = org.apache.commons.lang3.StringUtils.isEmpty(password) ? null : password;
        CustomViewer<?> customViewer = null;
        try {
            Path cacheDocumentDirectoryPath = createCacheDocumentDirectoryPath(documentPath);
            ViewerCache cache = new FileViewerCache(cacheDocumentDirectoryPath);

            if (viewerConfiguration.isHtmlMode()) {
                customViewer = new HtmlViewer(documentPath, cache, createLoadOptions(password));
            } else {
                customViewer = new PngViewer(documentPath, cache, createLoadOptions(password));
            }
            return getPdfFile(customViewer, cacheDocumentDirectoryPath);
        } catch (Exception ex) {
            logger.error("Exception in loading document page", ex);
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        } finally {
            if (customViewer != null) {
                customViewer.close();
            }
        }
    }

    private Path createCacheDocumentDirectoryPath(String documentGuid) {
        final Path path = Paths.get(documentGuid);
        final String fileName = path.getFileName().toString();
        String fileFolderName = fileName.replace(".", "_");
        final Path fileCachePath = createCacheDirectoryPath(fileFolderName);
        return fileCachePath;
    }

    private PageDescriptionEntity getPageDescriptionEntity(CustomViewer<?> customViewer, Path cacheDocumentDirectoryPath, int pageNumber) {
        customViewer.createCache();

        ViewInfo viewInfo = customViewer.getViewInfo();
        PageDescriptionEntity page = getPageInfo(viewInfo.getPages().get(pageNumber - 1), cacheDocumentDirectoryPath);
        page.setData(getPageContent(pageNumber, cacheDocumentDirectoryPath, false));

        return page;
    }

    private LoadDocumentEntity getLoadDocumentEntity(boolean loadAllPages, String documentPath, CustomViewer<?> customViewer, boolean printVersion) {
        try {
            if (loadAllPages) {
                customViewer.createCache();
            }

            ViewInfo viewInfo = customViewer.getViewInfo();
            if (viewInfo == null) {
                throw new TotalGroupDocsException("Can't get view info. Try again later.");
            }
            LoadDocumentEntity loadDocumentEntity = new LoadDocumentEntity();

            final Path cacheDocumentDirectoryPath = createCacheDocumentDirectoryPath(documentPath);
            PagesInfoStorage.createPagesInfo(cacheDocumentDirectoryPath, viewInfo, isViewerLicenseSet);

            List<Page> pages = viewInfo.getPages();
            for (int i = 0, pagesSize = pages.size(); i < pagesSize; i++) {
                if (!isViewerLicenseSet && i == 2) {
                    break; // only 2 pages in evaluation mode
                }
                Page page = pages.get(i);
                PageDescriptionEntity pageData = getPageInfo(page, cacheDocumentDirectoryPath);
                if (loadAllPages) {
                    pageData.setData(getPageContent(page.getNumber(), cacheDocumentDirectoryPath, printVersion));
                }

                loadDocumentEntity.getPages().add(pageData);
            }

            final String filesDirectory = viewerConfiguration.getFilesDirectory();
            loadDocumentEntity.setGuid(Utils.normalizePathToGuid(filesDirectory, documentPath));
            return loadDocumentEntity;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("At most 4 elements")) {
                throw new EvaluationModeException(documentPath);
            }
            logger.error("Something went wrong", e);
            throw new ReadWriteException(e);
        }
    }

    private String getPageContent(int pageNumber, Path cacheDocumentDirectoryPath, boolean printVersion) {
        try {
            if (viewerConfiguration.isHtmlMode() && !printVersion) {
                Path htmlFilePath = cacheDocumentDirectoryPath.resolve("p" + pageNumber + ".html");
                return FileUtils.readFileToString(htmlFilePath.toFile(), StandardCharsets.UTF_8);
            } else {
                Path pngFilePath = cacheDocumentDirectoryPath.resolve("p" + pageNumber + ".png");

                byte[] imageBytes = FileUtils.readFileToByteArray(pngFilePath.toFile());

                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (IOException e) {
            throw new ReadWriteException(e);
        }
    }

    private InputStream getPdfFile(CustomViewer<?> customViewer, Path cacheDocumentDirectoryPath) throws IOException {
        customViewer.createPdf();

        Path pngFilePath = cacheDocumentDirectoryPath.resolve("f.pdf");

        byte[] fileBytes = FileUtils.readFileToByteArray(pngFilePath.toFile());

        return new ByteArrayInputStream(fileBytes);
    }

    private void setLicense() {
        try {
            // set GroupDocs license
            final String licensePath = globalConfiguration.getApplication().getLicensePath();
            final String licenseExtension = DefaultDirectories.LIC;
            License license = new License();
            if (licensePath.startsWith("http://") || licensePath.startsWith("https://")) {
                final URL url = new URL(licensePath);
                try (final InputStream inputStream = new BufferedInputStream(url.openStream())) {
                    license.setLicense(inputStream);
                    isViewerLicenseSet = true;
                }
            } else {
                final java.nio.file.Path path = Paths.get(licensePath);
                if (Files.exists(path)) {
                    if (Files.isRegularFile(path)) {
                        license.setLicense(licensePath);
                        isViewerLicenseSet = true;
                    } else {
                        try (final Stream<Path> pathStream = Files.list(path)) {
                            final Optional<Path> first = pathStream.filter(it -> it.endsWith(licenseExtension)).findFirst();
                            first.ifPresent(license::setLicense);
                            isViewerLicenseSet = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Can not verify Comparison license!", e);
            isViewerLicenseSet = false;
        }
        if (isViewerLicenseSet) {
            logger.info("License was set!");
        }
    }

    private Path createCacheDirectoryPath(String... subPathParts) {
        final String filesDirectory = viewerConfiguration.getFilesDirectory();
        final String cacheFolderName = viewerConfiguration.getCacheFolderName();
        Path path = Paths.get(filesDirectory, cacheFolderName);
        try {
            if (isCacheDirectoryNotExist.get()) {
                synchronized (isCacheDirectoryNotExist) {
                    if (isCacheDirectoryNotExist.getAndSet(true) && Files.notExists(path)) {
                        Files.createDirectories(path);
                    }
                }
            }
        } catch (Exception e) {
            throw new DiskAccessException("create cache directory", path.toString());
        }
        for (String subPathPart : subPathParts) {
            path = path.resolve(subPathPart);
        }
        return path;
    }
}
