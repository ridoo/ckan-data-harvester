/**
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.series.ckan.da;

import java.util.Set;

public class MappingConfig {

    private Set<String> resultTime;
    private Set<String> longitude;
    private Set<String> latitude;
    private Set<String> crs;
    private Set<String> altitude;
    private Set<String> stationId;
    private Set<String> stationName;
    private Set<String> location;
    private Set<String> validTimeStart;
    private Set<String> validTimeEnd;

    public Set<String> getResultTime() {
        return resultTime;
    }

    public void setResultTime(Set<String> resultTime) {
        this.resultTime = resultTime;
    }

    public boolean isSetResultTime() {
        return getResultTime() != null && !getResultTime().isEmpty();
    }

    public Set<String> getLongitude() {
        return longitude;
    }

    public void setLongitude(Set<String> longitude) {
        this.longitude = longitude;
    }

    public boolean isSetLongitude() {
        return getLongitude() != null && !getLongitude().isEmpty();
    }

    public Set<String> getLatitude() {
        return latitude;
    }

    public void setLatitude(Set<String> latitude) {
        this.latitude = latitude;
    }

    public boolean isSetLatitude() {
        return getLatitude() != null && !getLatitude().isEmpty();
    }

    public Set<String> getCrs() {
        return crs;
    }

    public void setCrs(Set<String> crs) {
        this.crs = crs;
    }

    public boolean isSetCrs() {
        return getCrs() != null && !getCrs().isEmpty();
    }

    public Set<String> getAltitude() {
        return altitude;
    }

    public void setAltitude(Set<String> altitude) {
        this.altitude = altitude;
    }

    public boolean isSetAltitude() {
        return getAltitude() != null && !getAltitude().isEmpty();
    }

    public Set<String> getStationId() {
        return stationId;
    }

    public void setStationId(Set<String> stationId) {
        this.stationId = stationId;
    }

    public boolean isSetStationId() {
        return getStationId() != null && !getStationId().isEmpty();
    }

    public Set<String> getStationName() {
        return stationName;
    }

    public void setStationName(Set<String> stationName) {
        this.stationName = stationName;
    }

    public boolean isSetStationName() {
        return getStationName() != null && !getStationName().isEmpty();
    }

    public Set<String> getLocation() {
        return location;
    }

    public void setLocation(Set<String> location) {
        this.location = location;
    }

    public boolean isSetLocation() {
        return getLocation() != null && !getLocation().isEmpty();
    }

    public Set<String> getValidTimeStart() {
        return validTimeStart;
    }

    public void setValidTimeStart(Set<String> validTimeStart) {
        this.validTimeStart = validTimeStart;
    }
    
    public boolean isSetValidTimeStart() {
        return getValidTimeStart() != null && !getValidTimeStart().isEmpty();
    }

    public Set<String> getValidTimeEnd() {
        return validTimeEnd;
    }

    public void setValidTimeEnd(Set<String> validTimeEnd) {
        this.validTimeEnd = validTimeEnd;
    }
    
    public boolean isSetValidTimeEnd() {
        return getValidTimeEnd() != null && !getValidTimeEnd().isEmpty();
    }

}
