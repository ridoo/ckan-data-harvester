package org.n52.series.ckan.sos;

import java.nio.channels.IllegalSelectorException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.h2.tools.Csv;
import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.H2DatasetTable;
import org.n52.sos.ds.hibernate.InsertObservationDAO;
import org.n52.sos.ds.hibernate.InsertSensorDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DataStoreManager extends SosDataStoreManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(H2DataStoreManager.class);
    
    private final Map<ResourceMember, ResultSet> resultSetsByResourceMember;
    
    private String h2ConnectionString;
    
    private String h2User;
    
    private String h2Password;

    H2DataStoreManager() {
        this(null);
    }
    
    H2DataStoreManager(CkanSosReferenceCache ckanSosReferenceCache) {
        this(new InsertSensorDAO(), new InsertObservationDAO(), new DeleteObservationDAO(), ckanSosReferenceCache);
    }

    public H2DataStoreManager(InsertSensorDAO insertSensorDao,
            InsertObservationDAO insertObservationDao,
            DeleteObservationDAO deleteObservationDao,
            CkanSosReferenceCache ckanSosReferenceCache) {
        super(insertSensorDao, insertObservationDao, deleteObservationDao, ckanSosReferenceCache);
        this.resultSetsByResourceMember = new HashMap<>();
    }

    @Override
    public void insertOrUpdate(DataCollection dataCollection) {
        writeDataToH2(dataCollection);
        super.insertOrUpdate(dataCollection);
    }

    private void writeDataToH2(DataCollection dataCollection) {
        ResourceMember lastMember = null;
        Set<String> availableFieldIds = new HashSet<>();
        for (Entry<ResourceMember, DataFile> entry : dataCollection) {
            try/*(Connection connection = getConnection();)*/ {
                ResultSet result = writeToH2(entry);
                availableFieldIds.addAll(entry.getKey().getFieldIds());
//                if (lastMember != null) {
//                    connection.createStatement().executeQuery(sql)
//                    // TODO index joinable columns
//                }
                resultSetsByResourceMember.put(entry.getKey(), result);
                lastMember = entry.getKey();
            } catch (SQLException e) {
                LOGGER.warn("Could not write data file '{}' to H2.", entry.getValue(), e);
            }
        }
    }

    private ResultSet writeToH2(Entry<ResourceMember, DataFile> entry) throws SQLException {
        DataFile dataFile = entry.getValue();
        List<String> headers = entry.getKey().getColumnHeaders();
        String filename = dataFile.getFile().getAbsolutePath();
        String encoding = dataFile.getEncoding().toString();
        return new Csv().read(filename, headers.toArray(new String[0]), encoding);
    }

    @Override
    protected DataTable loadData(DataCollection dataCollection, Set<String> resourceTypesToInsert) {
        DataTable fullTable = new H2DatasetTable();
        Map<String, List<ResourceMember>> resourceMembersByType = dataCollection.getResourceMembersByType(resourceTypesToInsert);
        for (List<ResourceMember> membersWithCommonResourceTypes : resourceMembersByType.values()) {
            DataTable extendedTable = new H2DatasetTable();
            for (ResourceMember resourceMember: membersWithCommonResourceTypes) {
                ResultSet resultSet = resultSetsByResourceMember.get(resourceMember);
                DataTable table = new H2DatasetTable(resourceMember, resultSet);
                extendedTable.extendWith(table);
            }
            fullTable = fullTable.innerJoin(extendedTable);
        }
        
        return fullTable == null
                ? new H2DatasetTable()
                : fullTable;
    }
    
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
            return DriverManager.getConnection(h2ConnectionString, h2User, h2Password);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Could not create H2 database connection.", e);
            throw new IllegalStateException("No H2 connection possible.");
        }
    }

    public String getH2ConnectionString() {
        return h2ConnectionString;
    }

    public void setH2ConnectionString(String h2ConnectionString) {
        this.h2ConnectionString = h2ConnectionString;
    }

    public String getH2User() {
        return h2User;
    }

    public void setH2User(String h2User) {
        this.h2User = h2User;
    }

    public String getH2Password() {
        return h2Password;
    }

    public void setH2Password(String h2Password) {
        this.h2Password = h2Password;
    }

}
