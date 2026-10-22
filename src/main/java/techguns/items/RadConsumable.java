package techguns.items;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * 1.12 ItemRadAway and ItemRadpills (GenericItemConsumable): used for 32 ticks whatever the hunger, they give
 * {@code effects} and leave a {@code container}.
 */
public class RadConsumable extends Item {
	private final UseAnim animation;
	private final Supplier<? extends ItemLike> container;
	private final Supplier<List<MobEffectInstance>> effects;

	public RadConsumable(UseAnim animation, Supplier<? extends ItemLike> container, Supplier<List<MobEffectInstance>> effects) {
		super(new Item.Properties());
		this.animation = animation;
		this.container = container;
		this.effects = effects;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return animation;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide) effects.get().forEach(entity::addEffect);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.5f,
				level.random.nextFloat() * 0.1f + 0.9f);
		if (!(entity instanceof Player player)) return stack;
		player.awardStat(Stats.ITEM_USED.get(this));
		if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
		return ItemUtils.createFilledResult(stack, player, new ItemStack(container.get()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		PotionContents.addPotionTooltip(effects.get(), tooltip::add, 1, context.tickRate());
	}
}
