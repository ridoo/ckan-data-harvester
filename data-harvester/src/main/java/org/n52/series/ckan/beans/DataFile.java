/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package org.n52.series.ckan.beans;

import java.io.File;
import java.nio.charset.Charset;

import org.joda.time.DateTime;
import org.n52.series.ckan.da.CkanConstants;

import eu.trentorise.opendata.jackan.model.CkanResource;

public class DataFile {

    private final Charset encoding;

    private final CkanResource resource;

    private final String format;

    private final File file;

    public DataFile() {
        this(new CkanResource(), "", null);
    }

    public DataFile(CkanResource resource, String format, File file) {
        this(resource, format, file, null);
    }

    public DataFile(CkanResource resource, String format, File file, String encoding) {
        this.encoding = encoding == null
                ? CkanConstants.DEFAULT_CHARSET
                : Charset.forName(encoding);
        this.resource = resource == null
                ? new CkanResource()
                : resource;
        this.format = format;
        this.file = file;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public CkanResource getResource() {
        return resource;
    }

    public String getFormat() {
        return format;
    }

    public File getFile() {
        return file;
    }

    public DateTime getLastModified() {
        return DateTime.parse(resource.getLastModified());
    }

    public boolean isNewerThan(CkanResource ckanResource) {
        if (ckanResource == null) {
            return false;
        }

        String otherId = ckanResource.getId();
        String thisId = this.resource.getId();
        String otherLastModified = ckanResource.getLastModified();
        String thisLastModified = this.resource.getLastModified();
        if (thisId == null || otherLastModified == null) {
            return false;
        }

        DateTime probablyNewer = DateTime.parse(otherLastModified);
        DateTime current = DateTime.parse(thisLastModified);
        return thisId.equals(otherId)
                ? current.isAfter(probablyNewer)
                : false;

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String filePath = file != null
                ? file.getAbsolutePath()
                : "null";
        sb.append("resourceId: ")
          .append(resource.getId())
          .append(", ")
          .append("DataFile [file: ")
          .append(filePath)
          .append(", ")
          .append(" encoding: ")
          .append(encoding.toString());
        return sb.toString();
    }

}
