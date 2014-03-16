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
package com.collabnet.subversion.benchmark.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.collabnet.subversion.benchmark.command.SVNCommand;
import com.collabnet.subversion.benchmark.command.Tool;
import com.collabnet.subversion.benchmark.util.Keys;
import com.collabnet.subversion.benchmark.util.StatsCollector;

public abstract class AbstractTest {
    protected static final String DEFAULT_LOCAL = "/Users/mphippard/tests/benchmark";
    protected static final String DEFAULT_REPOS = "file:///Users/mphippard/repositories/benchmark";
    private static final int MAX_RETRIES = 60;
    protected Properties props;
    protected StatsCollector stats;
    protected boolean testFailed = false;
    protected boolean readOnlyTest = false;

    private Logger log = Logger.getLogger("AbstractTest");

    protected String url;
    protected String uuid;
    protected String testRoot;
    protected File workPath;
    protected String svnLogFile;
    protected SVNCommand svn;
    protected SVNCommand svnversion;

    public AbstractTest(StatsCollector stats, Properties properties) {
        super();
        uuid = UUID.randomUUID().toString();
        props = properties;
        this.stats = stats;
        this.svn = new SVNCommand(Tool.SVN, this.stats);
        this.svnversion = new SVNCommand(Tool.SVNVERSION, this.stats);
    }

    public abstract void executeTest() throws Exception;

    public abstract String getTestId();

    public abstract String getTestDescription();

    protected void setup() throws Exception {
        log.trace("Begin of setup for " + toString());

        stats.setTest(getTestId());
        stats.clearStep();
        svn.init("--version");
        svn.addArgument("-q");
        String version = svn.runAndReturnOutput();
        stats.setVersion(version);

        if (!readOnlyTest) {
            // Create folder for working copy
            workPath = new File(props.getProperty(Keys.SVN_LOCAL_ROOT,
                    DEFAULT_LOCAL) + File.separator + uuid);
            if (!workPath.mkdir())
                throw new Exception("Failed to create local work path "
                        + workPath.getAbsolutePath());
            log.debug("Succesfully created work path: "
                    + workPath.getAbsolutePath());
    
            // Create tests area in repository
            url = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS);
            testRoot = url + "/tests/" + uuid;
            svn.init("mkdir");
            svn.addArgument("--parents");
            svn.addArgument("-m");
            svn.addArgument("Create folder for this test run");
            svn.addArgument(testRoot);
            for (int i = 0; i < MAX_RETRIES; i++) {
                if (svn.run()) {
                    log.trace("Succesfully created path: " + testRoot);
                    waitForPath(testRoot);
                    break;
                }
                if ( i == MAX_RETRIES-1 ) {
                    throw new Exception("Failed to create " + testRoot);
                }
                else {
                    Thread.sleep(2000);
                }
            }
            if (svn.run()) {
                log.trace("Succesfully created path: " + testRoot);
                waitForPath(testRoot);
            } else {
                throw new Exception("Failed to create " + testRoot);
            }
        }

        log.trace("End of setup for " + toString());
    }

    /**
     * This method is run after creating a new path in the
     * repository.  It blocks execution until that path can
     * be retrieved.  This allows the tests to be run against
     * a replicated proxy server.
     * 
     * @param path - URL to wait for
     */
    protected void waitForPath(String path) throws InterruptedException  {
        stats.clearStep();
        svn.init("info");
        svn.addArgument(path);
        while (!svn.runNoLog()) {
            Thread.sleep(2000);
        }
    }

    protected void tearDown() throws InterruptedException {
        log.trace("Begin of teardown for " + toString());

        stats.clearStep();

        if (!readOnlyTest) {
            // Remove working copy area
            try {
                FileUtils.deleteDirectory(workPath);
            } catch (IOException e) {
                log.warn(
                        "Error cleaning up local work path "
                                + workPath.getAbsolutePath(), e);
            }
    
            // Remove URL from repository
    
            svn.init("rm");
            svn.addArgument("-m");
            svn.addArgument("Removing test folder");
            svn.addArgument(testRoot);
            while (!svn.run()) {
                Thread.sleep(2000);
            }
            log.trace("Succesfully removed test root path: " + testRoot);
        }
        log.trace("End of teardown for " + toString());

    }

    public void run() {
        try {
            setup();
        } catch (Exception e) {
            testFailed = true;
            log.error("Test " + getTestId() + " failed.", e);
            return;
        }
        try {
            executeTest();
        } catch (Exception e) {
            testFailed = true;
            log.error("Test " + getTestId() + " failed.", e);
        } finally {
            try {
                tearDown();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public String toString() {
        return getTestId() + " UUID: " + uuid;
    }

    protected boolean writeLineToFile(File f) {
        if (f.isDirectory())
            return false;
        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new FileWriter(f, true));
            bw.write("Adding line of text to end of file");
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            log.error(ioe);
        } finally { // always close the file
            if (bw != null)
                try {
                    bw.close();
                } catch (IOException ioe2) {
                    // just ignore it
                }
        } // end try/catch/finally
        return true;
    }

    protected void createTextFile(File f) throws IOException {
        f.createNewFile();
        writeLineToFile(f);
    }

}
