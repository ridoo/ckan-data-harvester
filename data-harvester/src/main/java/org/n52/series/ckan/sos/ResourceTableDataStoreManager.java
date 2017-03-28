package org.n52.series.ckan.sos;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceTable;
import org.n52.sos.ds.hibernate.InsertObservationDAO;
import org.n52.sos.ds.hibernate.InsertSensorDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;

public class ResourceTableDataStoreManager extends SosDataStoreManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTableDataStoreManager.class);

    ResourceTableDataStoreManager() {
        this(null);
    }
    
    ResourceTableDataStoreManager(CkanSosReferenceCache ckanSosReferenceCache) {
        this(new InsertSensorDAO(), new InsertObservationDAO(), new DeleteObservationDAO(), ckanSosReferenceCache);
    }

    public ResourceTableDataStoreManager(InsertSensorDAO insertSensorDao,
            InsertObservationDAO insertObservationDao,
            DeleteObservationDAO deleteObservationDao,
            CkanSosReferenceCache ckanSosReferenceCache) {
        super(insertSensorDao, insertObservationDao, deleteObservationDao, ckanSosReferenceCache);
    }

    protected DataTable loadData(DataCollection dataCollection, Set<String> resourceTypesToInsert) {
        CkanDataset dataset = dataCollection.getDataset();
        LOGGER.debug("load data for dataset '{}'", dataset.getName());
        DataTable fullTable = new ResourceTable();

        // TODO write test for it
        // TODO if dataset is newer than in cache -> set flag to re-insert whole datacollection

        Map<String, List<ResourceMember>> resourceMembersByType = dataCollection.getResourceMembersByType(resourceTypesToInsert);
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
            fullTable = fullTable.rowSize() > dataTable.rowSize()
                    ? dataTable.innerJoin(fullTable)
                    : fullTable.innerJoin(dataTable);
        }
        LOGGER.debug("Fully joined table: '{}'", fullTable);
        return fullTable;
    }

}
