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

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.collabnet.subversion.benchmark.util.Keys;
import com.collabnet.subversion.benchmark.util.StatsCollector;

public class BasicTest extends AbstractTest {

    private Logger log = Logger.getLogger("BasicTest");

    public BasicTest(StatsCollector stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    public void executeTest() throws Exception {
        log.trace("Executing " + toString());

        // Copy trunk to tests area to use in tests
        String srcPath = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS)
                + "/trunk";
        String testPath = testRoot + "/trunk";

        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Create trunk folder for tests");
        svn.addArgument(srcPath);
        svn.addArgument(testPath);
        if (svn.run())
            log.trace("Succesfully created test path: " + testPath);
        else {
            log.error("Aborting " + getTestId());
            return;
        }

        // Checkout trunk
        stats.setStep("Checkout");
        stats.setDescription("Checkout SVN 1.5 trunk");
        svn.init("co");
        svn.addArgument("-r4");
        svn.addArgument(srcPath);
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Update trunk to HEAD
        stats.setStep("Update");
        stats.setDescription("Update WC from 1.5 to 1.6.16");
        svn.init("up");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Make sure branch exists
        waitForPath(testPath);

        // Switch to tests folder
        stats.setStep("Switch");
        stats.setDescription("Switch to tests branch - this is a no-op");

        svn.init("sw");
        svn.addArgument(testPath);
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Get all properties
        stats.setStep("Proplist");
        stats.setDescription("Get all properties in the WC");
        svn.init("pl");
        svn.addArgument("-R");
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Edit a few files and check status
        editFiles();

        stats.setStep("Status");
        stats.setDescription("Get status of edits to WC");
        svn.init("st");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Commit files
        stats.setStep("Commit");
        stats.setDescription("Commit a small number of changes to test branch");
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit a few files");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Run svnversion
        stats.setStep("svnversion");
        stats.setDescription("Run the svnversion command");
        svnversion.init(null);
        svnversion.setWorkingDirectory(workPath);
        svnversion.run();

        // Recursive info
        stats.setStep("Info");
        stats.setDescription("Run the info command recursively");
        svn.init("info");
        svn.addArgument("-R");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
    }

    private int editFiles() {
        int count = 0;
        File[] files = new File(workPath, "subversion/libsvn_diff").listFiles();
        for (int i = 0; i < files.length; i++) {
            if (writeLineToFile(files[i]))
                count++;
        }
        return count;
    }

    @Override
    public String getTestId() {
        return "Basic Tests";
    }

    @Override
    public String getTestDescription() {
        return "Runs through a number of commands to test their execution time";
    }

}
