    package com.groupdocs.viewer.examples;


import com.groupdocs.viewer.examples.basic_usage.render_document_to_image.*;
import com.groupdocs.viewer.examples.basic_usage.render_document_to_pdf.AdjustQualityOfJpgImages;
import com.groupdocs.viewer.examples.basic_usage.render_document_to_pdf.GetPdfStream;
import com.groupdocs.viewer.examples.basic_usage.render_document_to_pdf.ProtectPdfDocument;
import com.groupdocs.viewer.examples.basic_usage.render_document_to_pdf.RenderToPdf;
import com.groupdocs.viewer.examples.howto.HowToDetermineFileType;
import com.groupdocs.viewer.examples.howto.HowToLogging;
import com.groupdocs.viewer.examples.quick_start.HelloWorld;
import com.groupdocs.viewer.examples.quick_start.SetLicenseFromUrl;

import java.io.IOException;

public class RunExamples {

    /**
     * The main method.
     */
    public static void main(String[] args) throws IOException {

        System.out.println("Uncomment the example(s) that you want to run in RunExamples.java file.");
        System.out.println("=======================================================================");

        Utils.cleanOutputDirectory();

        // region Quick Start

//        SetLicenseFromFile.run();
//        SetLicenseFromStream.run();
//        SetLicenseFromUrl.run();
//        SetMeteredLicense.run();
        Thread newG = new Thread(new Runnable() {
            @Override
            public void run() {
                RenderToPng.run();
            }
        });

        newG.start();
   // region HowTo
        HowToDetermineFileType.fromFileExtension();
        HowToDetermineFileType.fromMediaType();
        HowToDetermineFileType.fromStream();

        HowToLogging.toConsole();
        HowToLogging.toFile();

        // endregion

        System.out.println();
        System.out.println("All done.");
    }
}
