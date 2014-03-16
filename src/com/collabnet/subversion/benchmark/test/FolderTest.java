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

public class FolderTest extends AbstractTest {

    private Logger log = Logger.getLogger("FolderTest");

    public FolderTest(StatsCollector stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    public void executeTest() throws Exception {
        log.trace("Executing " + toString());

        // Copy trunk to tests area to use in tests
        String srcPath = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS);

        stats.clearStep();

        String testTrunk = testRoot + "/main";
        String testBranch = testRoot + "/branch1";

        svn.init("mkdir");
        svn.addArgument("-m");
        svn.addArgument("Create trunk folder to hold content");
        svn.addArgument(testTrunk);
        svn.run();
        
        // Make sure everything has synched
        waitForPath(testTrunk);

        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Copy trunk folder");
        svn.addArgument(srcPath + "/trunk");
        svn.addArgument(testTrunk + "/trunk");
        if (!svn.run()) {
            log.error("Aborting " + getTestId());
            return;
        }
        
        // Make sure everything has synched
        waitForPath(testTrunk + "/trunk");

        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Copy tags folder");
        svn.addArgument(srcPath + "/tags");
        svn.addArgument(testTrunk + "/tags");
        if (!svn.run()) {
            log.error("Aborting " + getTestId());
            return;
        }
        
        // Make sure everything has synched
        waitForPath(testTrunk + "/tags");

        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Copy tags folder again");
        svn.addArgument(srcPath + "/tags");
        svn.addArgument(testTrunk + "/more-tags");
        if (!svn.run()) {
            log.error("Aborting " + getTestId());
            return;
        }
        
        // Make sure everything has synched
        waitForPath(testTrunk + "/more-tags");

        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Copy branches folder");
        svn.addArgument(srcPath + "/branches");
        svn.addArgument(testTrunk + "/branches");
        if (!svn.run()) {
            log.error("Aborting " + getTestId());
            return;
        }
        
        // Make sure everything has synched
        waitForPath(testTrunk + "/branches");
        
        // Create a branch from this new trunk
        svn.init("cp");
        svn.addArgument("-m");
        svn.addArgument("Create branch for tests");
        svn.addArgument(testTrunk);
        svn.addArgument(testBranch);
        svn.run();

        // Checkout tests branch
        stats.setStep("Checkout");
        stats.setDescription("Checkout project with a lot of folders");
        svn.init("co");
        svn.addArgument(testTrunk);
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Modify some files
        editFiles(new File(workPath, "tags/1.6.0/notes"));
        editFiles(new File(workPath, "trunk/subversion/bindings/javahl/native"));
        // Copy one local file so we have a new file to check for after commit
        String NEWFILE = "trunk/changelog.txt";
        stats.clearStep();
        svn.init("cp");
        svn.addArgument("trunk/CHANGES");
        svn.addArgument(NEWFILE);
        svn.setWorkingDirectory(workPath);
        svn.run();
       
        // Check status of working copy

        stats.setStep("Status");
        stats.setDescription("Get status of edits to WC");
        svn.init("st");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Commit changes
        stats.setStep("Commit-1");
        stats.setDescription("Commit a small number of changes to test branch");
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit files from WC with lots of folders");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Wait for commit to synch to replica
        waitForPath(testTrunk + "/" + NEWFILE);
        
        // Update working copy

        stats.setStep("Update");
        stats.setDescription("Update the WC to HEAD");
        svn.init("up");
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
        
        // Switch the working copy

        stats.setStep("Switch");
        stats.setDescription("Switch the WC to branch");
        svn.init("sw");
        svn.addArgument(testBranch);
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Add and commit a file on branch
        File f = new File(workPath, "trunk/branch-file.txt");
        createTextFile(f);
        
        stats.clearStep();
        svn.init("add");
        svn.addArgument(f.getAbsolutePath());
        svn.run();
        
        stats.setStep("Commit-2");
        stats.setDescription("Commit addition of new file");
        svn.init("ci");
        svn.addArgument("-m");
        svn.addArgument("Commit new file to branch");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Wait for file to exist
        waitForPath(testBranch + "/trunk/branch-file.txt");
        
        // Update working copy

        stats.clearStep();
        svn.init("up");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
        // Merge all changes from trunk to branch
        stats.setStep("Merge");
        stats.setDescription("Synch up branch with trunk");
        svn.init("merge");
        svn.addArgument("--accept=theirs-full");
        svn.addArgument(testTrunk);
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
        svn.addArgument(testTrunk);
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

    @Override
    public String getTestId() {
        return "Folder Tests";
    }

    @Override
    public String getTestDescription() {
        return "This test creates a tree with a lot of folders and then runs through standard commands.";
    }

}
