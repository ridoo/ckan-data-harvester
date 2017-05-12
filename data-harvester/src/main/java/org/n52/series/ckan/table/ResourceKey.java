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
package org.n52.series.ckan.table;

import java.util.Objects;

import org.n52.series.ckan.beans.ResourceMember;

public class ResourceKey {

    private final String keyId;

    private final ResourceMember member;

    public ResourceKey() {
        this.keyId = "";
        this.member = null;
    }

    public ResourceKey(String keyId, ResourceMember member) {
        this.member = member;
        this.keyId = member.getId() + "_" + keyId;
    }

    public String getKeyId() {
        return keyId;
    }

    public ResourceMember getMember() {
        return member;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keyId, 5);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceKey other = (ResourceKey) obj;
        if (!Objects.equals(this.keyId, other.keyId)) {
            return false;
        }
        if (!Objects.equals(this.member, other.member)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ResourceKey{" + "keyId=" + keyId + ", member=" + member + '}';
    }

}
