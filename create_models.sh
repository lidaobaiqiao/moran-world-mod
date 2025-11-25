#!/bin/bash
# 创建所有缺失的blockstate和model文件

# 创建blockstate文件
for block in peach_brick peach_tile peach_roof_tile white_plaster carved_wood stone_lantern stone_bench tea_table bamboo_planks bamboo_mat cloud_block rainbow_block crystal_block sky_stone; do
  echo '{"variants": {"": {"model": "moran-mod:block/'$block'"}}}' > "src/main/resources/assets/moran-mod/blockstates/$block.json"
  echo '{"parent": "minecraft:block/cube_all", "textures": {"all": "moran-mod:block/'$block'"}}' > "src/main/resources/assets/moran-mod/models/block/$block.json"
  echo '{"parent": "minecraft:item/generated", "textures": {"layer0": "moran-mod:item/'$block'"}}' > "src/main/resources/assets/moran-mod/models/item/$block.json"
done