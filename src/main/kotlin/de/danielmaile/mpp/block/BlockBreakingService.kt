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
 *
 * This file contains (modified) code from others, which is licensed
 * under the GNU Lesser General Public License version 3:
 *
 * Copyright (c) 2023 xenodevs
 * Source: https://github.com/xenondevs/Nova
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.danielmaile.mpp.block

import de.danielmaile.mpp.block.utils.calculateBlockDamage
import de.danielmaile.mpp.inst
import de.danielmaile.mpp.item.ItemRegistry
import de.danielmaile.mpp.item.items.Tools
import de.danielmaile.mpp.packet.PacketHandler
import de.danielmaile.mpp.packet.PacketListener
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

private val MINING_FATIGUE = MobEffectInstance(MobEffects.DIG_SLOWDOWN, Integer.MAX_VALUE, 255, false, false, false)

object BlockBreakingService {
    private val damagedBlocks: HashMap<Player, DamagedBlock> = HashMap()

    @PacketListener(ignoreCancelled = true)
    fun onBlockDig(event: de.danielmaile.mpp.packet.PacketEvent<ServerboundPlayerActionPacket>) {
        val blockPos = event.packet.pos

        val player = event.player

        when (event.packet.action) {
            Action.START_DESTROY_BLOCK -> {
                val block = player.world.getBlockAt(blockPos.x, blockPos.y, blockPos.z)
                val blockType = BlockType.fromBlock(block)

                val itemStack = player.inventory.itemInMainHand
                val item = ItemRegistry.getItemFromItemstack(itemStack)

                if (blockType == null && (item == null || item !is Tools)) {
                    return
                }

                damageBlock(block, blockType, player)
            }

            Action.STOP_DESTROY_BLOCK, Action.ABORT_DESTROY_BLOCK -> {
                stopDamagingBlock(player)
            }

            else -> {
                return
            }
        }
    }

    fun initializeBreakingScheduler() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(inst(), {
            val iterator = damagedBlocks.entries.iterator()
            while (iterator.hasNext()) {
                val (player, block) = iterator.next()
                block.tick(player.calculateBlockDamage(block))
                if (block.isBroken) {
                    block.breakBlock(player.inventory.itemInMainHand)
                    damageTool(player)
                    stopDamagingBlock(player)
                }
            }
        }, 1L, 1L)
    }

    fun damageTool(player: Player) {
        val tool = player.inventory.itemInMainHand
        val mppTool = ItemRegistry.getItemFromItemstack(tool)

        if (tool.itemMeta !is Damageable) return

        val durabilityLvl = tool.getEnchantmentLevel(Enchantment.DURABILITY)
        if (Random.nextInt(0, durabilityLvl + 1) != 0) return

        if (mppTool is Tools) {
            mppTool.damage(tool)
            return
        }

        tool.apply {
            itemMeta = (itemMeta as Damageable).apply {
                this.damage += 1
            }
        }
    }

    fun damageBlock(block: Block, blockType: BlockType?, player: Player) {
        // break block instantly if player is in creative mode
        if (player.gameMode == GameMode.CREATIVE) {
            Bukkit.getScheduler().runTask(
                inst()
            ) { -> block.type = Material.AIR }
            return
        }

        player.disableBreakingAnimation()

        damagedBlocks.getOrPut(player) {
            DamagedBlock(
                block,
                blockType
            )
        }
    }

    private fun stopDamagingBlock(player: Player) {
        damagedBlocks.remove(player)?.stop() ?: return
        player.enableBreakingAnimation()
    }

    fun Player.disableBreakingAnimation() {
        val effect = this.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        val packet = if (effect != null) {
            // the player might actually have mining fatigue.
            // in this case, it is important to copy the hasIcon value to prevent it from disappearing.
            val effectInstance = MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                Int.MAX_VALUE,
                255,
                effect.isAmbient,
                effect.hasParticles(),
                effect.hasIcon()
            )
            ClientboundUpdateMobEffectPacket(this.entityId, effectInstance)
        } else {
            // the player does not have mining fatigue, we can use the default effect instance
            ClientboundUpdateMobEffectPacket(this.entityId, MINING_FATIGUE)
        }

        PacketHandler.sendPacket(this, packet)
    }

    fun Player.enableBreakingAnimation() {
        val effect = this.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        val packet = if (effect != null) {
            // if the player actually has mining fatigue, send the correct effect again
            val effectInstance = MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                effect.duration,
                effect.amplifier,
                effect.isAmbient,
                effect.hasParticles(),
                effect.hasIcon()
            )
            ClientboundUpdateMobEffectPacket(this.entityId, effectInstance)
        } else {
            // remove the effect
            ClientboundRemoveMobEffectPacket(this.entityId, MobEffects.DIG_SLOWDOWN)
        }

        PacketHandler.sendPacket(this, packet)
    }
}
