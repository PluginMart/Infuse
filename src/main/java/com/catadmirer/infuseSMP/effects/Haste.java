package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.EffectUnequipEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.ItemUtil;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class Haste extends InfuseEffect {
    private static final NamespacedKey fortuneKey = new NamespacedKey("infuse", "haste_fortune");
    private static final NamespacedKey efficiencyKey = new NamespacedKey("infuse", "haste_efficiency");
    private static final NamespacedKey unbreakingKey = new NamespacedKey("infuse", "haste_unbreaking");

    public Haste() {
        this(false);
    }

    public Haste(boolean augmented) {
        super("haste", EffectIds.HASTE, augmented, EffectConstants.potionColor(EffectIds.HASTE), EffectConstants.ritualColor(EffectIds.HASTE));
    }

    @Override
    public void equip(Player owner) {

    }

    @Override
    public void unequip(Player owner) {

    }

    @Override
    public void applyPassives(Player owner) {
        //todo: Move to PlayerItemHeldEvent listener
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        ItemStack item = owner.getInventory().getItemInMainHand();
        if (ItemUtil.isPickaxe(item) || ItemUtil.isAxe(item) || ItemUtil.isShovel(item) || ItemUtil.isHoe(item)) {
            ItemUtil.applySpecialEnchantment(item, fortuneKey, Enchantment.FORTUNE, plugin.getMainConfig().hasteFortuneLevel());
            ItemUtil.applySpecialEnchantment(item, efficiencyKey, Enchantment.EFFICIENCY, plugin.getMainConfig().hasteEfficiencyLevel());
            ItemUtil.applySpecialEnchantment(item, unbreakingKey, Enchantment.UNBREAKING, plugin.getMainConfig().hasteUnbreakingLevel());
        }
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "haste")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "haste", duration, cooldown);

        owner.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 15, 3));
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Haste();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Haste(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_HASTE_NAME : Message.MessageType.HASTE_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_HASTE_LORE : Message.MessageType.HASTE_LORE);
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void enchantHeldItem(PlayerItemHeldEvent event) {
        Infuse.LOGGER.debug("[Haste] PlayerItemHeldEvent triggered");

        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        Infuse.LOGGER.debug("[Haste] PlayerItemHeldEvent is for an haste user");

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (ItemUtil.isPickaxe(item) || ItemUtil.isAxe(item) || ItemUtil.isShovel(item) || ItemUtil.isHoe(item)) {
            Infuse.LOGGER.debug("[Haste] Haste user is holding a sword/axe/shove/hoe.  Enchanting with fortune, efficiency and unbreaking.");

            ItemUtil.applySpecialEnchantment(item, fortuneKey, Enchantment.FORTUNE, plugin.getMainConfig().hasteFortuneLevel());
            ItemUtil.applySpecialEnchantment(item, efficiencyKey, Enchantment.EFFICIENCY, plugin.getMainConfig().hasteEfficiencyLevel());
            ItemUtil.applySpecialEnchantment(item, unbreakingKey, Enchantment.UNBREAKING, plugin.getMainConfig().hasteUnbreakingLevel());
        }
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(plugin.getDataManager().hasEffect(player, this))) return;
        if (event.getView().getTopInventory().equals(event.getPlayer().getInventory())) return;

        for (ItemStack item : event.getView().getTopInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemUtil.removeSpecialEnchant(item, efficiencyKey, Enchantment.EFFICIENCY);
            ItemUtil.removeSpecialEnchant(item, fortuneKey, Enchantment.FORTUNE);
            ItemUtil.removeSpecialEnchant(item, unbreakingKey, Enchantment.UNBREAKING);
        }
    }

    @EventHandler
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        if (!(plugin.getDataManager().hasEffect(event.getPlayer(), this))) return;

        final ItemStack item = event.getItemDrop().getItemStack();
        ItemUtil.removeSpecialEnchant(item, efficiencyKey, Enchantment.EFFICIENCY);
        ItemUtil.removeSpecialEnchant(item, fortuneKey, Enchantment.FORTUNE);
        ItemUtil.removeSpecialEnchant(item, unbreakingKey, Enchantment.UNBREAKING);
    }

    @EventHandler
    public void onEffectUnequipEvent(EffectUnequipEvent event) {
        if (!(event.getEffect().equals(this))) return;

        for (ItemStack item : event.getPlayer().getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemUtil.removeSpecialEnchant(item, efficiencyKey, Enchantment.EFFICIENCY);
            ItemUtil.removeSpecialEnchant(item, fortuneKey, Enchantment.FORTUNE);
            ItemUtil.removeSpecialEnchant(item, unbreakingKey, Enchantment.UNBREAKING);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity2(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() != Material.SHIELD) return;
        if (!player.isBlocking()) return;
        // TODO: Handle if player blocks with main hand
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!ItemUtil.isAxe(attacker.getInventory().getItemInMainHand())) return;

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.setCooldown(Material.SHIELD, 50), 20L);
    }
}