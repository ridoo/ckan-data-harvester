package org.n52.series.ckan.sos;

import java.util.Map;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.table.DataTable;

public interface SosInsertStrategy {

    Map< String, DataInsertion> createDataInsertions(DataTable fullTable, DataCollection dataCollection);
}
