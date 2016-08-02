package org.n52.series.ckan.table;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.beans.CsvObservationsCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.beans.SchemaDescriptor;
import org.n52.series.ckan.cache.InMemoryCkanDataCache;
import org.n52.series.ckan.util.FileBasedCkanHarvestingService;

public class ResourceTestHelper {

    private FileBasedCkanHarvestingService service;
    
    private InMemoryCkanDataCache ckanDataCache;

    public ResourceTestHelper(TemporaryFolder testFolder) throws URISyntaxException, IOException {
        service = new FileBasedCkanHarvestingService(testFolder.getRoot());
        ckanDataCache = service.getCkanDataCache();
    }

    public ResourceTable readPlatformTable(String datasetId, String resourceId) {
        return readTable(datasetId, resourceId, "platforms");
    }
    
    public ResourceTable readObservationTable(String datasetId, String resourceId) {
        return readTable(datasetId, resourceId, "observations");
    }
    
    public ResourceTable readTable(String datasetId, String resourceId, String resourceType) {
        return readTable(datasetId, new ResourceMember(resourceId, resourceType));
    }
    
    public ResourceTable readTable(String datasetId, ResourceMember resourceMember) {
        CsvObservationsCollection collection = getDataCollection(datasetId);
        Entry<ResourceMember, DataFile> entry = collection.getDataEntry(resourceMember);
        if (entry == null) {
            return new ResourceTable();
        }
        ResourceTable table = new ResourceTable(entry.getKey(), entry.getValue());
        table.readIntoMemory();
        return table;
    }
    
    public ResourceMember getResourceMember(String datasetId, ResourceMember resourceMember) {
        CsvObservationsCollection collection = getDataCollection(datasetId);
        Entry<ResourceMember, DataFile> entry = collection.getDataEntry(resourceMember);
        return entry == null
                ? resourceMember
                : entry.getKey(); 
    }
    
    public CsvObservationsCollection getDataCollection(String datasetId) {
        return ckanDataCache.getCollection(datasetId);
    }
    
    public SchemaDescriptor getSchemaDescriptor(String datasetId) {
        CsvObservationsCollection dataCollection = getDataCollection(datasetId);
        return dataCollection.getSchemaDescriptor().getSchemaDescription();
    }


}
