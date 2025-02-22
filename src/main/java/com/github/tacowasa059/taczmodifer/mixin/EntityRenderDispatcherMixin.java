package com.github.tacowasa059.taczmodifer.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    public abstract  <T extends Entity> EntityRenderer<? super T> getRenderer(T p_114383_);
    @Inject(method="render",at=@At("HEAD"),cancellable = true)
    public <E extends Entity> void render(E p_114385_, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int p_114393_, CallbackInfo ci) {
        if(p_114385_ instanceof EntityKineticBullet bullet){
            EntityRenderer<? super E> entityrenderer = this.getRenderer(p_114385_);

            try {
                Vec3 vec3 = entityrenderer.getRenderOffset(p_114385_, partialTicks);
                double d2 = x + vec3.x();
                double d3 = y + vec3.y();
                double d0 = z + vec3.z();
                poseStack.pushPose();
                poseStack.translate(d2, d3, d0);
                GunRender(bullet, partialTicks, poseStack, bufferSource, p_114393_);

                poseStack.popPose();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
                p_114385_.fillCrashReportCategory(crashreportcategory);
                CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
                crashreportcategory1.setDetail("Assigned renderer", entityrenderer);
                crashreportcategory1.setDetail("Rotation", yaw);
                crashreportcategory1.setDetail("Delta", partialTicks);
                throw new ReportedException(crashreport);
            }
            ci.cancel();
        }
    }
    @Unique
    private static void GunRender(EntityKineticBullet bullet, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation gunId = bullet.getGunId();
        ResourceLocation gunDisplayId = bullet.getGunDisplayId();
        Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(gunDisplayId, gunId);
        if (display.isEmpty()) {
            return;
        }

        float @Nullable [] tracerColor = bullet.getTracerColorOverride().orElse(display.get().getTracerColor());
        ResourceLocation ammoId = bullet.getAmmoId();
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(ammoIndex -> {
            BedrockAmmoModel ammoEntityModel = ammoIndex.getAmmoEntityModel();
            ResourceLocation textureLocation = ammoIndex.getAmmoEntityTextureLocation();
            if (ammoEntityModel != null && textureLocation != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.pushPose();
                poseStack.translate(0.0, 1.5, 0.0);
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                ammoEntityModel.render(poseStack, ItemDisplayContext.GROUND, RenderType.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }

//                if (bullet.isTracerAmmo()) {
            float[] actualTracerColor = Objects.requireNonNullElse(tracerColor, ammoIndex.getTracerColor());
            renderTracerAmmo(bullet, actualTracerColor, partialTicks, poseStack, packedLight);
//                }

        });
    }
    @Unique
    private static void renderTracerAmmo(EntityKineticBullet bullet, float[] tracerColor, float partialTicks, PoseStack poseStack, int packedLight) {
        EntityBulletRenderer.getModel().ifPresent((model) -> {
            Entity shooter = bullet.getOwner();
            if (shooter != null) {
                poseStack.pushPose();
                float width = 0.005F;
                double trailLength = 0.85 * bullet.getDeltaMovement().length();

                width *= bullet.getTracerSizeOverride() * 1.2;

                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.translate(0.0, -0.2, -trailLength / 2.0 -1f);
                poseStack.scale(width, width, (float)trailLength);

                RenderType type = RenderType.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15.0F, 15.0F);

                model.render(poseStack, ItemDisplayContext.NONE, type, packedLight, OverlayTexture.NO_OVERLAY,
                        tracerColor[0], tracerColor[1], tracerColor[2], 1.0F);

                poseStack.popPose();

            }
        });
    }
}
