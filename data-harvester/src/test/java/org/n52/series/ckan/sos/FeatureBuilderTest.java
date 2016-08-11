package org.n52.series.ckan.sos;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;
import static java.util.Collections.singletonMap;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.FieldBuilder;
import static org.hamcrest.CoreMatchers.is;
import org.n52.series.ckan.da.CkanConstants;

public class FeatureBuilderTest {

    private static final String ORGA_NAME = "test_orga";

    private static final String RESOURCE_FIELD_TEMPLATE = "{"
            + "  \"field_id\": \"%s\","
            + "  \"short_name\": \"%s\","
            + "  \"long_name\": \"%s\""
            + "}";

    private FeatureBuilder featureBuilder;

    @Before
    public void setUp() {
        CkanDataset dataset = new CkanDataset("test_dataset");
        dataset.setOrganization(new CkanOrganization(ORGA_NAME));
        this.featureBuilder = new FeatureBuilder(dataset);
    }

    @Test
    public void when_noGeometry_then_featureWithUnknownType() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.STATION_ID)
                .create(), "foobar_station");
        String actual = featureBuilder.createFeature(row).getFeatureType();
        MatcherAssert.assertThat(actual, is("http://www.opengis.net/def/nil/OGC/0/unknown"));
    }

    @Test
    public void when_rowEntryWithStationId_then_featureHasAppropriateFoiId() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.STATION_ID)
                .create(), "foobar_station");
        String actual = featureBuilder.createFeature(row).getIdentifier();
        MatcherAssert.assertThat(actual, is(ORGA_NAME + "-foobar_station"));
    }

    @Test
    public void when_rowEntryWithStationName_then_featureHasAppropriateName() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.STATION_NAME)
                .create(), "foobar_station");
        String actual = featureBuilder.createFeature(row).getFirstName().getValue();
        MatcherAssert.assertThat(actual, is("foobar_station"));
    }

    @Test
    public void when_rowEntriesWithLonLatAlt_then_featureHasAppropriateGeometry() {
        Map<ResourceField, String> row = new HashMap<>();
        row.put(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LATITUDE)
                .withFieldType(CkanConstants.DataType.DOUBLE)
                .withUom("decimal degrees")
                .create(), "52.7");
        row.put(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LONGITUDE)
                .withFieldType(CkanConstants.DataType.DOUBLE)
                .withUom("decimal degrees")
                .create(), "7.5");
        row.put(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.ALTITUDE)
                .withFieldType(CkanConstants.DataType.DOUBLE)
                .withUom("m")
                .create(), "10");
        Geometry geometry = featureBuilder.createFeature(row).getGeometry();

//        final String actual = geometry.toText(); // toText() does not respect altitude
        final String actual = new WKTWriter(3).write(geometry);
        MatcherAssert.assertThat(actual, is("POINT (52.7 7.5 10)"));
    }

    @Test
    public void when_rowEntriesWithGeoJson_then_featureHasAppropriateGeometry() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LOCATION)
                .withFieldType("JsonObject")
                .withUom("geojson")
                .create(), "{'coordinates':[51.05, 13.74],'type':'Point'}");
        Geometry geometry = featureBuilder.createFeature(row).getGeometry();
        MatcherAssert.assertThat(geometry.toText(), is("POINT (51.05 13.74)"));
    }

    @Test
    public void when_rowEntriesWithGeoJsonAsPhenomenon_then_featureHasNoGeometry() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LOCATION)
                .withFieldType("JsonObject")
                .withPhenomenon("location")
                .withUom("geojson")
                .create(), "{'coordinates':[51.05, 13.74],'type':'Point'}");
        Geometry geometry = featureBuilder.createFeature(row).getGeometry();
        MatcherAssert.assertThat(geometry, is(CoreMatchers.nullValue(Geometry.class)));
    }

    //@Test
    public void when_locationPhenomenonAsJsonObject_then_WHAT_FEATURE_TYPE_SHALL_WE_EXPECT_HERE () {
        // TODO
    }
}
