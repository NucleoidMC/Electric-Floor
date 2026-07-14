package io.github.haykam821.electricfloor.game.map;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class ElectricFloorGuideText {	
	private static final ChatFormatting FORMATTING = ChatFormatting.GOLD;

	private static final Component TEXT = Component.empty()
		.append(Component.translatable("gameType.electricfloor.electric_floor").withStyle(ChatFormatting.BOLD))
		.append(CommonComponents.NEW_LINE)
		.append(Component.translatable("text.electricfloor.guide.keep_moving"))
		.append(CommonComponents.NEW_LINE)
		.append(Component.translatable("text.electricfloor.guide.red_eliminates"))
		.append(CommonComponents.NEW_LINE)
		.append(Component.translatable("text.electricfloor.guide.last_player_standing"))
		.withStyle(FORMATTING);

	private ElectricFloorGuideText() {
		return;
	}

	public static ElementHolder createElementHolder(boolean night) {
		TextDisplayElement element = new TextDisplayElement(TEXT);

		element.setBillboardMode(BillboardConstraints.CENTER);
		element.setLineWidth(350);
		element.setInvisible(true);

		if (night) {
			element.setBrightness(Brightness.FULL_BRIGHT);
		}

		ElementHolder holder = new ElementHolder();
		holder.addElement(element);

		return holder;
	}
}
