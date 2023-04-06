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

package de.danielmaile.mpp.item.recipe

import org.bukkit.inventory.ItemStack

abstract class CraftingRecipe : Recipe() {

    class StickRecipe(
        result: ItemStack,
        woodMaterial: ItemStack
    ) : ToolRecipe() {

        private val stickTopLeft = ShapedRecipe(
            result,
            arrayOf(
                woodMaterial, null, null,
                woodMaterial, null, null,
                null, null, null
            )
        ).spigotRecipes

        private val stickTopMiddle = ShapedRecipe(
            result,
            arrayOf(
                null, woodMaterial, null,
                null, woodMaterial, null,
                null, null, null
            )
        ).spigotRecipes

        private val stickTopRight = ShapedRecipe(
            result,
            arrayOf(
                null, null, woodMaterial,
                null, null, woodMaterial,
                null, null, null
            )
        ).spigotRecipes

        private val stickBottomLeft = ShapedRecipe(
            result,
            arrayOf(
                null, null, null,
                woodMaterial, null, null,
                woodMaterial, null, null
            )
        ).spigotRecipes

        private val stickBottomMiddle = ShapedRecipe(
            result,
            arrayOf(
                null, null, null,
                null, woodMaterial, null,
                null, woodMaterial, null
            )
        ).spigotRecipes

        private val stickBottomRight = ShapedRecipe(
            result,
            arrayOf(
                null, null, null,
                null, null, woodMaterial,
                null, null, woodMaterial
            )
        ).spigotRecipes

        override val spigotRecipes: List<org.bukkit.inventory.Recipe>
            get() = arrayListOf(
                stickTopLeft,
                stickTopMiddle,
                stickTopRight,
                stickBottomLeft,
                stickBottomMiddle,
                stickBottomRight
            ).flatten()
    }
}
