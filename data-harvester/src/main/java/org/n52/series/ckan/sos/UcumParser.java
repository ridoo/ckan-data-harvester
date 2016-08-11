package org.n52.series.ckan.sos;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcumParser implements UomParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(UcumParser.class);

    @Override
    public String parse(ResourceField field) {
        String value = field.getOther(CkanConstants.FieldPropertyName.UOM);
        final String unit = parseFromAlias(value);
        return unit != null
            ? unit
            : "";
    }

    protected String parseFromAlias(String uom) {
        try {
            // TODO non valid units from a mapping
            // return StandardUnitDB.instance().get(uom);
            return uom;
        }
        catch (Exception e) {
            LOGGER.error("Could not parse UOM '{}' to known UCUM symbol.", uom, e);
        }
        return null;
    }

}
