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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.n52.series.ckan.cache.InMemoryMetadataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;

public class SerializingMetadataStore extends InMemoryMetadataStore implements CkanSosReferenceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializingMetadataStore.class);

    private final Lock mutex = new ReentrantLock();

    private SerializedCache cache = new SerializedCache();

    private String filePath;

    public SerializingMetadataStore() {
        super();
    }

    public SerializingMetadataStore(String ckanMappingFile) {
        super(ckanMappingFile);
    }

    @PostConstruct
    public void init() {
        try {
            final File cacheFile = getMetadataFile();
            final String filePath = cacheFile.getAbsolutePath();
            LOGGER.debug("Try reading cache from '{}'", filePath);
            mutex.lock();
            if ( !cacheFile.exists()) {
                if ( !cacheFile.getParentFile().mkdirs() || !cacheFile.createNewFile()) {
                    LOGGER.error("Could not create file '{}'.", filePath);
                }
            } else {
                readCacheFromFile(cacheFile);
            }
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("Could not create file '{}'", filePath, e);
            throw new IllegalStateException("config parameter 'metadataCacheFile' is not valid.", e);
        }
        finally {
            mutex.unlock();
        }
    }

    private File getMetadataFile() throws URISyntaxException {
        final Path path = Paths.get(getClass().getResource("/").toURI());
        final File cacheFile = path.resolve(filePath).toFile();
        return cacheFile;
    }

    private void readCacheFromFile(File cacheFile) {
        final String filePath = cacheFile.getAbsolutePath();
        if (new File(filePath).exists()) {
            LOGGER.info("Read cache file from '{}'", filePath);
            try (ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(cacheFile))) {
                cache = (SerializedCache) objIn.readObject();
                putAll(cache.getDatasets());
            } catch (IOException e) {
                LOGGER.error("Could not deserialize from '{}'", filePath, e);
            } catch (ClassNotFoundException e) {
                 LOGGER.error("Cache file outdated: '{}'", filePath, e);
            }
        }
    }

    @Override
    public void addOrUpdate(CkanSosObservationReference reference) {
        if (reference != null) {
            cache.addOrUpdate(reference);
            serialize();
        }
    }

    @Override
    public void delete(CkanSosObservationReference reference) {
        if (reference != null) {
            cache.delete(reference);
            serialize();
        }
    }

    @Override
    public void delete(CkanResource resource) {
        if (resource != null) {
            cache.delete(resource);
            serialize();
        }
    }

    @Override
    public boolean exists(CkanResource reference) {
        return cache.exists(reference);
    }

    @Override
    public CkanSosObservationReference getReference(CkanResource resource) {
        return cache.getReference(resource);
    }

    @Override
    public void delete(CkanDataset dataset) {
        if (dataset != null) {
            super.delete(dataset);
            cache.setDatasets(getDatasets());
            serialize();
        }
    }

    @Override
    public void insertOrUpdate(CkanDataset dataset) {
        if (dataset != null) {
            super.insertOrUpdate(dataset);
            cache.setDatasets(getDatasets());
            serialize();
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Serialize metadata before shutting down ...");
        serialize();
        LOGGER.info("Serializing done.");
    }

    public void serialize() {
        try {
            final File cacheFile = getMetadataFile();
            String filePath = cacheFile.getAbsolutePath();
            LOGGER.info("Serializing metadata to file {}", filePath);
            mutex.lock();
            try (ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                objOut.writeObject(cache);
            } catch (IOException e) {
                LOGGER.error("Could not write cache to file '{}'", filePath, e);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Could not write cache.", e);
        }  finally {
            mutex.unlock();
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
