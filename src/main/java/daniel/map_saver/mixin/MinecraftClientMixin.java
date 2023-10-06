package daniel.map_saver.mixin;

import daniel.map_saver.client.screen.SaveMapScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    public HitResult crosshairTarget;

    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public ClientPlayerEntity player;

    @Inject(method="doItemUse", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/network/ClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void handleItemFrameRightClick(CallbackInfo ci) {
        if (crosshairTarget != null) {
            if (crosshairTarget.getType() == HitResult.Type.ENTITY && ((EntityHitResult) crosshairTarget).getEntity() instanceof ItemFrameEntity itemFrame) {

                ItemStack stack = itemFrame.getHeldItemStack();
                if (this.world != null && this.player != null && this.world.isClient && this.player.isSneaking() && stack.isOf(Items.FILLED_MAP)) {
                    Integer mapId = FilledMapItem.getMapId(stack);
                    MapState mapState = FilledMapItem.getMapState(mapId, this.world);

                    if (mapState != null) {
                        MinecraftClient.getInstance().setScreen(new SaveMapScreen(mapId));
                        ci.cancel();
                    }
                }

            }
        }
    }

}
