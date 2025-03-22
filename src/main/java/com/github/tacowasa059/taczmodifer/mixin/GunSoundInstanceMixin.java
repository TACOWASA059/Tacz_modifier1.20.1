/*
 * このファイルは [TACZ] (GPL 3.0) を基に改変されています。
 *
 * 改変者: tacowasa_059
 * 改変日: 2025-03-22
 *
 * 本ファイルは GNU General Public License v3.0 (GPL 3.0) に従って配布されます。
 * ライセンスの詳細については `LICENSE` ファイルを参照してください。
 */


package com.github.tacowasa059.taczmodifer.mixin;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.manager.SoundAssetsManager;
import com.tacz.guns.client.sound.GunSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.sound.sampled.AudioFormat;

@Mixin(value = GunSoundInstance.class, remap = false)
public class GunSoundInstanceMixin {
    @Final
    @Shadow
    private ResourceLocation registryName;
    @Final
    @Shadow
    private boolean mono;


    @Inject(method = "getSoundBuffer", at=@At("HEAD"), cancellable = true)
    public void getSoundBuffer(CallbackInfoReturnable<SoundBuffer> cir) {
        if(this.registryName == null) return;

        String nameSpace = registryName.getNamespace();
        String path = registryName.getPath();
        if(ClientAssetsManager.INSTANCE.getSoundBuffers(registryName) != null) return;

        ResourceLocation newName = taczSoundAdjust$processPath(registryName, nameSpace, path);
        if(newName==null) return;
        if(newName.getPath().equalsIgnoreCase(registryName.getPath()))return;



        SoundAssetsManager.SoundData soundData = ClientAssetsManager.INSTANCE.getSoundBuffers(newName);
        if (soundData == null) {
            cir.setReturnValue(null);
            cir.cancel();
        } else {
            AudioFormat rawFormat = soundData.audioFormat();
            if (this.mono && rawFormat.getChannels() > 1) {
                AudioFormat monoFormat = new AudioFormat(rawFormat.getEncoding(), rawFormat.getSampleRate(), rawFormat.getSampleSizeInBits(), 1, rawFormat.getFrameSize(), rawFormat.getFrameRate(), rawFormat.isBigEndian(), rawFormat.properties());
                cir.setReturnValue(new SoundBuffer(soundData.byteBuffer(), monoFormat));
                cir.cancel();
           } else {
                cir.setReturnValue(new SoundBuffer(soundData.byteBuffer(), soundData.audioFormat()));
                cir.cancel();
            }
        }
    }


    @Unique
    private ResourceLocation taczSoundAdjust$processPath(ResourceLocation location, String namespace, String path) {
        if (ClientAssetsManager.INSTANCE.getSoundBuffers(location) != null) {
            return location;
        }

        if (path.endsWith("silence_3p")) {
            return taczSoundAdjust$tryAlternative(namespace, path.replace("silence_3p", "silence"), path.replace("silence_3p", "shoot_3p"),path.replace("silence_3p", "shoot"));
        } else if (path.endsWith("shoot_3p")) {
            return taczSoundAdjust$tryAlternative(namespace, path.replace("shoot_3p", "shoot"));
        } else if (path.endsWith("shoot")) {
            return taczSoundAdjust$tryAlternative(namespace, path.replace("shoot", "shoot_3p"));
        } else if (path.endsWith("silence")){
            return taczSoundAdjust$tryAlternative(namespace, path.replace("silence", "silence_3p"), path.replace("silence", "shoot"),path.replace("silence", "shoot_3p"));
        }

        return location;
    }

    @Unique
    private ResourceLocation taczSoundAdjust$tryAlternative(String namespace, String... alternatives) {
        for (String alt : alternatives) {
            ResourceLocation sound = new ResourceLocation(namespace, alt);
            if (ClientAssetsManager.INSTANCE.getSoundBuffers(sound) != null) {
                return sound;
            }
        }
        return null;
    }
}
