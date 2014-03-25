/**
 * Copyright 2009 - 2011 Sergio Bossa (sergio.bossa@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package terrastore.store;

import terrastore.store.features.Update;
import java.util.Map;
import java.util.Set;
import terrastore.event.EventBus;
import terrastore.server.Keys;
import terrastore.server.Values;
import terrastore.store.features.Mapper;
import terrastore.store.features.Predicate;
import terrastore.store.operators.Function;
import terrastore.store.features.Range;
import terrastore.store.operators.Comparator;
import terrastore.store.operators.Condition;

/**
 * Bucket interface collecting and managing key/value entries.
 *
 * @author Sergio Bossa
 */
public interface Bucket {

    /**
     * Get the bucket name.
     *
     * @return The bucket name.
     */
    public String getName();

    /**
     * Put the given {@link Value} into this bucket under the given key, eventually replacing the old one.<br>
     * This publishes a {@link terrastore.event.ValueChangedEvent} to the {@link terrastore.event.EventBus}.
     *
     * @param key The key of the value to put.
     * @param value The value to put.
     */
    public void put(Key key, Value value);

    /**
     * Put the given {@link Value} into this bucket under the given key only if no value existed before,
     * or the existent value satisfies the given {@link terrastore.store.features.Predicate}.<br>
     * This publishes a {@link terrastore.event.ValueChangedEvent} to the {@link terrastore.event.EventBus} if the value is actually put.
     *
     * @param key The key of the value to put.
     * @param value The value to put.
     * @param predicate The predicate object containing data about the condition to evaluate on the old value.
     * @return True if the value has been actually put, false otherwise.
     * @throws StoreOperationException If unable to execute the conditional put.
     */
    public boolean conditionalPut(Key key, Value value, Predicate predicate) throws StoreOperationException;

    /**
     * Get the {@link Value} under the given key.
     *
     * @param key The key of the value to get.
     * @return The value corresponding to the given key.
     * @throws StoreOperationException If no value is found for the given key.
     */
    public Value get(Key key) throws StoreOperationException;

    /**
     * Get the {@link Value}s corresponding to the given set of keys.
     *
     * @param keys The key set.
     * @return The values corresponding to the given keys.
     * @throws StoreOperationException If no value is found for the given key.
     */
    public Values get(Set<Key> keys) throws StoreOperationException;

    /**
     * Get the {@link Value} under the given key if satisfying the given {@link terrastore.store.features.Predicate}.
     *
     * @param key The key of the value to get.
     * @param predicate The predicate object containing data about the condition to evaluate.
     * @return The value corresponding to the given key and satisfying the given predicate, or null if the value
     * doesn't satisfy it.
     * @throws StoreOperationException If no value is found for the given key.
     */
    public Value conditionalGet(Key key, Predicate predicate) throws StoreOperationException;

    /**
     * Get the {@link Value}s corresponding to the given set of key, if satisfying the given {@link terrastore.store.features.Predicate}.
     *
     * @param keys The key set.
     * @param predicate The predicate object containing data about the condition to evaluate.
     * @return The values corresponding to the given keys and satisfying the given predicate.
     * @throws StoreOperationException If no value is found for the given key.
     */
    public Values conditionalGet(Set<Key> keys, Predicate predicate) throws StoreOperationException;

    /**
     * Remove this {@link Value} under the given key under the condition that the provided
     * predicate is satisfied.
     * This publishes a {@link terrastore.event.ValueRemovedEvent} to the 
     * {@link terrastore.event.EventBus}.
     * 
     * @param key The key of the value to remove.
     * @param predicate The predicate that must be satisfied for the value to be removed
     * @return <code>true</code> if the value was removed, otherwise <code>false</code>.
     * @throws StoreOperationException if the predicate is invalid.
     */
    public boolean conditionalRemove(Key key, Predicate predicate) throws StoreOperationException;

    /**
     * Remove the {@link Value} under the given key.<br>
     * This publishes a {@link terrastore.event.ValueRemovedEvent} to the
     * {@link terrastore.event.EventBus}.
     *
     * @param key The key of the value to remove.
     * @throws StoreOperationException If no value to remove is found for the given key.
     */
    public void remove(Key key) throws StoreOperationException;

    /**
     * Update the {@link Value} under the given key.<br>
     * This publishes a {@link terrastore.event.ValueChangedEvent} to the
     * {@link terrastore.event.EventBus}.
     *
     * @param key The key of the value to update.
     * @param update The update object containing data about the function to apply.
     * @return The updated value.
     * @throws StoreOperationException If errors occur during updating.
     */
    public Value update(Key key, Update update) throws StoreOperationException;

    /**
     * Merge the {@link Value} under the given key.<br>
     * This publishes a {@link terrastore.event.ValueChangedEvent} to the
     * {@link terrastore.event.EventBus}.
     *
     * @param key The key of the value to update.
     * @param merge The value representing the merge operation.
     * @return The merged value.
     * @throws StoreOperationException If errors occur during updating.
     */
    public Value merge(Key key, Value merge) throws StoreOperationException;

    /**
     * Execute a map operation, as described by the {@link terrastore.store.features.Mapper} object,
     * over the given key.
     *
     * @param key The key to map to.
     * @param mapper The map description.
     * @return A map of key/value pairs as the result of the map operation, or null if the mapped key is not found.
     * @throws StoreOperationException If errors occur during map operation.
     */
    public Map<String, Object> map(Key key, Mapper mapper) throws StoreOperationException;

    /**
     * Clear all entries.
     */
    public void clear();

    /**
     * Get the bucket size.
     *
     * @return The bucket size.
     */
    public long size();

    /**
     * Get all keys contained into this bucket.
     *
     * @return The set of keys.
     */
    public Keys keys();

    /**
     * Get a set of all keys falling into the given range, sorted as determined by the given comparator.<br>
     * The range is always computed over a snapshot of all keys: however, if the snapshot is older than the given timeToLive
     * contained into the range object (in milliseconds),
     * a new one will be created with latest keys.<br>
     * The snapshot is computed (and managed) by using the configured {@link SnapshotManager} (see {@link #setSnapshotManager(SnapshotManager )}).
     *
     * @param range The range which keys must be fall into.
     * @return The sorted set of keys in range.
     * @throws StoreOperationException If errors occur.
     */
    public Keys keysInRange(Range range) throws StoreOperationException;

    /**
     * Flush all key/value entries contained into this bucket.
     * <br>
     * The actual decision whether the key must be flushed or not, is left to the given {@link FlushCondition}.
     *
     * @param flushStrategy The algorithm to execute for flushing keys.
     * @param flushCondition The condition to evaluate for flushing keys.
     */
    public void flush(FlushStrategy flushStrategy, FlushCondition flushCondition);

    /**
     * Set to true for compressing documents, false otherwise.
     */
    public void setCompressDocuments(boolean compressed);

    /**
     * Set the default {@link terrastore.store.operators.Comparator} used to compare keys when no other comparator is found.
     *
     * @param defaultComparator The default comparator.
     */
    public void setDefaultComparator(Comparator defaultComparator);

    /**
     * Set all supported {@link terrastore.store.operators.Comparator} by name.
     *
     * @param comparators A map of supported comparators.
     */
    public void setComparators(Map<String, Comparator> comparators);

    /**
     * Set all supported {@link terrastore.store.operators.Condition} by name.
     *
     * @param conditions  A map of supported conditions.
     */
    public void setConditions(Map<String, Condition> conditions);

    /**
     * Set all supported {@link terrastore.store.operators.Function}s by name.
     *
     * @param functions  A map of supported functions.
     */
    public void setUpdaters(Map<String, Function> functions);

    /**
     * Set all supported {@link terrastore.store.operators.Function}s by name.
     *
     * @param functions  A map of supported functions.
     */
    public void setMappers(Map<String, Function> functions);

    /**
     * Set the {@link SnapshotManager} used to compute the snapshot of the keys used in range queries.
     *
     * @param snapshotManager The {@link SnapshotManager} instance.
     */
    public void setSnapshotManager(SnapshotManager snapshotManager);

    /**
     * Set the {@link LockManager} used to lock read/write document operations.
     *
     * @param lockManager  The {@link LockManager} instance.
     */
    public void setLockManager(LockManager lockManager);

    /**
     * Set the {@link terrastore.event.EventBus} instance used for publishing events to {@link terrastore.event.EventListener}s.
     *
     * @param eventBus The event bus instance.
     */
    public void setEventBus(EventBus eventBus);

}
