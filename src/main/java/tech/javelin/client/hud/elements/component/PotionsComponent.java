package tech.javelin.client.hud.elements.component;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class PotionsComponent extends DraggableHudElement {
   private final BooleanSetting s1 = new BooleanSetting("1", true);
   private final BooleanSetting s2 = new BooleanSetting("2", true);
   private final BooleanSetting s3 = new BooleanSetting("3", true);
   private final BooleanSetting s4 = new BooleanSetting("4", true);
   private final BooleanSetting s5 = new BooleanSetting("5", true);
   private final BooleanSetting s6 = new BooleanSetting("6", true);
   private final BooleanSetting s7 = new BooleanSetting("7", true);
   private final Animation widthAnimation;
   private final Animation xLine;
   private final Animation alpha;
   private final List<PotionsComponent.PotionItem> potionItems;

   public PotionsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
      this.potionItems = new CopyOnWriteArrayList();
   }

   @Native
   public void render(CustomDrawContext ctx) {
      if (mc.player != null) {
         this.updatePotions();
         float posX = this.getX();
         float posY = this.getY();
         float headerHeight = 15.0F;
         float rowHeight = 14.0F;
         float rowSpacing = 1.5F;
         float rowGap = 2.0F;
         float effectIconSize = 7.0F;
         float effectIconLeftPadding = 4.5F;
         float effectIconTextGap = 2.5F;
         float effectRightPadding = 6.0F;
         float defaultWidth = 0.0F;
         float height = headerHeight;
         this.potionItems.sort(Comparator.comparing((pi) -> {
            return pi.name;
         }));
         boolean isFound = false;
         float maxLeftWidth = 0.0F;
         float maxDurationWidth = 0.0F;
         Iterator var8 = this.potionItems.iterator();

         String duration;
         while(var8.hasNext()) {
            PotionsComponent.PotionItem item = (PotionsComponent.PotionItem)var8.next();
            item.animation.update(item.active);
            if (item.animation.getValue() != 0.0F) {
               String name = I18n.translate(item.name, new Object[0]);
               String amp = this.getAmplifierText(item.amplifier);
               duration = this.formatDuration(item.durationTicks);
               float leftTextWidth = Fonts.MEDIUM.getWidth(name, 7.0F) + (amp.equals("1") ? 0.0F : Fonts.MEDIUM.getWidth(amp, 7.0F) + 2.5F);
               float leftBoxWidth = effectIconLeftPadding + effectIconSize + effectIconTextGap + leftTextWidth + effectRightPadding;
               if (leftBoxWidth > maxLeftWidth) {
                  maxLeftWidth = leftBoxWidth;
               }

               float durationWidth = Fonts.MEDIUM.getWidth(duration, 7.0F);
               if (durationWidth > maxDurationWidth) {
                  maxDurationWidth = durationWidth;
               }

               height += (rowHeight + rowSpacing) * item.animation.getValue();
               this.alpha.update(1.0F);
               isFound = true;
            }
         }

         this.xLine.update(maxDurationWidth);
         if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
         }

         if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
         }

         Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
         float titleWidth = Fonts.MEDIUM.getWidth("Potions", 8.0F);
         defaultWidth = Math.max(defaultWidth, titleWidth + 24.0F);
         float leftColumnWidth = Math.max(12.0F, maxLeftWidth);
         float durationBoxWidth = Math.max(12.0F, this.xLine.getValue() + 8.0F);
         float totalWidth = leftColumnWidth + rowGap + durationBoxWidth;
         this.widthAnimation.update(isFound ? totalWidth : Math.max(defaultWidth, totalWidth));
         float panelWidth = this.widthAnimation.getValue();
         DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, panelWidth, headerHeight, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(70, 70, 70, 255.0F * this.alpha.getValue()));
         ctx.drawText(Fonts.MEDIUM.getFont(8.0F), "Potions", posX + 16f, posY + 4.4F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
         ctx.drawText(Fonts.ICONS.getFont(9.0F), "O", posX + 4.2F, posY + 4.2f, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));
         float currentY = posY + headerHeight + rowSpacing;
         Iterator var17 = this.potionItems.iterator();

         while(var17.hasNext()) {
            PotionsComponent.PotionItem item = (PotionsComponent.PotionItem)var17.next();
            if (item.animation.getValue() != 0.0F) {
               String name = I18n.translate(item.name, new Object[0]);
               String amp = this.getAmplifierText(item.amplifier);
               duration = this.formatDuration(item.durationTicks);
               float rowAlpha = item.animation.getValue() * this.alpha.getValue();
               float rowOffset = (1.0F - item.animation.getValue()) * 3.0F;
               float rowY = currentY + rowOffset;
               float leftTextWidth = Fonts.MEDIUM.getWidth(name, 7.0F) + (amp.equals("1") ? 0.0F : Fonts.MEDIUM.getWidth(amp, 7.0F) + 2.5F);
               float leftBoxWidth = effectIconLeftPadding + effectIconSize + effectIconTextGap + leftTextWidth + effectRightPadding;
               float durationBoxX = posX + leftBoxWidth + rowGap;
               DrawUtil.drawBlur(ctx.getMatrices(), posX, rowY, leftBoxWidth, rowHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255.0F * rowAlpha));
               DrawUtil.drawBlur(ctx.getMatrices(), durationBoxX, rowY, durationBoxWidth, rowHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255.0F * rowAlpha));
               float iconX = posX + effectIconLeftPadding;
               float iconY = rowY + (rowHeight - effectIconSize) / 2.0F;
               Identifier effectIcon = this.getEffectIcon(item.effect.getEffectType().value());
               ctx.drawTexture(effectIcon, iconX, iconY, effectIconSize, effectIconSize, ColorRGBA.WHITE.withAlpha(rowAlpha * 255.0F));
               float textX = iconX + effectIconSize + effectIconTextGap;
               ctx.drawText(Fonts.MEDIUM.getFont(7.0F), name, textX, rowY + 4.2F, (new ColorRGBA(-1)).withAlpha(rowAlpha * 255.0F));
               if (!amp.equals("1")) {
                  float ampX = textX + Fonts.MEDIUM.getWidth(name, 7.0F) + 2.5F;
                  ctx.drawText(Fonts.MEDIUM.getFont(7.0F), amp, ampX, rowY + 4.2F, theme.getColor().withAlpha(rowAlpha * 255.0F));
               }

               float durationX = durationBoxX + (durationBoxWidth - Fonts.MEDIUM.getWidth(duration, 7.0F)) / 2.0F;
               ctx.drawText(Fonts.MEDIUM.getFont(7.0F), duration, durationX, rowY + 4.2F, (new ColorRGBA(-1)).withAlpha(rowAlpha * 255.0F));
               currentY += (rowHeight + rowSpacing) * item.animation.getValue();
            }
         }

         this.width = panelWidth;
         this.height = height;
      }
   }

   private String getAmplifierText(int amplifier) {
      return String.valueOf(amplifier + 1);
   }

   private String formatDuration(int durationTicks) {
      int totalSeconds = durationTicks / 20;
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      return String.format("%02d:%02d", minutes, seconds);
   }

   private Identifier getEffectIcon(StatusEffect effect) {
      String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
      return Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
   }

   @Native
   public void updatePotions() {
      if (mc.player != null) {
         Map<String, StatusEffectInstance> currentEffects = (Map)mc.player.getStatusEffects().stream().collect(Collectors.toMap((e) -> {
            String var10000 = Text.translatable(e.getTranslationKey()).getString();
            return var10000 + ":" + e.getAmplifier();
         }, (e) -> {
            return e;
         }, (e1, e2) -> {
            return e1;
         }));
         this.potionItems.forEach((item) -> {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = (StatusEffectInstance)currentEffects.get(key);
            if (effect != null) {
               item.durationTicks = effect.getDuration();
               if (!item.active) {
                  item.animation.setValue(1.0F);
               }

               item.active = true;
               currentEffects.remove(key);
            } else {
               item.active = false;
            }

         });
         currentEffects.forEach((key, effect) -> {
            this.potionItems.add(new PotionsComponent.PotionItem(Text.translatable(effect.getTranslationKey()).getString(), effect.getAmplifier(), effect.getDuration(), effect));
         });
         this.potionItems.removeIf((item) -> {
            return !item.active && item.animation.getValue() == 0.0F;
         });
      }
   }

   private static class PotionItem {
      String name;
      int amplifier;
      int durationTicks;
      boolean active;
      StatusEffectInstance effect;
      Animation animation;

      PotionItem(String name, int amplifier, int durationTicks, StatusEffectInstance effect) {
         this.animation = new Animation(250L, Easing.CUBIC_OUT);
         this.name = name;
         this.amplifier = amplifier;
         this.durationTicks = durationTicks;
         this.active = true;
         this.effect = effect;
      }
   }

   private static class PotionModule {
      private final Animation animation;
      private StatusEffectInstance effect;

      public PotionModule(StatusEffectInstance effect) {
         this.animation = new Animation(150L, 0.01F, Easing.QUAD_IN_OUT);
         this.effect = effect;
      }

      public boolean isDelete() {
         return this.animation.getValue() == 0.0F;
      }
   }
}
