package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.client.model.ModelDreadQueen;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerGenericGlowing;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.util.ResourceLocation;


import javax.annotation.Nullable;

public class RenderDreadQueen extends MobRenderer<EntityDreadQueen, ModelDreadQueen> {
    public static final ResourceLocation TEXTURE_EYES = new ResourceLocation("iceandfire:textures/models/dread/dread_queen_eyes.png");
    public static final ResourceLocation TEXTURE_0 = new ResourceLocation("iceandfire:textures/models/dread/dread_queen.png");

    public RenderDreadQueen(EntityRendererManager renderManager) {
        super(renderManager, new ModelDreadQueen(0.0F), 0.6F);
        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
        this.addLayer(new HeldItemLayer<>(this));
    }

    @Override
    protected void preRenderCallback(EntityDreadQueen entity, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.95F, 0.95F, 0.95F);
    }

    @Nullable
    @Override
    public ResourceLocation getEntityTexture(EntityDreadQueen entity) {

        return TEXTURE_0;
    }
}

