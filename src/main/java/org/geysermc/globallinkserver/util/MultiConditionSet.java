/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * A special list that checks every interval if its keys can be removed.
 * A key can only be removed if all provided conditions are satisfied.
 * Given an instance with three conditions, if the first condition fails for the given key the remaining conditions won't be checked for the key.
 * <p>
 * Every condition and the removal listeners are expected to be quick functions, as they block the removal check and thus the (potential) removal of the other keys.
 * In the case of {@link #remove(Object)} it blocks the caller thread instead
 * <p>
 * Make sure that the conditions and the removal listeners are thread safe (and the code its calling), because the executing thread is undefined.
 */
@NullMarked
public final class MultiConditionSet<K> {
    private final List<Object2BooleanFunction<K>> conditions = new ArrayList<>();
    private final Set<K> keys = Collections.synchronizedSet(new HashSet<>());
    private final List<Consumer<K>> removalListeners = new ArrayList<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public MultiConditionSet(int checkIntervalInMillis, Consumer<K> removalListener) {
        this.removalListeners.add(removalListener);
        this.executor.scheduleAtFixedRate(this::checkConditions, 0, checkIntervalInMillis, TimeUnit.MILLISECONDS);
    }

    private void checkConditions() {
        // should be fine to have everything in a synchronized block, since the concurrent access will be really limited
        // in a setting like this link server.
        synchronized (keys) {
            var iterator = keys.iterator();

            keys:
            while (iterator.hasNext()) {
                var key = iterator.next();
                for (Object2BooleanFunction<K> condition : conditions) {
                    if (!condition.apply(key)) {
                        continue keys;
                    }
                }
                // all conditions have been met, remove the key
                iterator.remove();
                for (Consumer<K> consumer : removalListeners) {
                    consumer.accept(key);
                }
            }
        }
    }

    public MultiConditionSet<K> addRemovalCondition(Object2BooleanFunction<K> condition) {
        conditions.add(condition);
        return this;
    }

    public MultiConditionSet<K> addRemovalListener(Consumer<K> removalListener) {
        removalListeners.add(removalListener);
        return this;
    }

    public void add(K key) {
        keys.add(key);
    }

    public void remove(K key) {
        keys.remove(key);
        for (Consumer<K> consumer : removalListeners) {
            consumer.accept(key);
        }
    }

    public void close() {
        executor.shutdown();
    }
}
