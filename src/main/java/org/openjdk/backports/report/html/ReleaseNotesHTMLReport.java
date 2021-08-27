/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
package org.openjdk.backports.report.html;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.openjdk.backports.Main;
import org.openjdk.backports.StringUtils;
import org.openjdk.backports.report.model.ReleaseNotesModel;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

public class ReleaseNotesHTMLReport extends AbstractHTMLReport {

    private final ReleaseNotesModel model;

    public ReleaseNotesHTMLReport(ReleaseNotesModel model, PrintStream debugLog, String logPrefix) {
        super(debugLog, logPrefix);
        this.model = model;
    }

    @Override
    protected void doGenerate(PrintStream out) {
        out.println("<h1>RELEASE NOTES FOR: " + model.release() + "</h1>");
        out.println();
        out.println("Notes generated: " + new Date());
        out.println();

        out.println("Hint: Prefix bug IDs with " + Main.JIRA_URL + "browse/ to reach the relevant JIRA entry.");
        out.println();

        out.println("<h2>JAVA ENHANCEMENT PROPOSALS (JEP)</h2>");
        out.println();

        List<Issue> jeps = model.jeps();
        if (jeps.isEmpty()) {
            out.println("  None.");
        }

        for (Issue i : jeps) {
            out.println("  " + i.getSummary());
            out.println();

            String[] par = StringUtils.paragraphs(i.getDescription());
            if (par.length > 2) {
                // Second one is summary
                out.println(StringUtils.leftPad(StringUtils.rewrap(par[1], 100, 2), 6));
            } else {
                out.println(StringUtils.leftPad("No description.", 6));
            }
            out.println();
        }
        out.println();

        out.println("<h2>RELEASE NOTES, BY COMPONENT:</h2>");
        out.println();

        Map<String, Multimap<Issue, Issue>> rns = model.relNotes();

        boolean haveRelNotes = false;
        for (String component : rns.keySet()) {
            boolean printed = false;
            Multimap<Issue, Issue> m = rns.get(component);
            for (Issue i : m.keySet()) {
                haveRelNotes = true;

                if (!printed) {
                    out.println(component + ":");
                    out.println();
                    printed = true;
                }

                printReleaseNotes(out, m.values());
            }
        }
        if (!haveRelNotes) {
            out.println("  None.");
        }
        out.println();

        out.println("<h2>ALL FIXED ISSUES, BY COMPONENT AND PRIORITY</h2>");
        out.println();

        Multimap<String, Issue> byComponent = model.byComponent();

        for (String component : byComponent.keySet()) {
            out.println(component + ":");
            Multimap<String, Issue> byPriority = TreeMultimap.create(String::compareTo, DEFAULT_ISSUE_SORT);
            for (Issue i : byComponent.get(component)) {
                byPriority.put(i.getPriority().getName(), i);
            }
            for (String prio : byPriority.keySet()) {
                for (Issue i : byPriority.get(prio)) {
                    out.printf("  (%s) %s: %s%n", prio, i.getKey(), i.getSummary());
                }
            }
            out.println();
        }
        out.println();

        if (model.includeCarryovers()) {
            out.println("<h2>CARRIED OVER FROM PREVIOUS RELEASES</h2>");
            out.println("  These have fixes for the given release, but they are also fixed in the previous");
            out.println("  minor version of the same major release.");
            out.println();

            SortedSet<Issue> carriedOver = model.carriedOver();

            if (carriedOver.isEmpty()) {
                out.println("  None.");
            }

            for (Issue i : carriedOver) {
                out.printf("  (%s) %s: %s%n", i.getPriority().getName(), i.getKey(), i.getSummary());
            }
            out.println();
        }
    }

    protected void printReleaseNotes(PrintStream ps, Collection<Issue> relNotes) {
        PrintWriter pw = new PrintWriter(ps);
        printReleaseNotes(pw, relNotes);
        pw.flush();
    }

    protected void printReleaseNotes(PrintWriter pw, Collection<Issue> relNotes) {
        Set<String> dup = new HashSet<>();
        for (Issue rn : relNotes) {
            String summary = StringUtils.leftPad(rn.getKey() + ": " + rn.getSummary().replaceFirst("Release Note: ", ""), 2);
            String descr = StringUtils.leftPad(StringUtils.rewrap(StringUtils.stripNull(rn.getDescription()), StringUtils.DEFAULT_WIDTH - 6), 6);
            if (dup.add(descr)) {
                pw.println(summary);
                pw.println();
                pw.println(descr);
                pw.println();
            }
        }
    }
}