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
package org.openjdk.backports.jira;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class Accessors {

    public static String getFixVersion(Issue issue) {
        Iterator<Version> it = issue.getFixVersions().iterator();
        if (!it.hasNext()) {
            return "N/A";
        }
        Version fixVersion = it.next();
        if (it.hasNext()) {
            throw new IllegalStateException("Multiple fix versions");
        }
        return fixVersion.getName();
    }

    public static String getPushURL(Issue issue) {
        for (Comment c : issue.getComments()) {
            if (c.getAuthor().getName().equals("hgupdate")) {
                return Parsers.parseURL(c.getBody());
            }
        }
        return "N/A";
    }

    public static String getPushDate(Issue issue) {
        for (Comment c : issue.getComments()) {
            if (c.getAuthor().getName().equals("hgupdate")) {
                return Parsers.parseDaysAgo(c.getBody()) + " day(s) ago";
            }
        }
        return "N/A";
    }

    public static String getPushUser(Issue issue) {
        for (Comment c : issue.getComments()) {
            if (c.getAuthor().getName().equals("hgupdate")) {
                return Parsers.parseUser(c.getBody());
            }
        }
        return "N/A";
    }

    public static long getPushDaysAgo(Issue issue) {
        for (Comment c : issue.getComments()) {
            if (c.getAuthor().getName().equals("hgupdate")) {
                return Parsers.parseDaysAgo(c.getBody());
            }
        }
        return -1;
    }

    public static long getPushSecondsAgo(Issue issue) {
        for (Comment c : issue.getComments()) {
            if (c.getAuthor().getName().equals("hgupdate")) {
                return Parsers.parseSecondsAgo(c.getBody());
            }
        }
        return -1;
    }

    public static String extractComponents(Issue issue) {
        StringJoiner joiner = new StringJoiner("/");
        for (BasicComponent c : issue.getComponents()) {
            joiner.add(c.getName());
        }
        IssueField subcomponent = issue.getFieldByName("Subcomponent");
        if (subcomponent != null && subcomponent.getValue() != null) {
            try {
                JSONObject o = new JSONObject(subcomponent.getValue().toString());
                joiner.add(o.get("name").toString());
            } catch (JSONException e) {
                // Do nothing
            }
        }
        return joiner.toString();
    }

    public static RetryableIssuePromise getParent(IssueRestClient cli, Issue start) {
        List<RetryableIssuePromise> backports = new ArrayList<>();
        for (IssueLink link : start.getIssueLinks()) {
            if (link.getIssueLinkType().getName().equals("Backport")) {
                String linkKey = link.getTargetIssueKey();
                backports.add(new RetryableIssuePromise(cli, linkKey));
            }
        }

        // If there is only a single "Backport link", report it as parent
        if (backports.size() == 1) {
            return backports.get(0);
        } else {
            return null;
        }
    }
    public static List<String> getReleaseNote(IssueRestClient cli, Issue start) {
        List<RetryableIssuePromise> relnotes = new ArrayList<>();

        // Search in sub-tasks
        for (Subtask link : start.getSubtasks()) {
            String linkKey = link.getIssueKey();
            relnotes.add(new RetryableIssuePromise(cli, linkKey));
        }

        // Search in related issues
//        for (IssueLink link : start.getIssueLinks()) {
//            if (link.getIssueLinkType().getName().equals("Relates")) {
//                String linkKey = link.getTargetIssueKey();
//                relnotes.add(new RetryableIssuePromise(cli, linkKey));
//            }
//        }

        List<String> releaseNotes = new ArrayList<>();

        // Direct hit?
        if (start.getLabels().contains("release-note")) {
            releaseNotes.add(start.getDescription());
        }

        for (RetryableIssuePromise p : relnotes) {
            Issue i = p.claim();
            if (i.getLabels().contains("release-note")) {
                releaseNotes.add(i.getDescription());
            }
        }

        return releaseNotes;
    }

}