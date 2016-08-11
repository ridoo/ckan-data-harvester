package org.n52.series.ckan.sos;

import org.n52.series.ckan.beans.ResourceField;

public interface UomParser {

    String parse(ResourceField field);
}
