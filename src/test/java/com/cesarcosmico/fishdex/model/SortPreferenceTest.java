package com.cesarcosmico.fishdex.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortPreferenceTest {

    @Test
    void roundTripsCriterionAndGrouping() {
        SortPreference pref = new SortPreference(SortMode.NAME, DiscoveryGrouping.UNDISCOVERED_FIRST);
        assertEquals(pref, SortPreference.parse(pref.serialize()));
    }

    @Test
    void legacyCriterionOnlyValueDefaultsToMixed() {
        assertEquals(new SortPreference(SortMode.NAME, DiscoveryGrouping.MIXED), SortPreference.parse("name"));
    }

    @Test
    void unknownTokensFallBack() {
        assertEquals(new SortPreference(SortMode.ENGINE, DiscoveryGrouping.DISCOVERED_FIRST),
                SortPreference.parse("nope;discovered-first"));
    }
}
