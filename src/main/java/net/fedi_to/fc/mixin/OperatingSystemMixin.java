package net.fedi_to.fc.mixin;

import net.fedi_to.fc.Fedicraft;
import net.fedi_to.fc.ffi.FcDll;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Mixin(Util.OperatingSystem.class)
public class OperatingSystemMixin {
    @Inject(method = "open(Ljava/net/URI;)V", at = @At("HEAD"), cancellable = true)
    void fedicraft_openUri(URI uri, CallbackInfo cb) {
        Util.OperatingSystem os = (Util.OperatingSystem) (Object) this;
        if (uri.getScheme() != null && uri.getScheme().startsWith("web+")) {
            cb.cancel();
            switch (os) {
                case WINDOWS -> {
                    if (!FcDll.openUri(uri)) {
                        Fedicraft.LOGGER.error("Couldn't open uri '{}'", uri);
                    };
                }
                case OSX -> {
                    // not supported
                }
                default -> {
                    try {
                        Desktop.getDesktop().browse(uri);
                    } catch (UnsupportedOperationException | IOException exception) {
                        try {
                            URI fallbackUri = Fedicraft.getFallbackUri(uri);
                            if (fallbackUri == null) {
                                Fedicraft.LOGGER.error("Couldn't open uri '{}'", uri, exception);
                            } else {
                                os.open(fallbackUri);
                            }
                        } catch (URISyntaxException e) {
                            Fedicraft.LOGGER.error("Couldn't open uri '{}'", uri, e);
                        }
                    }
                }
            }
        }
    }
}
