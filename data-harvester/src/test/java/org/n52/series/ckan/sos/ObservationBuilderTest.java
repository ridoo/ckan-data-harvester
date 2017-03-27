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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.sos.ObservationBuilder.SingleObservationValueBuilder;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.QuantityValue;

public class ObservationBuilderTest {

    @Test
    public void when_floatTypedValue_then_createQuantityObservation() {
        Phenomenon phenomenon = new Phenomenon("temperature", "Temperature", 0, "°K");
        ObservationBuilder builder = ObservationBuilder.create(phenomenon, new ResourceKey());
        SingleObservationValueBuilder valueBuilder = builder.new SingleObservationValueBuilder();
        
        ResourceField field = new FieldBuilder()
                .createViaTemplate("{"
                        + " \"field_id\" : \"value\", "
                        + " \"short_name\" : \"value\", "
                        + " \"long_name\" : \"value\","
                        + " \"phenomenon\" : \"temperature\","
                        + " \"uom\" : \"°K\","
                        + " \"field_type\" : \"Float\" }");
        SingleObservationValue<Double> value = valueBuilder.createQuantityValue(field, "25.0");
        
        Assert.assertNotNull(value);
        Assert.assertThat(value.getValue(), CoreMatchers.instanceOf(QuantityValue.class));
        Assert.assertThat(value.getValue().getValue(), CoreMatchers.is(25.0));
        Assert.assertThat(value.getValue().getUnit(), CoreMatchers.is("°K"));
    }

}
