package org.vmstudio.pumpkincarving.core.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.vmstudio.pumpkincarving.core.network.NetworkHelper;

public final class AddonNetworking {
    public static final ResourceLocation CARVE_PUMPKIN_C2S = new ResourceLocation(VisorPumpkinCarving.MOD_ID, "carve_pumpkin");

    private static boolean initialized;

    private AddonNetworking() {
    }

    public static void initCommon() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkHelper.registerServerReceiver(CARVE_PUMPKIN_C2S, (buf, player) -> {
            InteractionHand hand = buf.readEnum(InteractionHand.class);
            BlockPos pos = buf.readBlockPos();
            Direction face = buf.readEnum(Direction.class);

            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > AddonUtils.MAX_PLAYER_TO_BLOCK_DISTANCE_SQR) {
                return;
            }

            ItemStack heldStack = player.getItemInHand(hand);
            if (!(heldStack.getItem() instanceof ShearsItem)) {
                return;
            }

            ServerLevel level = (ServerLevel) player.level();
            BlockState state = level.getBlockState(pos);
            if (!AddonUtils.isCarvablePumpkin(state)) {
                return;
            }

            Direction carvedFacing = AddonUtils.resolveCarvedFacing(face, player.getDirection());
            Direction seedFace = AddonUtils.isValidPumpkinFace(face) ? face : carvedFacing;
            BlockState carvedState = Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(CarvedPumpkinBlock.FACING, carvedFacing);

            level.setBlock(pos, carvedState, 11);

            ItemEntity seeds = new ItemEntity(
                    level,
                    pos.getX() + 0.5D + seedFace.getStepX() * 0.65D,
                    pos.getY() + 0.1D,
                    pos.getZ() + 0.5D + seedFace.getStepZ() * 0.65D,
                    new ItemStack(Items.PUMPKIN_SEEDS, 4)
            );
            seeds.setDeltaMovement(
                    0.05D * seedFace.getStepX() + level.random.nextDouble() * 0.02D,
                    0.05D,
                    0.05D * seedFace.getStepZ() + level.random.nextDouble() * 0.02D
            );
            level.addFreshEntity(seeds);

            level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            heldStack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
        });
    }
}
