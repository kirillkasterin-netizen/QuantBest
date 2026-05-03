package tech.javelin.client.hud.elements.component;

import java.util.Locale;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;


public class WatermarkComponent extends DraggableHudElement {
   public static boolean isIslandMode = false;

   public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      float x = this.getX();
      float y = this.getY();
      
      float leftSize = 17.5F;
      float height = 16.5F;
      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      String rightIcon = "m";
      String rightText = "famous";
      String fpsIcon = "t";
      String fpsText = mc.getCurrentFps() + " fps";
      String msIcon = "u";
      String tpsIcon = "dfghjklz";
      String bpsIcon = "qwertyuiopas";
      String coordsIcon = "bnm";
      int ping = mc.player != null && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null
         ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0;
      String msText = ping + " ms";
      String tpsText = String.format(Locale.US, "%.1f tps", Javelin.getInstance().getServerHandler().getTPS());
      double bps = mc.player != null ? Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20.0D : 0.0D;
      String bpsText = String.format(Locale.US, "%.1f bps", bps);
      int posX = mc.player != null ? (int)Math.floor(mc.player.getX()) : 0;
      int posY = mc.player != null ? (int)Math.floor(mc.player.getY()) : 0;
      int posZ = mc.player != null ? (int)Math.floor(mc.player.getZ()) : 0;
      String coordsText = "x: " + posX + " y: " + posY + " z: " + posZ;

      float rightGap = 1.5F;
      float rightWidth = 4.0F + Fonts.ICONS.getWidth(rightIcon, 8.0F) + 3.0F + Fonts.MEDIUM.getWidth(rightText, 7.0F) + 5.0F;
      float fpsWidth = 4.0F + Fonts.ICONS.getWidth(fpsIcon, 8.0F) + 3.0F + Fonts.MEDIUM.getWidth(fpsText, 7.0F) + 5.0F;
      float msWidth = 4.0F + Fonts.ICONS.getWidth(msIcon, 8.0F) + 3.0F + Fonts.MEDIUM.getWidth(msText, 7.0F) + 5.0F;

      float topWidth = leftSize + rightGap + rightWidth + rightGap + fpsWidth + rightGap + msWidth;
      
      if (isIslandMode) {
         float screenWidth = mc.getWindow().getScaledWidth();
         x = screenWidth / 2.0f - topWidth / 2.0f;
         y = 5.0f;
      }

      DrawUtil.drawBlur(ctx.getMatrices(), x, y, topWidth, height, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(70, 70, 70, 255));
      
      float logoSize = 14.0F; // Увеличил размер
      String infinitySymbol = "∞";
      float textWidth = mc.textRenderer.getWidth(infinitySymbol);
      float logoX = x + (leftSize - textWidth) / 2.0F + 3.0F; // Сдвинул вправо на 3 пикселя
      
      // Создаем переливание между цветом темы и фиолетовым
      long time = System.currentTimeMillis();
      float cycle = (float)((time % 3000) / 3000.0); // 3 секунды на полный цикл
      float interpolation = (float)(Math.sin(cycle * Math.PI * 2) * 0.5 + 0.5); // Плавное переливание от 0 до 1
      
      ColorRGBA themeColor = theme.getColor();
      ColorRGBA purpleColor = new ColorRGBA(138, 43, 226, 255); // Фиолетовый цвет
      ColorRGBA logoColor = tech.javelin.utility.render.display.base.color.ColorUtil.interpolate(themeColor, purpleColor, interpolation);
      
      ctx.drawText(mc.textRenderer, infinitySymbol, (int)logoX, (int)(y + 4.5F), logoColor.getRGB(), true);

      float rightX = x + leftSize + rightGap;
      ctx.drawText(Fonts.ICONS.getFont(8.0F), rightIcon, rightX + 5.0F, y + 6.15F, theme.getColor());
      ctx.drawText(Fonts.MEDIUM.getFont(7.0F), rightText, rightX + 2.0F + Fonts.ICONS.getWidth(rightIcon, 8.0F) + 3.0F, y + 5.7F, ColorRGBA.WHITE);

      float fpsX = rightX + rightWidth + rightGap;
      ctx.drawText(Fonts.ICONS.getFont(8.0F), fpsIcon, fpsX + 5.0F, y + 6.15F, theme.getColor());
      ctx.drawText(Fonts.MEDIUM.getFont(7.0F), fpsText, fpsX + 3.0F + Fonts.ICONS.getWidth(fpsIcon, 8.0F) + 3.0F, y + 5.7F, ColorRGBA.WHITE);

      float msX = fpsX + fpsWidth + rightGap;
      ctx.drawText(Fonts.ICONS.getFont(8.0F), msIcon, msX + 5.0F, y + 5.8f, theme.getColor());
      ctx.drawText(Fonts.MEDIUM.getFont(7.0F), msText, msX + 4.0F + Fonts.ICONS.getWidth(msIcon, 8.0F) + 3.0F, y + 5.7F, ColorRGBA.WHITE);

      if (!isIslandMode) {
          float tpsWidth = 4.0F + Fonts.ICONS2.getWidth(tpsIcon, 9.0F) + 3.0F + Fonts.MEDIUM.getWidth(tpsText, 7.0F) + 5.0F;
          float bottomY = y + height + 1.5F;
          float coordsHeight = 16.5F;
          float coordsWidth = 5.0F + Fonts.ICONS2.getWidth(coordsIcon, 9.0F) + 3.0F + Fonts.MEDIUM.getWidth(coordsText, 7.0F) + 4.0F;
          float bpsWidth = 4.0F + Fonts.ICONS2.getWidth(bpsIcon, 9.0F) + 3.0F + Fonts.MEDIUM.getWidth(bpsText, 7.0F) + 5.0F;

          float bottomWidth = coordsWidth + rightGap + tpsWidth + rightGap + bpsWidth;

          DrawUtil.drawBlur(ctx.getMatrices(), x, bottomY, bottomWidth, coordsHeight, 9.0F, BorderRadius.all(2.5F), new ColorRGBA(95, 95, 95, 255));

          ctx.drawText(Fonts.ICONS2.getFont(9.0F), coordsIcon, x + 5.0F, bottomY + 5.9F, theme.getColor());
          ctx.drawText(Fonts.MEDIUM.getFont(7.0F), coordsText, x + 3.0F + Fonts.ICONS2.getWidth(coordsIcon, 9.0F) + 3.0F, bottomY + 5.5F, ColorRGBA.WHITE);
          
          float bottomTpsX = x + coordsWidth + rightGap;
          ctx.drawText(Fonts.ICONS2.getFont(9.0F), tpsIcon, bottomTpsX + 3.5F, bottomY + 5.9F, theme.getColor());
          ctx.drawText(Fonts.MEDIUM.getFont(7.0F), tpsText, bottomTpsX + 2.0F + Fonts.ICONS2.getWidth(tpsIcon, 9.0F) + 3.0F, bottomY + 5.7F, ColorRGBA.WHITE);
          
          float bottomBpsX = bottomTpsX + tpsWidth + rightGap;
          ctx.drawText(Fonts.ICONS2.getFont(9.0F), bpsIcon, bottomBpsX + 4.0F, bottomY + 5.9F, theme.getColor());
          ctx.drawText(Fonts.MEDIUM.getFont(7.0F), bpsText, bottomBpsX + 2.0F + Fonts.ICONS2.getWidth(bpsIcon, 9.0F) + 3.0F, bottomY + 5.95F, ColorRGBA.WHITE);

          this.width = Math.max(topWidth, bottomWidth);
          this.height = height + 1.5F + coordsHeight;
      } else {
          this.width = topWidth;
          this.height = height;
      }
   }
}
