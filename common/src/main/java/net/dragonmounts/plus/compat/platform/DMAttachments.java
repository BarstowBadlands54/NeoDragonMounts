package net.dragonmounts.plus.compat.platform;

import net.dragonmounts.plus.common.capability.FluteHolder;
import net.dragonmounts.plus.compat.Dummy;
import org.apache.commons.lang3.NotImplementedException;

@SuppressWarnings("unused")
public class DMAttachments {
    public static final AttachmentType<FluteHolder> FLUTE_HOLDER = Dummy.get();

    public static <T> boolean has(Object host, AttachmentType<T> type) {
        throw new NotImplementedException();
    }

    public static <T> T get(Object host, AttachmentType<T> type) {
        throw new NotImplementedException();
    }

    public static <T> T getOrCreate(Object host, AttachmentType<T> type) {
        throw new NotImplementedException();
    }

    public interface AttachmentType<T> {}
}
