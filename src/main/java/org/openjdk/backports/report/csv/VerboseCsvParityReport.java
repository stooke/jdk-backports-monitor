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
package org.openjdk.backports.report.csv;

import com.atlassian.jira.rest.client.api.domain.*;
import com.opencsv.CSVWriter;
import org.openjdk.backports.Main;
import org.openjdk.backports.report.model.ParityModel;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

public class VerboseCsvParityReport extends AbstractCSVReport {

    private final ParityModel model;
    private final boolean isSingleMode;

    public VerboseCsvParityReport(ParityModel model, PrintStream debugLog, String logPrefix, boolean isSingleMode) {
        super(debugLog, logPrefix);
        this.model = model;
        this.isSingleMode = isSingleMode;
    }

    @Override
    protected void doGenerate(PrintStream out) {
        try {
            String header = "bugid,creationDate,priority,component,openjdkRelease,oracleRelease,interest,backportRQ,summary,description";
            CSVWriter writer = new CSVWriter(new PrintWriter(out));
            writer.writeNext(header.split(","));

            if (isSingleMode) {
                printSingle(writer, model.getAll());
            } else {
                printWithVersion(writer, model.onlyOracle());
                printWithVersion(writer, model.onlyOpen());
            }

            if (out != System.out) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void printSingle(CSVWriter writer, Map<String, Issue> issues) {
        for (Issue issue : issues.values()) {
            String[] entries = new String[10];
            int index = 0;
            entries[index++] = issue.getKey();
            entries[index++] = issue.getCreationDate().toString("yyy-MM-dd");
            entries[index++] = issue.getPriority() != null ? issue.getPriority().getName() : "";
            String componentStr = null;
            for (BasicComponent c : issue.getComponents()) {
                componentStr = (componentStr == null) ? c.getName() : "," + c.getName();
            }
            entries[index++] = componentStr;
            entries[index++] = "";
            Resolution resolution = issue.getResolution();
            Status status = issue.getStatus();
            Iterable<Version> versions = issue.getFixVersions();
            entries[index++] = "";
            entries[index++] = "";
            entries[index++] = "";
            entries[index++] = issue.getSummary();
            entries[index] = issue.getDescription();
            writer.writeNext(entries);
        }
    }

    void printWithVersion(CSVWriter writer, Map<String, Map<Issue, ParityModel.SingleVersMetadata>> issues) {

        for (Map.Entry<String, Map<Issue, ParityModel.SingleVersMetadata>> kv : issues.entrySet()) {
            for (Map.Entry<Issue, ParityModel.SingleVersMetadata> kv2 : kv.getValue().entrySet()) {
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
                entries[index++] = kv2.getValue().firstOpenRaw();
                entries[index++] = kv2.getValue().firstOracleRaw();
                entries[index++] = kv2.getValue().interestTags();
                entries[index++] = kv2.getValue().backportRequested() ? "bp" : "";
                entries[index++] = p.getSummary();
                entries[index] = p.getDescription();
                writer.writeNext(entries);
            }
            for (Map.Entry<Issue, ParityModel.SingleVersMetadata> kv2 : kv.getValue().entrySet()) {
                String firstOpenRaw = kv2.getValue().firstOpenRaw() != null ? kv2.getValue().firstOpenRaw() : "";
                String firstOracle = kv2.getValue().firstOracleRaw() != null ? kv2.getValue().firstOracleRaw() : "";
                Main.debug.format("%s,%20s,%20s,%s\n", kv2.getKey(), firstOpenRaw, firstOracle, kv2.getValue());
            }
            Main.debug.println();
        }
    }
}
