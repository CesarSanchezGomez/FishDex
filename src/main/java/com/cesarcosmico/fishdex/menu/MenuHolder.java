package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.FishDexEntry;
import com.cesarcosmico.fishdex.model.FishDexSummary;
import com.cesarcosmico.fishdex.model.SortMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Marks an inventory as a FishDex menu and carries its state. A collection holds the base
 * (engine-order) entries, paging and the chosen sort; an index holds the aggregate progress and a
 * fixed page count, so navigation and re-sorting re-render without re-querying.
 */
public final class MenuHolder implements InventoryHolder {

    public enum Kind { COLLECTION, INDEX }

    private final Kind kind;
    private final String menuId;
    private final List<FishDexEntry> entries;
    private final int pageSize;
    private final FishDexSummary summary;
    private final int indexPageCount;
    private int page;
    private SortMode sort;
    private DiscoveryGrouping grouping;
    private Inventory inventory;

    private MenuHolder(Kind kind, String menuId, List<FishDexEntry> entries, int pageSize,
                          SortMode sort, DiscoveryGrouping grouping, FishDexSummary summary, int indexPageCount) {
        this.kind = kind;
        this.menuId = menuId;
        this.entries = entries;
        this.pageSize = Math.max(1, pageSize);
        this.sort = sort;
        this.grouping = grouping;
        this.summary = summary;
        this.indexPageCount = Math.max(1, indexPageCount);
    }

    public static MenuHolder collection(String menuId, List<FishDexEntry> entries, int pageSize,
                                           SortMode sort, DiscoveryGrouping grouping) {
        return new MenuHolder(Kind.COLLECTION, menuId, entries, pageSize, sort, grouping, null, 1);
    }

    public static MenuHolder index(String menuId, FishDexSummary summary, int pageCount) {
        return new MenuHolder(Kind.INDEX, menuId, List.of(), 1, SortMode.ENGINE, DiscoveryGrouping.MIXED, summary, pageCount);
    }

    /** The aggregate progress across every collection (index menus only). */
    public FishDexSummary summary() {
        return summary;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    public String menuId() {
        return menuId;
    }

    public List<FishDexEntry> entries() {
        return entries;
    }

    public int page() {
        return page;
    }

    public SortMode sort() {
        return sort;
    }

    public void setSort(SortMode sort) {
        this.sort = sort;
    }

    public DiscoveryGrouping grouping() {
        return grouping;
    }

    public void setGrouping(DiscoveryGrouping grouping) {
        this.grouping = grouping;
    }

    public int pageCount() {
        if (kind == Kind.INDEX) {
            return indexPageCount;
        }
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    public boolean setPage(int newPage) {
        int clamped = Math.max(0, Math.min(newPage, pageCount() - 1));
        if (clamped == page) {
            return false;
        }
        page = clamped;
        return true;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
