package org.n52.series.ckan.sos.matcher;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.n52.sos.ogc.sos.SosProcedureDescription;
import org.n52.sos.response.DescribeSensorResponse;

public class SensorDescriptionMatcher {

    public static Matcher<DescribeSensorResponse> isMobileProcedure(final String procedureId) {
        return new TypeSafeMatcher<DescribeSensorResponse>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("mobile capabilities should return ").appendValue(Boolean.TRUE);
            }
            @Override
            protected void describeMismatchSafely(DescribeSensorResponse item, Description mismatchDescription) {
                mismatchDescription.appendText("was").appendValue(Boolean.FALSE);
            }
            @Override
            protected boolean matchesSafely(DescribeSensorResponse response) {
                List<SosProcedureDescription> descriptions = response.getProcedureDescriptions();
                for (SosProcedureDescription sensorDescription : descriptions) {
                    if (procedureId.equals(sensorDescription.getIdentifier())) {
                        return sensorDescription.getMobile();
                    }
                }
                return false;
            }
        };
    }

    public static Matcher<DescribeSensorResponse> isInsituProcedure(final String procedureId) {
        return new TypeSafeMatcher<DescribeSensorResponse>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("insitu capabilities should return ").appendValue(Boolean.TRUE);
            }
            @Override
            protected void describeMismatchSafely(DescribeSensorResponse item, Description mismatchDescription) {
                mismatchDescription.appendText("was").appendValue(Boolean.FALSE);
            }
            @Override
            protected boolean matchesSafely(DescribeSensorResponse response) {
                List<SosProcedureDescription> descriptions = response.getProcedureDescriptions();
                for (SosProcedureDescription sensorDescription : descriptions) {
                    if (procedureId.equals(sensorDescription.getIdentifier())) {
                        return sensorDescription.getInsitu();
                    }
                }
                return false;
            }
        };
    }
}
