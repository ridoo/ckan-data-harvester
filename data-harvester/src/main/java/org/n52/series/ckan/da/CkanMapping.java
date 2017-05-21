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

package org.n52.series.ckan.da;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.base.Strings;

/**
 * Allows to map a common set of identifiers ({@link CkanConstants}) to an alternate set of terms. By default
 * <tt>config-ckan-mapping.json</tt> read from classpath. Defaults can be overridden by passing a different
 * mapping file to one of the overloaded {@link CkanMapping#loadCkanMapping()}.
 */
public class CkanMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(CkanMapping.class);

    private final static String DEFAULT_CKAN_MAPPING_FILE = "config-ckan-mapping.json";

    private final JsonNode fallbackMapping;

    private final JsonNode jsonMapping;

    public CkanMapping() {
        this(null);
    }

    public CkanMapping(JsonNode jsonCkanMapping) {
        this(jsonCkanMapping, null);
    }

    public CkanMapping(JsonNode jsonCkanMapping, JsonNode fallbackMapping) {
        this.jsonMapping = jsonCkanMapping == null
                ? MissingNode.getInstance()
                : jsonCkanMapping;
        this.fallbackMapping = fallbackMapping == null
                ? MissingNode.getInstance()
                : fallbackMapping;
    }

    public boolean hasDataTypeMappings(String datatypeValue, String mapping) {
        return hasMappings("datatype", datatypeValue, mapping);
    }

    public boolean hasFieldMappings(String fieldValue, String mapping) {
        return hasMappings("field", fieldValue, mapping);
    }

    public boolean hasPropertyMappings(String property, String mapping) {
        return hasMappings("property", property, mapping);
    }

    public boolean hasResourceTypeMappings(String resourceTypeValue, String mapping) {
        return hasMappings("resource_type", resourceTypeValue, mapping);
    }

    public boolean hasRoleMappings(String roleValue, String mapping) {
        return hasMappings("role", roleValue, mapping);
    }

    public boolean hasSchemaDescriptionMappings(String schemaDescriptionValue, String mapping) {
        return hasMappings("schema_descriptor", schemaDescriptionValue, mapping);
    }

    public boolean hasMappings(String group, String name, String mapping) {
        return getValueMappings(group, name).contains(mapping.toLowerCase(Locale.ROOT));
    }

    public Set<String> getPropertyMappings(String name) {
        return getValueMappings("property", name);
    }

    public Set<String> getDatatypeMappings(String name) {
        return getValueMappings("datatype", name);
    }

    public Set<String> getFieldMappings(String name) {
        return getValueMappings("field", name);
    }

    public Set<String> getResourceTypeMappings(String name) {
        return getValueMappings("resource_type", name);
    }

    public Set<String> getRoleMappings(String name) {
        return getValueMappings("role", name);
    }

    public Set<String> getSchemaDescriptionMappings(String name) {
        return getValueMappings("schema_descriptor", name);
    }
    }

    protected Set<String> getValueMappings(String group, String name) {
        String lowerCasedName = name != null
                ? name.toLowerCase(Locale.ROOT)
                : name;
        String lowerCasedGroup = !(group == null || group.isEmpty())
                ? "/" + group.toLowerCase(Locale.ROOT)
                : "";
        String path = lowerCasedGroup + "/" + lowerCasedName;
        JsonNode mappingArray = getConfigValueAt(path);
        if (mappingArray.isMissingNode()) {
            LOGGER.trace("try to get '{}' from fallback mapping.", path);
            mappingArray = fallbackMapping.at(path);
        }
        Set<String> mappingValues = new HashSet<>();
        for (JsonNode node : mappingArray) {
            mappingValues.add(node.asText()
                                  .toLowerCase(Locale.ROOT));
        }
        mappingValues.add(lowerCasedName);
        return new HashSet<>(mappingValues);
    }

    public JsonNode getConfigValueAt(String path) {
        return this.jsonMapping.at(path);
    }

    public static CkanMapping loadCkanMapping() {
        return new CkanMappingLoader().loadConfig();
    }

    public static CkanMapping loadCkanMapping(String configFile) {
        return new CkanMappingLoader().loadConfig(configFile);
    }

    public static CkanMapping loadCkanMapping(File configFile) {
        return new CkanMappingLoader().loadConfig(configFile);
    }

    public static CkanMapping loadCkanMapping(InputStream inputStream) {
        try {
            return new CkanMappingLoader().loadConfig(inputStream);
        } catch (IOException e) {
            LOGGER.error("Could not load from input stream. Using empty config.", e);
            return new CkanMapping();
        }
    }

    private static class CkanMappingLoader {

        private final static Logger LOGGER = LoggerFactory.getLogger(CkanMapping.CkanMappingLoader.class);

        private CkanMapping loadConfig() {
            return loadConfig((String) null);
        }

        private CkanMapping loadConfig(String configFile) {
            try {
                return loadConfig(createStreamFrom(configFile));
            } catch (IOException e) {
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

        private CkanMapping loadConfig(InputStream inputStream) throws IOException {
            return new CkanMapping(readJson(inputStream), readJson(loadDefaults()));
        }

        private JsonNode readJson(InputStream inputStream) throws IOException {
            try (InputStream mappingConfig = inputStream) {
                ObjectMapper om = new ObjectMapper();
                return om.readTree(mappingConfig);
            }
        }

        private InputStream createStreamFrom(String configFile) throws FileNotFoundException {
            File file = Strings.isNullOrEmpty(configFile)
                    ? getConfigFile(DEFAULT_CKAN_MAPPING_FILE)
                    : getConfigFile(configFile);
            return file.exists()
                    ? createStreamFrom(file)
                    : loadDefaults();
        }

        private File getConfigFile(String configFile) {
            try {
                Path path = Paths.get(CkanMapping.CkanMappingLoader.class.getResource("/")
                                                                         .toURI());
                File file = path.resolve(configFile)
                                .toFile();
                LOGGER.trace("Loading config from '{}'", file.getAbsolutePath());
                return file;
            } catch (URISyntaxException e) {
                LOGGER.info("Could not find config file '{}'. Load from compiled default.", configFile, e);
                return null;
            }
        }

        private InputStream createStreamFrom(File file) {
            if (file != null) {
                try {
                    LOGGER.trace("Loading config from '{}'", file.getAbsolutePath());
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    LOGGER.warn("Missing config file '{}'! Loading from jar.", file.getAbsolutePath());
                }
            }
            return loadDefaults();
        }

        private InputStream loadDefaults() {
            LOGGER.trace("Loading '{}' from jar.", DEFAULT_CKAN_MAPPING_FILE);
            return getClass().getResourceAsStream("/" + DEFAULT_CKAN_MAPPING_FILE);
        }

    }

}
