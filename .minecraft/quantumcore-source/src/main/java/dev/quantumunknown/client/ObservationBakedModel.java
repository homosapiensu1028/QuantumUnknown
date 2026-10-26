package dev.quantumunknown.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dynamically renders a shared mystery model until the registry object is observed.
 * It handles both the legacy and NeoForge model-data rendering paths.
 */
public final class ObservationBakedModel implements BakedModel {
    private final BakedModel original;
    private final BakedModel unknown;
    private final ResourceLocation registryId;
    private final boolean blockModel;
    private final ItemOverrides maskedItemOverrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!ClientKnowledge.isItemObserved(registryId)) return ObservationBakedModel.this;
            BakedModel resolved = original.getOverrides().resolve(original, stack, level, entity, seed);
            return resolved == null ? original : resolved;
        }
    };

    public ObservationBakedModel(BakedModel original, BakedModel unknown, ResourceLocation registryId, boolean blockModel) {
        this.original = original;
        this.unknown = unknown;
        this.registryId = registryId;
        this.blockModel = blockModel;
    }

    private boolean isUnknown() {
        return blockModel
            ? !ClientKnowledge.isBlockObserved(registryId)
            : !ClientKnowledge.isItemObserved(registryId);
    }

    private BakedModel activeModel() {
        return isUnknown() ? unknown : original;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return activeModel().getQuads(state, side, random);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        return activeModel().getQuads(state, side, random, data, renderType);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (blockModel && isUnknown()) return ModelData.EMPTY;
        return original.getModelData(level, pos, state, modelData);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return activeModel().getRenderTypes(state, random, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return activeModel().getRenderTypes(stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return isUnknown() ? List.of(this) : original.getRenderPasses(stack, fabulous);
    }

    @Override public boolean useAmbientOcclusion() { return activeModel().useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return activeModel().isGui3d(); }
    @Override public boolean usesBlockLight() { return activeModel().usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return !isUnknown() && original.isCustomRenderer(); }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return activeModel().getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return activeModel().getParticleIcon(data);
    }

    @Override public ItemTransforms getTransforms() { return activeModel().getTransforms(); }
    @Override public ItemOverrides getOverrides() { return blockModel ? original.getOverrides() : maskedItemOverrides; }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        return activeModel().applyTransform(transformType, poseStack, applyLeftHandTransform);
    }
}
