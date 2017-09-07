
package org.n52.series.ckan.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.table.ResourceTable;
import org.n52.series.ckan.table.ResourceTestHelper;
import org.n52.series.ckan.table.TableLoadException;
import org.n52.series.ckan.table.TableLoader;
import org.n52.series.ckan.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvResourceReaderTest {

    private static final String DWD_LARGE_RESOURCE_MEMBER = "b5b7e5cb-25c7-46e8-b6e5-22521cfc9a97";

    private static final String DWD_KREISE_DATASET_ID = "2518529a-fbf1-4940-8270-a1d4d0fa8c4d";

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvResourceReaderTest.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ResourceTestHelper testHelper;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        String trimmedData = "/files/" + TestConstants.TEST_COMPLETE_DATA_FOLDER;
        testHelper = new ResourceTestHelper(testFolder, trimmedData);
    }

    @Test
    public void readViaDefaultCsvReader() {
//         readResourceTable(CsvTableLoader::new);
        readResourceTable(CustomCsvTableLoader::new);
    }

    private void readResourceTable(BiFunction<ResourceTable, DataFile, TableLoader> tableLoaderSupplier) {
        DataCollection dataCollection = testHelper.getDataCollection(DWD_KREISE_DATASET_ID);
        Entry<ResourceMember, DataFile> entry = dataCollection.getDataEntry(DWD_LARGE_RESOURCE_MEMBER);
        readData(new ResourceTable(entry) {
            @Override
            protected TableLoader createTableLoader(DataFile file) {
                return tableLoaderSupplier.apply(this, entry.getValue());
            }
        });
    }

    private void readData(ResourceTable table) {
        long start = System.currentTimeMillis();
        table.readIntoMemory();
        LOGGER.info("Reading to memory took {}s", (System.currentTimeMillis() - start) / 1000d);
    }

    private static class CustomCsvTableLoader extends TableLoader {

        private int lineNbr;
        
        public CustomCsvTableLoader(DataTable table, DataFile datafile) {
            super(table, datafile);
        }

        @Override
        public void loadData() throws TableLoadException {
            DataFile dataFile = getDataFile();
            File file = dataFile.getFile();
            ResourceMember resourceMember = getResourceMember();

            lineNbr = 0;
            Pattern csvValuesPatter = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            List<String> columnHeaders = resourceMember.getColumnHeaders();
            int headerRows = resourceMember.getHeaderRows();
            try {
                BufferedReader reader = Files.newBufferedReader(file.toPath(), dataFile.getEncoding());
                reader.lines()
                     .skip(headerRows)
                     .forEach(l -> {
                         ResourceKey id = resourceMember.createResourceKey(lineNbr++);
//                         String[] values = l.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                         String[] values = csvValuesPatter.split(l);
                         for (int j = 0; j < columnHeaders.size(); j++) {
                             final ResourceField field = resourceMember.getField(j);
                             setCellValue(id, field, values[j]);
                         }
                         
                     });
            } catch (IOException e) {
                throw new TableLoadException("Unable to read file: " + file, e);
            }

        }

    }
}
