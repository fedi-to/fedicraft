package net.fedi_to.fc.mixin;

import net.fedi_to.fc.Fedicraft;
import net.fedi_to.fc.ffi.FcDll;
import net.fedi_to.fc.os.NonWinMac;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.util.Locale;

@Mixin(Util.OperatingSystem.class)
public class OperatingSystemMixin {
    @Inject(method = "open(Ljava/net/URI;)V", at = @At("HEAD"), cancellable = true)
    void fedicraft_openUri(URI uri, CallbackInfo cb) {
        Util.OperatingSystem os = (Util.OperatingSystem) (Object) this;
        if (uri.getScheme() != null && uri.getScheme().toLowerCase(Locale.ROOT).startsWith("web+")) {
            cb.cancel();
            if (!uri.getScheme().toLowerCase(Locale.ROOT).equals(uri.getScheme())) {
                // only accept lowercase
                Fedicraft.LOGGER.error("Couldn't open URI '{}' (scheme not normalized, should be lowercase)", uri);
                return;
            }
            switch (os) {
                case WINDOWS -> {
                    if (!FcDll.openUri(uri)) {
                        Fedicraft.LOGGER.error("Couldn't open URI '{}'", uri);
                    };
                }
                case OSX -> {
                    // not supported
                    Fedicraft.LOGGER.error("Couldn't open URI '{}'", uri);
                }
                default -> {
                    if (!NonWinMac.openUri(os, uri)) {
                        Fedicraft.LOGGER.error("Couldn't open URI '{}'", uri);
                    }
                }
            }
        }
    }
}
