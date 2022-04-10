package daniel.map_saver.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.text2speech.Narrator;
import daniel.map_saver.MapSaver;
import daniel.map_saver.mixin.MapRendererInvoker;
import daniel.map_saver.mixin.MapTextureAccessor;
import daniel.map_saver.mixin.ScreenshotRecorderInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.chunk.light.LightingProvider;

import javax.imageio.ImageTranscoder;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SaveMapScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier(MapSaver.MOD_ID, "textures/gui/map_saver_gui.png");
    private static final Text TITLE = new TranslatableText("title.map_saver");
    private MapState mapState;
    private final int mapId;

    private final int backgroundWidth = 97;
    private final int backgroundHeight = 166;

    private int x;
    private int y;

    private int titleX;
    private int titleY;


    public SaveMapScreen(MapState mapState, int mapId) {
        super(NarratorManager.EMPTY);

        //this.mapState = mapState;
        this.mapId = mapId;

    }

    @Override
    protected void init() {

        this.mapState = FilledMapItem.getMapState(mapId, this.client.world);

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        this.titleX = (this.backgroundWidth - textRenderer.getWidth(TITLE)) / 2 + this.x;
        this.titleY = this.y + 4;

        this.addDrawableChild(new ButtonWidget(this.x + 4, this.y + 105, 88, 20, new TranslatableText("button.map_saver.save"), (button) -> {
            MapRenderer.MapTexture txt = ((MapRendererInvoker)MinecraftClient.getInstance().gameRenderer.getMapRenderer()).invokeGetMapTexture(mapId, mapState);


            File screensDir = new File(MinecraftClient.getInstance().runDirectory, "map_screenshots");
            screensDir.mkdir();

            //pls Mojang make functions like these public so I don't have to use invokers
            File screenshot = ScreenshotRecorderInvoker.invokeGetScreenshotFileName(screensDir);

            Util.getIoWorkerExecutor().execute(() -> {
                try {
                    ((MapTextureAccessor)txt).getNativeImage().getImage().writeTo(screenshot);

                    Text text = (new LiteralText(screenshot.getName())).formatted(Formatting.UNDERLINE, Formatting.GREEN).styled((style) -> {
                        return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshot.getAbsolutePath()));
                    });
                    this.client.player.sendMessage(new TranslatableText("map_saver.success", "Map #" + mapId, text), false);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            this.close();
        }));

        super.init();
    }

    public void drawSaverBackground(MatrixStack matrices) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BACKGROUND);

        this.drawTexture(matrices, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.drawSaverBackground(matrices);
        //4210752 is a magic number i found in code, means gray
        this.textRenderer.draw(matrices, TITLE, this.titleX, this.titleY, 4210752);

        //this particular order of rendering functions is important for layering
        super.render(matrices, mouseX, mouseY, delta);

        matrices.push();

        //I spent 2 days trying to figure out why this wasn't working. Turns out you shouldn't scale before translating.
        matrices.translate(this.x + 4, this.y + 13, 1);
        matrices.scale(0.69f, 0.69f, 1f);


        VertexConsumerProvider.Immediate immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
        this.client.gameRenderer.getMapRenderer().draw(matrices, immediate, this.mapId, this.mapState, false, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        immediate.draw();

        matrices.pop();
    }
}
