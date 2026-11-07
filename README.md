# Builder's Tool Palette

A color picker and a gradient helper for finding blocks by color. It works in creative and in survival, on any server, because it is client side only.

## Opening it

- Click the two small buttons in your inventory: beside the recipe book in survival, and on the left of the creative inventory.
- Or set the "Open Color Picker" and "Open Gradient Helper" keys under Controls (they start unbound).

Both tools share one screen. A searchable list of every block, sorted by color, sits on the right, and your hotbar sits at the bottom.

## Color Picker

- Pick a color with the saturation/brightness square and the hue bar, the R G B and H S V sliders, or a hex code.
- The eyedropper slot takes the color of any block or item you put in it.
- "Similar blocks" shows the 24 blocks closest to that color.

## Gradient Helper

- Put blocks in two or more slots of the top row. The slots between them fill with the blocks that best match a smooth blend of their colors.
- Scroll over a result, or click its arrows, to step through the next closest matches.
- "Flip" reverses the row and "Clear" empties it.

## Filters

The F, S, O and T buttons narrow the suggestions in both tools:

- **Full Cube:** only blocks that fill their whole space
- **Solid:** only blocks you can't walk through
- **Opaque:** no see-through blocks
- **Same Texture:** only blocks that look the same on every side

## Taking blocks

Click any block to pick it up, then click a slot to put it there (right-click drops it).

- **Creative:** put blocks on your hotbar, shift-click to send one to the first free slot, press 1-9 over a block, or use "Copy to Hotbar" for a whole gradient.
- **Survival:** the screens show you which blocks to use but never move or create items. Blocks can still go into the eyedropper and the gradient row.

## How colors are worked out

Each block's color is the average of the textures on its model, weighted by how much of the block each face covers, with grass and leaf tints included. Colors are compared and blended in OKLab, so matches and gradients follow how bright and colorful blocks look to the eye. Resource packs change the colors to match.

## Downloads

- Loaders: Fabric (with Fabric API) and NeoForge
- Environment: client only

Each download covers a range of Minecraft versions, named in the file (for example `builders-tool-palette-fabric-mc26.1-26.3`):

| Fabric | NeoForge |
| --- | --- |
| 26.1 – 26.3 | 26.1 – 26.3 |

## Credits

Inspired by the Color Picker and Gradient Helper in [Axiom](https://modrinth.com/mod/axiom) by Moulberry. This mod is a separate project and contains none of Axiom's code.

## License

Licensed under [CC BY-NC 4.0](LICENSE.txt). You may use, modify and share the mod, including in modpacks, as long as it stays free of charge and you credit yeahimliam.
