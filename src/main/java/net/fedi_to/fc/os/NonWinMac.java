package net.fedi_to.fc.os;

import net.fedi_to.fc.Fedicraft;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Helper for non-Windows, non-Mac platforms.
 */
public final class NonWinMac {
    /**
     * Opens the given URI.
     *
     * @param os  The OS. Used for opening the fallback URI (if needed).
     * @param uri The URI.
     * @return {@code true} if either the URI or the fallback were opened.
     */
    public static boolean openUri(Util.OperatingSystem os, URI uri) {
        fallback:
        try {
            // xdg-mime is required by xdg-open, which is also used by vanilla
            Process mime = Runtime.getRuntime().exec(new String[] {"xdg-mime", "query", "default", "x-scheme-handler/" + uri.getScheme()});
            mime.getErrorStream().close();
            mime.getOutputStream().close();
            if (!mime.waitFor(10, TimeUnit.SECONDS)) {
                // it should not take this long. use fallback instead.
                Fedicraft.LOGGER.warn("protocol handler query took too long - using fallback");
                break fallback;
            }
            if (mime.getInputStream().read() < 0) {
                Fedicraft.LOGGER.debug("no protocol handler - using fallback");
                // EOS reached - no handler exists. use fallback.
                break fallback;
            }
            Fedicraft.LOGGER.info("opening protocol handler for URI '{}'", uri);
            // handler exists, use it.
            Process open = Runtime.getRuntime().exec(new String[] {"xdg-open", uri.toASCIIString()});
            open.getInputStream().close();
            open.getErrorStream().close();
            open.getOutputStream().close();
            return true;
        } catch (IOException | InterruptedException ignored) {
        }

        try {
            URI fallbackUri = Fedicraft.getFallbackUri(uri);
            if (fallbackUri != null) {
                os.open(fallbackUri);
                return true;
            }
        } catch (URISyntaxException ignored) {
        }

        return false;
    }
}
