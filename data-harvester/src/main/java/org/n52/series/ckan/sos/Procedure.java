package org.n52.series.ckan.sos;

import java.util.ArrayList;
import java.util.List;

import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;

public class Procedure {

    private String id;
    
    private String longName;

    Procedure(String id, String longName) {
        this.id = id;
        this.longName = longName;
    }
    
    public String getId() {
        return id;
    }

    public String getLongName() {
        return longName;
    }

    List<SmlIdentifier> createIdentifierList() {
        List<SmlIdentifier> idents = new ArrayList<>();
        SmlIdentifier uniqueId = new SmlIdentifier(
                OGCConstants.UNIQUE_ID,
                OGCConstants.URN_UNIQUE_IDENTIFIER,
                id);
        idents.add(uniqueId);
        
        if (longName != null) {
            SmlIdentifier longName = new SmlIdentifier(
                    "longName",
                    "urn:ogc:def:identifier:OGC:1.0:longName",
                    this.longName);
            idents.add(longName);
        }
        return idents;
    }
    
}
