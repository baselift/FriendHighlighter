package mod.icy_turtle.friendhighlighter.mixins;//package mod.icy_turtle.friendhighlighter.mixins;

import mod.icy_turtle.friendhighlighter.config.FHSettings;
import mod.icy_turtle.friendhighlighter.config.FriendsListHandler;
import mod.icy_turtle.friendhighlighter.util.FHUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>
{
    // stores a variable for use in the enhanced nametag mixin
    private Entity currentEntity;

    private Entity getEntityFromEntityRenderState(EntityRenderState state) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return state.entityType.create(world, null);
    }

    @Inject(method = "renderLabelIfPresent", at = @At(value = "HEAD"))
    private void captureEntity(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.currentEntity = getEntityFromEntityRenderState(state);
    }

    //  to override whether the entities name tag should be rendered (ei. when far away).
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;displayName:Lnet/minecraft/text/Text;", opcode = Opcodes.GETFIELD, ordinal = 0))
    public Text renderNameTag(EntityRenderState state) {
        if(FriendsListHandler.shouldRenderNametag(getEntityFromEntityRenderState(state)))
        {
            // Just return something non-null so the if statement is entered into
            return MutableText.of(new PlainTextContent.Literal("Non-null"));
        }
        return state.displayName;
    }

    //  to override the color the name tag should be rendered in, using its display name
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;displayName:Lnet/minecraft/text/Text;", opcode = Opcodes.GETFIELD, ordinal = 1))
    private Text forceNameColor(EntityRenderState state)
    {
        Entity entity = getEntityFromEntityRenderState(state);
        var friend = FriendsListHandler.getFriendFromEntity(entity);
        if(FriendsListHandler.shouldHighlightEntity(entity))
        {
            return FHUtils.getBoldAndColored(entity.getDisplayName().getString(), friend.getColor());
        }
        return entity.getDisplayName();
    }

    // forces nametag overlay to be transparent and forces nametag visibilty
    @ModifyArgs(method = "renderLabelIfPresent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I"))
    private void modifyNametagRendering(Args args) {
        if(currentEntity == null)
        {
            return;
        }
        if (FriendsListHandler.shouldHighlightEntity(currentEntity))
        {
            if(FHSettings.getSettings().enhancedNametags)
            {
                args.set(3, 0xFFFFFFFF);
                // enlarging nametag text
                //           args.set(5, ((Matrix4f) args.get(5)).scale(5,5,5));
            }
        }
    }

    // renders nametag while sneaking
    @Redirect(method = "renderLabelIfPresent", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;sneaking:Z"))
    private boolean redirectIsSneaky(EntityRenderState instance) {
        Entity entity = getEntityFromEntityRenderState(instance);
        if(FriendsListHandler.shouldHighlightEntity(entity))
        {
            return false;
        }
        return entity.isSneaky();
    }
}