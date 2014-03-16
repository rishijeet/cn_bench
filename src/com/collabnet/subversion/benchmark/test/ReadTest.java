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

import java.util.Properties;

import org.apache.log4j.Logger;

import com.collabnet.subversion.benchmark.util.Keys;
import com.collabnet.subversion.benchmark.util.StatsCollector;

public class ReadTest extends AbstractTest {

    private Logger log = Logger.getLogger("ReadTest");

    public ReadTest(StatsCollector stats, Properties properties) {
        super(stats, properties);
        this.readOnlyTest = true;
    }

    @Override
    public void executeTest() throws Exception {
        log.trace("Executing " + toString());

        String repos = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS);

        String trunk = repos + "/trunk";
        String branches = repos + "/branches";
        String tags = repos + "/tags";
        String bigTree = repos + "/big-tree";

        // History of trunk
        stats.setStep("log");
        stats.setDescription("History of trunk --verbose");
        svn.init("log");
        svn.addArgument("-v");
        svn.addArgument("-rHEAD:0");
        svn.addArgument(trunk);
        svn.run();

        // History of branches with merge
        stats.setStep("log-merge");
        stats.setDescription("History of branches with merge info");
        svn.init("log");
        svn.addArgument("-v");
        svn.addArgument("-g");
        svn.addArgument("-rHEAD:0");
        svn.addArgument(branches);
        svn.run();

        // History of big tree
        stats.setStep("log-big");
        stats.setDescription("History of big-tree");
        svn.init("log");
        svn.addArgument("-v");
        svn.addArgument("-rHEAD:0");
        svn.addArgument(bigTree);
        svn.run();

        // Diff
        stats.setStep("diff-url");
        stats.setDescription("Diff of URL");
        svn.init("diff");
        svn.addArgument("--old");
        svn.addArgument(tags+ "/1.6.15");
        svn.addArgument("--new");
        svn.addArgument(tags+ "/1.6.16");
        svn.run();

        // Diff summarize
        stats.setStep("diff-summarize");
        stats.setDescription("Diff summarize URL");
        svn.init("diff");
        svn.addArgument("--summarize");
        svn.addArgument("--old");
        svn.addArgument(tags+ "/1.6.0");
        svn.addArgument("--new");
        svn.addArgument(trunk);
        svn.run();

        // list of trunk
        stats.setStep("list");
        stats.setDescription("List of trunk --verbose");
        svn.init("list");
        svn.addArgument("--depth=infinity");
        svn.addArgument("-v");
        svn.addArgument("-r12");
        svn.addArgument(trunk);
        svn.run();
    }

    @Override
    public String getTestId() {
        return "Read Tests";
    }

    @Override
    public String getTestDescription() {
        return "Runs through a number of commands to test their execution time";
    }

}
