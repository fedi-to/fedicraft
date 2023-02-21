package net.fedi_to.fc.ffi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fedi_to.fc.Fedicraft;
import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;

import java.net.URI;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;

@Environment(EnvType.CLIENT)
public class FcDll {
    private static final SharedLibrary LIBRARY;
    private static final long FUNCTION;

    static {
        SharedLibrary lib;
        try {
            Fedicraft.LOGGER.debug("Trying to load x86 DLL.");
            lib = Library.loadNative(FcDll.class, "", "fc_x86.dll");
        } catch (UnsatisfiedLinkError a) {
            Fedicraft.LOGGER.debug("Couldn't load x86 DLL, trying x86_64.");
            try {
                lib = Library.loadNative(FcDll.class, "", "fc_x86_64.dll");
            } catch (UnsatisfiedLinkError b) {
                throw new IllegalStateException("Couldn't load x86 or x86_64 DLL.");
            }
        }
        LIBRARY = lib;
        FUNCTION = LIBRARY.getFunctionAddress("fc_open_uri");
        if (FUNCTION == MemoryUtil.NULL) {
            throw new IllegalStateException("Couldn't find fc_open_uri function.");
        }
    }

    public static boolean openUri(URI uri) {
        ByteBuffer byteBuffer = MemoryUtil.memUTF8(uri.toString());
        try {
            return JNI.invokePI(memAddress(byteBuffer), FUNCTION) != 0;
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }
    }
}
