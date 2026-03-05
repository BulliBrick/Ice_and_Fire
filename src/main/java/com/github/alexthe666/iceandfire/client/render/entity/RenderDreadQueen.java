package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.client.model.ModelDreadQueen;
import com.github.alexthe666.iceandfire.client.model.util.HideableLayer;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerGenericGlowing;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class RenderDreadQueen extends MobRenderer<EntityDreadQueen, ModelDreadQueen> {
    public static final ResourceLocation TEXTURE_EYES = new ResourceLocation("iceandfire:textures/models/dread/dread_queen_eyes.png");
    public static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire:textures/models/dread/dread_queen.png");
    //TODO: Add damaged inactive textures
    public final HideableLayer<EntityDreadQueen, ModelDreadQueen, ItemInHandLayer<EntityDreadQueen, ModelDreadQueen>> itemLayer;

    public RenderDreadQueen(EntityRendererProvider.Context context) {
        super(context, new ModelDreadQueen(0.0F), 0.6F);
        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
        this.itemLayer = new HideableLayer<>(new ItemInHandLayer<>(this), this);
        this.addLayer(this.itemLayer);
    }

    @Override
    protected void scale(EntityDreadQueen entity, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.95F, 0.95F, 0.95F);
        if (entity.getAnimation() == this.getModel().getSpawnAnimation()) {
            this.itemLayer.hidden = entity.getAnimationTick() <= this.getModel().getSpawnAnimation().getDuration() - 10;
            return;
        }
        this.itemLayer.hidden = false;
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityDreadQueen entity) {
        return TEXTURE;
    }
}
