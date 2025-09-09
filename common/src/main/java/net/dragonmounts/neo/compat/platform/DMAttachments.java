package net.dragonmounts.neo.compat.platform;

import net.dragonmounts.neo.common.capability.FluteHolder;
import net.dragonmounts.neo.compat.Dummy;

@SuppressWarnings("unused")
public class DMAttachments {
    public static final AttachmentType<FluteHolder> FLUTE_HOLDER = Dummy.get();

    public static <T> boolean has(Object host, AttachmentType<T> type) {
        return Dummy.get();
    }

    public static <T> T get(Object host, AttachmentType<T> type) {
        return Dummy.get();
    }

    public static <T> T getOrCreate(Object host, AttachmentType<T> type) {
        return Dummy.get();
    }

    public interface AttachmentType<T> {}
}
