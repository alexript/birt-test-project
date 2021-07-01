/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.napilnik.birt.test;

import com.ibm.icu.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IDataExtractionTask;
import org.eclipse.birt.report.engine.api.IDataIterator;
import org.eclipse.birt.report.engine.api.IExtractionResults;
import org.eclipse.birt.report.engine.api.IPDFRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IRenderTask;
import org.eclipse.birt.report.engine.api.IReportDocument;

import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IResultSetItem;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.PDFRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.api.TOCNode;
import org.eclipse.core.internal.registry.RegistryProviderFactory;

/**
 *
 * @author malyshev
 */
public class NewMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws EngineException {
        IReportEngine engine = null;
        try {
            EngineConfig config = new EngineConfig();
            //delete the following line if using BIRT 3.7 (or later) POJO runtime
            //As of 3.7.2, BIRT now provides an OSGi and a POJO Runtime.

            config.setLogConfig("c:/temp", Level.FINE);

            Platform.startup(config);
            //If using RE API in Eclipse/RCP application this is not needed.
            IReportEngineFactory factory = (IReportEngineFactory) Platform.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
            engine = factory.createReportEngine(config);
            engine.changeLogLevel(Level.WARNING);
        } catch (BirtException ex) {
            ex.printStackTrace();
        }
        // Run reports, etc.
        IReportDocument ird = engine.openReportDocument("reportdocument_v0.rptdocument");

        //get root node
        TOCNode td = ird.findTOC(null);
        List children = td.getChildren();
        //Loop through Top Level Children
        if (children != null && children.size() > 0) {
            for (int i = 0; i < children.size(); i++) {
                TOCNode child = (TOCNode) children.get(i);
                System.out.println("Node ID " + child.getNodeID());
                System.out.println("Node Display String " + child.getDisplayString());
                System.out.println("Node Bookmark " + child.getBookmark());
            }
        }

        //Create Data Extraction Task
        IDataExtractionTask iDataExtract = engine.createDataExtractionTask(ird);

        //Get list of result sets
        ArrayList resultSetList = (ArrayList) iDataExtract.getResultSetList();

        //Choose first result set
        IResultSetItem resultItem = (IResultSetItem) resultSetList.get(0);
        String dispName = resultItem.getResultSetName();
        iDataExtract.selectResultSet(dispName);

        IExtractionResults iExtractResults = iDataExtract.extract();
        IDataIterator iData = null;
        try {
            if (iExtractResults != null) {
                iData = iExtractResults.nextResultIterator();
                //iterate through the results
                if (iData != null) {
                    while (iData.next()) {
                        Object objColumn1;
                        Object objColumn2;
                        try {
                            objColumn1 = iData.getValue(0);
                        } catch (DataException e) {
                            objColumn1 = new String("");
                        }
                        try {
                            objColumn2 = iData.getValue(1);
                        } catch (DataException e) {
                            objColumn2 = new String("");
                        }
                        System.out.println(objColumn1 + " , " + objColumn2);
                    }
                    iData.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        iDataExtract.close();

        //Create Render Task
        IRenderTask task = engine.createRenderTask(ird);
        //Set parent classloader report engine
        task.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY,
                NewMain.class.getClassLoader());
        IRenderOption options = new RenderOption();

        options.setOutputFormat("html");
        if (options.getOutputFormat().equalsIgnoreCase("html")) {
            options.setOutputFileName("output/resample/eventorder.html");
            HTMLRenderOption htmlOptions = new HTMLRenderOption(options);
            htmlOptions.setImageDirectory("output/image");
            htmlOptions.setHtmlPagination(false);
            //set this if you want your image source url to be altered
            //If using the setBaseImageURL, make sure
            //to set image handler to HTMLServerImageHandler
            htmlOptions.setBaseImageURL("http://myhost/prependme?image=");
            htmlOptions.setHtmlRtLFlag(false);
            htmlOptions.setEmbeddable(false);
        } else if (options.getOutputFormat().equalsIgnoreCase("pdf")) {

            options.setOutputFileName("output/resample/eventorder.pdf");
            PDFRenderOption pdfOptions = new PDFRenderOption(options);
            pdfOptions.setOption(IPDFRenderOption.FIT_TO_PAGE, new Boolean(true));
            pdfOptions.setOption(IPDFRenderOption.PAGEBREAK_PAGINATION_ONLY, new Boolean(true));
        }
        //Use this method if you want to provide your own action handler
//        options.setActionHandler(new MyActionHandler());
        //file based images
        //options.setImageHandler(new HTMLCompleteImageHandler())
        //Web based images.  Allows setBaseImageURL to prepend to img src tag
        options.setImageHandler(new HTMLServerImageHandler());
        IRenderTask task2 = engine.createRenderTask(ird);
        task2.setRenderOption(options);
        task2.setPageRange("1-2");
        task2.render();
        ird.close();

        IReportRunnable irr = engine.openReportDesign("timeZoneTest.xml");
        IRunAndRenderTask createRunAndRenderTask = engine.createRunAndRenderTask(irr);
        createRunAndRenderTask.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY,
                NewMain.class.getClassLoader());

        createRunAndRenderTask.setLocale(com.ibm.icu.util.ULocale.US);
        task.setTimeZone(TimeZone.getTimeZone("UTC"));
        IRenderOption option = new HTMLRenderOption();
        option.setOutputFormat("html"); //$NON-NLS-1$

//        options = new RenderOption();
//        options.setOutputFormat("pdf");
        options.setOutputFileName("output/resample/timeZoneTest.html");
//        PDFRenderOption pdfOptions = new PDFRenderOption(options);
//        pdfOptions.setOption(IPDFRenderOption.FIT_TO_PAGE, new Boolean(true));
//        pdfOptions.setOption(IPDFRenderOption.PAGEBREAK_PAGINATION_ONLY, new Boolean(true));

        createRunAndRenderTask.setRenderOption(options);
        createRunAndRenderTask.run();

        // destroy the engine.
        if (engine != null) {
            engine.destroy();
        }
        Platform.shutdown();

        RegistryProviderFactory.releaseDefault();

    }

}
