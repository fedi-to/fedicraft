package net.fedi_to.fc;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class Fedicraft implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("fedicraft");

    @Environment(EnvType.CLIENT)
    // ref: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent
    private static final Escaper COMPONENT_ESCAPER = new PercentEscaper("-_.!~*'()", false);

    @Environment(EnvType.CLIENT)
    public static URI getFallbackUri(URI uri) throws URISyntaxException {
        // this is a bit more strict than how fedi-to does it but it's okay.
        if (uri.getRawAuthority() == null || uri.getRawAuthority().isEmpty()) {
            return null;
        }
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.setScheme("https");
        uriBuilder.setUserInfo(null);
        uriBuilder.setPath("/.well-known/protocol-handler");
        uriBuilder.setFragment(null);
        uriBuilder.setCustomQuery(null);
        return new URI(uriBuilder.toString() + "?target=" + COMPONENT_ESCAPER.escape(uri.toString()));
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("This software is made with love by a queer trans person.");
    }

    // test with /tellraw @a {"text":"Add Epic text here","color":"#02FF00","clickEvent":{"action":"open_url","value":"web+ganarchy://ganarchy.autistic.space/cc1081c3b8a81d2311bff299da2769da4065b394"}}
}
