/*
 * Copyright 2011 CollabNet, Inc.
 * 
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.collabnet.subversion.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.collabnet.subversion.benchmark.test.BasicTest;
import com.collabnet.subversion.benchmark.test.BinariesTest;
import com.collabnet.subversion.benchmark.test.FolderTest;
import com.collabnet.subversion.benchmark.test.MergeTest;
import com.collabnet.subversion.benchmark.test.ReadTest;
import com.collabnet.subversion.benchmark.test.SparseTest;
import com.collabnet.subversion.benchmark.util.Keys;
import com.collabnet.subversion.benchmark.util.StatsCollector;
import com.collabnet.subversion.benchmark.util.StatsCollector.Stat;

public class RunTests {

    private static final String LOG_PATTERN = "%m\n";
    private final Properties properties;
    private final Logger log;
    private final StatsCollector stats;
    private Date start;
    private Date end;
    private final DateFormat lf = new SimpleDateFormat("H:mm:ss.SSS");
    private final DateFormat df = new SimpleDateFormat("m:ss.SSS");

    public RunTests(Properties props) {
        super();
        properties = props;
        log = Logger.getRootLogger();
        if (props.getProperty(Keys.LOG4J_ROOT_LOGGER) == null) {
            log.addAppender(new ConsoleAppender(new PatternLayout(LOG_PATTERN)));
            log.setLevel(Level.INFO);
        } else {
            PropertyConfigurator.configure(props);
        }
        stats = new StatsCollector();
        df.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        lf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    public void run() {
        start = new Date();
        if (properties.getProperty("Basic", "true").equals("true"))
            new BasicTest(stats, properties).run();
        if (properties.getProperty("Merge", "true").equals("true"))
            new MergeTest(stats, properties).run();
        if (properties.getProperty("Folders", "true").equals("true"))
            new FolderTest(stats, properties).run();
        if (properties.getProperty("Binaries", "true").equals("true"))
            new BinariesTest(stats, properties).run();
        if (properties.getProperty("Read", "true").equals("true"))
            new ReadTest(stats, properties).run();
        if (properties.getProperty("Sparse", "true").equals("true"))
            new SparseTest(stats, properties).run();
        end = new Date();
        printStats();
    }

    private void printXmlStats() {
        Document dom = null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.newDocument();
        } catch (ParserConfigurationException pce) {
            log.error("Error while trying to instantiate DocumentBuilder " + pce);
        }
        
        Element root = dom.createElement("testsuites");
        dom.appendChild(root);
        
        Element testsuite = null;
        
        String lastTest = "";
        long testsuiteTime = 0;
        
        for (Stat stat : stats.getStats()) {
            if (!lastTest.equals(stat.getTest())) {
                if (testsuite != null)
                    testsuite.setAttribute("time", String.format("%d", testsuiteTime));
                
                testsuiteTime = 0;
                lastTest = stat.getTest();
                testsuite = dom.createElement("testsuite");
                testsuite.setAttribute("name", stat.getTest());
                root.appendChild(testsuite);
            }
            
            Element test = dom.createElement("testcase");
            testsuite.appendChild(test);
            test.setAttribute("name", stat.getStep());
            test.setAttribute("time", String.format("%d", stat.getElapsedTime()));
            
            testsuiteTime += stat.getElapsedTime();
        }
        
        // Set the total time for the last testsuite
        testsuite.setAttribute("time", String.format("%d", testsuiteTime));
        
        DOMImplementationLS DOMiLS = (DOMImplementationLS) dom.getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = DOMiLS.createLSSerializer();
        DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
        
        if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE))
            domConfiguration.setParameter("format-pretty-print", Boolean.TRUE);
        
        LSOutput output = DOMiLS.createLSOutput();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        output.setEncoding("UTF-8");
        output.setByteStream(outputStream);
        
        lsSerializer.write(dom, output);
        try {
            log.info(outputStream.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    
    private void printResults() {
        String lastTest = "";
        log.info("");
        log.info("=================== TEST RESULTS ==================");
        log.info("SVN Version: " + stats.getVersion());
        for (Stat stat : stats.getStats()) {
            if (!lastTest.equals(stat.getTest())) {
                lastTest = stat.getTest();
                printTestHeader(lastTest);
            }
            log.info(getTimeResult(stat.getStep(), stat.getElapsedTime()));
        }
        log.info("");
        log.info("===================  END RESULTS ==================");
        log.info(getTimeResult("Total execution time", end.getTime()
                - start.getTime()));
    }
    
    private void printWikiOutput() {
        log.info("");
        log.info("Results in wiki format:");
        log.info("");
        String lastTest = "";
        StringBuffer wikiResult = null;
        for (Stat stat : stats.getStats()) {
            if (!lastTest.equals(stat.getTest())) {
                if (wikiResult != null) {
                    log.info(wikiResult);
                    log.info("");
                }
                lastTest = stat.getTest();
                wikiResult = new StringBuffer("| " + stats.getVersion().trim() + " | rNNNNNNNN");
                log.info(lastTest + ":");
            }
            wikiResult.append(" | " + formatElapsedTime(stat.getElapsedTime()));
        }
        if (wikiResult != null)
            log.info(wikiResult);
    }

    private void printStats() {
        if (properties.getProperty("output.summary", "true").equals("true"))
            printResults();
        if (properties.getProperty("output.wiki", "true").equals("true"))
            printWikiOutput();
        if (properties.getProperty("output.xml", "false").equals("true"))
            printXmlStats();   
    }

    private void printTestHeader(String test) {
        log.info("");
        log.info("Tests: " + test);
        log.info(String.format("%22s %10s%10s", "Action", "Time", "Millis"));
        log.info(String.format("%22s %10s%10s", "----------", "---------",
                "---------"));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String propFile = null;
        Properties props = new Properties();
        if (args != null && args.length > 0)
            propFile = args[0];
        if (propFile != null) {
            FileInputStream inStream;
            try {
                inStream = new FileInputStream(propFile);
                props.load(inStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        new RunTests(props).run();
    }

    private String getTimeResult(String prefix, long elapsed) {
        String time = formatElapsedTime(elapsed);
        return String.format("%22s:%10s%10d", prefix, time, elapsed);
    }

    private String formatElapsedTime(long elapsed) {
        String time;
        if (elapsed > 3599999)
            time = lf.format(new Date(elapsed));
        else
            time = df.format(new Date(elapsed));
        return time;
    }

}
