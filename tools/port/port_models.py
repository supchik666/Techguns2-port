#!/usr/bin/env python3
"""Copies 1.12 model classes from legacy/ into src/, swapping imports for the ModelRenderer/LegacyGl shim.

Usage: python3 tools/port/port_models.py guns/ModelM4 items/ModelARMagazine ...
(paths under client/models/, without .java). Overwrites the target, so port fixes belong here, not in the copy.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "legacy/src/main/java/techguns/client/models"
DST = ROOT / "src/main/java/techguns/client/models"

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
    ("import net.minecraft.client.model.ModelRenderer;", "import techguns.client.models.ModelRenderer;"),
    ("import net.minecraft.client.renderer.GlStateManager;", "import techguns.client.models.LegacyGl;"),
    ("import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;", "import net.minecraft.world.item.ItemDisplayContext;"),
    ("import net.minecraft.entity.Entity;", "import net.minecraft.world.entity.Entity;"),
    ("import techguns.client.render.TGRenderHelper;", "import techguns.client.models.LegacyGl;"),
    ("import techguns.client.render.TGRenderHelper.RenderType;", "import techguns.client.models.LegacyGl.RenderType;"),
    ("import net.minecraft.client.model.ModelBase;", "import techguns.client.models.ModelBase;"),
    ("import org.lwjgl.opengl.GL11;\n", ""),
    ("import techguns.util.MathUtil;", "import net.minecraft.util.Mth;"),
    ("MathUtil.D2R", "Mth.DEG_TO_RAD"),
    ("GL11.glPushMatrix()", "LegacyGl.pushMatrix()"),
    ("GL11.glPopMatrix()", "LegacyGl.popMatrix()"),
    ("GL11.glScaled(", "LegacyGl.scale("),
    ("TransformType", "ItemDisplayContext"),
    ("GlStateManager.", "LegacyGl."),
    ("TGRenderHelper.", "LegacyGl."),
]


def main():
    for name in sys.argv[1:]:
        text = (SRC / (name + ".java")).read_text()
        for old, new in REPLACE:
            text = text.replace(old, new)
        out = DST / (name + ".java")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(text)
        print(out.relative_to(ROOT))


if __name__ == "__main__":
    main()
