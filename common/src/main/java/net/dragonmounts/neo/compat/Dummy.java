package net.dragonmounts.neo.compat;

public interface Dummy {
    @SuppressWarnings("InfiniteRecursion")
    static <T> T get() {
        return get();
    }
}