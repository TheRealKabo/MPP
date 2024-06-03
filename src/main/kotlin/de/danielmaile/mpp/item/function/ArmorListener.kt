/*
 * This file is part of MPP.
 * Copyright (c) 2023 by it's authors. All rights reserved.
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

package de.danielmaile.mpp.item.function

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import de.danielmaile.mpp.item.ArmorMaterial
import de.danielmaile.mpp.item.ItemRegistry
import de.danielmaile.mpp.item.items.Armors
import de.danielmaile.mpp.util.getEquippedArmorSet
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.ItemMeta

class ArmorListener : Listener {

    // FIXME: Is this event called on dropper armor equip?
    // This method is planned for subsequent updates
    @EventHandler
    fun playerEquipArmor(event: PlayerArmorChangeEvent) {
        val armorPiece = event.newItem
        val armorPieceItem = ItemRegistry.getItemFromItemstack(armorPiece) ?: return

        if (armorPieceItem is Armors) {
            val armorMaterial = armorPieceItem.armorMaterial
            val armorProtection = armorMaterial.armor

            val armor: Int = when (armorPieceItem.slot) {
                EquipmentSlot.HEAD -> armorProtection.helmet
                EquipmentSlot.CHEST -> armorProtection.chestPlate
                EquipmentSlot.LEGS -> armorProtection.leggings
                EquipmentSlot.FEET -> armorProtection.boots
                else -> return
            }

            val armorToughness = armorMaterial.armorToughness
            val knockBackResistance = armorMaterial.knockbackResistance

            val itemMeta = armorPiece.itemMeta
            itemMeta.setAttributeModifier(Attribute.GENERIC_ARMOR, armor.toDouble())
            itemMeta.setAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, armorToughness.toDouble())
            itemMeta.setAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockBackResistance.toDouble())

            armorPiece.itemMeta = itemMeta
        }
    }

    @EventHandler
    fun onArrowHit(event: ProjectileHitEvent) {
        if (event.entity !is Arrow) return
        val arrow = event.entity as Arrow

        if (event.hitEntity !is Player) return
        val player = event.hitEntity as Player

        val equippedSet = player.getEquippedArmorSet()

        if (equippedSet == ArmorMaterial.ZANITE || equippedSet == ArmorMaterial.GRAVITITE) {
            arrow.velocity = arrow.velocity.normalize().multiply(-0.1)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return
        if ((event.entity as Player).getEquippedArmorSet() == ArmorMaterial.GRAVITITE) {
            event.isCancelled = true
        }
    }

    fun ItemMeta.setAttributeModifier(attribute: Attribute, value: Double) {
        removeAttributeModifier(attribute)
        addAttributeModifier(attribute, AttributeModifier(attribute.key.key, value, AttributeModifier.Operation.ADD_NUMBER))
    }
}
