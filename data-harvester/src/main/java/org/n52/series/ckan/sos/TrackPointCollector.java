package org.n52.series.ckan.sos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.util.TimeFieldParser;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Geometry;

public class TrackPointCollector {

    private final FeatureBuilder foiBuilder;

    private final Map<String, List<TrackPoint>> trackPointsByTrackId;

    private final Map<String, SamplingFeature> featureByTrackId;

    private final CkanMapping ckanMapping;

    public TrackPointCollector(FeatureBuilder foibuilder) {
        this(foibuilder, CkanMapping.loadCkanMapping());
    }

    public TrackPointCollector(FeatureBuilder foiBuilder, CkanMapping ckanMapping) {
        this.trackPointsByTrackId = new HashMap<>();
        this.featureByTrackId = new HashMap<>();
        this.foiBuilder = foiBuilder;
        this.ckanMapping = ckanMapping;
    }

    public TrackPoint newTrackPoint() {
        return new TrackPoint();
    }

    public String addToTrack(TrackPoint trackPoint) {
        String trackId = getTrackId(trackPoint);
        if ( !trackPointsByTrackId.containsKey(trackId)) {
            ArrayList<TrackPoint> points = new ArrayList<TrackPoint>();
            trackPointsByTrackId.put(trackId, points);
        }
        trackPointsByTrackId.get(trackId).add(trackPoint);
        if ( !featureByTrackId.containsKey(trackId)) {
            featureByTrackId.put(trackId, createFeature(trackPoint));
        }
        return trackId;
    }
    public TrackPoint getStart(String trackId) {
        TrackPoint startPoint = null;
        if (trackPointsByTrackId.containsKey(trackId)) {
            for (TrackPoint candidate : trackPointsByTrackId.get(trackId)) {
                if (startPoint == null) {
                    startPoint = candidate;
                    continue;
                }
                if (startPoint.isLaterThan(candidate)) {
                    startPoint = candidate;
                }
            }
        }
        return startPoint;
    }

    public SamplingFeature createFeature(TrackPoint trackPoint) {
        String featureName = trackPoint.getFeatureName();
        String featureIdentifier = featureName != null
                ? getTrackId(trackPoint) + " - " + featureName
                : getTrackId(trackPoint);
        SamplingFeature feature = foiBuilder.createEmptyFeature();
        feature.setIdentifier(featureIdentifier);
        feature.addName(featureName);
        return feature;
    }

    public String getTrackId(TrackPoint trackPoint) {
        if (trackPoint.hasTrackId()) {
            return trackPoint.getTrackId();
        } else {
            String path = "/strategy/mobile/track_discriminator";
            JsonNode columns = ckanMapping.getConfigValueAt(path);
            return trackPoint.generateTrackId(columns);
        }
    }

    public static class TrackPoint {

        private final Map<ResourceField, String> valuesByField;

        private String featureName;

        private Geometry geometry;

        public TrackPoint() {
            this.valuesByField = new HashMap<>();
        }

        public TrackPoint withProperty(ResourceField field, String value) {
            valuesByField.put(field, value);
            return this;
        }

        public TrackPoint withGeometry(Geometry geometry) {
            this.geometry = geometry;
            return this;
        }

        public String getFeatureName() {
            String fieldId = CkanConstants.KnownFieldIdValue.TRACK_POINT;
            return getValue(fieldId, null);
        }

        public TimeInstant getTimestamp() {
            String fieldId = CkanConstants.KnownFieldIdValue.OBSERVATION_TIME;
            String defaultValue = new DateTime(0).toString();
            TimeFieldParser parser = new TimeFieldParser();
            String value = getValue(fieldId, defaultValue);
            return parser.parseTimestamp(value, getField(fieldId));
        }

        public String getTimestampAsIso8601String() {
            return getTimestampAsDateTime().toString();
        }

        private DateTime getTimestampAsDateTime() {
            return getTimestamp().getValue();
        }

        private boolean isLaterThan(TrackPoint other) {
            return getTimestampAsDateTime().isAfter(other.getTimestampAsDateTime());
        }

        private String getTrackId() {
            String fieldId = CkanConstants.KnownFieldIdValue.TRACK_ID;
            return getValue(fieldId, getTimestampAsIso8601String());
        }

        public boolean hasTrackId() {
            String fieldId = CkanConstants.KnownFieldIdValue.TRACK_ID;
            return getField(fieldId) != null;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        String generateTrackId(JsonNode columns) {
            String defaultValue = getTimestampAsIso8601String();
            if (columns.isMissingNode()) {
                // TODO obviously this wouldn't result in a track
                return defaultValue;
            }

            StringBuilder sb = new StringBuilder();
            for (JsonNode column : columns) {
                if (sb.length() != 0) {
                    String separator = column.has("separator")
                        ? column.get("separator").asText()
                                : "-";
                    sb.append(separator);
                }

                String discriminatorColumn = column.get("column").asText();
                String value = getValue(discriminatorColumn, defaultValue);
                JsonNode regex = column.get("pattern");
                if (regex == null || regex.isMissingNode()) {
                    sb.append(value);
                } else {
                    Pattern pattern = Pattern.compile(regex.asText());
                    Matcher matcher = pattern.matcher(value);
                    String discriminatorValue = matcher.matches()
                            ? matcher.toMatchResult().group(1)
                            : value;
                    sb.append(discriminatorValue);
                }
            }
            return sb.length() != 0
                    ? sb.toString()
                    : defaultValue;
        }

        public String getValue(String fieldId, String defaultValue) {
            ResourceField field = getField(fieldId);
            return field != null
                    ? valuesByField.get(field)
                    : defaultValue;
        }

        public ResourceField getField(String fieldId) {
            for (Entry<ResourceField, String> entry : valuesByField.entrySet()) {
                if (entry.getKey().isField(fieldId)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((featureName == null) ? 0 : featureName.hashCode());
            result = prime * result + ((geometry == null) ? 0 : geometry.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TrackPoint other = (TrackPoint) obj;
            if (featureName == null) {
                if (other.featureName != null) {
                    return false;
                }
            } else if (!featureName.equals(other.featureName)) {
                return false;
            }
            if (geometry == null) {
                if (other.geometry != null) {
                    return false;
                }
            } else if (!geometry.equals(other.geometry)) {
                return false;
            }
            return true;
        }
    }

}
