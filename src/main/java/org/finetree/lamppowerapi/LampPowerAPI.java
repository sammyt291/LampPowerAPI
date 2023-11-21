package org.finetree.lamppowerapi;

import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.blockdata.BlockDataManager;

public final class LampPowerAPI extends JavaPlugin {

    private static LampPowerAPI plugin;
    private static BlockDataManager manager;

    public LampPower lampPower;

    @Override
    public void onEnable() {
        // Plugin startup logic

        //Initialize plugin getter
        plugin = this;

        //Initialize DataBlock Manager from RedLib
        manager = BlockDataManager.createAuto(this, this.getDataFolder().toPath().resolve("blocks.db"), true, true);

        //Initialize our API var.
        lampPower = new LampPower();

        //Listen for events
        getServer().getPluginManager().registerEvents(new LampPower(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //Plugin Getter
    public static LampPowerAPI getPlugin() {
        return plugin;
    }

    public static BlockDataManager getBlockManager() {
        return manager;
    }

}
