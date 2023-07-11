package com.groupdocs.viewer.examples.basic_usage.render_document_to_pdf;

import com.groupdocs.viewer.Viewer;
import com.groupdocs.viewer.examples.TestFiles;
import com.groupdocs.viewer.examples.Utils;
import com.groupdocs.viewer.options.PdfViewOptions;

import java.nio.file.Path;

public class RenderToPdf {

    /**
     * This example demonstrates how to render document into PDF file.
     */

    public static void run() {
        Path outputDirectory = Utils.getOutputDirectoryPath("RenderToPdf");
        Path outputFilePath = outputDirectory.resolve("output.pdf");

        PdfViewOptions viewOptions = new PdfViewOptions(outputFilePath);

        try (Viewer viewer = new Viewer(TestFiles.SAMPLE_DOCX)) {
            viewer.view(viewOptions);
        }

        System.out.println(
                "\nSource document rendered successfully.\nCheck output in " + outputDirectory);
    }
}