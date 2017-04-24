package com.groupdocs.ui;

import com.groupdocs.viewer.converter.options.HtmlOptions;
import com.groupdocs.viewer.domain.Watermark;
import com.groupdocs.viewer.domain.WatermarkPosition;
import com.groupdocs.viewer.domain.html.PageHtml;
import com.groupdocs.viewer.handler.ViewerHtmlHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

/**
 * Query String parameters:
 * file: string: The name of input file in storage folder.
 * zoom: Integer: The zoom factor.
 * page: Integer: The page number that needs to be viewed.
 */
@WebServlet("/page/html")
public class PageHtmlServlet
        extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String filename = request.getParameter("file");
        response.setContentType("text/html");
        ViewerHtmlHandler handler = Utils.createViewerHtmlHandler();

        HtmlOptions o = new HtmlOptions();
    	Watermark watermark = new Watermark("atirtahir");
		//Set color for watermark with values Red, Green, Blue and Alpha(Transparency) 
		Color watermarkColor = new Color(200, 85, 75, 100);
		watermark.setColor(watermarkColor);
		watermark.setPosition(WatermarkPosition.BottomRight);
		watermark.setWidth(50f); 
		
		o.setWatermark(watermark);

        int pageNumber = Integer.valueOf(request.getParameter("page"));
        o.setPageNumbersToRender(Arrays.asList(pageNumber));
        o.setPageNumber(pageNumber);
        o.setCountPagesToRender(1);
        o.setHtmlResourcePrefix(String.format(
                "/page/resource?file=%s&page=%d&resource=",
                filename,
                pageNumber
        ));

        List<PageHtml> list = Utils.loadPageHtmlList(handler, filename, o);
        list.stream().filter(pageHtml -> pageHtml.getPageNumber() == pageNumber).findAny().ifPresent(pageHtml -> {
            String fullHtml = pageHtml.getHtmlContent();
            try {
                response.getWriter().write(fullHtml);
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        });
    }
}
