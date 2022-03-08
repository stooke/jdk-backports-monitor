/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
package org.openjdk.backports;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

public class Options {
    private final String[] args;
    private String authProps;
    private String labelReport;
    private String labelHistoryReport;
    private String pushesReport;
    private String pendingPushReport;
    private String releaseNotesReport;
    private String affiliationReport;
    private String issueReport;
    private Long filterReport;
    private String logPrefix;
    private String hgRepos;
    private Actionable minLevel;
    private boolean directOnly;
    private Integer parityReport;
    private boolean includeCarryovers;
    private boolean verboseReports;
    private String outputFilename;
    private String issueListString;
    private final Vector<String> issueList = new Vector<>(10);

    public Options(String[] args) {
        this.args = args;
    }

    public boolean parse() throws IOException {
        OptionParser parser = new OptionParser();

        OptionSpec<String> optAuthProps = parser.accepts("auth",
                "Use this property file with user/pass authentication pair.")
                .withRequiredArg().ofType(String.class).describedAs("file").defaultsTo("auth.props");

        OptionSpec<String> optLabelReport = parser.accepts("label",
                "Report the backporting status for a given label.")
                .withRequiredArg().ofType(String.class).describedAs("tag");

        OptionSpec<String> optLabelHistoryReport = parser.accepts("label-history",
                "Report the change history for a given label.")
                .withRequiredArg().ofType(String.class).describedAs("tag");

        OptionSpec<String> optPushesReport = parser.accepts("pushes",
                "Report the backport pushes for a given release.")
                .withRequiredArg().ofType(String.class).describedAs("release");

        OptionSpec<String> optPendingPushReport = parser.accepts("pending-push",
                "Report backports that were approved and are pending for push for a given release.")
                .withRequiredArg().ofType(String.class).describedAs("release");

        OptionSpec<String> optIssueReport = parser.accepts("issue",
                "Report single issue status.")
                .withRequiredArg().ofType(String.class).describedAs("bug-id");

        OptionSpec<Long> optFilterReport = parser.accepts("filter",
                "Report issues matching a given filter.")
                .withRequiredArg().ofType(long.class).describedAs("filter-id");

        OptionSpec<Integer> optParityReport = parser.accepts("parity",
                "Report parity statistics for a given release.")
                .withRequiredArg().ofType(Integer.class).describedAs("release-train");

        OptionSpec<String> optReleaseNotesReport = parser.accepts("release-notes",
                "Report release notes for a given release.")
                .withRequiredArg().ofType(String.class).describedAs("release");

        OptionSpec<String> optLogPrefix = parser.accepts("output-prefix",
                "Use this output file prefix")
                .withRequiredArg().ofType(String.class).describedAs("output prefix").defaultsTo("output");

        OptionSpec<String> optUpdateHgDB = parser.accepts("hg-repos",
                "Use these repositories for Mercurial metadata")
                .withRequiredArg().ofType(String.class).describedAs("paths-to-local-hg");

        OptionSpec<String> optIssueList = parser.accepts("issues",
                        "List of relevant issue numbers separated by comma ")
                .withRequiredArg().ofType(String.class).describedAs("issue-list-string");

        OptionSpec<Actionable> optMinLevel = parser.accepts("min-level",
                "Minimal actionable level to print.")
                .withRequiredArg().ofType(Actionable.class).describedAs("level").defaultsTo(Actionable.NONE);

        OptionSpec<Void> optAffiliationReport = parser.accepts("affiliation",
                "Report contributor affiliations.");

        OptionSpec<Void> optDirectOnly = parser.accepts("direct-only",
                "For push reports, ignore backports and handle direct pushes only.");

        OptionSpec<Void> optIncludeCarryovers = parser.accepts("include-carryovers",
                "For release reports, include carry-overs from other releases.");

        OptionSpec<String> optOutputFilename = parser.accepts("output", "Output filename (defaults to stdout)")
                .withRequiredArg().ofType(String.class).describedAs("filename");

        OptionSpec<Void> optVerboseReports = parser.accepts("verbose", "Create verbose reports (where implemented)");

        parser.accepts("h", "Print this help.");

        OptionSet set;
        try {
            set = parser.parse(args);
        } catch (OptionException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        if (!set.hasOptions()) {
            System.err.println("ERROR: Please specify what report to generate.");
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        if (set.has("h")) {
            parser.printHelpOn(System.out);
            return false;
        }

        authProps = optAuthProps.value(set);
        labelReport = optLabelReport.value(set);
        labelHistoryReport = optLabelHistoryReport.value(set);
        pushesReport = optPushesReport.value(set);
        pendingPushReport = optPendingPushReport.value(set);
        issueReport = optIssueReport.value(set);
        filterReport = optFilterReport.value(set);
        parityReport = optParityReport.value(set);
        releaseNotesReport = optReleaseNotesReport.value(set);
        affiliationReport = set.has(optAffiliationReport) ? "yes" : null;

        logPrefix = optLogPrefix.value(set);

        hgRepos = optUpdateHgDB.value(set);
        minLevel = optMinLevel.value(set);
        directOnly = set.has(optDirectOnly);
        includeCarryovers = set.has(optIncludeCarryovers);
        verboseReports = set.has(optVerboseReports);
        outputFilename = set.has(optOutputFilename) ? optOutputFilename.value(set) : "-";
        issueListString = optIssueList.value(set);

        return true;
    }

    public String getAuthProps() {
        return authProps;
    }

    public String getLabelReport() {
        return labelReport;
    }

    public String getLabelHistoryReport() {
        return labelHistoryReport;
    }

    public String getPushesReport() {
        return pushesReport;
    }

    public String getPendingPushReport() {
        return pendingPushReport;
    }

    public String getReleaseNotesReport() {
        return releaseNotesReport;
    }

    public String getIssueReport() {
        return issueReport;
    }

    public String getAffiliationReport() {
        return affiliationReport;
    }

    public Long getFilterReport() {
        return filterReport;
    }

    public Integer getParityReport() {
        return parityReport;
    }

    public String getHgRepos() { return hgRepos; }

    public Actionable getMinLevel() { return minLevel; }

    public Vector<String> getIssueList() {
        if (issueListString != null) {
            Collections.addAll(issueList, issueListString.split(","));
        }
        return issueList;
    }

    public boolean directOnly() { return directOnly; }

    public boolean includeCarryovers() { return includeCarryovers; }

    public boolean doVerboseReports() { return verboseReports; }

    public String getOutputFilename() {
        return outputFilename;
    }

    public String getLogPrefix() {
        return logPrefix;
    }
}
