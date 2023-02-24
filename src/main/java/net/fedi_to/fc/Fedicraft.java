package net.fedi_to.fc;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fedi_to.fc.resolve.AccountResolveTask;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Fedicraft implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("fedicraft");

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 4, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), new ThreadPoolExecutor.DiscardOldestPolicy());

    // ref: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent
    public static final Escaper COMPONENT_ESCAPER = new PercentEscaper("-_.!~*'()", false);

    private static final String LDH = "[a-zA-Z0-9-]";
    // maybe a domain name, and also maybe ipv4
    private static final String MAYBE_DOMAIN = "(?:" + LDH + "{0,63}\\.)*" + LDH + "{0,63}";
    private static final String MAYBE_IPV6 = "\\[[0-9a-fA-F:]*]";
    private static final String HOST = "(" + MAYBE_DOMAIN + "|" + MAYBE_IPV6 + ")";
    private static final String ACCOUNT = "([a-zA-Z0-9_]+)";
    // FIXME IDNA
    private static final Pattern FEDI_PATTERN = Pattern.compile("@" + ACCOUNT + "@" + HOST + "(?:[ )\\]}.,;+*&'\"]|$)");

    public static URI getFallbackUri(URI uri) throws URISyntaxException {
        // this is a bit more strict than how fedi-to does it but it's okay.
        if (uri.getRawAuthority() == null || uri.getRawAuthority().isEmpty()) {
            return null;
        }
        URI fallbackBase = new URI("https", null, uri.getHost(), uri.getPort(), "/.well-known/protocol-handler", null, null);
        return new URI(fallbackBase.toString() + "?target=" + COMPONENT_ESCAPER.escape(uri.toString()));
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("This software is made with love by a queer trans person.");

        EXECUTOR.allowCoreThreadTimeOut(true);

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            FEDI_PATTERN.matcher(message.getContent().getString()).results().limit(3).map(matchResult -> {
                var account = matchResult.group(1);
                var host = matchResult.group(2);
                var server = sender.getServer();
                return new AccountResolveTask(account, host, (webap) -> {
                    var newMessage = Texts.bracketed(Text.literal("@" + account + "@" + host).styled(uri -> {
                        return uri.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, webap));
                    }));
                    server.executeSync(() -> {
                        server.getPlayerManager().broadcast(Text.translatable("fedicraft.account.link", newMessage), false);
                    });
                });
            }).forEach(EXECUTOR::execute);
        });
    }

    // test with /tellraw @a {"text":"Add Epic text here","color":"#02FF00","clickEvent":{"action":"open_url","value":"web+ganarchy://ganarchy.autistic.space/cc1081c3b8a81d2311bff299da2769da4065b394"}}
}
