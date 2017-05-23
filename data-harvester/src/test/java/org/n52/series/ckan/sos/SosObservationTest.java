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
package org.n52.series.ckan.sos;

import static org.hamcrest.CoreMatchers.is;
import static org.n52.series.ckan.da.CkanConstants.DEFAULT_CHARSET;

import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.TextValue;

public class SosObservationTest {

    @Test
    public void when_equalConstellationAndPhenomenonTime_then_observationsAreEqual() {
        DateTime now = DateTime.now();
        SosObservation first = createSosObservation("id", now);
        SosObservation second = createSosObservation("id", now);
        MatcherAssert.assertThat(first, is(second));
    }
    
    @Test
    public void when_equalConstellationAndPhenomenonTime_then_hashCodesAreEqual() {
        DateTime now = DateTime.now();
        SosObservation first = createSosObservation("id", now);
        SosObservation second = createSosObservation("id", now);
        MatcherAssert.assertThat(first.hashCode(), is(second.hashCode()));
    }

    private SosObservation createSosObservation(String id, DateTime phenomenonTime) {
        OmObservation omObservation = new OmObservation();
        omObservation.setIdentifier(id);
        
//        DateTime yesterday = now.minus(Duration.parse("PT1D"));
//        omObservation.setValidTime(new TimePeriod(yesterday, now));

        TimeInstant time = new TimeInstant(phenomenonTime);
        SingleObservationValue<String> result = new SingleObservationValue<>(new TextValue("foo"));
        result.setPhenomenonTime(time);
        omObservation.setValue(result);
        omObservation.setResultTime(time);

        String observationType = "my_observation_type";
        OmObservationConstellation constellation = new OmObservationConstellation();
        OmObservableProperty observableProperty = createObservableProperty("1", "phe_1", "l");
        constellation.setObservableProperty(observableProperty);
        constellation.setObservationType(observationType);
        
        omObservation.setObservationConstellation(createConstellation(observableProperty, observationType));
        omObservation.setDefaultElementEncoding(DEFAULT_CHARSET.toString());
        SosObservation o = new SosObservation(omObservation, observationType);
        o.setPhenomenonTime(time);
        return o;
    }
    
    private OmObservableProperty createObservableProperty(String id, String label, String uom) {
        OmObservableProperty observableProperty = new OmObservableProperty(id);
        observableProperty.setHumanReadableIdentifier(label);
        observableProperty.setUnit(uom);
        return observableProperty;
    }

    private OmObservationConstellation createConstellation(OmObservableProperty observableProperty, String observationType) {
        OmObservationConstellation constellation = new OmObservationConstellation();
        constellation.setObservableProperty(observableProperty);
        constellation.setObservationType(observationType);
        return constellation;
    }
}
