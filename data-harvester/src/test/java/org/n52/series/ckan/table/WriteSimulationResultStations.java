package org.n52.series.ckan.table;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.joda.time.DateTime;
import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.cache.InMemoryDataStoreManager;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.util.FileBasedCkanHarvestingService;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class WriteSimulationResultStations {

    private static final String EMISSION_SIMULATION_DATASET_ID = "9f064e17-799e-4261-8599-d3ee31b5392b";

    public static void main(String[] args) throws URISyntaxException, IOException, NumberFormatException, SchemaException {

        new ShpWriter().write();

    }

    private static class ShpWriter {

        private static final URL OUTPUT_ROOT = WriteSimulationResultStations.class.getResource("/");

        private final InMemoryDataStoreManager dataManager;

        private final DataCollection dataCollection;

        private final Path exportPath;

        public ShpWriter() throws URISyntaxException, IOException, NumberFormatException, SchemaException {
            exportPath = Paths.get(OUTPUT_ROOT.toURI()).resolve("export");
            File folder = exportPath.toFile();
            if ( !folder.exists() && !folder.mkdir()) {
                throw new IllegalStateException("Unable to create output folder");
            }

            dataManager = new FileBasedCkanHarvestingService(folder).getCkanDataStoreManager();
            dataCollection = dataManager.getCollection(EMISSION_SIMULATION_DATASET_ID);
        }

        void write() throws NumberFormatException, IOException, SchemaException {
            Set<String> types = dataManager.getStationaryObservationTypes();
            Map<String, List<ResourceMember>> resourceMembersByType = dataCollection.getResourceMembersByType(types);
            List<ResourceMember> locations = resourceMembersByType.get(CkanConstants.ResourceType.OBSERVED_GEOMETRIES);

            String today = new DateTime().toString("YYYY-MM-dd");
            for (ResourceMember member : locations) {
                DataFile dataFile = dataCollection.getDataFile(member);
                String shpOutput = today + "-output.shp";
                File shpTarget = exportPath.resolve(shpOutput).toFile();
                writeLocations(dataFile.getFile(), shpTarget);
            }
        }
    }

    /**
     * Lent from http://docs.geotools.org/latest/userguide/tutorial/feature/csv2shp.html
     *
     * @param input CSV file
     * @param output SHP target file
     * @throws NumberFormatException
     * @throws IOException
     * @throws SchemaException
     */
    private static void writeLocations(File input, File output) throws NumberFormatException, IOException, SchemaException {

        SimpleFeatureType type = DataUtilities.createType("Location",
                "name:String," +    // <- ID
                "laenge:Double," +   // <- Laenge
                "flaeche:Double," +   // <- Flaeche
                "the_geom:Point:srid=4326,"// <- the geometry attribute: Point type
        );

        List<SimpleFeature> features = new ArrayList<>();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        try (BufferedReader reader = new BufferedReader(new FileReader(input)) ){
            /* First line of the data file is the header */
            String line = reader.readLine();

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.trim().length() > 0) { // skip blank lines
                    String tokens[] = line.split("\\,");

                    String id = tokens[0].trim();
                    double laenge = Double.parseDouble(tokens[1].trim());
                    double flaeche = Double.parseDouble(tokens[2].trim());
                    double latitude = Double.parseDouble(tokens[3].trim());
                    double longitude = Double.parseDouble(tokens[4].trim());

                    /* Longitude (= x coord) first ! */
                    Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

                    featureBuilder.add(id);
                    featureBuilder.add(laenge);
                    featureBuilder.add(flaeche);
                    featureBuilder.add(point);
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            }
        }
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", output.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

        /*
         * TYPE is used as a template to describe the file contents
         */
        newDataStore.createSchema(type);

        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
        /*
         * The Shapefile format has a couple limitations:
         * - "the_geom" is always first, and used for the geometry attribute name
         * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
         * - Attribute names are limited in length
         * - Not all data types are supported (example Timestamp represented as Date)
         *
         * Each data store has different limitations so check the resulting SimpleFeatureType.
         */
        System.out.println("SHAPE:"+SHAPE_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            /*
             * SimpleFeatureStore has a method to add features from a
             * SimpleFeatureCollection object, so we use the ListFeatureCollection
             * class to wrap our list of features.
             */
            SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
            System.exit(0); // success!
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
    }
}
