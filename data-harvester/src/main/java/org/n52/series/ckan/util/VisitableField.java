package org.n52.series.ckan.util;

public interface VisitableField {

    public <T> void accept(FieldVisitor<T> visitor, String value);
}
