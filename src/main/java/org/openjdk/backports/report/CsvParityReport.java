/*
 * Copyright (c) 2019,2021 Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.backports.report;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.opencsv.CSVWriter;
import org.openjdk.backports.Main;
import org.openjdk.backports.jira.Accessors;
import org.openjdk.backports.jira.InterestTags;
import org.openjdk.backports.jira.Versions;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;

public class CsvParityReport extends AbstractReport {

    private final JiraRestClient restClient;
    private final int majorVer;
    private final boolean verboseReport;

    public CsvParityReport(JiraRestClient restClient, int majorVer, boolean verboseReport) {
        super(restClient);
        this.restClient = restClient;
        this.majorVer = majorVer;
        this.verboseReport = verboseReport;
    }

    private static class IssueWithMetadata {
        final Issue issue;
        String firstOracleRaw;
        String firstOpenRaw;
        String interestTags;
        boolean backportRequested;

        IssueWithMetadata(Issue issue) {
            this.issue = issue;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", issue.getKey(), issue.getSummary());
        }
    }

    @Override
    public void run() {

        Multimap<Issue, Issue> mp = HashMultimap.create();

        List<String> vers = new ArrayList<>();
        int versLen = 0;

        Project proj = restClient.getProjectClient().getProject("JDK").claim();
        for (Version ver : proj.getVersions()) {
            String v = ver.getName();
            if (Versions.parseMajor(v) != majorVer) continue;
            if (Versions.isShared(v)) continue;
            vers.add(v);
            versLen = Math.max(versLen, v.length());
        }

        //vers.clear();
        //vers.add("11.0.13");

        for (String ver : vers) {
            Multimap<Issue, Issue> pb = jiraIssues.getIssuesWithBackportsOnly("project = JDK" +
                    " AND (status in (Closed, Resolved))" +
                    " AND (labels not in (release-note, openjdk-na, openjdk" + majorVer + "u-WNF) OR labels is EMPTY)" +
                    " AND (issuetype != CSR)" +
                    " AND (resolution not in (\"Won't Fix\", Duplicate, \"Cannot Reproduce\", \"Not an Issue\", Withdrawn, Other))" +
                    " AND fixVersion = " + ver);

            for (Issue parent : pb.keySet()) {
                if (Accessors.isOracleSpecific(parent)) {
                    // There is no parity with these
                    continue;
                }
                if (Accessors.isOpenJDKWontFix(parent, majorVer)) {
                    // Determined as won't fix for OpenJDK, skip
                    continue;
                }
                if (majorVer == 8 && Accessors.extractComponents(parent).startsWith("javafx")) {
                    // JavaFX is not the part of OpenJDK 8, no parity.
                    continue;
                }
                if (mp.containsKey(parent)) {
                    // Already parsed, skip
                    continue;
                }
                for (Issue backport : pb.get(parent)) {
                    if (Accessors.isDelivered(backport)) {
                        mp.put(parent, backport);
                    }
                }
            }
        }

        Map<String, Map<Issue, IssueWithMetadata>> onlyOpen = new TreeMap<>();
        Map<String, Map<Issue, IssueWithMetadata>> onlyOracle = new TreeMap<>();

        /* these reports are less useful so haven't been ported yet */
        SortedMap<Issue, String> exactOpenFirst = new TreeMap<>(DEFAULT_ISSUE_SORT);
        SortedMap<Issue, String> exactOracleFirst = new TreeMap<>(DEFAULT_ISSUE_SORT);
        SortedMap<Issue, String> exactUnknown = new TreeMap<>(DEFAULT_ISSUE_SORT);
        SortedMap<Issue, String> lateOpenFirst = new TreeMap<>(DEFAULT_ISSUE_SORT);
        SortedMap<Issue, String> lateOracleFirst = new TreeMap<>(DEFAULT_ISSUE_SORT);

        for (Issue p : mp.keySet()) {
            boolean isShared = false;
            String firstOracle = null;
            String firstOracleRaw = null;
            String firstOpen = null;
            String firstOpenRaw = null;
            LocalDateTime timeOracle = null;
            LocalDateTime timeOpen = null;

            boolean backportRequested = p.getLabels().contains("jdk" + majorVer + "u-fix-request");
            String interestTags = InterestTags.shortTags(p.getLabels());

            // Awkward hack: parent needs to be counted for parity, on the off-chance
            // it has the fix-version after the open/closed split.
            List<Issue> issues = new ArrayList<>();
            issues.addAll(mp.get(p)); // all sub-issues
            issues.add(p);            // and the issue itself

            for (Issue subIssue : issues) {
                IssueField rdf = subIssue.getField("resolutiondate");
                LocalDateTime rd = null;
                if (rdf != null && rdf.getValue() != null) {
                    String rds = rdf.getValue().toString();
                    rd = LocalDateTime.parse(rds.substring(0, rds.indexOf(".")));
                }

                for (String fv : Accessors.getFixVersions(subIssue)) {
                    if (Versions.parseMajor(fv) != majorVer) {
                        // Not the release we are looking for
                        continue;
                    }
                    if (Versions.isShared(fv)) {
                        isShared = true;
                    }

                    String sub = Versions.stripVendor(fv);
                    if (Versions.isOracle(fv)) {
                        if (firstOracle == null) {
                            firstOracle = sub;
                            firstOracleRaw = fv;
                            timeOracle = rd;
                        } else {
                            if (Versions.compare(sub, firstOracle) < 0) {
                                firstOracle = sub;
                                firstOracleRaw = fv;
                                timeOracle = rd;
                            }
                        }
                    } else {
                        if (firstOpen == null) {
                            firstOpen = sub;
                            firstOpenRaw = fv;
                            timeOpen = rd;
                        } else {
                            if (Versions.compare(sub, firstOpen) < 0) {
                                firstOpen = sub;
                                firstOpenRaw = fv;
                                timeOpen = rd;
                            }
                        }
                    }
                }
            }

            if (isShared) {
                continue;
            }

            if (firstOracle == null && firstOpen != null) {
                Map<Issue, IssueWithMetadata> map = onlyOpen.computeIfAbsent(firstOpen, k -> new TreeMap<>(DEFAULT_ISSUE_SORT));
                IssueWithMetadata xissue = new IssueWithMetadata(p);
                xissue.firstOracleRaw = "";
                xissue.firstOpenRaw = firstOpenRaw;
                xissue.interestTags = interestTags;
                xissue.backportRequested = backportRequested;
                map.put(p, xissue);
            }

            if (firstOracle != null && firstOpen == null) {
                Map<Issue, IssueWithMetadata> map = onlyOracle.computeIfAbsent(firstOracle, k -> new TreeMap<>(DEFAULT_ISSUE_SORT));
                IssueWithMetadata xissue = new IssueWithMetadata(p);
                xissue.firstOracleRaw = firstOracleRaw;
                xissue.firstOpenRaw = "";
                xissue.interestTags = interestTags;
                xissue.backportRequested = backportRequested;
                map.put(p, xissue);
            }

            if (verboseReport) {
                if (firstOracle != null && firstOpen != null && Versions.compare(firstOracleRaw, firstOpen) == 0) {
                    if (timeOpen == null || timeOracle == null) {
                        exactUnknown.put(p, String.format("  %-" + versLen + "s ... %-" + versLen + "s, %s: %s",
                                firstOpenRaw, firstOracleRaw, p.getKey(), p.getSummary()));
                    } else if (timeOpen.compareTo(timeOracle) < 0) {
                        exactOpenFirst.put(p, String.format("  %-" + versLen + "s -> %-" + versLen + "s, %s: %s",
                                firstOpenRaw, firstOracleRaw, p.getKey(), p.getSummary()));
                    } else {
                        exactOracleFirst.put(p, String.format("  %-" + versLen + "s -> %-" + versLen + "s, %s: %s",
                                firstOracleRaw, firstOpenRaw, p.getKey(), p.getSummary()));
                    }
                }

                if (firstOracle != null && firstOpen != null && Versions.compare(firstOpen, firstOracle) < 0) {
                    lateOpenFirst.put(p, String.format("  %-" + versLen + "s -> %-" + versLen + "s, %s: %s",
                            firstOpenRaw, firstOracleRaw, p.getKey(), p.getSummary()));
                }

                if (firstOracle != null && firstOpen != null && Versions.compare(firstOpen, firstOracle) > 0) {
                    lateOracleFirst.put(p, String.format("  %-" + versLen + "s -> %-" + versLen + "s, %s: %s",
                            firstOracleRaw, firstOpenRaw, p.getKey(), p.getSummary()));
                }
            }
        }

        try {
            String header = "bugid,creationDate,priority,component,openjdkRelease,oracleRelease,interest,backportRQ,summary,description";
            CSVWriter writer = new CSVWriter(new PrintWriter(out));
            writer.writeNext(header.split(","));

            printWithVersion(writer, onlyOracle);
            printWithVersion(writer, onlyOpen);

            if (verboseReport) {
                printSimple(lateOpenFirst);
                printSimple(lateOracleFirst);
                printSimple(exactOpenFirst);
                printSimple(exactOracleFirst);
                printSimple(exactUnknown);
            }

            if (out != System.out) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void printWithVersion(CSVWriter writer, Map<String, Map<Issue, IssueWithMetadata>> issues) {

        for (Map.Entry<String, Map<Issue, IssueWithMetadata>> kv : issues.entrySet()) {
            if (verboseReport) {
                Main.debug.println(kv.getKey() + " (" + kv.getValue().size() + " issues):");
            }
            for (Map.Entry<Issue, IssueWithMetadata> kv2 : kv.getValue().entrySet()) {
                String[] entries = new String[10];
                int index = 0;
                Issue p = kv2.getKey();
                entries[index++] = p.getKey();
                entries[index++] = p.getCreationDate().toString("yyy-MM-dd");
                entries[index++] = p.getPriority() != null ? p.getPriority().getName() : "";
                String componentStr = null;
                for (BasicComponent c : p.getComponents()) {
                    componentStr = (componentStr == null) ? c.getName() : "," + c.getName();
                }
                entries[index++] = componentStr;
                entries[index++] = kv2.getValue().firstOpenRaw;
                entries[index++] = kv2.getValue().firstOracleRaw;
                entries[index++] = kv2.getValue().interestTags;
                entries[index++] = kv2.getValue().backportRequested ? "bp" : "";
                entries[index++] = p.getSummary();
                entries[index] = p.getDescription();
                writer.writeNext(entries);
            }
            for (Map.Entry<Issue, IssueWithMetadata> kv2 : kv.getValue().entrySet()) {
                Main.debug.format("%20s %s\n", kv2.getValue().firstOpenRaw + kv2.getValue().firstOracleRaw, kv2.getValue());
            }
            Main.debug.println();
        }
    }

    void printSimple(Map<Issue, String> issues) {
        for (Map.Entry<Issue,String> kv : issues.entrySet()) {
            out.println(kv.getValue());
        }
        out.println();
    }
}
