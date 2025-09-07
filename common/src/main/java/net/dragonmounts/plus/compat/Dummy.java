package net.dragonmounts.plus.compat;

public interface Dummy {
    @SuppressWarnings("InfiniteRecursion")
    static <T> T get() {
        return get();
    }
}