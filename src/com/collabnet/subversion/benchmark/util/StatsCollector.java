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
package com.collabnet.subversion.benchmark.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatsCollector {

    private List<Stat> stats = new ArrayList<Stat>();
    private Set<String> steps = new HashSet<String>();
    private String test;
    private String step;
    private String description;
    private String version;

    public void setStep(String s) throws Exception {
        if (steps.contains(s))
            throw new Exception("Step name not unique: " + s);
        step = s;
        steps.add(s);
    }

    public void setDescription(String s) {
        steps.clear();
        description = s;
    }

    public void addResult(Date startTime, Date endTime) {
        if (step != null) {
            stats.add(new Stat(test, step, description, startTime, endTime));
        }
    }

    /**
     * Clear the active step in the stats collection
     */
    public void clearStep() {
        step = null;
        description = null;
    }

    public class Stat {

        private String test;
        private String step;
        private String description;
        private Date startTime;
        private Date endTime;

        public Stat(String test, String step, String description, Date startTime,
                Date endTime) {
            super();
            this.test = test;
            this.step = step;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStep() {
            return step;
        }

        public String getDescription() {
            return description;
        }

        public long getElapsedTime() {
            return endTime.getTime() - startTime.getTime();
        }

        public Date getStartTime() {
            return startTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public String getTest() {
            return test;
        }

    }

    public List<Stat> getStats() {
        return stats;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

}
