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

public class MergeTest extends AbstractTest {

    private Logger log = Logger.getLogger("MergeTest");

    public MergeTest(StatsCollector stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    public void executeTest() throws Exception {
        log.trace("Executing " + toString());

        // Copy trunk to tests area to use in tests
        String srcPath = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS)
                + "/trunk";
        String testPath = testRoot + "/branch1";

        stats.clearStep();
        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Create branch folder for tests");
        svn.addArgument("-r12");
        svn.addArgument(srcPath);
        svn.addArgument(testPath);
        if (svn.run())
            log.trace("Succesfully created test path: " + testPath);
        else {
            log.error("Aborting " + getTestId());
            return;
        }
        
        // Wait for branch
        waitForPath(testPath);

        // Checkout branch
        svn.init("co");
        svn.addArgument(testPath);
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Merge all changes from trunk
        stats.setStep("Merge-1");
        stats.setDescription("Merge all changes from trunk");
        svn.init("merge");
        svn.addArgument(srcPath);
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Revert
        stats.setStep("Revert");
        stats.setDescription("Revert all merge changes");
        svn.init("revert");
        svn.addArgument("-R");
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Make some edits to branch
        editFiles(new File(workPath, "notes"));
        editFiles(new File(workPath, "subversion/bindings/javahl/native"));
        // Copy one local file so we have a new file to check for after commit
        String NEWFILE = "changelog.txt";
        stats.clearStep();
        svn.init("cp");
        svn.addArgument("CHANGES");
        svn.addArgument(NEWFILE);
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Commit changes to branch
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit changes to branch");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Wait for commit to synch to replica
        waitForPath(testPath + "/" + NEWFILE);

        // Update to single revision
        svn.init("up");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Merge all changes from trunk
        stats.setStep("Merge-2");
        stats.setDescription("Synch up branch with trunk");
        svn.init("merge");
        svn.addArgument("--accept=theirs-full");
        svn.addArgument(srcPath);
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Commit changes to branch
        stats.clearStep();
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Synch up branch with trunk");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Create a new folder in repos to check for synch
        String SYNC_CHECK = testRoot + "/sync1";
        svn.init("mkdir");
        svn.addArgument("-m");
        svn.addArgument("Create folder for synch check");
        svn.addArgument(SYNC_CHECK);
        while (!svn.runNoLog()) {
            Thread.sleep(2000);
        }

        // Switch to trunk
        svn.init("sw");
        svn.addArgument(srcPath);
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Wait for previous commit to appear in replica
        waitForPath(SYNC_CHECK);

        // Reintegrate merge
        stats.setStep("Reintegrate");
        stats.setDescription("Reintegrate branch to trunk");
        svn.init("merge");
        svn.addArgument("--accept=theirs-full");
        svn.addArgument("--reintegrate");
        svn.addArgument(testPath);
        svn.setWorkingDirectory(workPath);
        svn.run();

    }

    @Override
    public String getTestId() {
        return "Merge Tests";
    }

    @Override
    public String getTestDescription() {
        return "Runs through a number of merge commands to test their execution time";
    }

    private void editFiles(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    writeLineToFile(files[i]);
                }
            } else {
                writeLineToFile(path);
            }
        }
    }

}
