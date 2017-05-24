package org.n52.series.ckan.util;

import org.n52.series.ckan.beans.ResourceField;

public abstract class ObservationFieldVisitor<T> implements FieldVisitor<T> {

    @Override
    public void visit(ResourceField field, String value) {
        if ( !field.isObservationField()) {
            // currently visit observation fields only
            return;
        } else {
            visitObservationField(field, value);
        }
    }

    public abstract void visitObservationField(ResourceField field, String value);

}
