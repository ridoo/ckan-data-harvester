package org.n52.series.ckan.sos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobileInsertStrategy implements SosInsertStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileInsertStrategy.class);

    private final CkanSosReferenceCache ckanSosReferenceCache;

    private UomParser uomParser = new UcumParser();
    
    public MobileInsertStrategy() {
        this(null);
    }

    MobileInsertStrategy(CkanSosReferenceCache ckanSosReferencingCache) {
        this.ckanSosReferenceCache = ckanSosReferencingCache;
    }

    @Override
    public Map<String, DataInsertion> createDataInsertions(DataTable dataTable, DataCollection dataCollection) {
        PhenomenonParser phenomenonParser = new PhenomenonParser(uomParser);
        List<ResourceField> resourceFields = dataTable.getResourceMember().getResourceFields();
        final List<Phenomenon> phenomena = phenomenonParser.parse(resourceFields);
        LOGGER.debug("Phenomena: {}", phenomena);

        LOGGER.debug("Create stationary insertions ...");
        Map<String, DataInsertion> dataInsertions = new HashMap<>();
        for (Entry<ResourceKey, Map<ResourceField, String>> rowEntry : dataTable.getTable().rowMap().entrySet()) {
            
        }
        return dataInsertions;
    }



}
