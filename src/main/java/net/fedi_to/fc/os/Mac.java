package net.fedi_to.fc.os;

import net.fedi_to.fc.Fedicraft;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Helper for non-Windows, non-Mac platforms.
 */
public final class Mac {
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
            // try to open the thing, but monitor stderr
            Process open = Runtime.getRuntime().exec(new String[] {"open", uri.toASCIIString()});
            open.getInputStream().close();
            open.getOutputStream().close();
            try (var reader = open.errorReader(Charset.defaultCharset())) {
                if (!open.waitFor(10, TimeUnit.SECONDS)) {
                    // it should not take this long. assume it opened.
                    Fedicraft.LOGGER.debug("open took too long, assuming it opened.");
                    return true;
                }
                var line = reader.readLine();
                if (line != null && line.contains(uri.toASCIIString())) {
                    Fedicraft.LOGGER.debug("no protocol handler - using fallback");
                    // found an error with the uri, open fallback
                    break fallback;
                }
            }
            Fedicraft.LOGGER.info("opened protocol handler for URI '{}'", uri);
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
