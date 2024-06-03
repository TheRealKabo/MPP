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

package de.danielmaile.mpp.block.utils

import de.danielmaile.mpp.block.BlockType
import de.danielmaile.mpp.block.DamagedBlock
import de.danielmaile.mpp.item.ItemRegistry
import de.danielmaile.mpp.item.items.Tools
import de.danielmaile.mpp.item.items.Tools.ToolType
import de.danielmaile.mpp.util.getPotionEffectLevel
import de.danielmaile.mpp.util.isGrounded
import net.minecraft.tags.TagKey
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.DiggerItem
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.PickaxeItem
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import kotlin.math.min
import kotlin.math.pow
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

val Block.nms: BlockState
    get() = (this as CraftBlock).nms

fun Block.isToolRequired(): Boolean {
    val nmsBlock = this.nms
    return !nmsBlock.canBeReplaced() && nmsBlock.requiresCorrectToolForDrops()
}

val Block.hardness: Float
    get() = nms.destroySpeed

val ItemStack.nmsItem: Item
    get() = CraftItemStack.asNMSCopy(this).item

val ItemStack.blocks: TagKey<net.minecraft.world.level.block.Block>?
    get() {
        val nmsItem = this.nmsItem
        if (nmsItem !is DiggerItem) return null

        val blocks = DiggerItem::class.declaredMemberProperties.firstOrNull { it.name == "a" } ?: return null
        blocks.isAccessible = true

        return try {
            blocks.getter.call(nmsItem) as? TagKey<net.minecraft.world.level.block.Block>?
        } catch (e: ClassCastException) {
            null
        }
    }

fun ItemStack.isToolTypeCorrect(blockType: BlockType): Boolean {
    val item = nmsItem
    if (item !is DiggerItem) return blockType.tier == 0

    return when (blockType.toolType) {
        ToolType.SHOVEL -> item is ShovelItem
        ToolType.PICKAXE -> item is PickaxeItem
        ToolType.AXE -> item is AxeItem
        ToolType.HOE -> item is HoeItem
    } || (item.tier.level == 0)
}

fun ItemStack.isTierEnough(blockType: BlockType): Boolean {
    val item = nmsItem
    if (item !is DiggerItem) return blockType.tier == 0

    val i = item.tier.level
    return (i >= blockType.tier)
}

fun ItemStack.isToolCorrect(blockType: BlockType): Boolean {
    return isToolTypeCorrect(blockType) && isTierEnough(blockType)
}

fun ItemStack.tierDestroyDamage(): Float {
    val item = nmsItem
    if (item !is DiggerItem) return 1f
    return item.tier.speed
}

fun Player.calculateBlockDamage(damagedBlock: DamagedBlock): Float {
    val itemStack = this.inventory.itemInMainHand
    val item = ItemRegistry.getItemFromItemstack(itemStack)

    val blockType = damagedBlock.blockType
    val block = damagedBlock.block

    if (blockType == null && (item == null || item !is Tools)) {
        return 1f // Instant break as a damaged block was created unintentionally
    }

    // BEST TOOL: Tool affects block but tier is not necessarily enough
    val bestTool: Boolean

    // CAN HARVEST: Tool and tier are correct for breaking the block
    val canHarvest: Boolean

    var speedMultiplier = 1f

    when (item) {
        null -> {
            canHarvest = itemStack.isToolCorrect(blockType!!) || !block.isToolRequired()
            bestTool = itemStack.isToolTypeCorrect(blockType)
            if (bestTool) speedMultiplier = itemStack.tierDestroyDamage()
        }

        is Tools -> {
            canHarvest = item.isToolCorrect(block) || !block.isToolRequired()
            bestTool = item.isToolTypeCorrect(block)
            if (bestTool) speedMultiplier = item.toolTier.miningSpeed
        }

        else -> {
            canHarvest = !block.isToolRequired()
            bestTool = false
        }
    }

    if (!canHarvest) speedMultiplier = 1f

    // + EFFICIENCY (IF BEST TOOL & EFFICIENCY)
    val efficiencyLevel = itemStack.getEnchantmentLevel(Enchantment.DIG_SPEED)
    if (bestTool && efficiencyLevel > 0) speedMultiplier += (1 + efficiencyLevel * efficiencyLevel)

    // * HASTE (IF ANY)
    val hasteLevel = this.getPotionEffectLevel(PotionEffectType.FAST_DIGGING)
    if (hasteLevel > 0) speedMultiplier *= 0.2f * hasteLevel + 1

    // * FATIGUE (IF ANY)
    val fatigueLevel = this.getPotionEffectLevel(PotionEffectType.SLOW_DIGGING)
    if (fatigueLevel > 0) speedMultiplier *= 0.3.pow(min(fatigueLevel, 4)).toFloat()

    // PLAYER IN WATER -> /5
    if (this.isInWater) {
        val helmet = this.inventory.helmet
        if ((helmet?.getEnchantmentLevel(Enchantment.WATER_WORKER) ?: 0) < 1) speedMultiplier /= 5
    }

    // PLAYER NOT ON GROUND -> /5
    if (!this.isGrounded()) speedMultiplier /= 5

    return speedMultiplier / (if (canHarvest) 30 else 100)
}
