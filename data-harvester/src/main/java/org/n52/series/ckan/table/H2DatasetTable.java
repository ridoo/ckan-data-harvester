package org.n52.series.ckan.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;

import org.h2.tools.SimpleResultSet;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DatasetTable extends DataTable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(H2DatasetTable.class);

    public H2DatasetTable() {
        this(new ResourceMember(), new SimpleResultSet());
    }
    
    public H2DatasetTable(ResourceMember resourceMember) {
        this(resourceMember, new SimpleResultSet());
    }
    
    public H2DatasetTable(Entry<ResourceMember, ResultSet> dataEntry) {
        this(dataEntry.getKey(), dataEntry.getValue());
    }

    public H2DatasetTable(ResourceMember resourceMember, ResultSet resultSet) {
        super(resourceMember);
        readData(resultSet);
    }

    private void readData(ResultSet resultSet) {
        try {
            int lineNbr = 0;
            while (resultSet.next()) {
                for (int i = 0 ; i < resourceMember.getColumnHeaders().size() ; i++) {
                    ResourceKey id = resourceMember.createResourceKey(lineNbr);
                    ResourceField field = resourceMember.getField(i);
                    String value = resultSet.getString(i + 1);
                    setCellValue(id, field, value);
                    LOGGER.debug("Line #{}, {}={}", lineNbr, field.getFieldId(), value);
                }
                lineNbr++;
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to read data from result set. ResourceMember '{}'.", resourceMember, e);
        }
    }

}
