package com.groupdocs.ui.viewer.spring.service;

import com.groupdocs.ui.viewer.spring.common.entity.web.FileDescriptionEntity;
import com.groupdocs.ui.viewer.spring.common.entity.web.LoadDocumentEntity;
import com.groupdocs.ui.viewer.spring.common.entity.web.PageDescriptionEntity;
import com.groupdocs.ui.viewer.spring.common.entity.web.request.LoadDocumentPageRequest;
import com.groupdocs.ui.viewer.spring.common.entity.web.request.LoadDocumentRequest;
import com.groupdocs.ui.viewer.spring.common.entity.web.request.RotateDocumentPagesRequest;
import com.groupdocs.ui.viewer.spring.config.ViewerConfiguration;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.util.List;

public interface ViewerService {

    /**
     * Get configuration for viewer module
     *
     * @return viewer configuration
     */
    ViewerConfiguration getViewerConfiguration();

    /**
     * Get list of files in directory
     *
     * @return list of file's descriptions
     */
    List<FileDescriptionEntity> getFileList(String path);

    /**
     * Get document description
     *
     * @param loadDocumentRequest document data for loading
     * @param loadAllPages        to load all pages data of the document
     * @return document info container
     */
    LoadDocumentEntity loadDocument(LoadDocumentRequest loadDocumentRequest, boolean loadAllPages, boolean printVersion);

    /**
     * Get document page
     *
     * @param loadDocumentPageRequest document page request data
     * @return document page info
     */
    PageDescriptionEntity loadDocumentPage(LoadDocumentPageRequest loadDocumentPageRequest);

    /**
     * Rotate document pages
     *
     * @param rotateDocumentPagesRequest rotate pages request info
     * @return list of rotated pages
     */
    PageDescriptionEntity rotateDocumentPages(RotateDocumentPagesRequest rotateDocumentPagesRequest);

    /**
     * Loads resources, as css, js or images
     *
     * @param guid         file guid
     * @param resourceName resource name
     * @return resource data
     */
    ResponseEntity<byte[]> getResource(String guid, String resourceName);

    /**
     * Retrieves PDF file
     *
     * @param loadDocumentRequest document data for loading
     * @return PDF data
     */
    InputStream getPdf(LoadDocumentRequest loadDocumentRequest);

}
