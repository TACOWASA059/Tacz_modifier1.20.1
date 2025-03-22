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

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.client.event.ClientHitMark;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientHitMark.class, remap = false)
public class ClientHitMarkMixin {
    @Inject(method = "onEntityHurt", at = @At("HEAD"), cancellable = true)
    private static void beforeOnEntityHurt(EntityHurtByGunEvent.Post event, CallbackInfo ci) {
        if(event.getAttacker() instanceof Player attacker && event.getHurtEntity() instanceof Player hurtPlayer){
            Team team = attacker.getTeam();
            Team team1 = hurtPlayer.getTeam();
            if(team!=null && team.equals(team1) && !team.isAllowFriendlyFire()){
                ci.cancel();
            }
        }
    }
}
