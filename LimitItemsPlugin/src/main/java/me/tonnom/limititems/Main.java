package me.tonnom.limititems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.*;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private final Map<Material, Integer> limits = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::checkAllPlayers, 40, 40);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("limit")) {

            if (args.length != 2) {
                sender.sendMessage("§c/limit <item> <nombre>");
                return true;
            }

            Material mat = Material.matchMaterial(args[0].toUpperCase());
            if (mat == null) {
                sender.sendMessage("§cItem invalide");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (Exception e) {
                sender.sendMessage("§cNombre invalide");
                return true;
            }

            limits.put(mat, amount);
            sender.sendMessage("§aLimite globale pour " + mat + " = " + amount);

            return true;
        }

        return false;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        Material mat = item.getType();

        if (!limits.containsKey(mat)) return;

        if (countTotal(mat) >= limits.get(mat)) {
            event.setCancelled(true);
            event.getItem().remove();
            player.sendMessage("§cLimite atteinte pour " + mat);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {

        ItemStack result = event.getRecipe().getResult();
        Material mat = result.getType();

        if (!limits.containsKey(mat)) return;

        if (countTotal(mat) >= limits.get(mat)) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).sendMessage("§cCraft bloqué (limite atteinte)");
        }
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {

        Material mat = event.getResult().getType();

        if (!limits.containsKey(mat)) return;

        if (countTotal(mat) >= limits.get(mat)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {

        event.getDrops().removeIf(item ->
                limits.containsKey(item.getType()) &&
                countTotal(item.getType()) >= limits.get(item.getType())
        );
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        event.getInventory().forEach(item -> {
            if (item == null) return;

            Material mat = item.getType();

            if (limits.containsKey(mat) && countTotal(mat) >= limits.get(mat)) {
                item.setAmount(0);
            }
        });
    }

    private void checkAllPlayers() {

        for (Player p : Bukkit.getOnlinePlayers()) {

            for (Map.Entry<Material, Integer> entry : limits.entrySet()) {

                Material mat = entry.getKey();
                int max = entry.getValue();

                int total = countTotal(mat);

                if (total > max) {
                    removeExtra(mat, total - max);
                }
            }
        }
    }

    private int countTotal(Material mat) {

        int total = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && item.getType() == mat) {
                    total += item.getAmount();
                }
            }
        }

        return total;
    }

    private void removeExtra(Material mat, int toRemove) {

        for (Player p : Bukkit.getOnlinePlayers()) {

            for (ItemStack item : p.getInventory().getContents()) {

                if (item == null || item.getType() != mat) continue;

                int amount = item.getAmount();

                if (amount <= toRemove) {
                    toRemove -= amount;
                    item.setAmount(0);
                } else {
                    item.setAmount(amount - toRemove);
                    return;
                }

                if (toRemove <= 0) return;
            }
        }
    }
}
