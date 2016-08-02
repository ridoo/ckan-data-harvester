package org.n52.series.ckan.beans;

import static org.hamcrest.Matchers.is;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class SchemaDescriptorTest {

    @Test
    public void when_simpleCreation_then_noExceptions() {
        new SchemaDescriptor();
    }
    
    @Test
    public void when_relateWithNullDataFiles_then_emptyResult() {
        SchemaDescriptor trivialDescriptor = new SchemaDescriptor();
        MatcherAssert.assertThat(trivialDescriptor.relateWithDataFiles(null).size(), is(0));
    }
}
