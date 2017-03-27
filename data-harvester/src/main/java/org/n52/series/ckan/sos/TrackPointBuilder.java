package org.n52.series.ckan.sos;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.sos.TrackPointCollector.TrackPoint;
import org.n52.series.ckan.util.AbstractRowVisitor;
import org.n52.series.ckan.util.GeometryBuilder;

public class TrackPointBuilder extends AbstractRowVisitor<String> {

    private final TrackPointCollector trackPointCollector;

    private GeometryBuilder geometryBuilder;

    private TrackPoint trackPoint;

    public TrackPointBuilder(TrackPointCollector trackPointCollector) {
        this.trackPointCollector = trackPointCollector;
    }

    @Override
    public void init() {
        this.trackPoint = trackPointCollector.newTrackPoint();
        this.geometryBuilder = GeometryBuilder.create();
    }

    @Override
    public void visit(ResourceField field, String value) {
        if (field.isField(CkanConstants.KnownFieldIdValue.OBSERVATION_TIME)) {
            trackPoint.withProperty(field, value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.TRACK_ID)) {
            trackPoint.withProperty(field, value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.TRACK_POINT)) {
            trackPoint.withProperty(field, value);
        } else {
            trackPoint.withProperty(field, value);
        }
        geometryBuilder.visit(field, value);
    }

    @Override
    public boolean hasResult() {
        return trackPoint.isValid();
    }

    @Override
    public String getResult() {
        if (geometryBuilder.canBuildGeometry()) {
            trackPoint.withGeometry(geometryBuilder.getGeometry());
        }
        return trackPointCollector.addToTrack(trackPoint);
    }
}
