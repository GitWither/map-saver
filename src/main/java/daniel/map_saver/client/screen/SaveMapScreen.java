package daniel.map_saver.client.screen;

import daniel.map_saver.MapSaver;
import daniel.map_saver.mixin.MapRendererInvoker;
import daniel.map_saver.mixin.MapTextureAccessor;
import daniel.map_saver.mixin.ScreenshotRecorderInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.File;
import java.io.IOException;

public class SaveMapScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier(MapSaver.MOD_ID, "textures/gui/map_saver_gui.png");
    private static final Text TITLE = Text.translatable("title.map_saver");
    private MapState mapState;
    private final int mapId;

    private final int backgroundWidth = 97;
    private final int backgroundHeight = 166;

    private int x;
    private int y;

    private int titleX;
    private int titleY;


    public SaveMapScreen(int mapId) {
        super(NarratorManager.EMPTY);
        this.mapId = mapId;
    }

    @Override
    protected void init() {

        this.mapState = FilledMapItem.getMapState(mapId, this.client.world);

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        this.titleX = (this.backgroundWidth - textRenderer.getWidth(TITLE)) / 2 + this.x;
        this.titleY = this.y + 4;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.map_saver.save"), (button) -> {
            MapRenderer.MapTexture txt = ((MapRendererInvoker)MinecraftClient.getInstance().gameRenderer.getMapRenderer()).invokeGetMapTexture(mapId, mapState);


            File screensDir = new File(MinecraftClient.getInstance().runDirectory, "map_screenshots");
            if(!screensDir.exists() && !screensDir.mkdir()) {
                MapSaver.LOGGER.error("Could not create directory " + screensDir.getAbsolutePath() + " cannot continue!");
                return;
            }

            //pls Mojang make functions like these public so I don't have to use invokers
            File screenshot = ScreenshotRecorderInvoker.invokeGetScreenshotFileName(screensDir);

            Util.getIoWorkerExecutor().execute(() -> {
                try {
                    ((MapTextureAccessor)txt).getNativeImage().getImage().writeTo(screenshot);

                    Text text = (Text.literal (screenshot.getName())).formatted(Formatting.UNDERLINE, Formatting.GREEN).styled((style) ->
                            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshot.getAbsolutePath())));
                    this.client.player.sendMessage(Text.translatable("map_saver.success", "Map #" + mapId, text), false);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            this.close();
        }).dimensions(this.x + 4, this.y + 105, 88, 20).build());

        super.init();
    }

    public void drawSaverBackground(DrawContext context) {
        context.drawTexture(BACKGROUND, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.drawSaverBackground(context);
        //4210752 is a magic number i found in code, means gray
        context.drawText(this.textRenderer, TITLE, this.titleX, this.titleY, 4210752, false);

        //this particular order of rendering functions is important for layering
        super.render(context, mouseX, mouseY, delta);

        context.getMatrices().push();

        //I spent 2 days trying to figure out why this wasn't working. Turns out you shouldn't scale before translating.
        context.getMatrices().translate(this.x + 4, this.y + 13, 1);
        context.getMatrices().scale(0.69f, 0.69f, 1f);


        VertexConsumerProvider.Immediate immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
        this.client.gameRenderer.getMapRenderer().draw(context.getMatrices(), immediate, this.mapId, this.mapState, false, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        immediate.draw();

        context.getMatrices().pop();
    }
}
