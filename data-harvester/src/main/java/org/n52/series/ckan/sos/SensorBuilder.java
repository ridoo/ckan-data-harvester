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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.n52.sos.encode.SensorMLEncoderv101;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SensorML20Constants;
import org.n52.sos.ogc.sensorML.SmlContact;
import org.n52.sos.ogc.sensorML.SmlContactList;
import org.n52.sos.ogc.sensorML.SmlResponsibleParty;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.ogc.sensorML.elements.SmlCapability;
import org.n52.sos.ogc.sensorML.elements.SmlClassifier;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sensorML.elements.SmlIo;
import org.n52.sos.ogc.sos.SosOffering;
import org.n52.sos.ogc.swe.SweAbstractDataComponent;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.SweSimpleDataRecord;
import org.n52.sos.ogc.swe.simpleType.SweBoolean;
import org.n52.sos.ogc.swe.simpleType.SweObservableProperty;
import org.n52.sos.ogc.swe.simpleType.SweText;
import org.n52.sos.request.InsertSensorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;
import eu.trentorise.opendata.jackan.model.CkanTag;

public class SensorBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SensorBuilder.class);

    private final List<Phenomenon> phenomenona;

    private AbstractFeature feature;

    private Procedure procedure;

    private CkanDataset dataset;

    private Boolean insitu = Boolean.TRUE;

    private Boolean mobile = Boolean.FALSE;

    public static SensorBuilder create() {
        return new SensorBuilder();
    }

    private SensorBuilder() {
        this.phenomenona = new ArrayList<>();
    }

    public SensorBuilder copy() {
        SensorBuilder copy = new SensorBuilder();
        copy.phenomenona.addAll(phenomenona);
        copy.feature = feature;
        copy.procedure = procedure;
        copy.dataset = dataset;
        copy.insitu = insitu;
        copy.mobile = mobile;
        return copy;
    }

    public SensorBuilder addPhenomenon(Phenomenon phenomenon) {
        this.phenomenona.add(phenomenon);
        return this;
    }

    public SensorBuilder withFeature(AbstractFeature feature) {
        this.feature = feature;
        return this;
    }

    public SensorBuilder withProcedure(Procedure procedure) {
        this.procedure = procedure;
        return this;
    }

    public SensorBuilder withDataset(CkanDataset dataset) {
        this.dataset = dataset;
        return this;
    }

    public SensorBuilder setMobile(boolean mobile) {
        this.mobile = mobile;
        return this;
    }

    public SensorBuilder setInsitu(boolean insitu) {
        this.insitu = insitu;
        return this;
    }

    public AbstractFeature getFeature() {
        return feature;
    }

    public Procedure getProcedure() {
        if (procedure != null) {
            return procedure;
        }
        String procedureId = createProcedureId();
        String longName = createProcedureLongName();
        procedure = new Procedure(procedureId, longName);
        return procedure;
    }

    public String getProcedureId() {
        return createProcedureId();
    }

    private String createProcedureId() {
        if (procedure != null) {
            return procedure.getId();
        }

        // TODO procedure is dataset

        StringBuilder sb = new StringBuilder();
        sb.append(dataset.getName()).append("_");
        sb = feature.isSetName()
                ?  sb.append(feature.getFirstName().getValue())
                : sb.append(feature.getIdentifier());
        return sb.toString();
    }

    private List<String> phenomenaToIdList() {
        List<String> ids = new ArrayList<>();
        for (Phenomenon phenomenon : phenomenona) {
            ids.add(phenomenon.getId());
        }
        return ids;
    }

    public InsertSensorRequest build() {
        if (feature == null) {
            throw new NullPointerException("feature cannot be null!");
        }
        if (phenomenona.isEmpty()) {
            throw new NullPointerException("no phenomenona!");
        }
        final InsertSensorRequest insertSensorRequest = new InsertSensorRequest();
        insertSensorRequest.setObservableProperty(phenomenaToIdList());
        insertSensorRequest.setProcedureDescriptionFormat("http://www.opengis.net/sensorML/1.0.1");

        final org.n52.sos.ogc.sensorML.System system = new org.n52.sos.ogc.sensorML.System();
        if (dataset != null) {
            system.setDescription(dataset.getNotes());
        }

        final String procedureId = getProcedure().getId();
        final SosOffering sosOffering = new SosOffering(procedureId);
        system.setInputs(createInputs())
                .setOutputs(createOutputs())
                .setKeywords(createKeywordList())
                .setIdentifications(createIdentificationList())
                .setClassifications(createClassificationList())
                .addCapabilities(createCapabilities(sosOffering))
                // .addContact(createContact(schemaDescription.getDataset())) // TODO
                // ... // TODO
                .setValidTime(createValidTimePeriod()).setIdentifier(procedureId);

        SensorML sml = new SensorML();
        sml.addMember(system);
        system.setSensorDescriptionXmlString(encodeToXml(sml));

        insertSensorRequest.setAssignedOfferings(Collections.singletonList(sosOffering));
        insertSensorRequest.setAssignedProcedureIdentifier(procedureId);
        insertSensorRequest.setProcedureDescription(sml);
        return insertSensorRequest;
    }

    private static String encodeToXml(final SensorML sml) {
        try {
            return new SensorMLEncoderv101().encode(sml).xmlText();
        }
        catch (OwsExceptionReport ex) {
            LOGGER.error("Could not encode SML to valid XML.", ex);
            return ""; // TODO empty but valid sml
        }
    }

    private List<SmlIo< ? >> createInputs() {
        List<SmlIo<?>> ios = new ArrayList<>();
        for (Phenomenon phenomenon: phenomenona) {
            SweAbstractDataComponent observableProperty = new SweObservableProperty()
                    .setDefinition(phenomenon.getId());
            ios.add(new SmlIo<>(observableProperty).setIoName(phenomenon.getId()));
        }
        return ios;
    }

    private List<SmlIo< ? >> createOutputs() {
        return createInputs();
    }

    private List<String> createKeywordList() {
        List<String> keywords = new ArrayList<>();
        keywords.add("CKAN data");
        if (feature.isSetName()) {
            keywords.add(feature.getFirstName().getValue());
        }
        for (Phenomenon phenomenon: phenomenona) {
            keywords.add(phenomenon.getLabel());
            keywords.add(phenomenon.getId());
        }
        addDatasetTags(keywords);
        return keywords;
    }

    private void addDatasetTags(List<String> keywords) {
        if (dataset != null) {
            for (CkanTag tag : dataset.getTags()) {
                final String displayName = tag.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    keywords.add(displayName);
                }
            }
        }
    }

    private List<SmlIdentifier> createIdentificationList() {
        return getProcedure().createIdentifierList();
    }

    private String createProcedureLongName() {
        if (procedure != null) {
            return procedure.getLongName();
        }
        String datasetname = dataset.getName();
        StringBuilder procedureName = new StringBuilder();
        procedureName.append(datasetname).append("@");
        if (feature.isSetName()) {
            procedureName.append(feature.getFirstName().getValue());
        }
        else {
            procedureName.append(feature.getIdentifier());
        }
        return procedureName.toString();
    }

    private List<SmlClassifier> createClassificationList() {
        List<SmlClassifier> classifiers = new ArrayList<>();
        for (Phenomenon phenomenon: phenomenona) {
            classifiers.add(new SmlClassifier(
                    "phenomenon",
                    "urn:ogc:def:classifier:OGC:1.0:phenomenon",
                    null,
                    phenomenon.getId()));
        }
        return classifiers;
    }

    private TimePeriod createValidTimePeriod() {
        return new TimePeriod(new Date(), null);
    }

    private List<SmlCapabilities> createCapabilities(SosOffering offering) {
        List<SmlCapabilities> capabilities = new ArrayList<>();
        capabilities.add(createOfferingCapabilities(offering));
        capabilities.add(createMetadataCapabilities());
        // capabilities.add(createBboxCapabilities(feature)); // TODO
        return capabilities;
    }

    private SmlCapabilities createOfferingCapabilities(SosOffering offering) {

        // TODO offering is dataset

        SmlCapabilities offeringCapabilities = new SmlCapabilities("offerings");
        offering.setIdentifier("Offering_" + getProcedureId());
        SweField field = createTextField(
                "field_0",
                SensorML20Constants.OFFERING_FIELD_DEFINITION,
                offering.getIdentifier());
        final SweSimpleDataRecord record = new SweSimpleDataRecord().addField(field);
        return offeringCapabilities.setDataRecord(record);
    }

    private SmlCapabilities createMetadataCapabilities() {
        SmlCapabilities capabilities = new SmlCapabilities("metadata");
        capabilities.addCapability(new SmlCapability("insitu", createBool("insitu", this.insitu)));
        capabilities.addCapability(new SmlCapability("mobile", createBool("mobile", this.mobile)));
        return capabilities;
    }

    private SweBoolean createBool(String definition, Boolean bool) {
        SweBoolean sweBool = new SweBoolean();
        sweBool.setDefinition(definition);
        sweBool.setValue(bool);
        return sweBool;
    }

    private SweField createTextField(String name, String definition, String value) {
        return new SweField(name, new SweText().setValue(value).setDefinition(definition));
    }

    private SmlCapabilities createBboxCapabilities() {
        SmlCapabilities offeringCapabilities = new SmlCapabilities("observedBBOX");

        // TODO

        return offeringCapabilities;
    }

    private SmlContact createContact(CkanDataset dataset) {
        CkanOrganization organisation = dataset.getOrganization();
        SmlContactList contactList = new SmlContactList();
        final SmlResponsibleParty responsibleParty = new SmlResponsibleParty();
        responsibleParty.setOrganizationName(organisation.getTitle());

        // TODO

        contactList.addMember(responsibleParty);
        return contactList;
    }

}
