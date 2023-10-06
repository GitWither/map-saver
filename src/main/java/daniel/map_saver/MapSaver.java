package daniel.map_saver;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MapSaver implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "map_saver";

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initialized Map Saver");
    }
}
