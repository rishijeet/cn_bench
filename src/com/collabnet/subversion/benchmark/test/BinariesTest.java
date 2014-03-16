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

public class BinariesTest extends AbstractTest {

    private Logger log = Logger.getLogger("BinariesTest");

    public BinariesTest(StatsCollector stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    public void executeTest() throws Exception {
        log.trace("Executing " + toString());

        // Copy big-tree to tests area to use in tests
        String srcPath = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS)
                + "/big-tree";
        String testPath = testRoot + "/big-tree";
        String testBranch = testRoot + "/big-tree-branch";

        stats.clearStep();
        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Create folder for tests");
        svn.addArgument("-r26");
        svn.addArgument(srcPath);
        svn.addArgument(testPath);
        if (svn.run())
            log.trace("Succesfully created test path: " + testPath);
        else {
            log.error("Aborting " + getTestId());
            return;
        }
        
        waitForPath(testPath);
        
        // Create branch
        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Create branch for tests");
        svn.addArgument(testPath);
        svn.addArgument(testBranch);
        svn.run();

        // Checkout test
        stats.setStep("Checkout");
        stats.setDescription("Checkout the big-tree folder");
        svn.init("co");
        svn.addArgument(srcPath);
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Update test
        stats.setStep("Update");
        stats.setDescription("Backdate the WC to r25");
        svn.init("up");
        svn.addArgument("-r25");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Make sure branch exists
        waitForPath(testPath);

        // Switch test
        stats.setStep("Switch-1");
        stats.setDescription("Switch to the tests folder (also updates lots of files)");
        svn.init("sw");
        svn.addArgument(testPath);
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Cleanup test
        stats.setStep("Cleanup");
        stats.setDescription("Run cleanup on big WC");
        svn.init("cleanup");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Delete some new files.
        stats.setStep("Delete");
        stats.setDescription("Delete some files in one of the folders");
        svn.init("rm");
        svn.addArgument("intl");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Status test
        stats.setStep("Status-1");
        stats.setDescription("Check status of WC");
        svn.init("st");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Commit files
        stats.setStep("Commit-1");
        stats.setDescription("Commit the deleted files");
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit the delete of a few files");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Move a folder.
        stats.setStep("Rename");
        stats.setDescription("Move a folder into one of the folders");
        svn.init("mv");
        svn.addArgument("places");
        svn.addArgument("combined");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Add a new file
        File fnew = new File(workPath, "new-main-file.txt");
        createTextFile(fnew);
        
        stats.clearStep();
        svn.init("add");
        svn.addArgument(fnew.getAbsolutePath());
        svn.run();

        // Status test
        stats.setStep("Status-2");
        stats.setDescription("Check status of WC after move");
        svn.init("st");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Commit files
        stats.setStep("Commit-2");
        stats.setDescription("Commit the moved folder");
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit the moved folder");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Run svnversion
        stats.setStep("svnversion");
        stats.setDescription("Run the svnversion command");
        svnversion.init(null);
        svnversion.setWorkingDirectory(workPath);
        svnversion.run();
        
        // Make sure new file exists before we switch
        waitForPath(testPath + "/new-main-file.txt");

        // Recursive info
        stats.setStep("Info");
        stats.setDescription("Run the info command recursively");
        svn.init("info");
        svn.addArgument("-R");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Switch to branch
        stats.setStep("Switch-2");
        stats.setDescription("Switch the WC to branch");
        svn.init("sw");
        svn.addArgument(testBranch);
        svn.setWorkingDirectory(workPath);
        svn.run();
       
        // Add and commit file on branch
        File f = new File(workPath, "branch-file.txt");
        createTextFile(f);
        
        stats.clearStep();
        svn.init("add");
        svn.addArgument(f.getAbsolutePath());
        svn.run();
        
        stats.setStep("Commit-3");
        stats.setDescription("Commit addition of new file");
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit new file to branch");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Make sure file exists
        waitForPath(testBranch + "/branch-file.txt");
        
        // Update working copy

        stats.clearStep();
        svn.init("up");
        svn.setWorkingDirectory(workPath);
        svn.run();
       
        // Synch up merge
        stats.setStep("Merge");
        stats.setDescription("Synch up branch with trunk");
        svn.init("merge");
        svn.addArgument("--accept=theirs-full");
        svn.addArgument(testPath);
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Commit changes to branch
        stats.clearStep();
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Synch up branch with big-tree");
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
        svn.addArgument(testPath);
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
        svn.addArgument(testBranch);
        svn.setWorkingDirectory(workPath);
        svn.run();
    }

    @Override
    public String getTestId() {
        return "Binaries Tests";
    }

    @Override
    public String getTestDescription() {
        return "Runs some tests using a sample structure containing thousands of small binary files.  Mainly testing the handling of lots of files in single folder.";
    }

}
