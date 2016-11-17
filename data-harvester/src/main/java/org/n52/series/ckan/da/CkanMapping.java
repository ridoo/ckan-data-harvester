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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

public class CkanMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(CkanMapping.class);

    private final Map<String, Set<String>> mappingsByName;

    private final Set<String> unmappedKeys;

    public CkanMapping() {
        this.mappingsByName = new HashMap<>();
        this.unmappedKeys = new HashSet<>();
    }

    public boolean hasMappings(String id) {
        return mappingsByName.containsKey(id) && !mappingsByName.get(id).isEmpty();
    }

    public boolean hasMapping(String name, String mapping) {
        return getMappings(name).contains(mapping.toLowerCase(Locale.ROOT));
    }

    public Set<String> getMappings(String name) {
        String lowerCasedName = name != null
                ? name.toLowerCase(Locale.ROOT)
                : name;
        if ( !mappingsByName.containsKey(lowerCasedName)) {
            if ( !unmappedKeys.contains(lowerCasedName)) {
                LOGGER.debug("No mapping for name '{}' (lowercased)", lowerCasedName);
                unmappedKeys.add(lowerCasedName);
            }
            return Collections.singleton(lowerCasedName);
        }
        else {
            Set<String> mappings = mappingsByName.get(lowerCasedName);
            mappings.add(lowerCasedName); // add self
            return mappings;
        }
    }

    @JsonAnyGetter
    public Map<String, Set<String>> getMappingsByName() {
        return mappingsByName;
    }

    @JsonAnySetter
    public CkanMapping addMapping(String name, Set<String> mappings) {
        if (name != null) {
            Set<String> lowerCasedMappings = toLowerCase(mappings);
            this.mappingsByName.put(name.toLowerCase(Locale.ROOT), lowerCasedMappings);
        }
        return this;
    }

    private Set<String> toLowerCase(Set<String> mappings) {
        HashSet<String> lowerCased = new HashSet<>();
        for (String mapping : mappings) {
            lowerCased.add(mapping.toLowerCase(Locale.ROOT));
        }
        return lowerCased;
    }

    public static CkanMapping loadCkanMapping() {
        return new PropertyIdMappingLoader().loadConfig();
    }

    public static CkanMapping loadCkanMapping(String configFile) {
        return new PropertyIdMappingLoader().loadConfig(configFile);
    }

    public static CkanMapping loadCkanMapping(File configFile) {
        return new PropertyIdMappingLoader().loadConfig(configFile);
    }

    private static class PropertyIdMappingLoader {

        private final static Logger LOGGER = LoggerFactory.getLogger(CkanMapping.PropertyIdMappingLoader.class);

        private final static String DEFAULT_CKAN_MAPPING_FILE = "config-ckan-mapping.json";

        private CkanMapping loadConfig() {
            return loadConfig((String) null);
        }

        private CkanMapping loadConfig(String configFile) {
            try {
                return loadConfig(createStreamFrom(configFile));
            }
            catch (IOException e) {
                LOGGER.error("Could not load from '{}'. Using empty config.", configFile, e);
                return new CkanMapping();
            }
        }

        private CkanMapping loadConfig(File file) {
            try {
                return loadConfig(createStreamFrom(file));
            } catch (IOException e) {
                LOGGER.error("Could not load {}. Using empty config.", file.getAbsolutePath(), e);
                return new CkanMapping();
            }
        }

        private CkanMapping loadConfig(InputStream intputStream) throws IOException {
            try (InputStream taskConfig = intputStream) {
                ObjectMapper om = new ObjectMapper();
                return om.readValue(taskConfig, CkanMapping.class);
            }
        }

        private InputStream createStreamFrom(String configFile) throws FileNotFoundException {
            File file = Strings.isNullOrEmpty(configFile)
                    ? getConfigFile(DEFAULT_CKAN_MAPPING_FILE)
                    : getConfigFile(configFile);
            return file.exists()
                    ? createStreamFrom(file)
                    : getClass().getResourceAsStream("/" + DEFAULT_CKAN_MAPPING_FILE);
        }

        private File getConfigFile(String configFile) {
            try {
                Path path = Paths.get(CkanMapping.PropertyIdMappingLoader.class.getResource("/").toURI());
                File file = path.resolve(configFile).toFile();
                LOGGER.debug("Loading config from '{}'", file.getAbsolutePath());
                return file;
            } catch (URISyntaxException e) {
                LOGGER.info("Could not find config file '{}'. Load from compiled default.", configFile, e);
                return null;
            }
        }

        private InputStream createStreamFrom(File file) {
            if (file != null) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    LOGGER.debug("Missing config file '{}'! Loading from jar.", file.getAbsolutePath());
                }
            }
            return loadDefaults();
        }

        private InputStream loadDefaults() {
            LOGGER.debug("Loading '{}' from jar.", DEFAULT_CKAN_MAPPING_FILE);
            return CkanMapping.PropertyIdMappingLoader.class.getResourceAsStream(DEFAULT_CKAN_MAPPING_FILE);
        }

    }

}
