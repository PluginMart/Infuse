package com.catadmirer.infuseSMP.util;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.Invis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PacketEventsUtil extends PacketListenerAbstract {

    public static void onLoad(Infuse plugin) {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
        PacketEvents.getAPI().load();
    }

    public static void init() {
        PacketEvents.getAPI().init();

        // Registering Invis armor hider listener
        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsUtil());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacketType().equals(PacketType.Play.Server.ENTITY_EQUIPMENT))) return;

        final WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
        final Player receiver = event.getPlayer();

        final Entity entity = SpigotReflectionUtil.getEntityById(wrapper.getEntityId());
        if (!(entity instanceof final Player target) || receiver.equals(target)) return;
        if (!(Invis.getVanishedPlayers().contains(target))) return;

        final List<Equipment> modified = new ArrayList<>();
        for (Equipment equipment : wrapper.getEquipment()) {
            final EquipmentSlot slot = equipment.getSlot();

            if (slot.equals(EquipmentSlot.HELMET) || slot.equals(EquipmentSlot.CHEST_PLATE) || slot.equals(EquipmentSlot.LEGGINGS) || slot.equals(EquipmentSlot.BOOTS)) {
                modified.add(new Equipment(slot, ItemStack.builder().type(ItemTypes.AIR).build()));
            } else {
                modified.add(equipment);
            }
        }

        wrapper.setEquipment(modified);
        event.markForReEncode(true);
    }

    public static void showArmor(Player player) {
        final List<Equipment> equipment = new ArrayList<>();

        equipment.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getHelmet())));
        equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getChestplate())));
        equipment.add(new Equipment(EquipmentSlot.LEGGINGS, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getLeggings())));
        equipment.add(new Equipment(EquipmentSlot.BOOTS, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getBoots())));

        final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(player.getEntityId(), equipment);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player) || !(player.getWorld().equals(other.getWorld()))) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(other, packet);
        }
    }

    public static void hideArmor(Player player) {
        final List<Equipment> equipment = new ArrayList<>();
        final ItemStack air = ItemStack.builder().type(ItemTypes.AIR).build();

        equipment.add(new Equipment(EquipmentSlot.HELMET, air));
        equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, air));
        equipment.add(new Equipment(EquipmentSlot.LEGGINGS, air));
        equipment.add(new Equipment(EquipmentSlot.BOOTS, air));

        final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(player.getEntityId(), equipment);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player) || !(player.getWorld().equals(other.getWorld()))) continue;

            PacketEvents.getAPI().getPlayerManager().sendPacket(other, packet);
        }
    }

}
