/*
 * This file is part of MPP.
 * Copyright (c) 2022 by it's authors. All rights reserved.
 * MPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.danielmaile.mpp.util

import de.danielmaile.mpp.item.ArmorMaterial
import de.danielmaile.mpp.item.ItemRegistry
import de.danielmaile.mpp.item.items.Armors
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.FluidCollisionMode
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

fun Player.isGrounded(): Boolean {
    return !this.isFlying && this.velocity.y == 0.0
}

fun Player.getBlockBelow(): Block {
    return location.block.getRelative(BlockFace.DOWN)
}

fun Player.setMaximumHealth(value: Double) {
    getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = value
}

fun Player.addPermEffect(type: PotionEffectType) {
    addPotionEffect(PotionEffect(type, Int.MAX_VALUE, 0, false, false))
}

/**
 * @return nearest object (block or entity) in sight of player or null if none was found.
 */
fun Player.getNearestObjectInSight(range: Int): Any? {
    val block = getTargetBlockExact(range, FluidCollisionMode.NEVER)
    val entity = getTargetEntity(range)

    val blockRange = if (block != null) location.distance(block.location) else Double.MAX_VALUE
    val entityRange = if (entity != null) location.distance(entity.location) else Double.MAX_VALUE

    return if (entity == null && block == null) null else (if (blockRange < entityRange) block else entity)
}

fun Player.getEquippedArmorSet(): ArmorMaterial? {
    val armorItems = this.equipment.armorContents.map { ItemRegistry.getItemFromItemstack(it) }
    if (armorItems.all { it is Armors }) {
        val armorMaterials = armorItems.map { (it as Armors).armorMaterial }
        if (armorMaterials.distinct().size == 1) {
            return armorMaterials[0]
        }
    }
    return null
}

/**
 * @return the block face of the direction the player is facing (only yaw).
 */
fun Player.getDirection(): BlockFace {
    val yaw = this.location.yaw
    return if (yaw > 135 || yaw <= -135) BlockFace.NORTH else if (yaw > -135 && yaw <= -45) BlockFace.EAST else if (yaw > -45 && yaw <= 45) BlockFace.SOUTH else BlockFace.WEST
}

/**
 * Sends all given packets to player.
 */
fun Player.sendPackets(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

fun Player.getPotionEffectLevel(potionEffectType: PotionEffectType): Int {
    val portionEffect = this.getPotionEffect(potionEffectType) ?: return 0
    return portionEffect.amplifier + 1
}
