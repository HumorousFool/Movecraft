package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatusSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getW();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            Block block = location.toBukkit(world).getBlock();
            if(block.getState() instanceof Sign){
                Sign sign = (Sign) block.getState();
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Status:")) {
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                    sign.update();
                }
            }
        }
    }

    @EventHandler
    public final void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("Status:")) {
            return;
        }
        int fuel=0;
        int totalBlocks=0;
        Map<Material, Integer> foundBlocks = new HashMap<>();
        for (MovecraftLocation ml : craft.getHitBox()) {
            Integer blockID = craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getTypeId();

            if (foundBlocks.containsKey(blockID)) {
                Integer count = foundBlocks.get(blockID);
                if (count == null) {
                    foundBlocks.put(blockID, 1);
                } else {
                    foundBlocks.put(blockID, count + 1);
                }
            } else {
                foundBlocks.put(blockID, 1);
            }

            if (blockType == Material.FURNACE) {
                InventoryHolder inventoryHolder = (InventoryHolder) craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getState();
                for (Material fuelType : Settings.FuelTypes.keySet()){
                    if (!inventoryHolder.getInventory().contains(fuelType)){
                        continue;
                    }
                    ItemStack[] content = inventoryHolder.getInventory().getContents();
                    for (ItemStack item : content){
                        if (item == null)
                            continue;
                        fuel += item.getAmount() * Settings.FuelTypes.get(fuelType);
                    }
                }

            }
            if (blockType != Material.AIR) {
                totalBlocks++;
            }
        }
        int signLine=1;
        int signColumn=0;
        for(List<Integer> alFlyBlockID : craft.getType().getFlyBlocks().keySet()) {
            int flyBlockID=alFlyBlockID.get(0);
            Double minimum=craft.getType().getFlyBlocks().get(alFlyBlockID).get(0);
            if(foundBlocks.containsKey(flyBlockID) && minimum>0) { // if it has a minimum, it should be considered for sinking consideration
                int amount=foundBlocks.get(flyBlockID);
                Double percentPresent=(double) (amount*100/totalBlocks);

                String signText="";
                if(percentPresent>minimum*1.04) {
                    signText+= ChatColor.GREEN;
                } else if(percentPresent>minimum*1.02) {
                    signText+=ChatColor.YELLOW;
                } else {
                    signText+=ChatColor.RED;
                }
                if(flyBlock == Material.REDSTONE_BLOCK) {
                    signText+="R";
                } else if(flyBlock == Material.IRON_BLOCK) {
                    signText+="I";
                } else {
                    signText+= flyBlock.toString().charAt(0);
                }

                signText+=" ";
                signText+=percentPresent.intValue();
                signText+="/";
                signText+=minimum.intValue();
                signText+="  ";
                if(signColumn==0) {
                    event.setLine(signLine,signText);
                    signColumn++;
                } else if(signLine < 3) {
                    String existingLine=event.getLine(signLine);
                    existingLine+=signText;
                    event.setLine(signLine, existingLine);
                    signLine++;
                    signColumn=0;
                }
            }
        }
        String fuelText="";
        int fuelRange=(int) ((fuel*(1+craft.getType().getCruiseSkipBlocks()))/craft.getType().getFuelBurnRate());
        if(fuelRange>1000) {
            fuelText+=ChatColor.GREEN;
        } else if(fuelRange>100) {
            fuelText+=ChatColor.YELLOW;
        } else {
            fuelText+=ChatColor.RED;
        }
        fuelText+="Fuel range:";
        fuelText+=fuelRange;
        event.setLine(signLine,fuelText);
    }
}
