package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.hud.elements.component.SwapIndicatorComponent;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.KeySetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.utility.game.other.InventoryUtil;
import tech.javelin.utility.math.StopWatch;

@ModuleAnnotation(
   name = "AutoSwap",
   category = Category.COMBAT,
   description = "крутой авто свап!"
)
public final class AutoSwap extends Module {
   public static final AutoSwap INSTANCE = new AutoSwap();
   
   public final ModeSetting firstItemSetting = new ModeSetting("Первый предмет", new String[]{"Шар", "Золотое яблоко", "Щит", "Тотем"});
   public final ModeSetting secondItemSetting = new ModeSetting("Второй предмет", new String[]{"Тотем 2", "Золотое яблоко 2", "Щит 2", "Шар 2"});
   public final KeySetting bind = new KeySetting("Кнопка", -1);
   public final BooleanSetting swaprender = new BooleanSetting("Показ свапа", true);
   public final BooleanSetting onlyEnchanted = new BooleanSetting("Только Чар. тотемы", false);
   
   private boolean isFirstItem = true;
   private boolean triggerSwap;
   private final StopWatch swapWatch = new StopWatch();
   private final StopWatch swapWatchK = new StopWatch();
   private boolean bypassActive;
   private boolean bypassSwapped;
   private int bypassSlot = -1;
   private String bypassItemName = "";
   private int freezeTicks = 0;
   private int inventoryDelayTicks = 0;
   private int inventoryOpenTicks = 0;
   private boolean inventoryOpened = false;
   private SwapIndicatorComponent swapIndicator;
   
   public void setSwapIndicator(SwapIndicatorComponent indicator) {
      this.swapIndicator = indicator;
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (mc.player == null) return;
      
      // Полная остановка на 3-4 тика во время свапа
      if (freezeTicks > 0) {
         freezeTicks--;
         mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
         return;
      }
      
      ScreenHandler screenHandler = mc.player.playerScreenHandler;

      if (bypassActive) {
         // Задержка перед открытием инвентаря (2-3 тика)
         if (inventoryDelayTicks > 0) {
            inventoryDelayTicks--;
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
         }
         
         if (!inventoryOpened) {
            // Открываем инвентарь
            mc.setScreen(new InventoryScreen(mc.player));
            inventoryOpened = true;
            inventoryOpenTicks = 4; // Задержка 4 тика в открытом инвентаре
            return;
         }
         
         // Ждем пока пройдет время в открытом инвентаре
         if (inventoryOpenTicks > 0) {
            inventoryOpenTicks--;
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
         }
         
         if (!bypassSwapped && bypassSlot != -1) {
            int syncSlot = bypassSlot < 9 ? bypassSlot + 36 : bypassSlot;
            boolean wasSprinting = mc.player.isSprinting();
            
            if (wasSprinting) {
               mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            
            mc.interactionManager.clickSlot(screenHandler.syncId, syncSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(screenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(screenHandler.syncId, syncSlot, 0, SlotActionType.PICKUP, mc.player);
            
            // Закрываем инвентарь
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(screenHandler.syncId));
            mc.setScreen(null);
            
            if (wasSprinting) {
               mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
            
            if (swaprender.isEnabled()) {
               // Добавляем в SwapIndicator вместо уведомления
               if (swapIndicator != null) {
                  ItemStack swappedItem = mc.player.getInventory().getStack(bypassSlot);
                  swapIndicator.addSwap(bypassItemName, swappedItem);
               }
            }
            
            bypassSwapped = true;
            freezeTicks = 4; // Заморозка на 4 тика после свапа
            swapWatch.reset();
         }
         
         if (bypassSwapped && swapWatch.getElapsedTime() >= 35) {
            bypassActive = false;
            bypassSwapped = false;
            bypassSlot = -1;
            inventoryOpened = false;
         }
         return;
      }

      if (this.triggerSwap) {
         String selectedMode = isFirstItem ? firstItemSetting.get() : secondItemSetting.get();
         String modeBase = selectedMode.replace(" 2", "");
         
         switch (modeBase) {
            case "Шар" -> this.prepareSwap(Items.PLAYER_HEAD, "Шар", false);
            case "Тотем" -> this.prepareSwap(Items.TOTEM_OF_UNDYING, "Тотем", onlyEnchanted.isEnabled());
            case "Золотое яблоко" -> this.prepareSwap(Items.GOLDEN_APPLE, "Золотое яблоко", false);
            case "Щит" -> this.prepareSwap(Items.SHIELD, "Щит", false);
         }
         
         this.isFirstItem = !this.isFirstItem;
         this.triggerSwap = false;
      }
   }

   @EventTarget
   public void input(EventKey event) {
      if (mc.currentScreen == null && swapWatchK.getElapsedTime() >= 300) {
         if (event.isKeyDown(bind.getKeyCode())) {
            this.triggerSwap = true;
            swapWatchK.reset();
         }
      }
   }

   private void prepareSwap(Item item, String itemName, boolean enchTotem) {
      int slot = item == Items.TOTEM_OF_UNDYING ? findTotem(enchTotem) : InventoryUtil.findItem(item, 0, 35);
      
      if (slot != -1) {
         bypassActive = true;
         bypassSwapped = false;
         bypassSlot = slot;
         bypassItemName = itemName;
         inventoryDelayTicks = 3; // Задержка 3 тика перед открытием инвентаря
         swapWatch.reset();
      } else {
         if (swaprender.isEnabled()) {
            Javelin.getInstance().getNotifyManager().addNotification("\uf06a", Text.literal("Предмет " + itemName + " не найден!"));
         }
      }
   }

   private int findTotem(boolean enchanted) {
      for (int i = 35; i >= 0; i--) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            if (enchanted && !stack.hasEnchantments()) continue;
            return i;
         }
      }
      return -1;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      bypassActive = false;
      bypassSwapped = false;
      bypassSlot = -1;
      isFirstItem = true;
      freezeTicks = 0;
      inventoryDelayTicks = 0;
      inventoryOpenTicks = 0;
      inventoryOpened = false;
   }
}
