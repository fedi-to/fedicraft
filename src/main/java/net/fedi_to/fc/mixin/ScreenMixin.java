package net.fedi_to.fc.mixin;

import com.google.common.collect.Sets;
import net.fedi_to.fc.Fedicraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Screen.class)
public class ScreenMixin {
    @Redirect(
		method = "handleTextClick",
		at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z")
	)
    private boolean fedicraft_allowedProtocolsCheck(Set<?> set, Object object) {
        if (object instanceof String string) {
            return string.startsWith("web+") && string.codePoints().skip(4).allMatch(i -> 'a' <= i && i <= 'z') || set.contains(object);
        }
        return set.contains(object);
    }
}
