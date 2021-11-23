package com.groupdocs.viewer.examples.advanced_usage.rendering.rendering_options_by_document_type.rendering_image_files;

import com.groupdocs.viewer.Viewer;
import com.groupdocs.viewer.examples.TestFiles;
import com.groupdocs.viewer.examples.Utils;
import com.groupdocs.viewer.options.HtmlViewOptions;
import com.groupdocs.viewer.options.JpgViewOptions;
import com.groupdocs.viewer.options.PdfViewOptions;
import com.groupdocs.viewer.options.PngViewOptions;
import com.groupdocs.viewer.utils.PathUtils;

/**
 * This example demonstrates how to render Animated PNG format
 */
public class RenderingApng {
    public static void run() {
        String outputDirectory = Utils.getOutputDirectoryPath("RenderingApng");
        String pageFilePathFormat = PathUtils.combine(outputDirectory, "apng_result.html");

        // TO HTML
        try (Viewer viewer = new Viewer(TestFiles.SAMPLE_APNG)) {
            HtmlViewOptions options = HtmlViewOptions.forEmbeddedResources(pageFilePathFormat);

            viewer.view(options);
        }


        // TO JPG
        pageFilePathFormat = PathUtils.combine(outputDirectory, "apng_result_{0}.jpg");

        try (Viewer viewer = new Viewer(TestFiles.SAMPLE_APNG)) {
            JpgViewOptions options = new JpgViewOptions(pageFilePathFormat);

            viewer.view(options);
        }

        // TO PNG
        pageFilePathFormat = PathUtils.combine(outputDirectory, "apng_result_{0}.png");

        try (Viewer viewer = new Viewer(TestFiles.SAMPLE_APNG)) {
            PngViewOptions options = new PngViewOptions(pageFilePathFormat);

            viewer.view(options);
        }

        // TO PDF
        pageFilePathFormat = PathUtils.combine(outputDirectory, "apng_result.pdf");

        try (Viewer viewer = new Viewer(TestFiles.SAMPLE_APNG)) {
            PdfViewOptions options = new PdfViewOptions(pageFilePathFormat);

            viewer.view(options);
        }

        System.out.println("\nSource document rendered successfully.\nCheck output in " + outputDirectory);
    }
}
