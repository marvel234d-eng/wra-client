package com.wra.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Forge 1.20.1 client-only features. No packet spoofing, anticheat bypass, or server-side modifications. */
public final class ClientFeatures {
    private ClientFeatures() {}

    public static final String[] CATEGORIES = {"COMBAT", "MOVEMENT", "RENDER", "PLAYER", "WORLD"};

    public static final class Feature {
        public final String name, description;
        public final int category;
        public boolean enabled;
        Feature(int category, String name, String description) {
            this.category = category; this.name = name; this.description = description;
        }
    }

    public static final List<Feature> FEATURES = new ArrayList<>();
    private static Feature add(int group, String name, String description) {
        Feature feature = new Feature(group, name, description);
        FEATURES.add(feature);
        return feature;
    }
    static {
        add(0, "CPS Counter", "Clicks per second tracker");
        add(0, "Cooldown HUD", "Attack recharge indicator");
        add(0, "Target Health", "Health of aimed living entity");
        add(0, "Crosshair Plus", "Colorful center crosshair");
        add(1, "Auto Sprint", "Sprint while moving forward");
        add(1, "Auto Sneak", "Keep sneak held automatically");
        add(1, "Auto Jump", "Jump while moving on the ground");
        add(1, "Auto Walk", "Keep forward movement active");
        add(2, "Fullbright", "Raise gamma in dark areas");
        add(2, "No Bob", "Disable camera bobbing");
        add(2, "Zoom", "Reduce field of view");
        add(2, "World Clock", "Display game time");
        add(3, "Auto Tool", "Select fastest mining hotbar tool");
        add(3, "Auto Eat", "Eat available food when hungry");
        add(3, "Auto Respawn", "Automatically request respawn");
        add(3, "Durability", "Display held item durability");
        add(4, "Coordinates", "Display XYZ coordinates");
        add(4, "Biome HUD", "Display current biome ID");
        add(4, "Light Level", "Display block light at your feet");
        add(4, "Compass", "Display facing direction");
    }

    public static boolean isOn(String name) {
        for (Feature f : FEATURES) if (f.name.equals(name)) return f.enabled;
        return false;
    }
    public static void toggle(Feature f) {
        f.enabled = !f.enabled;
        if (!f.enabled) restoreFeature(f.name);
    }

    public static final KeyMapping OPEN_MENU = new KeyMapping(
        "key.wraclient.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.wraclient");
    private static double previousGamma;
    private static int previousFov;
    private static boolean previousBob;
    private static boolean gammaModified, fovModified, bobModified;
    private static boolean autoEating;
    private static int previousFoodSlot = -1;
    private static long clickTime;
    private static final List<Long> clicks = new ArrayList<>();
    private static boolean lastAttackDown;

    @Mod.EventBusSubscriber(modid = WRAClient.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(OPEN_MENU); }
    }

    @Mod.EventBusSubscriber(modid = WRAClient.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class GameEvents {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_MENU.consumeClick()) {
                if (mc.screen instanceof WRAScreen) mc.setScreen(null);
                else if (mc.screen == null) mc.setScreen(new WRAScreen());
            }
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) return;
            boolean inGame = mc.screen == null;

            if (isOn("Fullbright")) {
                if (!gammaModified) { previousGamma = mc.options.gamma().get(); gammaModified = true; }
                mc.options.gamma().set(12.0);
            } else restoreGamma();
            if (isOn("Zoom")) {
                if (!fovModified) { previousFov = mc.options.fov().get(); fovModified = true; }
                mc.options.fov().set(30);
            } else restoreFov();
            if (isOn("No Bob")) {
                if (!bobModified) { previousBob = mc.options.bobView().get(); bobModified = true; }
                mc.options.bobView().set(false);
            } else restoreBob();

            // Only synthesize input while focused in the actual world.
            mc.options.keyShift.setDown(inGame && isOn("Auto Sneak") || (inGame && InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue())));
            if (isOn("Auto Walk")) mc.options.keyUp.setDown(inGame);
            if (!inGame) {
                stopEating(mc); lastAttackDown = false;
                if (isOn("Auto Respawn") && mc.screen instanceof DeathScreen) {
                    player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                }
                return;
            }
            if (isOn("Auto Sprint") && player.input.up && !player.isShiftKeyDown()) player.setSprinting(true);
            if (isOn("Auto Jump") && player.onGround() && player.input.up && !player.isShiftKeyDown()) player.jumpFromGround();

            if (isOn("Auto Tool") && mc.options.keyAttack.isDown() && mc.hitResult instanceof BlockHitResult hit) {
                BlockState state = mc.level.getBlockState(hit.getBlockPos());
                int bestSlot = player.getInventory().selected;
                float best = player.getInventory().getItem(bestSlot).getDestroySpeed(state);
                for (int i = 0; i < 9; i++) {
                    float speed = player.getInventory().getItem(i).getDestroySpeed(state);
                    if (speed > best) { best = speed; bestSlot = i; }
                }
                player.getInventory().selected = bestSlot;
            }

            boolean attacking = mc.options.keyAttack.isDown();
            if (attacking && !lastAttackDown) {
                clicks.add(System.currentTimeMillis());
                clickTime = System.currentTimeMillis();
            }
            lastAttackDown = attacking;
            clicks.removeIf(t -> System.currentTimeMillis() - t > 1000);

            if (isOn("Auto Eat") && player.getFoodData().needsFood() && (autoEating || !mc.options.keyUse.isDown())) {
                int foodSlot = -1;
                for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).isEdible()) { foodSlot = i; break; }
                if (foodSlot >= 0) {
                    if (!autoEating) { previousFoodSlot = player.getInventory().selected; autoEating = true; }
                    player.getInventory().selected = foodSlot;
                    mc.options.keyUse.setDown(true);
                }
            } else if (autoEating) stopEating(mc);
        }

        @SubscribeEvent public static void renderHud(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (p == null || mc.level == null || mc.screen instanceof WRAScreen) return;
            GuiGraphics g = event.getGuiGraphics();
            int y = 9;
            if (isOn("CPS Counter")) y = hud(g, mc, y, "CPS  " + clicks.size());
            if (isOn("Cooldown HUD")) y = hud(g, mc, y, "Attack  " + (int)(p.getAttackStrengthScale(0) * 100) + "%");
            if (isOn("Target Health") && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target)
                y = hud(g, mc, y, "Target  " + target.getName().getString() + "  " + (int)target.getHealth() + "/" + (int)target.getMaxHealth());
            if (isOn("World Clock")) y = hud(g, mc, y, "World Time  " + (mc.level.getDayTime() % 24000));
            if (isOn("Durability")) {
                ItemStack item = p.getMainHandItem();
                if (item.isDamageableItem()) y = hud(g, mc, y, "Durability  " + (item.getMaxDamage() - item.getDamageValue()) + "/" + item.getMaxDamage());
            }
            if (isOn("Coordinates")) y = hud(g, mc, y, String.format("XYZ  %.0f  %.0f  %.0f", p.getX(), p.getY(), p.getZ()));
            if (isOn("Biome HUD")) y = hud(g, mc, y, "Biome  " + mc.level.getBiome(p.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("unknown"));
            if (isOn("Light Level")) y = hud(g, mc, y, "Light  " + mc.level.getMaxLocalRawBrightness(p.blockPosition()));
            if (isOn("Compass")) y = hud(g, mc, y, "Facing  " + p.getDirection().getName());
            if (isOn("Crosshair Plus") && !mc.options.hideGui) {
                int x = g.guiWidth()/2, cy = g.guiHeight()/2;
                int color = 0xFF54F3DC;
                g.fill(x-8,cy-1,x-3,cy+1,color); g.fill(x+3,cy-1,x+8,cy+1,color);
                g.fill(x-1,cy-8,x+1,cy-3,color); g.fill(x-1,cy+3,x+1,cy+8,color);
            }
        }
    }

    private static int hud(GuiGraphics g, Minecraft mc, int y, String text) {
        int w = mc.font.width(text);
        g.fill(6, y-3, w+20, y+13, 0xBD101829);
        g.fill(6, y-3, 8, y+13, 0xFF61EEDB);
        g.drawString(mc.font, text, 13, y, 0xFFF1F8FF, false);
        return y + 19;
    }
    private static void restoreFeature(String name) {
        Minecraft mc = Minecraft.getInstance();
        switch (name) {
            case "Fullbright" -> restoreGamma();
            case "Zoom" -> restoreFov();
            case "No Bob" -> restoreBob();
            case "Auto Walk" -> mc.options.keyUp.setDown(false);
            case "Auto Sneak" -> mc.options.keyShift.setDown(false);
            case "Auto Eat" -> stopEating(mc);
        }
    }
    private static void restoreGamma() { if (gammaModified) { Minecraft.getInstance().options.gamma().set(previousGamma); gammaModified = false; } }
    private static void restoreFov() { if (fovModified) { Minecraft.getInstance().options.fov().set(previousFov); fovModified = false; } }
    private static void restoreBob() { if (bobModified) { Minecraft.getInstance().options.bobView().set(previousBob); bobModified = false; } }
    private static void stopEating(Minecraft mc) {
        if (!autoEating) return;
        mc.options.keyUse.setDown(false);
        if (mc.player != null && previousFoodSlot >= 0) mc.player.getInventory().selected = previousFoodSlot;
        previousFoodSlot = -1;
        autoEating = false;
    }
}
