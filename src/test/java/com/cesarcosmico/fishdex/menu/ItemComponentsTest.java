package com.cesarcosmico.fishdex.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ItemComponentsTest {

    @Test
    void sameTextureAlwaysGetsTheSameProfileId() {
        assertEquals(ItemComponents.profileId("texture-a"), ItemComponents.profileId("texture-a"));
        assertNotEquals(ItemComponents.profileId("texture-a"), ItemComponents.profileId("texture-b"));
    }
}
