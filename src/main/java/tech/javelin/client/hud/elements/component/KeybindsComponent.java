package tech.javelin.client.hud.elements.component;

import java.util.Iterator;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.client.modules.api.Module;
import tech.javelin.utility.render.display.Keyboard;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class KeybindsComponent extends DraggableHudElement {
   private final Animation widthAnimation;
   private final Animation xLine;
   private final Animation alpha;

   public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
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
      float moduleIconSize = 6.0F;
      float moduleIconLeftPadding = 5.0F;
      float moduleTextGap = 2.5F;
      float moduleRightPadding = 6.0F;
      float height = headerHeight;
      boolean isFound = false;
      Iterator var7 = Javelin.getInstance().getModuleManager().getModules().iterator();

      while(var7.hasNext()) {
         Module module = (Module)var7.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            isFound = true;
            break;
         }
      }

      if (isFound || mc.currentScreen instanceof ChatScreen) {
         this.alpha.update(1.0F);
      } else {
         this.alpha.update(0.0F);
      }

      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      float titleWidth = Fonts.MEDIUM.getWidth("Hotkeys", 8.0F);
      defaultWidth = Math.max(defaultWidth, titleWidth + 26.0F);
      float bindWidth = 0.0F;
      Iterator var9 = Javelin.getInstance().getModuleManager().getModules().iterator();

      Module module;
      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            float localBindWidth = Fonts.MEDIUM.getWidth(Keyboard.getKeyName(module.getKeyCode()), 6.75F);
            if (localBindWidth > bindWidth) {
               bindWidth = localBindWidth;
            }
         }
      }

      this.xLine.update(bindWidth);
      var9 = Javelin.getInstance().getModuleManager().getModules().iterator();

      float maxFunctionWidth = 0.0F;
      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            float rowAnim = module.getAnimation().getValue();
            height += (rowHeight + rowSpacing) * rowAnim;
            String moduleName = module.getName();
            float iconWidth = Fonts.ICONS.getWidth(module.getCategory().getIcon(), moduleIconSize);
            float functionWidth = moduleIconLeftPadding + iconWidth + moduleTextGap + Fonts.REGULAR.getWidth(moduleName, 7.0F) + moduleRightPadding;
            if (functionWidth > maxFunctionWidth) {
               maxFunctionWidth = functionWidth;
            }
         }
      }

      float functionBoxWidth = Math.max(12.0F, maxFunctionWidth);
      float bindBoxWidth = Math.max(12.0F, this.xLine.getValue() + 6.0F);
      float totalWidth = functionBoxWidth + rowGap + bindBoxWidth;
      this.widthAnimation.update(isFound ? totalWidth : Math.max(defaultWidth, totalWidth));
      float panelWidth = this.widthAnimation.getValue();
      DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, panelWidth, headerHeight, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(70, 70, 70, 255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.MEDIUM.getFont(8.0F), "Hotkeys", posX + 16f, posY + 4.4f, ColorRGBA.WHITE.withAlpha(255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.ICONS.getFont(9.0F), "O", posX + 4.2F, posY + 4.2f, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));
      float currentY = posY + headerHeight + rowSpacing;
      var9 = Javelin.getInstance().getModuleManager().getModules().iterator();

      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            String bind = Keyboard.getKeyName(module.getKeyCode());
            String moduleName = module.getName();
            float rowAnim = module.getAnimation().getValue();
            float rowAlpha = rowAnim * this.alpha.getValue();
            float rowOffset = (1.0F - rowAnim) * 3.0F;
            float rowY = currentY + rowOffset;
            String categoryIcon = module.getCategory().getIcon();
            float iconWidth = Fonts.ICONS.getWidth(categoryIcon, moduleIconSize);
            float functionWidth = moduleIconLeftPadding + iconWidth + moduleTextGap + Fonts.REGULAR.getWidth(moduleName, 7.0F) + moduleRightPadding;
            float bindBoxX = posX + functionWidth + rowGap;
            DrawUtil.drawBlur(ctx.getMatrices(), posX, rowY, functionWidth, rowHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255.0F * rowAlpha));
            DrawUtil.drawBlur(ctx.getMatrices(), bindBoxX, rowY, bindBoxWidth, rowHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255.0F * rowAlpha));
            float iconX = posX + moduleIconLeftPadding;
            float textX = iconX + iconWidth + moduleTextGap;
            ctx.drawText(Fonts.ICONS.getFont(moduleIconSize), categoryIcon, iconX, rowY + 4.15F, theme.getColor().withAlpha(rowAlpha * 255.0F));
            ctx.drawText(Fonts.MEDIUM.getFont(7.0F), moduleName, textX, rowY + 4.2F, ColorRGBA.WHITE.withAlpha(rowAlpha * 255.0F));
            float bindX = bindBoxX + (bindBoxWidth - Fonts.MEDIUM.getWidth(bind, 7.0F)) / 1.8F;
            ctx.drawText(Fonts.MEDIUM.getFont(7.0F), bind, bindX, rowY + 4.5F, ColorRGBA.WHITE.withAlpha(rowAlpha * 255.0F));
            currentY += (rowHeight + rowSpacing) * rowAnim;
         }
      }

      this.width = panelWidth;
      this.height = height;
   }
}
