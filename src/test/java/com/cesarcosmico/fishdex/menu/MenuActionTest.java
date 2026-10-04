package com.cesarcosmico.fishdex.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuActionTest {

    @Test
    void parsesEveryAction() {
        assertInstanceOf(MenuAction.None.class, MenuAction.parse(null));
        assertInstanceOf(MenuAction.None.class, MenuAction.parse("  "));
        assertInstanceOf(MenuAction.OpenRoot.class, MenuAction.parse("open:root"));
        assertEquals(new MenuAction.Open("rare"), MenuAction.parse("open:rare"));
        assertInstanceOf(MenuAction.PreviousPage.class, MenuAction.parse("previous_page"));
        assertInstanceOf(MenuAction.NextPage.class, MenuAction.parse("NEXT_PAGE"));
        assertInstanceOf(MenuAction.Close.class, MenuAction.parse("close"));
        assertInstanceOf(MenuAction.Sort.class, MenuAction.parse("sort"));
    }

    @Test
    void rejectsTyposAndEmptyTargets() {
        assertThrows(IllegalArgumentException.class, () -> MenuAction.parse("next-page"));
        assertThrows(IllegalArgumentException.class, () -> MenuAction.parse("open:"));
    }

    @Test
    void controlsShowOnEveryPageAndLinksOnlyOnTheirOwn() {
        assertTrue(MenuAction.parse("close").onEveryPageByDefault());
        assertTrue(MenuAction.parse("open:root").onEveryPageByDefault());
        assertFalse(MenuAction.parse("open:rare").onEveryPageByDefault());
        assertFalse(MenuAction.parse("").onEveryPageByDefault());
    }
}
