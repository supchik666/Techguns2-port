#!/usr/bin/env python3
"""Copies 1.12 model classes from legacy/ into src/, swapping imports for the ModelRenderer/LegacyGl shim.

Usage: python3 tools/port/port_models.py guns/ModelM4 items/ModelARMagazine ...
(paths under client/models/, without .java). Overwrites the target, so port fixes belong here, not in the copy.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "legacy/src/main/java/techguns/client/models"
DST = ROOT / "src/main/java/techguns/client/models"

# Regexes applied to the legacy text before REPLACE.
RE_REPLACE = [
    # ModelJetPack spawned its flames from the model each frame; TGPlayerClient spawns them each tick instead.
    (r"\n\t\tif \(entityIn!=null && entityIn instanceof EntityPlayer\)\{.*?\n\t\t\}\n",
     "\n\t\t// Flames: TGPlayerClient spawns them each tick for players boosting.\n"),
]

# Applied in order: imports first, then the bare names they bring in.
REPLACE = [
    # ModelGuidedMissileLauncher: the lock-on lamp reads the launcher's client lock state instead of TGExtendedPlayer.
    ("import net.minecraft.entity.player.EntityPlayer;\nimport techguns.capabilities.TGExtendedPlayer;\nimport techguns.client.models.ModelMultipart;\n"
     "import techguns.client.render.TGRenderHelper;\nimport techguns.items.guns.GenericGun;\n",
     "import techguns.client.models.ModelMultipart;\nimport techguns.client.render.TGRenderHelper;\nimport techguns.items.guns.GuidedMissileLauncher;\n"),
    ("\t\t\tif (entityIn != null && entityIn instanceof EntityPlayer) {\n\t\t\t\tEntityPlayer player = (EntityPlayer) entityIn;\n"
     "\t\t\t\tTGExtendedPlayer tge = TGExtendedPlayer.get(player);\n"
     "\t\t\t\tif (tge != null && player.getActiveItemStack() != null && player.getActiveItemStack().getItem() instanceof GenericGun) {\n"
     "\t\t\t\t\tGenericGun gun = (GenericGun)player.getActiveItemStack().getItem();\n"
     "\t\t\t\t\tif (tge.lockOnTicks >= gun.getLockOnTicks()) {\n\t\t\t\t\t\tlocked = true;\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t}\n",
     "\t\t\tlocked = entityIn instanceof net.minecraft.world.entity.player.Player player\n"
     "\t\t\t\t\t&& player.getUseItem().getItem() instanceof GuidedMissileLauncher gun && gun.isLockedOn();\n"),
    # ModelChemLab: the glass parts draw on the translucent layer.
    ("import net.minecraft.client.renderer.GlStateManager.DestFactor;\nimport net.minecraft.client.renderer.GlStateManager.SourceFactor;\n",
     "import techguns.client.models.LegacyGl.RenderType;\n"),
    ("GlStateManager.enableBlend();\n\t    GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);",
     "LegacyGl.enableBlendMode(RenderType.ALPHA);"),
    ("GlStateManager.disableBlend();", "LegacyGl.disableBlendMode(RenderType.ALPHA);"),
    ("import net.minecraft.client.model.ModelRenderer;", "import techguns.client.models.ModelRenderer;"),
    ("import net.minecraft.client.renderer.GlStateManager;", "import techguns.client.models.LegacyGl;"),
    ("import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;", "import net.minecraft.world.item.ItemDisplayContext;"),
    ("import net.minecraft.entity.Entity;", "import net.minecraft.world.entity.Entity;"),
    ("import techguns.client.render.TGRenderHelper;", "import techguns.client.models.LegacyGl;"),
    ("import techguns.client.render.TGRenderHelper.RenderType;", "import techguns.client.models.LegacyGl.RenderType;"),
    ("import net.minecraft.client.model.ModelBase;", "import techguns.client.models.ModelBase;"),
    ("import net.minecraft.client.model.ModelBiped;", "import techguns.client.models.ModelBiped;"),
    ("import net.minecraft.entity.player.EntityPlayer;", "import net.minecraft.world.entity.player.Player;"),
    ("import net.minecraft.init.MobEffects;", "import net.minecraft.world.effect.MobEffects;"),
    ("import techguns.client.ClientProxy;\n", ""),
    ("EntityPlayer", "Player"),
    ("entityIn.isSneaking()", "entityIn.isCrouching()"),
    ("ply.getActivePotionEffect(MobEffects.NIGHT_VISION) != null", "ply.hasEffect(MobEffects.NIGHT_VISION)"),
    # Bloom on the glowing parts (ModelRenderer.glow): the anti-grav core and emitters, the goggles' lenses.
    ("\t\tthis.BackBack.addChild(box2);\n", "\t\tthis.BackBack.addChild(box2);\n\t\tBack3.glow = box1.glow = box2.glow = box3.glow = box4.glow = true;\n"),
    ("\t\tthis.bipedHead.addChild(Left);\n", "\t\tthis.bipedHead.addChild(Left);\n\t\tRight.glow = Left.glow = true;\n"),
    ("import org.lwjgl.opengl.GL11;\n", ""),
    ("import techguns.util.MathUtil;", "import net.minecraft.util.Mth;"),
    ("MathUtil.D2R", "Mth.DEG_TO_RAD"),
    ("GL11.glPushMatrix()", "LegacyGl.pushMatrix()"),
    ("GL11.glPopMatrix()", "LegacyGl.popMatrix()"),
    ("GL11.glScaled(", "LegacyGl.scale("),
    ("TransformType", "ItemDisplayContext"),
    ("GlStateManager.", "LegacyGl."),
    ("TGRenderHelper.", "LegacyGl."),
    # ModelAlienBug, ModelGhastling (M6.2)
    ("import net.minecraft.util.math.MathHelper;", "import net.minecraft.util.Mth;"),
    ("MathHelper.", "Mth."),
    ("import net.minecraft.client.model.ModelGhast;\n", ""),
    ("import net.minecraftforge.fml.relauncher.Side;\n", ""),
    ("import net.minecraftforge.fml.relauncher.SideOnly;\n", ""),
    ("@SideOnly(Side.CLIENT)\n", ""),
]


def main():
    for name in sys.argv[1:]:
        text = (SRC / (name + ".java")).read_text()
        for pattern, new in RE_REPLACE:
            text = re.sub(pattern, lambda m: new, text, flags=re.S)
        for old, new in REPLACE:
            text = text.replace(old, new)
        out = DST / (name + ".java")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(text)
        print(out.relative_to(ROOT))


if __name__ == "__main__":
    main()
