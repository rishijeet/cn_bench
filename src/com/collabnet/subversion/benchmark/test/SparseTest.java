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

public class SparseTest extends AbstractTest {

    private Logger log = Logger.getLogger("SparseTest");

    public SparseTest(StatsCollector stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    public void executeTest() throws Exception {
        log.trace("Executing " + toString());

        // Copy trunk to tests area to use in tests
        String rootURL = props.getProperty(Keys.SVN_REPOS_URL, DEFAULT_REPOS);

        // Checkout trunk
        stats.setStep("Checkout");
        stats.setDescription("Checkout repos root");
        svn.init("co");
        svn.addArgument("--depth=immediates");
        svn.addArgument(rootURL);
        svn.addArgument(".");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Expand trunk folder
        stats.setStep("expand-trunk");
        stats.setDescription("Expand trunk folder");
        svn.init("up");
        svn.addArgument("--set-depth=infinity");
        svn.addArgument("trunk");
        svn.setWorkingDirectory(workPath);
        svn.run();

        // Expand branches folder
        stats.setStep("expand-branch");
        stats.setDescription("Expand branches folder");
        svn.init("up");
        svn.addArgument("--set-depth=infinity");
        svn.addArgument("branches");
        svn.setWorkingDirectory(workPath);
        svn.run();
        
    }

    @Override
    public String getTestId() {
        return "Sparse Tests";
    }

    @Override
    public String getTestDescription() {
        return "Runs through a number of commands to test their execution time";
    }

}
