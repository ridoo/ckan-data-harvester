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
package org.n52.series.ckan.da;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.DescriptionFile;
import org.n52.series.ckan.beans.SchemaDescriptor;
import org.n52.series.ckan.cache.CkanDataSink;
import org.n52.series.ckan.cache.CkanMetadataCache;
import org.n52.series.ckan.util.JsonUtil;
import org.n52.series.ckan.util.ResourceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import eu.trentorise.opendata.jackan.CkanClient;
import eu.trentorise.opendata.jackan.CkanQuery;
import eu.trentorise.opendata.jackan.SearchResults;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import eu.trentorise.opendata.traceprov.internal.org.apache.commons.io.FileUtils;
import javax.servlet.ServletContext;
import org.springframework.web.context.ServletContextAware;

public class CkanHarvestingService implements ServletContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CkanHarvestingService.class);

    private Path resourceDownloadBaseFolder;

    private int pagingLimit = 20;

    private CkanClient ckanClient;

    private ResourceClient resourceClient;

    private CkanMetadataCache metadataCache;

    private CkanDataSink dataCache;

    public CkanHarvestingService() {
        this.resourceDownloadBaseFolder = resolveDownloadFolder("");
    }

    public void harvestDatasets() {
        harvestDatasets(CkanQuery.filter());
    }

    public void harvestDatasets(CkanQuery query) {
        LOGGER.info("Start harvesting CKAN datasets from '{}'", ckanClient.getCatalogUrl());
        int limit = pagingLimit;
        int offset = 0;
        int lastSize = -1;
        while (hasMorePages(lastSize)) {
            offset += lastSize > 0 ? lastSize : 0;
            SearchResults<CkanDataset> datasets = ckanClient.searchDatasets(query, limit, offset);
            for (CkanDataset dataset : datasets.getResults()) {
                LOGGER.debug("Inserting dataset '{}' ...", dataset.getName());
                metadataCache.insertOrUpdate(dataset);
            }
            lastSize = datasets.getCount();
        }
        LOGGER.info("Finished harvesting CKAN datasets (got #{} datasets).", metadataCache.size());
    }

    private boolean hasMorePages(int lastSize) {
        boolean atStart = lastSize < 0;
        boolean hadResults = lastSize > 0;
        boolean fullPage = lastSize == pagingLimit;
        return atStart || hadResults && fullPage;
    }

    public void harvestResources() throws IOException {
        LOGGER.info("Start harvesting data resources.");
        int observationCollectionCount = 0;
        for (CkanDataset dataset : metadataCache.getDatasets()) {
            if (metadataCache.hasSchemaDescriptor(dataset)) {
                LOGGER.debug("Download resources for dataset {} (Name: {}).",
                        dataset.getId(), dataset.getName());
                DescriptionFile description = getSchemaDescription(dataset);

                List<String> resourceIds = getResourceIds(description.getSchemaDescription());
                Map<String, DataFile> dataFiles = downloadFiles(dataset, resourceIds);

                LOGGER.debug("downloaded data files for '{}': {}", dataset.getName(), dataFiles.keySet());

                // TODO check when to delete or update resource

                dataCache.insertOrUpdate(new DataCollection(dataset, description, dataFiles));
                observationCollectionCount++;
            }
        }
        LOGGER.info("Finished harvesting data resources (got #{} csv-observation-collections).", observationCollectionCount);
    }

    private DescriptionFile getSchemaDescription(CkanDataset dataset) throws IOException {
        saveToFile("dataset.json", dataset, JsonUtil.getCkanObjectMapper().writeValueAsString(dataset));
        SchemaDescriptor schemaDescription = metadataCache.getSchemaDescription(dataset.getId());
        File file = saveToFile("schema_descriptor.json", dataset, schemaDescription.getNode());
        LOGGER.trace("Downloaded resource description to {}.", file.getAbsolutePath());
        return new DescriptionFile(dataset, file, schemaDescription);
    }

    private File saveToFile(String filename, CkanDataset dataset, Object content) throws IOException {
        return saveToFile(filename, dataset, JsonUtil.getObjectMapper().writeValueAsString(content));
    }

    private File saveToFile(String filename, CkanDataset dataset, String content) throws IOException {
        File file = getDatasetDownloadFolder(dataset)
                .resolve(filename)
                .toFile();
        FileUtils.writeStringToFile(file, content, CkanConstants.DEFAULT_CHARSET);
        return file;
    }

    protected List<String> getResourceIds(SchemaDescriptor resourceDescription) {
        List<String> resourceIds = new ArrayList<>();
        JsonNode descriptionNode = resourceDescription.getNode();
        for (JsonNode node : descriptionNode.at("/members")) {
            JsonNode resourceId = node.findValue(CkanConstants.FieldPropertyName.RESOURCE_NAME);
            if (resourceId.isArray()) {
                Iterator<JsonNode> iter = resourceId.iterator();
                while (iter.hasNext()) {
                    resourceIds.add(iter.next().asText());
                }
            } else {
                resourceIds.add(resourceId.asText());
            }
        }
        return resourceIds;
    }

    private Map<String, DataFile> downloadFiles(CkanDataset dataset, List<String> resourceIds) {
        Map<String, DataFile> files = new HashMap<>();
        Path datasetDownloadFolder = getDatasetDownloadFolder(dataset);
        for (CkanResource resource : dataset.getResources()) {
            if (resourceIds.contains(resource.getId())) {
                try {
                    DataFile datafile = downloadFile(resource, datasetDownloadFolder);
                    files.put(resource.getId(), datafile);
                } catch (IOException e) {
                    String url = resource.getUrl();
                    LOGGER.error("Could not download resource from {}.", url, e);
                }
            }
        }
        return files;
    }

    protected DataFile downloadFile(CkanResource resource, Path datasetDownloadFolder) throws IOException {
        String format = resource.getFormat() != null
                ? resource.getFormat().toLowerCase()
                : ".csv"; // XXX as default ok?
        final String resourceName = resource.getId() + "." + format;
        File file = datasetDownloadFolder.resolve(resourceName).toFile();

        // TODO download only when newer

        downloadToFile(resource.getUrl(), file);
        return new DataFile(resource, format, file);
    }

    private String extractFileName(CkanResource resource) {
        final String url = resource.getUrl();
        return url != null
                ? url.substring(url.lastIndexOf("/") + 1)
                : resource.getName() + "." + resource.getFormat().toLowerCase();
    }

    private Path getDatasetDownloadFolder(CkanDataset dataset) {
        return resourceDownloadBaseFolder.resolve(dataset.getName());
    }

    private void downloadToFile(String url, File file) throws IOException {
        String csvContent = resourceClient.downloadTextResource(url);
        FileUtils.writeStringToFile(file, csvContent, CkanConstants.DEFAULT_CHARSET);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        final String realPath = servletContext.getRealPath("/");
        LOGGER.debug("Download location via servlet context: {}", realPath);
        resourceDownloadBaseFolder = Paths.get(realPath);
    }

    public String getResourceDownloadBaseFolder() throws URISyntaxException {
        return resourceDownloadBaseFolder.toString();
    }

    public void setResourceDownloadBaseFolder(String downloadFolder) throws URISyntaxException {
        this.resourceDownloadBaseFolder = new URI(downloadFolder).isAbsolute()
                ? Paths.get(new URI(downloadFolder))
                : resolveDownloadFolder(downloadFolder);
    }

    protected final Path resolveDownloadFolder(String folder) {
        try {
            return Paths.get(getClass().getResource("/").toURI()).resolve(folder);
        }catch (URISyntaxException e) {
            LOGGER.error("Could not set download base folder!", e);
            return null;
        }
    }

    public int getPagingLimit() {
        return pagingLimit;
    }

    public void setPagingLimit(int pagingLimit) {
        this.pagingLimit = pagingLimit;
    }

    public CkanClient getCkanClient() {
        return ckanClient;
    }

    public void setCkanClient(CkanClient ckanClient) {
        this.ckanClient = ckanClient;
    }

    public ResourceClient getResourceClient() {
        return resourceClient;
    }

    public void setResourceClient(ResourceClient resourceClient) {
        this.resourceClient = resourceClient;
    }

    public CkanDataSink getDataCache() {
        return dataCache;
    }

    public void setDataCache(CkanDataSink dataCache) {
        this.dataCache = dataCache;
    }

    public CkanMetadataCache getMetadataCache() {
        return metadataCache;
    }

    public void setMetadataCache(CkanMetadataCache metadataCache) {
        this.metadataCache = metadataCache;
    }

}
