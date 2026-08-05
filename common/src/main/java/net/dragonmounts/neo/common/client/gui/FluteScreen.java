package net.dragonmounts.neo.common.client.gui;

import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.common.network.c2s.RecallDragonHomePayload;
import net.dragonmounts.neo.common.network.c2s.SetDragonHomePayload;
import net.dragonmounts.neo.common.network.c2s.TeleportDragonPayload;
import net.dragonmounts.neo.common.network.c2s.ToggleFollowingPayload;
import net.dragonmounts.neo.common.network.c2s.ToggleSittingByUUIDPayload;
import net.dragonmounts.neo.compat.platform.ClientNetworkHandler;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FluteScreen extends Screen {
    private static final int BUTTON_SPACING = 8;
    private static final int BUTTON_WIDTH = 210;
    private static final Component TITLE = Component.translatable("gui.neodragonmounts.flute");
    private static final Component TELEPORT_TO_PLAYER = Component.translatable("button.neodragonmounts.teleport_to_player");
    private static final Component TOGGLE_SITING = Component.translatable("button.neodragonmounts.toggle_siting");
    private static final Component TOGGLE_FOLLOWING = Component.translatable("button.neodragonmounts.toggle_following");
    private static final Component SET_HOME = Component.translatable("button.neodragonmounts.set_home");
    private static final Component SEND_HOME = Component.translatable("button.neodragonmounts.send_home");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    public final UUID uuid;
    /**
     * The flute's cached copy of the dragon's home, or null if it has none. Read from the item
     * rather than the entity because the dragon is usually out of client tracking range by the
     * time anyone reaches for this button.
     */
    public final @Nullable GlobalPos home;

    public FluteScreen(UUID uuid, @Nullable GlobalPos home) {
        super(TITLE);
        this.uuid = uuid;
        this.home = home;
    }

    @Override
    protected void init() {
        super.init();
        this.layout.addTitleHeader(TITLE, this.font);
        LinearLayout linear = this.layout.addToContents(LinearLayout.vertical()).spacing(BUTTON_SPACING);
        linear.defaultCellSetting().alignHorizontallyCenter();
        linear.addChild(Button.builder(
                TELEPORT_TO_PLAYER,
                this::teleportDragon
        ).width(BUTTON_WIDTH).build());
        linear.addChild(Button.builder(
                TOGGLE_SITING,
                this::toggleSiting
        ).width(BUTTON_WIDTH).build());
        linear.addChild(Button.builder(
                TOGGLE_FOLLOWING,
                this::toggleFollowing
        ).width(BUTTON_WIDTH).build());
        var homeButton = Button.builder(
                this.home == null ? SET_HOME : SEND_HOME,
                this::useHome
        ).width(BUTTON_WIDTH);
        if (this.home != null) {
            var pos = this.home.pos();
            homeButton.tooltip(Tooltip.create(Component.translatable(
                    "button.neodragonmounts.send_home.tooltip", pos.getX(), pos.getY(), pos.getZ()
            )));
        } else {
            homeButton.tooltip(Tooltip.create(Component.translatable("button.neodragonmounts.set_home.tooltip")));
        }
        linear.addChild(homeButton.build());
        this.layout.addToFooter(Button.builder(
                CommonComponents.GUI_CANCEL,
                button -> this.onClose()
        ).width(Button.BIG_WIDTH).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public boolean isPauseScreen() {
        return ClientConfig.INSTANCE.pauseOnFluting.get();
    }

    public void teleportDragon(@Nullable Button ignored) {
        assert this.minecraft != null;
        if (this.minecraft.hitResult instanceof BlockHitResult hit) {
            ClientNetworkHandler.send(new TeleportDragonPayload(this.uuid, hit.getBlockPos()));
        } else {
            this.minecraft.gui.setOverlayMessage(Component.translatable("message.neodragonmounts.flute.invalid_pos"), false);
            var player = this.minecraft.player;
            if (player != null) {
                player.clientLevel.playLocalSound(player, DMSounds.FLUTE_BLOW_LONG, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        this.onClose();
    }

    public void toggleSiting(@Nullable Button ignored) {
        ClientNetworkHandler.send(new ToggleSittingByUUIDPayload(this.uuid));
        this.onClose();
    }

    public void toggleFollowing(@Nullable Button ignored) {
        ClientNetworkHandler.send(new ToggleFollowingPayload(this.uuid));
        this.onClose();
    }

    /**
     * One button, two jobs. With no home recorded it marks one out; with a home recorded it
     * sends the dragon back to it. Unlike the teleport button this has no failure case for
     * "not looking at a block" — an unusable hit result just falls back to the player's own
     * feet, which is where you would stand to declare a nest anyway.
     */
    public void useHome(@Nullable Button ignored) {
        assert this.minecraft != null;
        if (this.home != null) {
            ClientNetworkHandler.send(new RecallDragonHomePayload(this.uuid));
            this.onClose();
            return;
        }
        BlockPos pos = this.minecraft.hitResult instanceof BlockHitResult hit ? hit.getBlockPos() : null;
        if (pos == null) {
            var player = this.minecraft.player;
            if (player != null) pos = player.blockPosition();
        }
        if (pos != null) {
            ClientNetworkHandler.send(new SetDragonHomePayload(this.uuid, pos));
        }
        this.onClose();
    }
}
