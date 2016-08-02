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
import java.util.Set;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.n52.series.ckan.beans.CsvObservationsCollection;
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

    DefaultSosInsertionStrategy(InsertSensorDAO insertSensorDao, InsertObservationDAO insertObservationDao,
            DeleteObservationDAO deleteObservationDao, CkanSosReferenceCache ckanSosReferencingCache) {
        this.insertSensorDao = insertSensorDao;
        this.insertObservationDao = insertObservationDao;
        this.deleteObservationDao = deleteObservationDao;
        this.ckanSosReferencingCache = ckanSosReferencingCache;
    }

    @Override
    public void insertOrUpdate(CsvObservationsCollection csvObservationsCollection) {
        Map<ResourceMember, DataFile> platformDataCollections = csvObservationsCollection.getPlatformDataCollections();
        SchemaDescriptor schemaDescription = csvObservationsCollection.getSchemaDescriptor().getSchemaDescription();
        boolean dataInsertedUpdated = false;

        // TODO join all tables together, don't care what type the collection is
        // Think of the join index when joining two observation tables !! (if of same type, then do not join but just add rows?!)

        CkanDataset dataset = csvObservationsCollection.getDataset();
        LOGGER.debug("insertOrUpdate dataset '{}'", dataset.getName());
        for (Map.Entry<ResourceMember, DataFile> platformEntry : platformDataCollections.entrySet()) {
            ResourceTable platformTable = new ResourceTable(platformEntry.getKey(), platformEntry.getValue());
            platformTable.readIntoMemory();
            if (insertOrUpdateData(dataset.getOrganization().getName(), platformTable, schemaDescription, csvObservationsCollection)) {
                dataInsertedUpdated = true;
            }
        }
        // Trigger SOS Capabilities cache reloading after insertion
        try {
            if (dataInsertedUpdated) {
                Configurator.getInstance().getCacheController().update();
            }
        } catch (OwsExceptionReport e) {
            LOGGER.warn("Error while reloading SOS Capabilities cache", e);
        }
    }

    private static class SensorInsertion {
        private final InsertSensorRequest request;

        private final AbstractFeature feature;

        private final List<OmObservation> observations;

        SensorInsertion(InsertSensorRequest request, AbstractFeature feature) {
            this.request = request;
            this.feature = feature;
            this.observations = new ArrayList<>();
        }

        @Override
        public String toString() {
            String featureName = "Feature: '" + feature.getFirstName() + "'";
            String observationCount = "#" + observations.size();
            return getClass().getSimpleName() + " [ " + featureName + ", " + observationCount + "]";
        }
    }

    boolean insertOrUpdateData(String organizationName, ResourceTable platformTable, SchemaDescriptor schemaDescription,
            CsvObservationsCollection csvObservationsCollection) {
        boolean observationsInserted = false;
        final List<Phenomenon> phenomena = parseObservableProperties(platformTable, schemaDescription);
        Map<ResourceMember, DataFile> observationCollections =
                csvObservationsCollection.getObservationDataCollections();

        for (Map.Entry<ResourceMember, DataFile> observationEntry : observationCollections.entrySet()) {
            final DataFile dataFile = observationEntry.getValue();
            final CkanResource resource = dataFile.getResource();
            LOGGER.info("insert/update data for resource '{}' (data file: '{}')", resource.getId(), dataFile);

            if (ckanSosReferencingCache != null) {
                try {
                    if (!ckanSosReferencingCache.exists(resource)) {
                        CkanSosObservationReference reference = new CkanSosObservationReference(resource);
                        ckanSosReferencingCache.addOrUpdate(reference);
                    } else {
                        CkanSosObservationReference reference = ckanSosReferencingCache.getReference(resource);
                        final CkanResource ckanResource = reference.getResource().getCkanResource();
                        if (!dataFile.isNewerThan(ckanResource)) {
                            LOGGER.debug("Resource with id '{}' has no data update since {}.", ckanResource.getId(),
                                    ckanResource.getLastModified());
                            continue;
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
                            } catch (OwsExceptionReport e) {
                                LOGGER.error("could not delete observation with id '{}'", observationIdentifier, e);
                            }
                        }
                        LOGGER.debug("deleted #{} observations.", count);
                    }
                } catch (IOException e) {
                    LOGGER.error("Serialization error:  resource with id '{}'", resource.getId(), e);
                }
            }

            LOGGER.debug("start insertion ...");
            ResourceTable observationTable = new ResourceTable(observationEntry.getKey(), observationEntry.getValue());
            observationTable.readIntoMemory();

            DataTable joinedTable = platformTable.innerJoin(observationTable);
            Map<String, SensorInsertion> sensorInsertions = new HashMap<>();
            try {
                for (Map.Entry<ResourceKey, Map<ResourceField, String>> rowEntry : joinedTable.getTable().rowMap().entrySet()) {
                    AbstractFeature feature = createFeatureRelation(organizationName, rowEntry.getValue());
                    for (Phenomenon phenomenon : phenomena) {
                        String procedureId = createProcedureId(feature, phenomenon);
                        if ( !sensorInsertions.containsKey(procedureId)) {
                            LOGGER.debug("inserting procedure '{}' with phenomenon '{}' (unit '{}')",
                                procedureId, phenomenon.getLabel(), phenomenon.getUom());
                            InsertSensorRequest insertSensorRequest = prepareSmlInsertSensorRequest(feature, phenomenon, schemaDescription);
                            insertSensorRequest.setObservableProperty(phenomenaToIdList(phenomena));
                            insertSensorRequest.setProcedureDescriptionFormat("http://www.opengis.net/sensorML/1.0.1");
                            insertSensorRequest.setMetadata(createInsertSensorMetadata());
                            insertSensorDao.insertSensor(insertSensorRequest);
                            sensorInsertions.put(procedureId, new SensorInsertion(insertSensorRequest, feature));
                        }
                        SensorInsertion sensorInsertion = sensorInsertions.get(procedureId);
                        InsertSensorRequest insertSensorRequest = sensorInsertion.request;
                        List<String> offerings = offeringsToIdList(insertSensorRequest.getAssignedOfferings());
                        OmObservationConstellation constellation = new OmObservationConstellation();
                        constellation.setObservableProperty(createPhenomenon(phenomenon));
                        constellation.setFeatureOfInterest(sensorInsertion.feature);
                        constellation.setOfferings(offerings);
                        constellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
                        constellation.setProcedure(insertSensorRequest.getProcedureDescription());
                        final OmObservation observation = createObservation(rowEntry, constellation, phenomenon);
                        if (observation != null) {
                            sensorInsertion.observations.add(observation);
                        }
                    }
                }

                LOGGER.debug("Inserted #{} sensors: {}", sensorInsertions.size(), sensorInsertions);

                for (Map.Entry<String, SensorInsertion> sensorEntries : sensorInsertions.entrySet()) {
                    final SensorInsertion sensorEntry = sensorEntries.getValue();
                    final List<OmObservation> observations = sensorEntry.observations;
                    if (observations.size() > 0) {
                        if (ckanSosReferencingCache != null) {
                            CkanSosObservationReference reference = ckanSosReferencingCache.getReference(resource);
                            if (reference == null) {
                                LOGGER.warn("No CKAN-SOS reference entry for resource '{}'!", resource.getId());
                            } else {
                                addObservationsToCacheReference(reference, observations);
                                ckanSosReferencingCache.addOrUpdate(reference);
                            }
                        }
                        LOGGER.debug("insert #{} observations for sensor '{}'", observations.size(), sensorEntries.getKey());
                        long start = System.currentTimeMillis();
                        InsertObservationRequest insertObservationRequest = new InsertObservationRequest();
                        final List<SosOffering> assignedOfferings = sensorEntries.getValue().request.getAssignedOfferings();
                        insertObservationRequest.setOfferings(offeringsToIdList(assignedOfferings));
                        insertObservationRequest.setObservation(observations);
                        insertObservationDao.insertObservation(insertObservationRequest);
                        LOGGER.debug("Insertion of observations completed in {} s.",  (System.currentTimeMillis() - start) / 1000d);
                        observationsInserted = true;
                    } else {
                        LOGGER.debug("No observations to insert.");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Could not insert or update procedure/observation data.", e);
            }
        }
        return observationsInserted;
    }

    private List<String> phenomenaToIdList(List<Phenomenon> phenomena) {
        List<String> ids = new ArrayList<>();
        for (Phenomenon phenomenon : phenomena) {
            ids.add(phenomenon.getId());
        }
        return ids;
    }

    private List<String> offeringsToIdList(List<SosOffering> offerings) {
        List<String> ids = new ArrayList<>();
        for (SosOffering offering : offerings) {
            ids.add(offering.getIdentifier());
        }
        return ids;
    }

    AbstractFeature createFeatureRelation(String organizationName, Map<ResourceField, String> platform) {
        final GeometryBuilder geometryBuilder = GeometryBuilder.create();
        final SamplingFeature feature = new SamplingFeature(null);
        for (Map.Entry<ResourceField, String> fieldEntry : platform.entrySet()) {
            ResourceField field = fieldEntry.getKey();
            if (field.isField(CkanConstants.KnownFieldId.CRS)) {
                geometryBuilder.withCrs(fieldEntry.getValue());
            }
            if (field.isField(CkanConstants.KnownFieldId.LATITUDE)) {
                geometryBuilder.setLatitude(fieldEntry.getValue());
            }
            if (field.isField(CkanConstants.KnownFieldId.LONGITUDE)) {
                geometryBuilder.setLongitude(fieldEntry.getValue());
            }
            if (field.isField(CkanConstants.KnownFieldId.ALTITUDE)) {
                geometryBuilder.setAltitude(fieldEntry.getValue());
            }
            if (field.isField(CkanConstants.KnownFieldId.STATION_ID)) {
                String identifier = fieldEntry.getValue();
                if (field.isOfType(Integer.class)) {
                    identifier = Integer.toString(Integer.parseInt(identifier));
                }
                feature.setIdentifier(organizationName + "-" + identifier);
            }
            if (field.isField(CkanConstants.KnownFieldId.STATION_NAME)) {
                feature.addName(fieldEntry.getValue());
            }
            if (field.isField(CkanConstants.KnownFieldId.LOCATION)) {
                if (field.getFieldType().equalsIgnoreCase("JsonObject")) {
                    setFeatureGeometry(feature, geometryBuilder.fromGeoJson(fieldEntry.getValue()));
                }
            }
        }
        if (geometryBuilder.hasCoordinates()) {
            setFeatureGeometry(feature, geometryBuilder.getPoint());
        }
        feature.setFeatureType(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT);
        // return new SwesFeatureRelationship("samplingPoint", feature);
        return feature;
    }

    void setFeatureGeometry(SamplingFeature feature, Geometry point) {
        try {
            feature.setGeometry(point);
        } catch (InvalidSridException e) {
            LOGGER.error("could not set feature's geometry.", e);
        }
    }

    List<Phenomenon> parseObservableProperties(ResourceTable platformTable, SchemaDescriptor schemaDescription) {
        ResourceMember resourceMember = platformTable.getResourceMember();
        List<ResourceMember> members = schemaDescription.getMembers();
        Set<Phenomenon> observableProperties = new HashSet<>();
        for (ResourceMember member : members) {
            Set<ResourceField> joinableFields = resourceMember.getJoinableFields(member);
            if (!joinableFields.isEmpty()) {
                for (ResourceField joinableField : joinableFields) {
                    if (joinableField.hasProperty(CkanConstants.KnownFieldProperty.PHENOMENON)) {
                        // check for content of fieldId and longName, if not readable phenomenon name use "phenomenon" field
                        String phenomenonId;
                        if ("value".equalsIgnoreCase(joinableField.getFieldId())) {
                            phenomenonId = joinableField.getOther(CkanConstants.KnownFieldProperty.PHENOMENON);
                        } else {
                            phenomenonId = joinableField.getFieldId();
                        }
                        String phenomenonName;
                        if ("value".equalsIgnoreCase(joinableField.getLongName())) {
                            phenomenonName = joinableField.getOther(CkanConstants.KnownFieldProperty.PHENOMENON);
                        } else {
                            phenomenonName = joinableField.getFieldId();
                        }
                        observableProperties.add(new Phenomenon(phenomenonId,
                                phenomenonName,
                                joinableField.getIndex(),
                                parseToUcum(joinableField.getOther(CkanConstants.KnownFieldProperty.UOM))));
                    }
                }
            }
        }
        return new ArrayList<>(observableProperties);
    }


    private InsertSensorRequest prepareSmlInsertSensorRequest(AbstractFeature feature, Phenomenon phenomenon, SchemaDescriptor schemaDescription) {
        final InsertSensorRequest insertSensorRequest = new InsertSensorRequest();
        final org.n52.sos.ogc.sensorML.System system = new org.n52.sos.ogc.sensorML.System();
        system.setDescription(schemaDescription.getDataset().getNotes());

        final String procedureId = createProcedureId(feature, phenomenon);
        final SosOffering sosOffering = new SosOffering(procedureId);
        system
                .setInputs(Collections.<SmlIo<?>>singletonList(createInput(phenomenon)))
                .setOutputs(Collections.<SmlIo<?>>singletonList(createOutput(phenomenon)))
                .setKeywords(createKeywordList(feature, phenomenon, schemaDescription))
                .setIdentifications(createIdentificationList(feature, phenomenon))
                .setClassifications(createClassificationList(feature, phenomenon))
                .addCapabilities(createCapabilities(feature, phenomenon, sosOffering))
//                .addContact(createContact(schemaDescription.getDataset())) // TODO
                // ... // TODO
                .setValidTime(createValidTimePeriod())
                .setIdentifier(procedureId)
                ;

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
        } catch (OwsExceptionReport ex) {
            LOGGER.error("Could not encode SML to valid XML.", ex);
            return "";  // TODO empty but valid sml
        }
    }

    private SmlIo<?> createInput(Phenomenon phenomeon) {
        return new SmlIo<>(new SweObservableProperty()
                .setDefinition(phenomeon.getId()))
                .setIoName(phenomeon.getId());
    }

    private SmlIo<?> createOutput(Phenomenon phenomeon) {
        return new SmlIo<>(new SweQuantity()
                .setUom(phenomeon.getUom())
                .setDefinition(phenomeon.getId()))
                .setIoName(phenomeon.getId());
    }

    private List<String> createKeywordList(AbstractFeature feature, Phenomenon phenomenon, SchemaDescriptor schemaDescription) {
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
        StringBuilder procedureId = new StringBuilder();
        procedureId.append(phenomenon.getLabel()).append("_");
        if (feature.isSetName()) {
            procedureId.append(feature.getFirstName().getValue()).append("_");
        }
        procedureId.append(feature.getIdentifier());
        return procedureId.toString();
    }

    private String createProcedureLongName(AbstractFeature feature, Phenomenon phenomenon) {
        StringBuilder phenomenonName = new StringBuilder();
        phenomenonName.append(phenomenon.getLabel()).append("@");
        if (feature.isSetName()) {
            phenomenonName.append(feature.getFirstName().getValue());
        } else {
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

    private List<SmlCapabilities> createCapabilities(AbstractFeature feature, Phenomenon phenomenon, SosOffering offering) {
        List<SmlCapabilities> capabilities = new ArrayList<>();
        capabilities.add(createFeatureCapabilities(feature));
        capabilities.add(createOfferingCapabilities(feature, phenomenon, offering));
//        capabilities.add(createBboxCapabilities(feature)); // TODO
        return capabilities;
    }

    private SmlCapabilities createFeatureCapabilities(AbstractFeature feature) {
        SmlCapabilities featuresCapabilities = new SmlCapabilities("featuresOfInterest");
        final SweSimpleDataRecord record = new SweSimpleDataRecord()
                .addField(createTextField(
                        SensorML20Constants.FEATURE_OF_INTEREST_FIELD_NAME,
                        SensorML20Constants.FEATURE_OF_INTEREST_FIELD_DEFINITION,
                        feature.getIdentifier()));
        return featuresCapabilities.setDataRecord(record);
    }

    private SmlCapabilities createOfferingCapabilities(AbstractFeature feature, Phenomenon phenomenon, SosOffering offering) {
        SmlCapabilities offeringCapabilities = new SmlCapabilities("offerings");
        offering.setIdentifier("Offering_" + createProcedureId(feature, phenomenon));
        final SweSimpleDataRecord record = new SweSimpleDataRecord()
                .addField(createTextField(
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

    private OmObservation createObservation(Map.Entry<ResourceKey, Map<ResourceField, String>> observationEntry,
            OmObservationConstellation constellation, Phenomenon phenomenon) {
        SingleObservationValue<?> value = null;
        Time time = null;
        TimeInstant validStart = null;
        TimeInstant validEnd = null;

        OmObservation omObservation = new OmObservation();
        omObservation.setObservationConstellation(constellation);
        omObservation.setDefaultElementEncoding(CkanConstants.DEFAULT_CHARSET.toString());
        final GeometryBuilder pointBuilder = GeometryBuilder.create();
        Geometry geom = null;
        for (Map.Entry<ResourceField, String> cells : observationEntry.getValue().entrySet()) {

            ResourceField field = cells.getKey();
            String resourceType = field.getQualifier().getResourceType();
            if (resourceType.equalsIgnoreCase(CkanConstants.ResourceType.OBSERVATIONS)) {
                // if (field.hasProperty(CkanConstants.KnownFieldProperty.PHENOMENON)) {
                if (field.getIndex() == phenomenon.getFieldIdx()) {
                    // TODO check index vs fieldId comparison
                    String phenomenonField = field.getFieldId();
                    String phenomenonId = constellation.getObservableProperty().getIdentifier();
                    // check equality with "phenomeon" field, because fieldId does not always contain the valid phenomenon name
                    if (phenomenonField.equalsIgnoreCase(phenomenonId) || phenomenonId.equals(field.getOther(CkanConstants.KnownFieldProperty.PHENOMENON))) {
                        // TODO value null in case of NO_DATA
                        value = createQuantityObservationValue(field, cells.getValue());
                        omObservation.setIdentifier(observationEntry.getKey().getKeyId() + "_" + phenomenonId);
                    }
                } else if (field.isField(CkanConstants.KnownFieldId.RESULT_TIME)) {
                    time = parseTimestamp(field, cells.getValue());
                } else if (field.isField(CkanConstants.KnownFieldId.LOCATION)) {
                    if (field.isOfType(Geometry.class)) {
                        geom = GeometryBuilder.create()
                                .fromGeoJson(cells.getValue());
                    }
                } else if (field.isField(CkanConstants.KnownFieldId.CRS)) {
                    pointBuilder.withCrs(cells.getValue());
                } else if (field.isField(CkanConstants.KnownFieldId.LATITUDE)) {
                    pointBuilder.setLatitude(cells.getValue());
                } else if (field.isField(CkanConstants.KnownFieldId.LONGITUDE)) {
                    pointBuilder.setLongitude(cells.getValue());
                } else  if (field.isField(CkanConstants.KnownFieldId.ALTITUDE)) {
                    pointBuilder.setAltitude(cells.getValue());
                } else if (field.isField(CkanConstants.KnownFieldId.VALID_TIME_START)) {
                    validStart = parseTimestamp(field, cells.getValue());
                } else if (field.isField(CkanConstants.KnownFieldId.VALID_TIME_END)) {
                    validEnd = parseTimestamp(field, cells.getValue());
                }

            }
        }
        // TODO feature geometry vs samplingGeometry, how to identify mobile sensor???
        // Other strategy???
        if (geom == null && pointBuilder.hasCoordinates()) {
            geom = pointBuilder.getPoint();
        }
        if (geom != null && constellation.getFeatureOfInterest() instanceof SamplingFeature) {
            if (((SamplingFeature)constellation.getFeatureOfInterest()).isSetGeometry()) {

            } else {
                setFeatureGeometry((SamplingFeature)constellation.getFeatureOfInterest(), geom);
            }
        }
        if (validStart != null || validEnd != null) {
            TimePeriod validTime = null;
            if (validStart != null && validEnd == null) {
                validTime = new TimePeriod(validStart, new TimeInstant(TimeIndeterminateValue.unknown));
            } else if (validStart == null && validEnd != null) {
                validTime = new TimePeriod(new TimeInstant(TimeIndeterminateValue.unknown), validEnd);
            } else {
                validTime = new TimePeriod(validStart, validEnd);
            }
            omObservation.setValidTime(validTime);
        }

        // TODO remove value == null if this works out for NO_DATA
        if (value == null || time == null) {
            LOGGER.debug("ignore observation having no value/phenomenonTime.");
            return null;
        } else {
            value.setPhenomenonTime(time);
            omObservation.setValue(value);
            return omObservation;
        }
    }

    protected TimeInstant parseTimestamp(ResourceField field, String dateValue) {
        return !hasDateFormat(field)
                ? new TimeInstant(new Date(Long.parseLong(dateValue)))
                : parseDateValue(dateValue, parseDateFormat(field));
    }

    protected String parseDateFormat(ResourceField field) {
        if (hasDateFormat(field)) {
            String format = field.getOther(CkanConstants.KnownFieldProperty.DATE_FORMAT);
            format = (!format.endsWith("Z") && !format.endsWith("z"))
                        ? format + "Z"
                        : format;
            return format.replace("DD", "dd").replace("hh", "HH"); // XXX hack to fix wrong format
        }
        return null;
    }

    private boolean hasDateFormat(ResourceField field) {
        return field.hasProperty(CkanConstants.KnownFieldProperty.DATE_FORMAT);
    }

    protected TimeInstant parseDateValue(String dateValue, String dateFormat) {
        try {
            TimeInstant timeInstant = new TimeInstant();
            if (!hasOffsetInfo(dateValue)) {
                dateValue += "Z";
            }
            DateTime dateTime = parseIsoString2DateTime(dateValue, dateFormat);
            timeInstant.setValue(dateTime);
            return timeInstant;
        } catch (Exception ex) {
            if (ex instanceof DateTimeParseException) {
                LOGGER.error("Cannot parse date string {} with format {}", dateValue, dateFormat);
            } else {
                LOGGER.error("Cannot parse date string {} with format {}", dateValue, dateFormat, ex);
            }

            return null;
        }

    }

    /**
     * Parses a time String to a Joda Time DateTime object
     *
     * @param timeString
     *            Time String
     * @param format
     *            Format of the time string
     * @return DateTime object
     * @throws DateTimeParseException
     *             If an error occurs.
     */
    protected DateTime parseIsoString2DateTime(final String timeString, String format) throws DateTimeParseException {
        if (Strings.isNullOrEmpty(timeString)) {
            return null;
        }
        try {
            if (!Strings.isNullOrEmpty(format)){
                return DateTime.parse(timeString, DateTimeFormat.forPattern(format));
            } else if (timeString.contains("+") || Pattern.matches("-\\d", timeString) || timeString.contains("Z")) {
                return ISODateTimeFormat.dateOptionalTimeParser().withOffsetParsed().parseDateTime(timeString);
            } else {
                return ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC).parseDateTime(timeString);
            }
        } catch (final RuntimeException uoe) {
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
                quantityValue.setUnit(parseToUcum(field.getOther(CkanConstants.KnownFieldProperty.UOM)));
                obsValue.setValue(quantityValue);
                return obsValue;
            }
        } catch (Exception e) {
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
//            return StandardUnitDB.instance().get(uom);
            return uom;
        } catch (Exception e) {
            LOGGER.error("Could not parse UOM '{}' to known UCUM symbol.", uom, e);
        }
        return null;
    }

    private AbstractPhenomenon createPhenomenon(Phenomenon phenomenon) {
        return new OmObservableProperty(phenomenon.getId());
    }

    private void addObservationsToCacheReference(CkanSosObservationReference reference, List<OmObservation> observations) {
        for (OmObservation observation : observations) {
            reference.addObservationReference(observation);
        }
    }

}
