package com.groupdocs.viewer.examples.advanced_usage.rendering.common_rendering_options;

import com.groupdocs.viewer.Viewer;
import com.groupdocs.viewer.examples.TestFiles;
import com.groupdocs.viewer.examples.Utils;
import com.groupdocs.viewer.options.HtmlViewOptions;

import java.io.File;
import java.nio.file.Path;

public class ReplaceMissingFont {

    /**
     * This example demonstrates how to use pre-defined font instead of missing
     * font.
     */

    public static void run() {
        Path outputDirectory = Utils.getOutputDirectoryPath("ReplaceMissingFont");
        Path pageFilePathFormat = outputDirectory.resolve("page_{0}.html");

        HtmlViewOptions viewOptions = HtmlViewOptions.forEmbeddedResources(pageFilePathFormat);
        viewOptions.setDefaultFontName("Courier New");

        try (Viewer viewer = new Viewer(TestFiles.MISSING_FONT_PPTX)) {
            viewer.view(viewOptions);
        }

        System.out.println(
                "\nSource document rendered successfully.\nCheck output in " + outputDirectory);
    }
}