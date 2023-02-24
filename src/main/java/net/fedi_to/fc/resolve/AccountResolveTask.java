package net.fedi_to.fc.resolve;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static net.fedi_to.fc.Fedicraft.*;

public record AccountResolveTask(String account, String host, Consumer<String> callback) implements Runnable {
    @Override
    public void run() {
        // this is pretty unsafe actually but we Expect these to have a restricted charset
        var acct = "acct:" + account + "@" + host;
        URI acctUri;
        try {
            acctUri = new URI(acct);
        } catch (URISyntaxException e) {
            return;
        }
        LOGGER.debug("attempting to resolve {}", acct);

        var req = HttpRequest.newBuilder();
        req.GET();
        try {
            req.uri(new URI("https://" + host + "/.well-known/webfinger?resource=" + COMPONENT_ESCAPER.escape(acct)));
        } catch (URISyntaxException e) {
            return;
        }
        try {
            // FIXME proxy settings
            var body = HttpClient.newBuilder().build().send(req.build(), HttpResponse.BodyHandlers.ofString()).body();
            var json = new Gson().fromJson(body, JsonElement.class);
            if (!json.isJsonObject()) {
                return;
            }
            var webfinger = json.getAsJsonObject();
            var subject = webfinger.get("subject");
            if (!subject.isJsonPrimitive()) {
                return;
            }
            if (!subject.getAsJsonPrimitive().isString()) {
                return;
            }
            if (!new URI(subject.getAsString()).equals(acctUri)) {
                return;
            }
            var links = webfinger.get("links");
            if (!links.isJsonArray()) {
                return;
            }
            StreamSupport.stream(links.getAsJsonArray().spliterator(), false)
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .filter(link -> {
                    var rel = link.get("rel");
                    if (!rel.isJsonPrimitive()) {
                        return false;
                    }
                    if (!rel.getAsJsonPrimitive().isString()) {
                        return false;
                    }
                    if (!rel.getAsString().equals("self")) {
                        return false;
                    }
                    var href = link.get("href");
                    if (!href.isJsonPrimitive()) {
                        return false;
                    }
                    if (!href.getAsJsonPrimitive().isString()) {
                        return false;
                    }
                    var type = link.get("type");
                    if (!type.isJsonPrimitive()) {
                        return false;
                    }
                    if (!type.getAsJsonPrimitive().isString()) {
                        return false;
                    }
                    return type.getAsString().equals("application/activity+json")
                        || type.getAsString().equals("application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"");
                })
                .map(link -> link.get("href").getAsString())
                .findFirst()
                .ifPresent(targetUri -> {
                    // we could do a whole *thing* here but instead we're just gonna assume this is a mastodon-compatible
                    // webfinger response and just do the checks we actually care about
                    try {
                        URI uri = new URI(targetUri);
                        if (!"https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
                            return;
                        }
                    } catch (URISyntaxException e) {
                        return;
                    }
                    var webap = "web+ap" + targetUri.substring(5);
                    var webapcheck = HttpRequest.newBuilder();
                    webapcheck.GET();
                    try {
                        URI uri = getFallbackUri(URI.create(webap));
                        if (uri == null) {
                            return;
                        }
                        webapcheck.uri(uri);
                    } catch (URISyntaxException e) {
                        return;
                    }
                    try {
                        // FIXME proxy settings
                        var status = HttpClient.newBuilder().build().send(webapcheck.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
                        if (status == 200 || status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                            callback.accept(webap);
                        } else {
                            LOGGER.warn("The server at " + host + " is not compatible with FediCraft. Not linking to @" + account + "@" + host);
                        }
                    } catch (IOException | InterruptedException | JsonSyntaxException ignored) {
                    }
                });
        } catch (IOException | InterruptedException | JsonSyntaxException | URISyntaxException ignored) {
        }
    }
}
