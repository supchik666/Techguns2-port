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
    ("import net.minecraft.client.model.ModelRenderer;", "import techguns.client.models.ModelRenderer;"),
    ("import net.minecraft.client.renderer.GlStateManager;", "import techguns.client.models.LegacyGl;"),
    ("import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;", "import net.minecraft.world.item.ItemDisplayContext;"),
    ("import net.minecraft.entity.Entity;", "import net.minecraft.world.entity.Entity;"),
    ("import techguns.client.render.TGRenderHelper;", "import techguns.client.models.LegacyGl;"),
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
