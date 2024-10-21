package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.client.model.ModelDreadQueenSkull;

import com.github.alexthe666.iceandfire.entity.EntityDreadQueenSkull;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.util.ResourceLocation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;

import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;



import javax.annotation.Nullable;

public class RenderDreadQueenSkull extends EntityRenderer<EntityDreadQueenSkull> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire:textures/models/dread/dread_queen_skull.png");
    private static final ModelDreadQueenSkull MODEL_SPRINT = new ModelDreadQueenSkull();

    public RenderDreadQueenSkull() {
        super(Minecraft.getInstance().getRenderManager());
    }

    public void render(EntityDreadQueenSkull entity, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        float f = 0.0625F;
        if(entity.ticksExisted > 3){
            matrixStackIn.push();
            matrixStackIn.scale(1.5F, -1.5F, 1.5F);
            float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            matrixStackIn.translate(0F, 0F, 0F);
            matrixStackIn.rotate(new Quaternion(Vector3f.YP, yaw - 180, true));
            IVertexBuilder ivertexbuilder = ItemRenderer.getBuffer(bufferIn, RenderType.getEyes(TEXTURE), false, false);
            MODEL_SPRINT.render(matrixStackIn, ivertexbuilder, 240, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
        }

        super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private float interpolateValue(float start, float end, float pct) {
        return start + (end - start) * pct;
    }
  
    @Nullable
    @Override
    public ResourceLocation getEntityTexture(EntityDreadQueenSkull entity) {
        return TEXTURE;
    }

}

