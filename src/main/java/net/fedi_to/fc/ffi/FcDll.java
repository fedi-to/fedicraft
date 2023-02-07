package net.fedi_to.fc.ffi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.LibFFI;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Environment(EnvType.CLIENT)
public class FcDll {
    private static final SharedLibrary library;
    private static final int size;
    private static final long function;

    private static final FFICIF cif;

    static {
        SharedLibrary lib;
        int sz;
        try {
            lib = Library.loadNative(FcDll.class, "", "fc_x86.dll");
            sz = 4;
        } catch (UnsatisfiedLinkError a) {
            try {
                lib = Library.loadNative(FcDll.class, "", "fc_x86_64.dll");
                sz = 8;
            } catch (UnsatisfiedLinkError b) {
                throw new IllegalStateException("Couldn't load x86 or x86_64 DLL.");
            }
        }
        size = sz;
        library = lib;
        function = library.getFunctionAddress("fc_open_uri");
        if (function == 0L) {
            throw new IllegalStateException("Couldn't find fc_open_uri function.");
        }

        // leak these since they are static final.
        MemoryUtil.MemoryAllocator allocator = MemoryUtil.getAllocator();
        cif = FFICIF.create(allocator.malloc(FFICIF.SIZEOF));
        PointerBuffer atypes = PointerBuffer.create(allocator.malloc(Pointer.POINTER_SIZE), 1);
        atypes.put(LibFFI.ffi_type_pointer);
        if (LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, LibFFI.ffi_type_sint32, atypes) != LibFFI.FFI_OK) {
            throw new RuntimeException("unreachable");
        }
    }

    public static boolean openUri(URI uri) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size);
        bb.order(ByteOrder.nativeOrder());
        PointerBuffer avalues = PointerBuffer.allocateDirect(1);
        ByteBuffer byteBuffer = MemoryUtil.memUTF8(uri.toString());
        try {
            PointerBuffer s = MemoryUtil.memAllocPointer(1);
            try {
                s.put(byteBuffer);
                avalues.put(s);
                LibFFI.ffi_call(cif, function, bb, avalues);
            } finally {
                MemoryUtil.memFree(s);
            }
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }
        return switch (size) {
            case 4 -> bb.getInt() != 0;
            case 8 -> bb.getLong() != 0;
            default -> throw new RuntimeException("unreachable");
        };
    }
}
