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
package com.collabnet.subversion.benchmark.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.collabnet.subversion.benchmark.util.StatsCollector;
import com.devdaily.system.SystemCommandExecutor;

public class SVNCommand {

    private final Tool tool;
    private final StatsCollector stats;
    private final Logger log;
    private final List<String> command;
    private File path;

    public SVNCommand(Tool tool, StatsCollector stats) {
        super();
        this.tool = tool;
        this.stats = stats;
        this.log = Logger.getLogger("SVN");
        this.command = new ArrayList<String>();
    }

    public boolean run() {
        return run(false, true).getCode() == 0;
    }

    public boolean runNoLog() {
        return run(false, false).getCode() == 0;
    }

    public String runAndReturnOutput() {
        return run(true, true).getOutput();
    }

    private Result run(boolean wantOutput, boolean logOutput) {

        SystemCommandExecutor commandExecutor = new SystemCommandExecutor(
                command, path);
        if (logOutput)
            logCommandLine();
        Date start = new Date();
        int result = -1;
        try {
            result = commandExecutor.executeCommand();
            stats.addResult(start, new Date());
            if (logOutput) {
                if (log.isDebugEnabled())
                    log.debug(commandExecutor.getStandardOutputFromCommand());
                if (result > 0)
                    log.error(commandExecutor.getStandardErrorFromCommand());
            }
            if (wantOutput)
                return new Result(result, commandExecutor.getStandardOutputFromCommand()
                        .toString());
        } catch (Exception e) {
            log.error("Error running SVN command", e);
        }
        return new Result(result, null);
    }

    private void logCommandLine() {
        String[] args = getArguments();
        StringBuffer sb = new StringBuffer();
        sb.append("$");
        for (int i = 0; i < args.length; i++) {
            sb.append(" " + args[i]);
        }
        log.info(sb.toString());
    }

    private String[] getArguments() {
        String[] args = new String[command.size()];
        command.toArray(args);
        return args;
    }

    public void init(String subcommand) {
        command.clear();
        switch (tool) {
        case SVNVERSION:
            addArgument("svnversion");
            break;
        case SVNLOOK:
            addArgument("svnlook");
            break;
        case SVNADMIN:
            addArgument("svnadmin");
            break;
        case SVNSYNC:
            addArgument("svnsync");
            break;
        default:
            addArgument("svn");
        }
        if (subcommand != null) {
            addArgument(subcommand);
            if (!log.isDebugEnabled()
                    && supportsQuiteMode(subcommand))
                addArgument("-q");
        }
    }

    private boolean supportsQuiteMode(String subcommand) {
        return !(subcommand.equals("info") || 
                subcommand.equals("cleanup") ||
                subcommand.equals("log") ||
                subcommand.equals("list") ||
                subcommand.equals("diff") ||
                subcommand.equals("--version"));
    }

    public void addArgument(String arg) {
        command.add(arg);
    }

    public void setWorkingDirectory(File workPath) {
        this.path = workPath;

    }
    
    private class Result {
        private int code;
        private String output;

        public Result(int code, String output) {
            super();
            this.code = code;
            this.output = output;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getOutput() {
            return output;
        }
        
    }

}
