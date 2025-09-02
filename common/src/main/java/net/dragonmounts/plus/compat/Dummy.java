package net.dragonmounts.plus.compat;

import org.apache.commons.lang3.NotImplementedException;

public interface Dummy {
    static <T> T get() {
        throw new NotImplementedException();
    }
}
