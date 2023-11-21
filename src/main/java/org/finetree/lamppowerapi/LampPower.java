package org.finetree.lamppowerapi;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.RedstoneWallTorch;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.PressureSensor;
import org.bukkit.material.Redstone;
import redempt.redlib.blockdata.DataBlock;

import java.util.ArrayList;
import java.util.Arrays;


public class LampPower implements Listener {

    public static ArrayList<BlockFace> allFaces = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH));
    static ArrayList<BlockFace> redstoneFaces = new ArrayList<>(Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH));
    static ArrayList<BlockFace> torchFaces = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH));
    static ArrayList<BlockFace> trappedChestFaces = new ArrayList<>(Arrays.asList(BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH));

    //Returns power if it's a redstone component. else returns -1
    public static int isRedstone(BlockState state){

        if ( state instanceof PressureSensor ) {
            return ( (PressureSensor) state.getBlockData() ).isPressed() ? 15 : 0;
        } else if (state instanceof Redstone) {
            return ( (Redstone) state.getBlockData() ).isPowered() ? 15 : 0;
        } else if (state instanceof Powerable) {
            return ( (Powerable) state.getBlockData() ).isPowered() ? 15 : 0;
        } else if (state instanceof AnaloguePowerable) {
            return ( (AnaloguePowerable) state.getBlockData() ).getPower();
        }else{

            //These don't have a getPower method as they are always-on
            //TODO check faces.
            switch (state.getType()) {
                case REDSTONE_TORCH:
                case REDSTONE_WALL_TORCH:
                case REDSTONE_BLOCK:
                    return 15;
            }

            return -1;
        }

    }

    public static ArrayList<Block> getAdjacentLamps(BlockState state) {
        ArrayList<Block> lamps = new ArrayList<>();

        switch(state.getType()) {
            case REDSTONE_WIRE: {
                RedstoneWire wire = (RedstoneWire) state.getBlockData();

                for(BlockFace face : redstoneFaces) { // get all 5 sides and loop through them
                    boolean sideConnected = wire.getFace(face) != RedstoneWire.Connection.NONE; // check if connector != NONE
                    if(sideConnected){
                        addIfLamp(lamps, state, face);
                    }
                }
                //Remember to check down direction separately as redstone dust has no down face
                addIfLamp(lamps, state, BlockFace.DOWN);
                break;
            }
            case OBSERVER:
            case REPEATER:
            case COMPARATOR: {
                Directional repeater = (Directional) state.getBlockData();
                BlockFace curFace = repeater.getFacing().getOppositeFace();
                addIfLamp(lamps, state, curFace);
                break;
            }
            case LECTERN:
            case DETECTOR_RAIL:
            case POLISHED_BLACKSTONE_PRESSURE_PLATE:
            case ACACIA_PRESSURE_PLATE:
            case BIRCH_PRESSURE_PLATE:
            case CRIMSON_PRESSURE_PLATE:
            case STONE_PRESSURE_PLATE:
            case DARK_OAK_PRESSURE_PLATE:
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case JUNGLE_PRESSURE_PLATE:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case OAK_PRESSURE_PLATE:
            case SPRUCE_PRESSURE_PLATE:
            case WARPED_PRESSURE_PLATE:
            case BIRCH_BUTTON:
            case ACACIA_BUTTON:
            case CRIMSON_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case OAK_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
            case SPRUCE_BUTTON:
            case STONE_BUTTON:
            case WARPED_BUTTON:
            case LEVER:
            case REDSTONE_LAMP: // also get all lamps adjacent to this lamp
            case DAYLIGHT_DETECTOR:
            case TARGET:
            case REDSTONE_BLOCK: {
                for(BlockFace face : allFaces){// get all sides
                    addIfLamp(lamps, state, face);
                }
                break;
            }
            case REDSTONE_TORCH: {
                for(BlockFace face : torchFaces){// get all 5 sides
                    addIfLamp(lamps, state, face);
                }
                break;
            }
            case REDSTONE_WALL_TORCH: {
                RedstoneWallTorch torch = (RedstoneWallTorch) state.getBlockData();
                BlockFace curFace = torch.getFacing().getOppositeFace();
                for(BlockFace face : allFaces){// get all sides
                    if(curFace == face){continue;}
                    addIfLamp(lamps, state, face);
                }
                break;
            }
            case TRAPPED_CHEST: {
                for(BlockFace face : trappedChestFaces){// get all 5 sides
                    addIfLamp(lamps, state, face);
                }
                break;
            }
        }

        return lamps;
    }

    private static void addIfLamp(ArrayList<Block> lamps, BlockState state, BlockFace face) {
        Block targ = state.getLocation().getBlock().getRelative(face);
        if (targ.getType() == Material.REDSTONE_LAMP) {
            lamps.add(targ);
        }
    }

    @EventHandler
    public void redstoneEvent(BlockRedstoneEvent e) {
        BlockState redstoneBlock = e.getBlock().getState();
        int newPower = e.getNewCurrent();
        int oldPower = e.getOldCurrent();

        //If nothing changed, do nothing.
        if(newPower == oldPower){return;}

        //Get lamps that this component connects to.
        ArrayList<Block> lamps = getAdjacentLamps(redstoneBlock);

        //Update the lamps found if our redstone is higher power than it.
        for(Block lamp : lamps) {
            int lampPower = getLampPower(lamp);

            //If we are powering the lamp higher, just set it.
            if(newPower > lampPower){
                setLampPower(lamp, newPower);
            }else{ //If we are potentially lowering the lamp power, ask the lamp to recalculate.
                lampRecalculate(lamp);
            }

        }
    }

    public static void lampRecalculate(Block lamp){
        //Init our return Variable at 0 incase nothing is found to be powering the lamp
        int calculatedPower = 0;

        //Check all sides
        for(BlockFace face : allFaces){

            //Get the block on that side and check if it's a redstone component.
            Block component = lamp.getRelative(face);
            int componentPower = isRedstone(component.getState());

            //Found a redstone component as it has power
            if(componentPower != -1){

                //Get the component's connected lamps
                ArrayList<Block> lamps = getAdjacentLamps(component.getState());

                //Are we one of the lamps?
                for(Block toCheck : lamps){
                    //If the locations are the same, it's connected to us.
                    if( toCheck.getLocation().equals( lamp.getLocation() ) ){

                        //If the connected component power is greater, then that's our new max.
                        if(componentPower > calculatedPower){
                            calculatedPower = componentPower;
                        }

                        //End the for-loop as we found self.
                        break;
                    }
                }

            }

        }//Check all nearby components

        //Set final calculated power on lamp.
        setLampPower(lamp, calculatedPower);
    }

    public static void setLampPower(Block lamp, int pwr){
        DataBlock db = LampPowerAPI.getBlockManager().getDataBlock(lamp, true);
        db.set("power", pwr);
    }

    public static int getLampPower(Block lamp){
        DataBlock db = LampPowerAPI.getBlockManager().getDataBlock(lamp, false);

        //This is a regular lamp with no power set
        if(db == null){return 0;}

        //if we have a power var, return it.
        return db.contains("power") ? db.getInt("power") : 0;
    }

    @EventHandler
    public void blockPlaceEvent(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Material t = b.getType();

        //If it's a NC redstone component then we need to manually deal with it.
        drawbridgeChangeComponent(b.getState(), t);
    }

    public static void drawbridgeChangeComponent(BlockState state, Material t){
        if (t == Material.REDSTONE_TORCH || t == Material.REDSTONE_WALL_TORCH || t == Material.REDSTONE_BLOCK) {
            //Recalculate nearby lamps when NC components are placed.
            ArrayList<Block> lamps = getAdjacentLamps(state);
            for (Block lamp : lamps) {
                lampRecalculate(lamp);
            }
        }
    }

}

