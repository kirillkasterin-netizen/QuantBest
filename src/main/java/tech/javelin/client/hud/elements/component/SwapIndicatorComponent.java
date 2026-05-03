package tech.javelin.client.hud.elements.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.theme.Theme;
import tech.javelin.Javelin;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.utility.render.display.Keyboard;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class SwapIndicatorComponent extends DraggableHudElement {
   
   private final Animation widthAnimation;
   private final Animation alpha;
   private final List<SwapEntry> swapHistory;
   
   public SwapIndicatorComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
      this.swapHistory = new ArrayList<>();
   }
   
   public void addSwap(String itemName, ItemStack itemStack) {
      SwapEntry entry = new SwapEntry(itemName, itemStack);
      swapHistory.add(0, entry);
      
      // Ограничиваем историю до 3 элементов
      if (swapHistory.size() > 3) {
         swapHistory.remove(swapHistory.size() - 1);
      }
   }
   
   @Native
   public void render(CustomDrawContext ctx) {
      float posX = this.getX();
      float posY = this.getY();
      float headerHeight = 15.0F;
      float rowHeight = 14.0F;
      float rowSpacing = 1.5F;
      float rowGap = 2.0F;
      float defaultWidth = 0.0F;
      float itemIconSize = 10.0F;
      float itemIconLeftPadding = 3.0F;
      float itemTextGap = 2.5F;
      float itemRightPadding = 6.0F;
      float height = headerHeight;
      
      // Обновляем анимации для записей
      Iterator<SwapEntry> iterator = swapHistory.iterator();
      while (iterator.hasNext()) {
         SwapEntry entry = iterator.next();
         entry.update();
         if (entry.shouldRemove()) {
            iterator.remove();
         }
      }
      
      boolean isFound = !swapHistory.isEmpty();
      
      if (isFound || mc.currentScreen instanceof ChatScreen) {
         this.alpha.update(1.0F);
      } else {
         this.alpha.update(0.0F);
      }
      
      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      float titleWidth = Fonts.MEDIUM.getWidth("Swap", 8.0F);
      defaultWidth = Math.max(defaultWidth, titleWidth + 26.0F);
      
      // Вычисляем максимальную ширину для предметов и биндов
      float maxItemWidth = 0.0F;
      float maxBindWidth = 0.0F;
      
      for (SwapEntry entry : swapHistory) {
         if (entry.animation.getValue() != 0.0F) {
            float rowAnim = entry.animation.getValue();
            height += (rowHeight + rowSpacing) * rowAnim;
            
            String itemName = entry.itemName;
            float itemWidth = itemIconLeftPadding + itemIconSize + itemTextGap + Fonts.REGULAR.getWidth(itemName, 7.0F) + itemRightPadding;
            if (itemWidth > maxItemWidth) {
               maxItemWidth = itemWidth;
            }
            
            // Для бинда используем фиксированную ширину или вычисляем
            float bindWidth = Fonts.MEDIUM.getWidth("SWAP", 6.75F) + 6.0F;
            if (bindWidth > maxBindWidth) {
               maxBindWidth = bindWidth;
            }
         }
      }
      
      float itemBoxWidth = Math.max(12.0F, maxItemWidth);
      float bindBoxWidth = Math.max(12.0F, maxBindWidth);
      float totalWidth = itemBoxWidth + rowGap + bindBoxWidth;
      
      this.widthAnimation.update(isFound ? totalWidth : Math.max(defaultWidth, totalWidth));
      float panelWidth = this.widthAnimation.getValue();
      
      DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, panelWidth, headerHeight, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(70, 70, 70, 255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.ICONS2.getFont(8.0F), "\uf0ec", posX + 5.0F, posY + 5f, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.MEDIUM.getFont(8.0F), "Swap", posX + 14f, posY + 4.4f, ColorRGBA.WHITE.withAlpha(255.0F * this.alpha.getValue()));
      
      float currentY = posY + headerHeight + rowSpacing;
      
      for (SwapEntry entry : swapHistory) {
         if (entry.animation.getValue() != 0.0F) {
            String itemName = entry.itemName;
            float rowAnim = entry.animation.getValue();
            float rowAlpha = rowAnim * this.alpha.getValue();
            float rowOffset = (1.0F - rowAnim) * 3.0F;
            float rowY = currentY + rowOffset;
            
            float itemWidth = itemIconLeftPadding + itemIconSize + itemTextGap + Fonts.REGULAR.getWidth(itemName, 7.0F) + itemRightPadding;
            float bindBoxX = posX + itemWidth + rowGap;
            
            // Рисуем бокс предмета
            DrawUtil.drawBlur(ctx.getMatrices(), posX, rowY, itemWidth, rowHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255.0F * rowAlpha));
            
            // Рисуем бокс бинда
            DrawUtil.drawBlur(ctx.getMatrices(), bindBoxX, rowY, bindBoxWidth, rowHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255.0F * rowAlpha));
            
            float iconX = posX + itemIconLeftPadding;
            float textX = iconX + itemIconSize + itemTextGap;
            
            // Рисуем иконку предмета
            if (entry.itemStack != null && !entry.itemStack.isEmpty()) {
               ctx.drawItem(entry.itemStack, (int)iconX, (int)(rowY + 2.0F));
            }
            
            ctx.drawText(Fonts.MEDIUM.getFont(7.0F), itemName, textX, rowY + 4.2F, ColorRGBA.WHITE.withAlpha(rowAlpha * 255.0F));
            
            // Рисуем текст бинда
            String bindText = "SWAP";
            float bindX = bindBoxX + (bindBoxWidth - Fonts.MEDIUM.getWidth(bindText, 7.0F)) / 1.8F;
            ctx.drawText(Fonts.MEDIUM.getFont(7.0F), bindText, bindX, rowY + 4.5F, ColorRGBA.WHITE.withAlpha(rowAlpha * 255.0F));
            
            currentY += (rowHeight + rowSpacing) * rowAnim;
         }
      }
      
      this.width = panelWidth;
      this.height = height;
   }
   
   private static class SwapEntry {
      private final String itemName;
      private final ItemStack itemStack;
      private final Animation animation;
      private final long createdTime;
      private static final long DISPLAY_DURATION = 3000; // 3 секунды
      
      public SwapEntry(String itemName, ItemStack itemStack) {
         this.itemName = itemName;
         this.itemStack = itemStack;
         this.animation = new Animation(200L, Easing.CUBIC_OUT);
         this.createdTime = System.currentTimeMillis();
         this.animation.update(1.0F);
      }
      
      public void update() {
         long elapsed = System.currentTimeMillis() - createdTime;
         if (elapsed > DISPLAY_DURATION) {
            animation.update(0.0F);
         } else {
            animation.update(1.0F);
         }
      }
      
      public boolean shouldRemove() {
         return animation.getValue() == 0.0F && System.currentTimeMillis() - createdTime > DISPLAY_DURATION + 200;
      }
   }
}
