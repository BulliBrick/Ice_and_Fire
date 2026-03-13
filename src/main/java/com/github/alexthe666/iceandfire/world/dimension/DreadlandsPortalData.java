package com.github.alexthe666.iceandfire.world.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * Persistent world data tracking the state of all dread portals in the Dreadlands dimension.
 *
 * When a boss fight is triggered, portals are globally deactivated.
 * When the boss is defeated, they reactivate.
 *
 * This is stored per-dimension in the Dreadlands level's data folder.
 */
public class DreadlandsPortalData extends SavedData {

    private static final String DATA_NAME = "iceandfire_dreadlands_portals";
    private static final String TAG_PORTALS_ACTIVE = "PortalsActive";
    private static final String TAG_BOSS_FIGHT_ACTIVE = "BossFightActive";

    private boolean portalsActive = true;
    private boolean bossFightActive = false;

    public DreadlandsPortalData() {
    }

    public static DreadlandsPortalData load(CompoundTag tag) {
        DreadlandsPortalData data = new DreadlandsPortalData();
        data.portalsActive = tag.getBoolean(TAG_PORTALS_ACTIVE);
        data.bossFightActive = tag.getBoolean(TAG_BOSS_FIGHT_ACTIVE);
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.putBoolean(TAG_PORTALS_ACTIVE, portalsActive);
        tag.putBoolean(TAG_BOSS_FIGHT_ACTIVE, bossFightActive);
        return tag;
    }

    public boolean arePortalsActive() {
        return portalsActive;
    }

    public boolean isBossFightActive() {
        return bossFightActive;
    }

    /**
     * Called when the Dread Queen boss fight is triggered.
     * Deactivates all portals in the dimension.
     */
    public void onBossFightStart() {
        this.portalsActive = false;
        this.bossFightActive = true;
        this.setDirty();
    }

    /**
     * Called when the Dread Queen is defeated.
     * Reactivates all portals in the dimension.
     */
    public void onBossFightEnd() {
        this.portalsActive = true;
        this.bossFightActive = false;
        this.setDirty();
    }

    /**
     * Gets the portal data for the Dreadlands dimension.
     * Creates it if it doesn't exist yet.
     */
    public static DreadlandsPortalData get(ServerLevel dreadlandsLevel) {
        return dreadlandsLevel.getDataStorage().computeIfAbsent(
                DreadlandsPortalData::load,
                DreadlandsPortalData::new,
                DATA_NAME
        );
    }
}