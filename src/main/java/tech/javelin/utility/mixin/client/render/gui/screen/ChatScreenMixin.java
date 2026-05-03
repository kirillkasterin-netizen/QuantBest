package tech.javelin.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.javelin.utility.interfaces.IMinecraft;

@Mixin({ChatScreen.class})
public class ChatScreenMixin extends Screen implements IMinecraft {
   protected ChatScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"sendMessage(Ljava/lang/String;Z)V"},
      at = {@At("HEAD")},
      cancellable = false
   )
   private void onSendMessage(String text, boolean addToHistory, CallbackInfo ci) {
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"))
   private void onMouseClicked(double mouseX, double mouseY, int button, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
       if (button == 1) { // 1 = Right click
           tech.javelin.client.hud.elements.component.WatermarkComponent.isIslandMode = !tech.javelin.client.hud.elements.component.WatermarkComponent.isIslandMode;
       }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRender(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
       String mode = tech.javelin.client.hud.elements.component.WatermarkComponent.isIslandMode ? "Mode: Island" : "Mode: Crystal";
       int w = this.client.getWindow().getScaledWidth();
       int h = this.client.getWindow().getScaledHeight();
       
       tech.javelin.utility.render.display.base.color.ColorRGBA color = tech.javelin.Javelin.getInstance().getThemeManager().getCurrentTheme().getColor();
       
       context.drawText(this.client.textRenderer, mode, w - this.client.textRenderer.getWidth(mode) - 5, h - 15, color.getRGB(), true);
   }
}
