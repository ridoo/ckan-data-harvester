package org.n52.series.ckan.beans;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DescriptorVersionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void when_invalid_version_then_throwException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("unparsable");
        thrown.expectMessage("foobar");
        new DescriptorVersion("foobar");
    }

    @Test
    public void when_negative_version_then_throwException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("negative");
        new DescriptorVersion("-1.4");
    }

    @Test
    public void when_valid_version_then_printVersionString() {
        DescriptorVersion version = new DescriptorVersion("1.4");
        MatcherAssert.assertThat(version.toString(), is("1.4"));
    }

    @Test
    public void when_dotPrefixedValid_version_then_printCanonicalVersionString() {
        DescriptorVersion version = new DescriptorVersion(".4");
        MatcherAssert.assertThat(version.toString(), is("0.4"));
    }

    @Test
    public void when_versionGreater_then_greaterOrEquals() {
        DescriptorVersion oneVersion = new DescriptorVersion("0.3");
        DescriptorVersion otherVersion = new DescriptorVersion("0.4");
        MatcherAssert.assertThat(oneVersion.isGreaterOrEquals(otherVersion), is(false));
    }

    @Test
    public void when_versionWithMoreDetails_then_considerMayorAndMinorOnly() {
        DescriptorVersion oneVersion = new DescriptorVersion("0.4.1-alpha.1");
        DescriptorVersion otherVersion = new DescriptorVersion("0.4.1-alpha.2");
        // only minor and mayor parts are considered
        MatcherAssert.assertThat(otherVersion.isGreaterOrEquals(oneVersion), is(true));
    }

    @Test
    public void when_valid_version_then_getMayor() {
        DescriptorVersion version = new DescriptorVersion("01.4");
        MatcherAssert.assertThat(version.getMayor(), is(1));
    }

    @Test
    public void when_valid_version_then_getMinor() {
        DescriptorVersion version = new DescriptorVersion("01.4");
        MatcherAssert.assertThat(version.getMinor(), is(4));
    }

    @Test
    public void when_twoSameVersions_then_compare() {
        DescriptorVersion oneVersion = new DescriptorVersion("01.4");
        DescriptorVersion otherVersion = new DescriptorVersion("1.4");
        MatcherAssert.assertThat(oneVersion.compareTo(otherVersion), is(0));
    }

    @Test
    public void when_differentMajorVersions_then_compare() {
        DescriptorVersion oneVersion = new DescriptorVersion("01.4.5");
        DescriptorVersion otherVersion = new DescriptorVersion("0.4.5");
        MatcherAssert.assertThat(oneVersion.compareTo(otherVersion), greaterThan(0));
    }

    @Test
    public void when_differentMajorVersions_then_greaterThanTest() {
        DescriptorVersion oneVersion = new DescriptorVersion("01.4.5");
        DescriptorVersion otherVersion = new DescriptorVersion("0.4.5");
        MatcherAssert.assertThat(oneVersion.isGreaterOrEquals(otherVersion), is(true));
    }

    @Test
    public void when_inArray_then_sortable() {
        DescriptorVersion[] versions = new DescriptorVersion[] {
                new DescriptorVersion("01.4"),
                new DescriptorVersion("0.4"),
                new DescriptorVersion("0.10"),
                new DescriptorVersion("100.10"),
                new DescriptorVersion("02.010")
        };
        Arrays.sort(versions);
        MatcherAssert.assertThat(Arrays.asList(versions), contains(
                new DescriptorVersion("0.4"),
                new DescriptorVersion("0.10"),
                new DescriptorVersion("1.4"),
                new DescriptorVersion("2.10"),
                new DescriptorVersion("100.10")));
    }
}
