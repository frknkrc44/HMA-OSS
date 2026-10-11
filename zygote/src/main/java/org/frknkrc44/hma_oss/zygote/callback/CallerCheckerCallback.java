package org.frknkrc44.hma_oss.zygote.callback;

@FunctionalInterface
public interface CallerCheckerCallback {
    boolean accept(String caller);
}
