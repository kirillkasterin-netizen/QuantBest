package tech.javelin.client.hud.elements.component;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Font;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.font.MsdfRenderer;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.client.modules.impl.combat.Aura;
import tech.javelin.client.modules.impl.misc.NameProtect;
import tech.javelin.client.modules.impl.misc.ScoreboardHealth;
import tech.javelin.utility.game.player.PlayerIntersectionUtil;
import tech.javelin.utility.mixin.accessors.DrawContextAccessor;
import tech.javelin.utility.render.display.StencilUtil;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class TargetHudComponent extends DraggableHudElement {
   private final Animation healthAnimation;
   private final Animation outdatedHealthAnimation;
   private final Animation gappleAnimation;
   private final Animation toggleAnimation;
   private final Animation toggleAnimationMetanoise;
   private LivingEntity target;
   private long targetLostTime = 0;
   private boolean isTargetLost = false;

   public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
      this.gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.toggleAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.toggleAnimationMetanoise = new Animation(1850L, Easing.CUBIC_OUT);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      Aura aura = Aura.INSTANCE;
      LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
      this.setTarget((LivingEntity)target);
      if (this.toggleAnimationMetanoise.getValue() != 0.0F && this.target != null) {
         this.renderTargetHud(ctx, this.target, this.toggleAnimation.getValue());
      }
   }

   @Native
   private void renderTargetHud(CustomDrawContext ctx, LivingEntity target, float animation) {
      float posX = this.x;
      float posY = this.y;
      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();
      this.healthAnimation.update(hp / target.getMaxHealth());
      if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
         this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
         this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
      } else {
         this.outdatedHealthAnimation.update(hp / target.getMaxHealth());
      }

      this.gappleAnimation.update(target.getAbsorptionAmount() / target.getMaxHealth());
      
      // Проверяем режим отображения HP
      tech.javelin.client.modules.impl.render.Interface interfaceModule = tech.javelin.client.modules.impl.render.Interface.INSTANCE;
      String healthMode = interfaceModule.targetHudHealthModeSetting.getValue().getName();
      
      float width = healthMode.equals("Circle") ? 116.0F : 86.0F; // Увеличиваем ширину для круга
      float height = 30.0F;
      
      StencilUtil.push();
      DrawUtil.drawMetanoise(ctx.getMatrices(), posX, posY, width, height, this.toggleAnimationMetanoise.getValue(), 3.0F, new ColorRGBA(0, 0, 0, 140), theme.getColor().withAlpha(255));
      StencilUtil.read(1);
      // Отключаем blur для режима Circle, чтобы не было свечения
      if (!healthMode.equals("Circle")) {
         DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, width, height, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 255.0F * animation));
      }
      DrawUtil.drawMetanoise(ctx.getMatrices(), posX, posY, width, height, this.toggleAnimationMetanoise.getValue(), 3.0F, new ColorRGBA(0, 0, 0, 140), theme.getColor().withAlpha(255));
      Identifier skinTextures = null;
      Iterator var11 = mc.getNetworkHandler().getPlayerList().iterator();

      while(var11.hasNext()) {
         PlayerListEntry playerListEntry = (PlayerListEntry)var11.next();
         if (playerListEntry.getProfile().getName().equals(target.getNameForScoreboard())) {
            skinTextures = playerListEntry.getSkinTextures().texture();
         }
      }

      if (skinTextures == null) {
         skinTextures = DefaultSkinHelper.getSteve().texture();
      }

      // Рендерим лицо в зависимости от режима
      this.drawFace(ctx, skinTextures, posX + 4.0F, posY + 4.0F, 22.0F, animation);
      
      MsdfRenderer.renderText(Fonts.REGULAR, target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard(), 7.25F, ColorRGBA.WHITE.withAlpha(animation * 255.0F).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), posX + 29.0F, posY + 5.5F, 0.0F, true, 0.7F, 1.0F, 56.0F);
      
      if (healthMode.equals("Circle")) {
         // В режиме Circle показываем броню вместо HP текста
         if (target instanceof PlayerEntity) {
            this.drawArmor(ctx, (PlayerEntity)target, posX + 29.0F, posY + 14.25F, 0.0F, 0.0F, 0.0F);
         }
      } else {
         // В режиме Bar показываем HP текст
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), "HP: " + String.format("%.0f", hp) + (target.getAbsorptionAmount() > 0.0F ? String.format(" (%.1f)", target.getAbsorptionAmount()) : "").replace(",", "."), posX + 29.75F, posY + 14.25F, ColorRGBA.WHITE.withAlpha(animation * 255.0F));
      }
      
      if (healthMode.equals("Circle")) {
         // Круг будет нарисован после StencilUtil.pop()
      } else {
         // Рисуем обычную полоску HP
         DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + 29.0F, posY + 22.0F, width - 33.0F, 3.25F, BorderRadius.all(0.25F), theme.getSecondColor().darker(0.5F).withAlpha(animation * 255.0F), theme.getSecondColor().darker(0.5F).withAlpha(animation * 255.0F), theme.getColor().darker(0.5F).withAlpha(animation * 255.0F), theme.getColor().darker(0.5F).withAlpha(animation * 255.0F));
         DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + 29.0F, posY + 22.0F, MathHelper.clamp((width - 33.0F) * this.outdatedHealthAnimation.getValue(), 0.0F, width - 33.0F), 3.25F, BorderRadius.all(0.25F), theme.getSecondColor().darker(0.35F).withAlpha(animation * 255.0F), theme.getSecondColor().darker(0.35F).withAlpha(animation * 255.0F), theme.getColor().darker(0.35F).withAlpha(animation * 255.0F), theme.getColor().darker(0.35F).withAlpha(animation * 255.0F));
         if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + 29.0F, posY + 22.0F, MathHelper.clamp((width - 33.0F) * this.healthAnimation.getValue(), 0.0F, width - 33.0F), 3.25F, BorderRadius.all(0.25F), theme.getSecondColor().withAlpha(animation * 255.0F), theme.getSecondColor().withAlpha(animation * 255.0F), theme.getColor().withAlpha(animation * 255.0F), theme.getColor().withAlpha(animation * 255.0F));
         }

         DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + 29.0F, posY + 22.0F, MathHelper.clamp((width - 33.0F) * this.gappleAnimation.getValue(), 0.0F, width - 33.0F), 3.25F, BorderRadius.all(0.25F), new ColorRGBA(255, 209, 0, animation * 255.0F), new ColorRGBA(255, 209, 0, animation * 255.0F), new ColorRGBA(255, 246, 20, animation * 255.0F), new ColorRGBA(255, 246, 20, animation * 255.0F));
      }
      
      StencilUtil.pop();
      
      // Рисуем круговой индикатор HP ПОСЛЕ StencilUtil.pop(), чтобы он был поверх
      if (healthMode.equals("Circle")) {
         this.drawCircleHealth(ctx, posX + width - 15.0F, posY + 15.0F, 9.0F, hp, target.getMaxHealth(), target.getAbsorptionAmount(), animation, theme);
      }
      
      // Броня рисуется только в режиме Bar (в режиме Circle броня уже показана вверху)
      if (!healthMode.equals("Circle") && target instanceof PlayerEntity) {
         this.drawArmor(ctx, (PlayerEntity)target, posX + 3.0F, posY - 12.0F, 0.0F, 0.0F, 0.0F);
      }

      this.width = width;
      this.height = height;
   }

   private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float headSize, float padding, float fontSize) {
      float boxSizeItem = 10.0F;
      float paddingItem = 0.0F;
      float iconX = posX + (5.0F - this.toggleAnimation.getValue() * 5.0F);
      float iconY = posY + 1.0F + (5.0F - this.toggleAnimation.getValue() * 5.0F);
      
      // Проверяем режим отображения HP
      tech.javelin.client.modules.impl.render.Interface interfaceModule = tech.javelin.client.modules.impl.render.Interface.INSTANCE;
      String healthMode = interfaceModule.targetHudHealthModeSetting.getValue().getName();
      
      List<ItemStack> armor = player.getInventory().armor;
      ItemStack[] items;
      
      if (healthMode.equals("Circle")) {
         // В режиме Circle показываем только броню горизонтально
         items = new ItemStack[]{(ItemStack)armor.get(3), (ItemStack)armor.get(2), (ItemStack)armor.get(1), (ItemStack)armor.get(0)};
         iconX = posX;
         iconY = posY;
         paddingItem = 1.0F;
      } else {
         // В режиме Bar показываем все предметы вертикально
         items = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack(), (ItemStack)armor.get(3), (ItemStack)armor.get(2), (ItemStack)armor.get(1), (ItemStack)armor.get(0)};
      }
      
      Font font = Fonts.MEDIUM.getFont(5.0F);
      ItemStack[] var15 = items;
      int var16 = items.length;

      for(int var17 = 0; var17 < var16; ++var17) {
         ItemStack stack = var15[var17];
         if (!stack.isEmpty()) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((double)iconX + ((double)boxSizeItem - 9.6D) / 2.0D, (double)iconY + ((double)boxSizeItem - 9.6D) / 2.0D, 0.0D);
            ctx.getMatrices().scale(0.6F * this.toggleAnimation.getValue(), 0.6F * this.toggleAnimation.getValue(), 0.6F * this.toggleAnimation.getValue());
            ctx.drawItem(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
            ctx.getMatrices().pop();
            
            if (healthMode.equals("Circle")) {
               iconX += boxSizeItem + paddingItem; // Горизонтально
            } else {
               iconX += boxSizeItem + paddingItem; // Вертикально (как было)
            }
         }
      }

   }

   private void drawFace(CustomDrawContext ctx, Identifier skinTexture, float x, float y, float size, float alpha) {
      tech.javelin.client.modules.impl.render.Interface interfaceModule = tech.javelin.client.modules.impl.render.Interface.INSTANCE;
      String mode = interfaceModule.targetHudModeSetting.getValue().getName();
      
      // Проверяем нужно ли показывать outsmile анимацию
      if (this.isTargetLost && interfaceModule.targetHudOutSetting.isEnabled()) {
         long elapsed = System.currentTimeMillis() - this.targetLostTime;
         int frame = (int) (elapsed / 30) + 1; // 30ms на кадр
         if (frame >= 1 && frame <= 29) {
            Identifier outsmileTexture = Identifier.of("javelin", "textures/targethud/outsmile/" + String.format("%04d", frame) + ".png");
            ctx.drawTexture(outsmileTexture, x, y, size, size, ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
            return;
         }
      }
      
      if (mode.equals("Rocket")) {

         int frame = (int) ((System.currentTimeMillis() / 30) % 180);
         Identifier rocketTexture = Identifier.of("javelin", "textures/targethud/rocket/" + String.format("%03d", frame) + ".png");
         ctx.drawTexture(rocketTexture, x, y, size, size, ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
      } else if (mode.equals("Duck")) {

         int frame = (int) ((System.currentTimeMillis() / 30) % 180);
         Identifier duckTexture = Identifier.of("javelin", "textures/targethud/duck/" + String.format("%03d", frame) + ".png");
         ctx.drawTexture(duckTexture, x, y, size, size, ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
      } else if (mode.equals("3D")) {

         float rot = (System.currentTimeMillis() % 6000L) / 6000.0f * 360.0f;
         float baseX = x + size / 2.0f;
         float baseY = y + size + 4.0f;
         float scale = size * 0.7f;
         this.drawEntity3D(ctx, this.target, baseX, baseY, scale, rot, alpha);
      } else {

         DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTexture, x, y, size, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
      }
   }

   private void drawEntity3D(CustomDrawContext ctx, LivingEntity entity, float x, float y, float scale, float yawDeg, float alpha) {
      if (entity == null) return;
      
      net.minecraft.client.render.entity.EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      net.minecraft.client.util.math.MatrixStack matrices = ctx.getMatrices();
      
      matrices.push();
      matrices.translate(x, y, 50.0);
      matrices.scale(scale, scale, scale);
      matrices.multiply(new org.joml.Quaternionf().rotateZ((float) Math.PI));
      matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
      
      dispatcher.setRenderShadows(false);
      com.mojang.blaze3d.systems.RenderSystem.enableBlend();
      com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
      com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
      com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      
      net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
      net.minecraft.client.render.entity.EntityRenderer renderer = dispatcher.getRenderer(entity);
      if (renderer != null) {
         int light = renderer.getLight(entity, mc.getRenderTickCounter().getTickDelta(false));
         net.minecraft.client.render.entity.state.EntityRenderState state = renderer.getAndUpdateRenderState(entity, mc.getRenderTickCounter().getTickDelta(false));
         if (state != null) {
            renderer.render(state, matrices, immediate, light);
         }
      }
      immediate.draw();
      
      dispatcher.setRenderShadows(true);
      com.mojang.blaze3d.systems.RenderSystem.disableBlend();
      com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.pop();
   }

   public void setTarget(LivingEntity target) {
      if (target == null) {
         if (this.target != null && !this.isTargetLost) {
            // Таргет только что потерян - запускаем outsmile анимацию
            this.isTargetLost = true;
            this.targetLostTime = System.currentTimeMillis();
         }
         
         this.toggleAnimation.update(0.0F);
         this.toggleAnimationMetanoise.update(0.0F);
         this.toggleAnimationMetanoise.setDuration(2200L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         if (this.toggleAnimationMetanoise.getValue() == 0.0F) {
            this.target = null;
            this.isTargetLost = false;
         }
      } else {
         this.target = target;
         this.isTargetLost = false;
         this.toggleAnimationMetanoise.update(1.0F);
         this.toggleAnimationMetanoise.setDuration(1300L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         this.toggleAnimation.update(1.0F);
      }

   }

   /**
    * Рисует круговой индикатор HP в виде тонкого кольца
    */
   private void drawCircleHealth(CustomDrawContext ctx, float centerX, float centerY, float radius, float currentHp, float maxHp, float absorption, float alpha, Theme theme) {
      float healthPercent = MathHelper.clamp(currentHp / maxHp, 0.0F, 1.0F);
      
      net.minecraft.client.util.math.MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      
      org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();
      
      float outerRadius = radius;
      float innerRadius = radius - 1.5F; // Тонкое кольцо
      
      // Рисуем круговой прогресс HP
      if (healthPercent > 0.0F) {
         int themeColor = theme.getColor().getRGB();
         int bgColor = new ColorRGBA(40, 40, 40, (int)(150 * alpha)).getRGB();
         this.drawCircularProgressGradient(matrix, centerX, centerY, outerRadius, innerRadius, this.healthAnimation.getValue(), themeColor, bgColor, (int)(alpha * 255));
      }
      
      // Absorption (золотой)
      if (this.gappleAnimation.getValue() > 0.0F) {
         int absColor = new ColorRGBA(255, 215, 0, (int)(alpha * 255)).getRGB();
         int bgColor = new ColorRGBA(40, 40, 40, (int)(150 * alpha)).getRGB();
         this.drawCircularProgressGradient(matrix, centerX, centerY, outerRadius, innerRadius, this.gappleAnimation.getValue(), absColor, bgColor, (int)(alpha * 255));
      }
      
      matrices.pop();
      
      // Текст HP в центре круга
      String hpText = String.format("%.0f", currentHp);
      Font font = Fonts.BOLD.getFont(6.0F);
      float textWidth = font.width(hpText);
      float textHeight = font.height();
      ctx.drawText(font, hpText, centerX - textWidth / 2.0F, centerY - textHeight / 2.0F, ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
   }
   
   /**
    * Рисует круговой прогресс-бар с градиентом и сглаживанием краев
    */
   private void drawCircularProgressGradient(org.joml.Matrix4f matrix, float centerX, float centerY, float outerRadius, float innerRadius, float progress, int progressColor, int bgColor, int alpha) {
      com.mojang.blaze3d.systems.RenderSystem.enableBlend();
      com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
      com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
      
      // Включаем сглаживание
      org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
      org.lwjgl.opengl.GL11.glHint(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH_HINT, org.lwjgl.opengl.GL11.GL_NICEST);
      org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_POLYGON_SMOOTH);
      org.lwjgl.opengl.GL11.glHint(org.lwjgl.opengl.GL11.GL_POLYGON_SMOOTH_HINT, org.lwjgl.opengl.GL11.GL_NICEST);
      
      int segments = 360;
      float angleStep = (float) (2 * Math.PI / segments);
      float startAngle = (float) (-Math.PI / 2);
      float aaWidth = 0.5F;
      
      int bgR = (bgColor >> 16) & 0xFF;
      int bgG = (bgColor >> 8) & 0xFF;
      int bgB = bgColor & 0xFF;
      
      // Рисуем фон (полное кольцо)
      net.minecraft.client.render.BufferBuilder builder = net.minecraft.client.render.Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_STRIP, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= segments; i++) {
         float angle = startAngle + angleStep * i;
         float cos = (float) Math.cos(angle);
         float sin = (float) Math.sin(angle);
         float outerX = centerX + cos * outerRadius;
         float outerY = centerY + sin * outerRadius;
         float innerX = centerX + cos * innerRadius;
         float innerY = centerY + sin * innerRadius;
         builder.vertex(matrix, outerX, outerY, 0).color(bgColor);
         builder.vertex(matrix, innerX, innerY, 0).color(bgColor);
      }
      net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builder.end());
      
      // Рисуем прогресс с градиентом
      if (progress > 0.001f) {
         int progressSegments = (int) (segments * progress);
         int r = (progressColor >> 16) & 0xFF;
         int g = (progressColor >> 8) & 0xFF;
         int b = progressColor & 0xFF;
         
         builder = net.minecraft.client.render.Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_STRIP, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
         for (int i = 0; i <= progressSegments; i++) {
            float angle = startAngle + angleStep * i;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float outerX = centerX + cos * outerRadius;
            float outerY = centerY + sin * outerRadius;
            float innerX = centerX + cos * innerRadius;
            float innerY = centerY + sin * innerRadius;
            
            float gradientProgress = (float) i / progressSegments;
            float brightness = 1.0f - (gradientProgress * 0.3f);
            int currentR = (int)(r * brightness);
            int currentG = (int)(g * brightness);
            int currentB = (int)(b * brightness);
            int currentColor = (alpha << 24) | (currentR << 16) | (currentG << 8) | currentB;
            
            builder.vertex(matrix, outerX, outerY, 0).color(currentColor);
            builder.vertex(matrix, innerX, innerY, 0).color(currentColor);
         }
         net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builder.end());
      }
      
      org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_POLYGON_SMOOTH);
      org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
      com.mojang.blaze3d.systems.RenderSystem.disableBlend();
   }
   
   /**
    * Рисует сегмент кольца (тонкий круг) с градиентом
    */
   private void drawRingSegment(net.minecraft.client.render.BufferBuilder buffer, org.joml.Matrix4f matrix, float x, float y, float radius, float thickness, int segments, float startPercent, float endPercent, ColorRGBA color1, ColorRGBA color2) {
      if (endPercent <= startPercent) return;
      
      float startAngle = (float) (-Math.PI / 2.0 + 2.0 * Math.PI * startPercent);
      float endAngle = (float) (-Math.PI / 2.0 + 2.0 * Math.PI * endPercent);
      
      int segmentCount = (int) Math.ceil(segments * (endPercent - startPercent));
      float innerRadius = radius - thickness / 2.0F;
      float outerRadius = radius + thickness / 2.0F;
      
      for (int i = 0; i <= segmentCount; i++) {
         float t = (float) i / segmentCount;
         float angle = startAngle + (endAngle - startAngle) * t;
         float cos = (float) Math.cos(angle);
         float sin = (float) Math.sin(angle);
         
         // Интерполяция цвета
         ColorRGBA color = this.lerpColor(color1, color2, t);
         
         // Внутренняя точка
         buffer.vertex(matrix, x + cos * innerRadius, y + sin * innerRadius, 0)
               .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
         
         // Внешняя точка
         buffer.vertex(matrix, x + cos * outerRadius, y + sin * outerRadius, 0)
               .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
      }
   }

   /**
    * Линейная интерполяция между двумя цветами
    */
   private ColorRGBA lerpColor(ColorRGBA c1, ColorRGBA c2, float t) {
      int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t);
      int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
      int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
      int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
      return new ColorRGBA(r, g, b, a);
   }
}
