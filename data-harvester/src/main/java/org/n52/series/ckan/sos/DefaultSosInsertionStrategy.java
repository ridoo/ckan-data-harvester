/*
 * Copyright (C) 2015-2016 52°North Initiative for Geospatial Open Source
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.beans.SchemaDescriptor;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.table.ResourceTable;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.sos.ds.hibernate.InsertObservationDAO;
import org.n52.sos.ds.hibernate.InsertSensorDAO;
import org.n52.sos.encode.SensorMLEncoderv101;
import org.n52.sos.exception.ows.concrete.DateTimeParseException;
import org.n52.sos.exception.ows.concrete.InvalidSridException;
import org.n52.sos.ext.deleteobservation.DeleteObservationConstants;
import org.n52.sos.ext.deleteobservation.DeleteObservationDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationRequest;
import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.Time.TimeIndeterminateValue;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.AbstractPhenomenon;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SensorML20Constants;
import org.n52.sos.ogc.sensorML.SmlContact;
import org.n52.sos.ogc.sensorML.SmlContactList;
import org.n52.sos.ogc.sensorML.SmlResponsibleParty;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.ogc.sensorML.elements.SmlClassifier;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sensorML.elements.SmlIo;
import org.n52.sos.ogc.sos.SosInsertionMetadata;
import org.n52.sos.ogc.sos.SosOffering;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.SweSimpleDataRecord;
import org.n52.sos.ogc.swe.simpleType.SweObservableProperty;
import org.n52.sos.ogc.swe.simpleType.SweQuantity;
import org.n52.sos.ogc.swe.simpleType.SweText;
import org.n52.sos.request.InsertObservationRequest;
import org.n52.sos.request.InsertSensorRequest;
import org.n52.sos.service.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.Geometry;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;
import eu.trentorise.opendata.jackan.model.CkanResource;
import eu.trentorise.opendata.jackan.model.CkanTag;
import org.n52.sos.ogc.om.values.GeometryValue;

class DefaultSosInsertionStrategy implements SosInsertionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSosInsertionStrategy.class);

    private final InsertSensorDAO insertSensorDao;

    private final InsertObservationDAO insertObservationDao;

    private final DeleteObservationDAO deleteObservationDao;

    private final CkanSosReferenceCache ckanSosReferencingCache;

    DefaultSosInsertionStrategy() {
        this(null);
    }

    DefaultSosInsertionStrategy(CkanSosReferenceCache ckanSosReferenceCache) {
        this(new InsertSensorDAO(), new InsertObservationDAO(), new DeleteObservationDAO(), ckanSosReferenceCache);
    }

    DefaultSosInsertionStrategy(InsertSensorDAO insertSensorDao,
                                InsertObservationDAO insertObservationDao,
                                DeleteObservationDAO deleteObservationDao,
                                CkanSosReferenceCache ckanSosReferencingCache) {
        this.insertSensorDao = insertSensorDao;
        this.insertObservationDao = insertObservationDao;
        this.deleteObservationDao = deleteObservationDao;
        this.ckanSosReferencingCache = ckanSosReferencingCache;
    }

    @Override
    public void insertOrUpdate(DataCollection dataCollection) {
        try {
            DataTable fullTable = loadData(dataCollection);
            if (insertOrUpdateData(fullTable, dataCollection)) {
                // Trigger SOS Capabilities cache reloading after insertion
                Configurator.getInstance().getCacheController().update();
            }
        }
        catch (OwsExceptionReport e) {
            LOGGER.warn("Error while reloading SOS Capabilities cache", e);
        }
    }

    private DataTable loadData(DataCollection dataCollection) {
        CkanDataset dataset = dataCollection.getDataset();
        LOGGER.debug("insertOrUpdate dataset '{}'", dataset.getName());
        DataTable fullTable = new ResourceTable();

        // TODO write test for it
        // TODO if dataset is newer than in cache -> set flag to re-insert whole datacollection

        Map<String, List<ResourceMember>> resourceMembersByType = dataCollection.getResourceMembersByType();
        for (List<ResourceMember> membersWithCommonResourceTypes : resourceMembersByType.values()) {
            DataTable dataTable = new ResourceTable();
            for (ResourceMember member : membersWithCommonResourceTypes) {

                // TODO write test for it

                DataFile dataFile = dataCollection.getDataFile(member);
                CkanResource resource = dataFile.getResource();
                if (isUpdateNeeded(resource, dataFile)) {
                    ResourceTable singleDatatable = new ResourceTable(dataCollection.getDataEntry(member));
                    singleDatatable.readIntoMemory();
                    LOGGER.debug("Extend table with: '{}'", singleDatatable);
                    dataTable = dataTable.extendWith(singleDatatable);
                }
            }
            String resourceType = membersWithCommonResourceTypes.get(0).getResourceType();
            LOGGER.debug("Fully extended table for resource '{}': '{}'", resourceType, dataTable);
            fullTable = fullTable.innerJoin(dataTable);
        }
        LOGGER.debug("Fully joined table: '{}'", fullTable);
        return fullTable;
    }

    private static class DataInsertion {

        // XXX separate to own type to encapsulate

        private final InsertSensorRequest request;

        private final AbstractFeature feature;

        private final List<OmObservation> observations;

        private CkanSosObservationReference reference;

        DataInsertion(InsertSensorRequest request, AbstractFeature feature) {
            this.request = request;
            this.feature = feature;
            this.observations = new ArrayList<>();
        }

        InsertObservationRequest createInsertObservationRequest() throws OwsExceptionReport {
            InsertObservationRequest insertObservationRequest = new InsertObservationRequest();
            insertObservationRequest.setOfferings(getOfferingIds());
            insertObservationRequest.setObservation(observations);
            return insertObservationRequest;
        }

        boolean hasObservationsReference() {
            return reference != null;
        }

        CkanSosObservationReference getObservationsReference() {
            if (hasObservationsReference()) {
                for (OmObservation observation : observations) {
                    reference.addObservationReference(observation);
                }
            }
            return reference;
        }

        List<String> getOfferingIds() {
            List<String> ids = new ArrayList<>();
            for (SosOffering offering : request.getAssignedOfferings()) {
                ids.add(offering.getIdentifier());
            }
            return ids;
        }

        @Override
        public String toString() {
            String featureIdentifier = "Feature: '" + feature.getIdentifier() + "'";
            String observationCount = ", Observations: #" + observations.size();
            return getClass().getSimpleName() + " [ " + featureIdentifier + ", " + observationCount + "]";
        }

        public boolean hasObservations() {
            return observations != null && !observations.isEmpty();
        }
    }

    boolean insertOrUpdateData(DataTable dataTable, DataCollection dataCollection) {
        boolean dataInserted = false;
        SchemaDescriptor schemaDescription = dataCollection.getSchemaDescriptor().getSchemaDescription();
        final List<Phenomenon> phenomena = parseObservableProperties(dataTable);

        LOGGER.debug("Start insertion ...");
        Map<String, DataInsertion> dataInsertions = new HashMap<>();
        for (Entry<ResourceKey, Map<ResourceField, String>> rowEntry : dataTable.getTable().rowMap().entrySet()) {

            // TODO how and what to create in which order depends on the actual strategy chosen

            FeatureBuilder foiBuilder = new FeatureBuilder(dataCollection.getDataset());
            AbstractFeature feature = foiBuilder.createFeature(rowEntry.getValue());
            for (Phenomenon phenomenon : phenomena) {
                String procedureId = createProcedureId(feature, phenomenon);
                if ( !dataInsertions.containsKey(procedureId)) {
                    LOGGER.debug("InsertSensor with: procedure '{}' with phenomenon '{}' (unit '{}')",
                                 procedureId,
                                 phenomenon.getLabel(),
                                 phenomenon.getUom());
                    InsertSensorRequest insertSensorRequest = prepareSmlInsertSensorRequest(feature,
                                                                                            phenomenon,
                                                                                            schemaDescription);
                    insertSensorRequest.setObservableProperty(phenomenaToIdList(phenomena));
                    insertSensorRequest.setProcedureDescriptionFormat("http://www.opengis.net/sensorML/1.0.1");
                    insertSensorRequest.setMetadata(createInsertSensorMetadata());

                    // TODO check mobile/insitu/stationary/remote

                    DataInsertion dataInsertion = new DataInsertion(insertSensorRequest, feature);
                    dataInsertions.put(procedureId, dataInsertion);

                    if (ckanSosReferencingCache != null) {
                        ResourceMember member = rowEntry.getKey().getMember();
                        DataFile dataFile = dataCollection.getDataFile(member);
                        CkanResource resource = dataFile.getResource();
                        dataInsertion.reference = CkanSosObservationReference.create(resource);
                    }

                }

                DataInsertion dataInsertion = dataInsertions.get(procedureId);
                InsertSensorRequest insertSensorRequest = dataInsertion.request;
                List<String> offerings = dataInsertion.getOfferingIds();
                OmObservationConstellation constellation = new OmObservationConstellation();
                constellation.setObservableProperty(createPhenomenon(phenomenon));
                constellation.setFeatureOfInterest(dataInsertion.feature);
                constellation.setOfferings(offerings);
                constellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
                constellation.setProcedure(insertSensorRequest.getProcedureDescription());
                final OmObservation observation = createObservation(rowEntry, constellation, phenomenon);
                if (observation != null) {
                    // TODO refactor getting ckanResource to cache!?
                    dataInsertion.observations.add(observation);
                }
            }
        }

        LOGGER.debug("#{} data insertions: {}", dataInsertions.size(), dataInsertions);
        for (DataInsertion dataInsertion : dataInsertions.values()) {
            try {
                long start = System.currentTimeMillis();
                insertSensorDao.insertSensor(dataInsertion.request);
                if (dataInsertion.hasObservations()) {
                    insertObservationDao.insertObservation(dataInsertion.createInsertObservationRequest());
                }
                LOGGER.debug("Insertion completed in {}s.", (System.currentTimeMillis() - start) / 1000d);
                dataInserted = true;

                if (ckanSosReferencingCache != null && dataInsertion.hasObservationsReference()) {
                    ckanSosReferencingCache.addOrUpdate(dataInsertion.getObservationsReference());
                }
            }
            catch (Exception e) {
                LOGGER.error("Could not insert: {}", dataInsertion, e);
            }
        }

        return dataInserted;
    }

    private boolean isUpdateNeeded(CkanResource resource, DataFile dataFile) {
        if (ckanSosReferencingCache == null) {
            return true;
        }

        try {
            if ( !ckanSosReferencingCache.exists(resource)) {
                CkanSosObservationReference reference = new CkanSosObservationReference(resource);
                ckanSosReferencingCache.addOrUpdate(reference);
                return true;
            }
            CkanSosObservationReference reference = ckanSosReferencingCache.getReference(resource);
            final CkanResource ckanResource = reference.getResource().getCkanResource();

            if ( !dataFile.isNewerThan(ckanResource)) {
                LOGGER.debug("Resource with id '{}' has no data update since {}.",
                             ckanResource.getId(),
                             ckanResource.getLastModified());
                return false;
            }

            long count = 0;
            LOGGER.debug("start deleting existing observation data before updating data.");
            for (String observationIdentifier : reference.getObservationIdentifiers()) {
                try {
                    String namespace = DeleteObservationConstants.NS_SOSDO_1_0;
                    DeleteObservationRequest doRequest = new DeleteObservationRequest(namespace);
                    doRequest.addObservationIdentifier(observationIdentifier);
                    deleteObservationDao.deleteObservation(doRequest);
                    count++;
                }
                catch (OwsExceptionReport e) {
                    LOGGER.error("could not delete observation with id '{}'", observationIdentifier, e);
                }
            }
            LOGGER.debug("deleted #{} observations.", count);
        }
        catch (IOException e) {
            LOGGER.error("Serialization error:  resource with id '{}'", resource.getId(), e);
        }
        return true;
    }

    private List<String> phenomenaToIdList(List<Phenomenon> phenomena) {
        List<String> ids = new ArrayList<>();
        for (Phenomenon phenomenon : phenomena) {
            ids.add(phenomenon.getId());
        }
        return ids;
    }

    List<Phenomenon> parseObservableProperties(DataTable dataTable) {

        // TODO evaluate separating observableProperty parsing to schemaDescription

        ResourceMember resourceMember = dataTable.getResourceMember();
        Set<Phenomenon> observableProperties = new HashSet<>();
        List<ResourceField> fields = resourceMember.getResourceFields();
        for (ResourceField field : fields) {
            if (field.hasProperty(CkanConstants.FieldPropertyName.PHENOMENON)) {
                // check for content of fieldId and longName, if not readable phenomenon name use
                // "phenomenon" field
                String phenomenonId;
                if ("value".equalsIgnoreCase(field.getFieldId())) {
                    phenomenonId = field.getOther(CkanConstants.FieldPropertyName.PHENOMENON);
                }
                else {
                    phenomenonId = field.getFieldId();
                }
                String phenomenonName;
                if ("value".equalsIgnoreCase(field.getLongName())) {
                    phenomenonName = field.getOther(CkanConstants.FieldPropertyName.PHENOMENON);
                }
                else {
                    phenomenonName = field.getFieldId();
                }
                observableProperties.add(new Phenomenon(phenomenonId,
                                                        phenomenonName,
                                                        field.getIndex(),
                                                        parseToUcum(field.getOther(CkanConstants.FieldPropertyName.UOM))));
            }
        }
        return new ArrayList<>(observableProperties);
    }

    private InsertSensorRequest prepareSmlInsertSensorRequest(AbstractFeature feature,
                                                              Phenomenon phenomenon,
                                                              SchemaDescriptor schemaDescription) {
        final InsertSensorRequest insertSensorRequest = new InsertSensorRequest();
        final org.n52.sos.ogc.sensorML.System system = new org.n52.sos.ogc.sensorML.System();
        system.setDescription(schemaDescription.getDataset().getNotes());

        final String procedureId = createProcedureId(feature, phenomenon);
        final SosOffering sosOffering = new SosOffering(procedureId);
        system.setInputs(Collections.<SmlIo< ? >> singletonList(createInput(phenomenon))).setOutputs(Collections.<SmlIo< ? >> singletonList(createOutput(phenomenon))).setKeywords(createKeywordList(feature,
                                                                                                                                                                                                     phenomenon,
                                                                                                                                                                                                     schemaDescription)).setIdentifications(createIdentificationList(feature,
                                                                                                                                                                                                                                                                     phenomenon)).setClassifications(createClassificationList(feature,
                                                                                                                                                                                                                                                                                                                              phenomenon)).addCapabilities(createCapabilities(feature,
                                                                                                                                                                                                                                                                                                                                                                              phenomenon,
                                                                                                                                                                                                                                                                                                                                                                              sosOffering))
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

    private SmlIo< ? > createInput(Phenomenon phenomeon) {
        return new SmlIo<>(new SweObservableProperty().setDefinition(phenomeon.getId())).setIoName(phenomeon.getId());
    }

    private SmlIo< ? > createOutput(Phenomenon phenomeon) {
        return new SmlIo<>(new SweQuantity().setUom(phenomeon.getUom()).setDefinition(phenomeon.getId())).setIoName(phenomeon.getId());
    }

    private List<String> createKeywordList(AbstractFeature feature,
                                           Phenomenon phenomenon,
                                           SchemaDescriptor schemaDescription) {
        List<String> keywords = new ArrayList<>();
        keywords.add("CKAN data");
        if (feature.isSetName()) {
            keywords.add(feature.getFirstName().getValue());
        }
        keywords.add(phenomenon.getLabel());
        keywords.add(phenomenon.getId());
        addDatasetTags(schemaDescription.getDataset(), keywords);
        return keywords;
    }

    private void addDatasetTags(CkanDataset dataset, List<String> keywords) {
        for (CkanTag tag : dataset.getTags()) {
            final String displayName = tag.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                keywords.add(displayName);
            }
        }
    }

    private List<SmlIdentifier> createIdentificationList(AbstractFeature feature, Phenomenon phenomenon) {
        List<SmlIdentifier> idents = new ArrayList<>();
        idents.add(new SmlIdentifier(
                                     OGCConstants.UNIQUE_ID,
                                     OGCConstants.URN_UNIQUE_IDENTIFIER,
                                     // TODO check feautre id vs name
                                     createProcedureId(feature, phenomenon)));
        idents.add(new SmlIdentifier(
                                     "longName",
                                     "urn:ogc:def:identifier:OGC:1.0:longName",
                                     createProcedureLongName(feature, phenomenon)));
        return idents;
    }

    private String createProcedureId(AbstractFeature feature, Phenomenon phenomenon) {

        // TODO procedure is dataset

        StringBuilder procedureId = new StringBuilder();
        procedureId.append(phenomenon.getLabel()).append("_");
        procedureId = feature.isSetName()
                ?  procedureId.append(feature.getFirstName().getValue())
                : procedureId.append(feature.getIdentifier());
        return procedureId.toString();
    }

    private String createProcedureLongName(AbstractFeature feature, Phenomenon phenomenon) {
        StringBuilder phenomenonName = new StringBuilder();
        phenomenonName.append(phenomenon.getLabel()).append("@");
        if (feature.isSetName()) {
            phenomenonName.append(feature.getFirstName().getValue());
        }
        else {
            phenomenonName.append(feature.getIdentifier());
        }
        return phenomenonName.toString();
    }

    private List<SmlClassifier> createClassificationList(AbstractFeature feature, Phenomenon phenomenon) {
        return Collections.singletonList(new SmlClassifier(
                                                           "phenomenon",
                                                           "urn:ogc:def:classifier:OGC:1.0:phenomenon",
                                                           null,
                                                           phenomenon.getId()));
    }

    private TimePeriod createValidTimePeriod() {
        return new TimePeriod(new Date(), null);
    }

    private List<SmlCapabilities> createCapabilities(AbstractFeature feature,
                                                     Phenomenon phenomenon,
                                                     SosOffering offering) {
        List<SmlCapabilities> capabilities = new ArrayList<>();
        capabilities.add(createFeatureCapabilities(feature));
        capabilities.add(createOfferingCapabilities(feature, phenomenon, offering));
        // capabilities.add(createBboxCapabilities(feature)); // TODO
        return capabilities;
    }

    private SmlCapabilities createFeatureCapabilities(AbstractFeature feature) {
        SmlCapabilities featuresCapabilities = new SmlCapabilities("featuresOfInterest");
        final SweSimpleDataRecord record = new SweSimpleDataRecord().addField(createTextField(
                                                                                              SensorML20Constants.FEATURE_OF_INTEREST_FIELD_NAME,
                                                                                              SensorML20Constants.FEATURE_OF_INTEREST_FIELD_DEFINITION,
                                                                                              feature.getIdentifier()));
        return featuresCapabilities.setDataRecord(record);
    }

    private SmlCapabilities createOfferingCapabilities(AbstractFeature feature,
                                                       Phenomenon phenomenon,
                                                       SosOffering offering) {

        // TODO offering is dataset

        SmlCapabilities offeringCapabilities = new SmlCapabilities("offerings");
        offering.setIdentifier("Offering_" + createProcedureId(feature, phenomenon));
        final SweSimpleDataRecord record = new SweSimpleDataRecord().addField(createTextField(
                                                                                              "field_0",
                                                                                              SensorML20Constants.OFFERING_FIELD_DEFINITION,
                                                                                              offering.getIdentifier()));
        return offeringCapabilities.setDataRecord(record);
    }

    private SweField createTextField(String name, String definition, String value) {
        return new SweField(name, new SweText().setValue(value).setDefinition(definition));
    }

    private SmlCapabilities createBboxCapabilities(AbstractFeature feature) {
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

    private SosInsertionMetadata createInsertSensorMetadata() {
        SosInsertionMetadata metadata = new SosInsertionMetadata();
        metadata.setFeatureOfInterestTypes(Collections.singleton(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_FEATURE));
        metadata.setObservationTypes(Collections.singleton(OmConstants.OBS_TYPE_MEASUREMENT));
        return metadata;
    }

    private OmObservation createObservation(Map.Entry<ResourceKey, Map<ResourceField, String>> rowEntry,
                                            OmObservationConstellation constellation,
                                            Phenomenon phenomenon) {
        SingleObservationValue< ? > value = null;
        Time time = null;
        TimeInstant validStart = null;
        TimeInstant validEnd = null;

        OmObservation omObservation = new OmObservation();
        omObservation.setObservationConstellation(constellation);
        omObservation.setDefaultElementEncoding(CkanConstants.DEFAULT_CHARSET.toString());
        final GeometryBuilder geometryBuilder = GeometryBuilder.create();
        for (Map.Entry<ResourceField, String> cells : rowEntry.getValue().entrySet()) {

            ResourceField field = cells.getKey();
            String resourceType = field.getQualifier().getResourceType();
            if ( !resourceType.equalsIgnoreCase(CkanConstants.ResourceType.OBSERVATIONS)) {
                continue;
            }

            if (field.getIndex() == phenomenon.getFieldIdx()) {
                String phenomenonId = constellation.getObservableProperty().getIdentifier();
                omObservation.setIdentifier(rowEntry.getKey().getKeyId() + "_" + phenomenonId);
                // TODO support NO_DATA
                if (field.isOfType(CkanConstants.DataType.DOUBLE)) {
                    value = createQuantityObservationValue(field, cells.getValue());
                }
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.RESULT_TIME)) {
                time = parseTimestamp(field, cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LOCATION)) {
                if (field.isOfType(Geometry.class)) {
                    geometryBuilder.withGeoJson(cells.getValue());
                }
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.CRS)) {
                geometryBuilder.withCrs(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LATITUDE)) {
                geometryBuilder.setLatitude(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LONGITUDE)) {
                geometryBuilder.setLongitude(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.ALTITUDE)) {
                geometryBuilder.setAltitude(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_START)) {
                validStart = parseTimestamp(field, cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_END)) {
                validEnd = parseTimestamp(field, cells.getValue());
            }
        }

        if (validStart != null || validEnd != null) {
            TimePeriod validTime;
            if (validStart != null && validEnd == null) {
                validTime = new TimePeriod(validStart, new TimeInstant(TimeIndeterminateValue.unknown));
            }
            else if (validStart == null && validEnd != null) {
                validTime = new TimePeriod(new TimeInstant(TimeIndeterminateValue.unknown), validEnd);
            }
            else {
                validTime = new TimePeriod(validStart, validEnd);
            }
            omObservation.setValidTime(validTime);
        }

        // TODO support NO_DATA
        if (time == null) {
            LOGGER.debug("ignore observation having no phenomenonTime.");
            return null;
        }
        if (value == null) {
            SingleObservationValue<Geometry> obsValue = new SingleObservationValue<>();
            obsValue.setValue(geometryBuilder.createGeometryValue());
            value = obsValue;
        } else {
            omObservation.addParameter(geometryBuilder.createNamedValue());
        }
        value.setPhenomenonTime(time);
        omObservation.setValue(value);
        return omObservation;
    }

    protected TimeInstant parseTimestamp(ResourceField field, String dateValue) {
        return !hasDateFormat(field)
            ? new TimeInstant(new Date(Long.parseLong(dateValue)))
            : parseDateValue(dateValue, parseDateFormat(field));
    }

    protected String parseDateFormat(ResourceField field) {
        if (hasDateFormat(field)) {
            String format = field.getOther(CkanConstants.FieldPropertyName.DATE_FORMAT);
            format = ( !format.endsWith("Z") && !format.endsWith("z"))
                ? format + "Z"
                : format;
            return format.replace("DD", "dd").replace("hh", "HH"); // XXX hack to fix wrong format
        }
        return null;
    }

    private boolean hasDateFormat(ResourceField field) {
        return field.hasProperty(CkanConstants.FieldPropertyName.DATE_FORMAT);
    }

    protected TimeInstant parseDateValue(String dateValue, String dateFormat) {
        try {
            TimeInstant timeInstant = new TimeInstant();
            if ( !hasOffsetInfo(dateValue)) {
                dateValue += "Z";
            }
            DateTime dateTime = parseIsoString2DateTime(dateValue, dateFormat);
            timeInstant.setValue(dateTime);
            return timeInstant;
        }
        catch (Exception ex) {
            if (ex instanceof DateTimeParseException) {
                LOGGER.error("Cannot parse date string {} with format {}", dateValue, dateFormat);
            }
            else {
                LOGGER.error("Cannot parse date string {} with format {}", dateValue, dateFormat, ex);
            }

            return null;
        }

    }

    /**
     * Parses a time String to a Joda Time DateTime object
     *
     * @param timeString
     *        Time String
     * @param format
     *        Format of the time string
     * @return DateTime object
     * @throws DateTimeParseException
     *         If an error occurs.
     */
    protected DateTime parseIsoString2DateTime(final String timeString, String format) throws DateTimeParseException {
        if (Strings.isNullOrEmpty(timeString)) {
            return null;
        }
        try {
            if ( !Strings.isNullOrEmpty(format)) {
                return DateTime.parse(timeString, DateTimeFormat.forPattern(format));
            }
            else if (timeString.contains("+") || Pattern.matches("-\\d", timeString) || timeString.contains("Z")) {
                return ISODateTimeFormat.dateOptionalTimeParser().withOffsetParsed().parseDateTime(timeString);
            }
            else {
                return ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC).parseDateTime(timeString);
            }
        }
        catch (final RuntimeException uoe) {
            throw new DateTimeParseException(timeString, uoe);
        }
    }

    private static boolean hasOffsetInfo(String dateValue) {
        return dateValue.endsWith("Z")
                || dateValue.contains("+")
                || Pattern.matches("-\\d", dateValue);
    }

    protected SingleObservationValue<Double> createQuantityObservationValue(ResourceField field, String value) {
        try {
            SingleObservationValue<Double> obsValue = new SingleObservationValue<>();
            if (field.isOfType(Integer.class)
                    || field.isOfType(Float.class)
                    || field.isOfType(Double.class)
                    || field.isOfType(String.class)) {
                QuantityValue quantityValue = new QuantityValue(Double.parseDouble(value));
                quantityValue.setUnit(parseToUcum(field.getOther(CkanConstants.FieldPropertyName.UOM)));
                obsValue.setValue(quantityValue);
                return obsValue;
            }
        }
        catch (Exception e) {
            LOGGER.error("could not parse value {}", value, e);
        }
        return null;
    }

    private String parseToUcum(String uom) {
        final String unit = parseFromAlias(uom);
        return unit != null
            ? unit
            : "";
    }

    protected String parseFromAlias(String uom) {
        try {
            // TODO non valid units from a mapping
            // return StandardUnitDB.instance().get(uom);
            return uom;
        }
        catch (Exception e) {
            LOGGER.error("Could not parse UOM '{}' to known UCUM symbol.", uom, e);
        }
        return null;
    }

    private AbstractPhenomenon createPhenomenon(Phenomenon phenomenon) {
        return new OmObservableProperty(phenomenon.getId());
    }

}
