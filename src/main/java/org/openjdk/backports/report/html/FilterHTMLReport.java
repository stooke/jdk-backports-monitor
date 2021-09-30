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
import org.openjdk.backports.report.model.FilterModel;

import java.io.PrintStream;
import java.util.Date;

public class FilterHTMLReport extends AbstractHTMLReport {

    private final FilterModel model;

    public FilterHTMLReport(FilterModel model, PrintStream debugLog, String logPrefix) {
        super(debugLog, logPrefix);
        this.model = model;
    }

    @Override
    public void doGenerate(PrintStream out) {
        out.println("<h1>FILTER REPORT: " + model.name() + "</h1>");
        out.println("<p>Report generated: " + new Date() + "</p>");

        Multimap<String, Issue> byComponent = model.byComponent();
        for (String component : byComponent.keySet()) {
            out.println("<h2>" + component + "</h2>");
            out.println("<table>");
            out.println("<tr>");
            out.println("<th nowrap>Bug</th>");
            out.println("<th nowrap width=\"99%\">Synopsis</th>");
            out.println("</tr>");
            for (Issue i : byComponent.get(component)) {
                 out.println("<tr>");
                 out.println("<td>" + issueLink(i) + "</td>");
                 out.println("<td>" + i.getSummary() + "</td>");
                 out.println("</tr>");
            }
            out.println("</table>");
        }
    }
}
