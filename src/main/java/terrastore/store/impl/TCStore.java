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
package terrastore.store.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.collections.ClusteredMap;
import terrastore.common.ErrorMessage;
import terrastore.internal.tc.TCMaster;
import terrastore.event.EventBus;
import terrastore.server.Buckets;
import terrastore.store.comparators.LexicographicalComparator;
import terrastore.store.Bucket;
import terrastore.store.FlushCondition;
import terrastore.store.FlushStrategy;
import terrastore.store.Key;
import terrastore.store.LockManager;
import terrastore.store.SnapshotManager;
import terrastore.store.Store;
import terrastore.store.StoreOperationException;
import terrastore.store.Value;
import terrastore.store.features.Mapper;
import terrastore.store.features.Reducer;
import terrastore.store.operators.Aggregator;
import terrastore.store.operators.Comparator;
import terrastore.store.operators.Condition;
import terrastore.store.operators.Function;
import terrastore.store.operators.OperatorException;
import terrastore.util.collect.parallel.MapCollector;
import terrastore.util.collect.parallel.MapTask;
import terrastore.util.collect.parallel.ParallelExecutionException;
import terrastore.util.collect.parallel.ParallelUtils;
import terrastore.util.concurrent.GlobalExecutor;
import terrastore.util.json.JsonUtils;

/**
 * @author Sergio Bossa
 */
public class TCStore implements Store {

    private static final Logger LOG = LoggerFactory.getLogger(TCStore.class);
    //
    // TODO: buckets marked with tombstones aren't currently removed, only cleared.
    private final static String TOMBSTONE = TCStore.class.getName() + ".TOMBSTONE";
    //
    private final ClusteredMap<String, String> buckets;
    private final ConcurrentMap<String, Bucket> instances;
    private final Map<String, Comparator> comparators = new HashMap<String, Comparator>();
    private final Map<String, Condition> conditions = new HashMap<String, Condition>();
    private final Map<String, Function> updaters = new HashMap<String, Function>();
    private final Map<String, Function> mappers = new HashMap<String, Function>();
    private final Map<String, Aggregator> combiners = new HashMap<String, Aggregator>();
    private final Map<String, Aggregator> reducers = new HashMap<String, Aggregator>();
    private Comparator defaultComparator = new LexicographicalComparator(true);
    private SnapshotManager snapshotManager;
    private LockManager lockManager;
    private EventBus eventBus;
    private boolean compressedDocuments;

    public TCStore() {
        buckets = TCMaster.getInstance().getAutolockedMap(TCStore.class.getName() + ".buckets");
        instances = new ConcurrentHashMap<String, Bucket>();
    }

    @Override
    public Bucket getOrCreate(String bucket) {
        Bucket requested = instances.get(bucket);
        if (requested == null) {
            buckets.lockEntry(bucket);
            try {
                if (!instances.containsKey(bucket)) {
                    Bucket created = new TCBucket(bucket);
                    hydrateBucket(created);
                    instances.put(bucket, created);
                    if (!buckets.containsKey(bucket) || buckets.get(bucket).equals(TOMBSTONE)) {
                        buckets.putNoReturn(bucket, bucket);
                    }
                    requested = created;
                } else {
                    requested = instances.get(bucket);
                }
            } finally {
                buckets.unlockEntry(bucket);
            }
        }
        return requested;
    }

    @Override
    public Bucket get(String bucket) {
        Bucket requested = instances.get(bucket);
        if (requested == null) {
            buckets.lockEntry(bucket);
            try {
                if (!instances.containsKey(bucket)) {
                    if (buckets.containsKey(bucket)) {
                        Bucket created = new TCBucket(bucket);
                        hydrateBucket(created);
                        instances.put(bucket, created);
                        requested = created;
                    }
                } else {
                    requested = instances.get(bucket);
                }
            } finally {
                buckets.unlockEntry(bucket);
            }
        }
        return requested;
    }

    @Override
    public void remove(String bucket) {
        buckets.lockEntry(bucket);
        try {
            Bucket removed = instances.remove(bucket);
            if (removed != null) {
                removed.clear();
            } else {
                Bucket instance = new TCBucket(bucket);
                hydrateBucket(instance);
                instance.clear();
            }
            buckets.putNoReturn(bucket, TOMBSTONE);
        } finally {
            buckets.unlockEntry(bucket);
        }
    }

    @Override
    public Buckets buckets() {
        Set<String> result = new HashSet<String>();
        Set<Entry<String, String>> entries = buckets.entrySet();
        for (Entry<String, String> keyValue : entries) {
            String bucket = keyValue.getKey();
            buckets.lockEntry(bucket);
            try {
                if (!keyValue.getValue().equals(TOMBSTONE)) {
                    result.add(bucket);
                } else {
                    Bucket instance = instances.get(bucket);
                    if (instance == null) {
                        instance = new TCBucket(bucket);
                        hydrateBucket(instance);
                        instances.put(bucket, instance);
                    }
                    if (instance.size() > 0) {
                        buckets.put(bucket, bucket);
                        result.add(bucket);
                    }
                }
            } finally {
                buckets.unlockEntry(bucket);
            }
        }
        return new Buckets(result);
    }

    @Override
    public Map<String, Object> map(final String bucketName, final Set<Key> keys, final Mapper mapper) throws StoreOperationException {
        Bucket bucket = get(bucketName);
        if (bucket != null) {
            List<Map<String, Object>> mapResults = doMap(bucket, keys, mapper);
            Aggregator aggregator = getAggregator(combiners, mapper.getCombinerName());
            return doAggregate(mapResults, aggregator, mapper.getTimeoutInMillis(), mapper.getParameters());
        } else {
            throw new StoreOperationException(new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No bucket found with name: " + bucketName));
        }
    }

    @Override
    public Value reduce(List<Map<String, Object>> values, Reducer reducer) throws StoreOperationException {
        Aggregator aggregator = getAggregator(reducers, reducer.getReducerName());
        Map<String, Object> aggregation = doAggregate(values, aggregator, reducer.getTimeoutInMillis(), reducer.getParameters());
        return JsonUtils.fromMap(aggregation);
    }

    @Override
    public void flush(FlushStrategy flushStrategy, FlushCondition flushCondition) {
        for (Bucket bucket : instances.values()) {
            bucket.flush(flushStrategy, flushCondition);
        }
    }

    @Override
    public void setCompressDocuments(boolean compressed) {
        this.compressedDocuments = compressed;
    }

    @Override
    public void setDefaultComparator(Comparator defaultComparator) {
        this.defaultComparator = defaultComparator;
    }

    @Override
    public void setComparators(Map<String, Comparator> comparators) {
        this.comparators.clear();
        this.comparators.putAll(comparators);
    }

    @Override
    public void setUpdaters(Map<String, Function> functions) {
        this.updaters.clear();
        this.updaters.putAll(functions);
    }

    @Override
    public void setMappers(Map<String, Function> functions) {
        this.mappers.clear();
        this.mappers.putAll(functions);
    }

    @Override
    public void setConditions(Map<String, Condition> conditions) {
        this.conditions.clear();
        this.conditions.putAll(conditions);
    }

    @Override
    public void setCombiners(Map<String, Aggregator> aggregators) {
        this.combiners.clear();
        this.combiners.putAll(aggregators);
    }

    @Override
    public void setReducers(Map<String, Aggregator> aggregators) {
        this.reducers.clear();
        this.reducers.putAll(aggregators);
    }

    @Override
    public void setSnapshotManager(SnapshotManager snapshotManager) {
        this.snapshotManager = snapshotManager;
    }

    @Override
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private void hydrateBucket(Bucket bucket) {
        // We need to manually set all of this because of TC not supporting injection ...
        bucket.setCompressDocuments(compressedDocuments);
        bucket.setDefaultComparator(defaultComparator);
        bucket.setComparators(comparators);
        bucket.setConditions(conditions);
        bucket.setUpdaters(updaters);
        bucket.setMappers(mappers);
        bucket.setSnapshotManager(snapshotManager);
        bucket.setLockManager(lockManager);
        bucket.setEventBus(eventBus);
        // TODO: verify this is not a perf problem.
    }

    private Aggregator getAggregator(Map<String, Aggregator> aggregators, String aggregatorName) throws StoreOperationException {
        if (aggregators.containsKey(aggregatorName)) {
            return aggregators.get(aggregatorName);
        } else {
            throw new StoreOperationException(new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "Wrong aggregator name: " + aggregatorName));
        }
    }

    private List<Map<String, Object>> doMap(final Bucket bucket, final Set<Key> keys, final Mapper mapper) throws StoreOperationException {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        Future<List<Map<String, Object>>> task = null;
        try {
            task = GlobalExecutor.getQueryExecutor().submit(new Callable<List<Map<String, Object>>>() {

                @Override
                public List<Map<String, Object>> call() throws Exception {
                    List<Map<String, Object>> result = ParallelUtils.parallelSliceMap(
                            keys, 1000, // FIXME: make the slice size configurable
                            new MapTask<Key, Map<String, Object>>() {

                        @Override
                        public Map<String, Object> map(Key input) throws ParallelExecutionException {
                            try {
                                if (cancelled.get()) {
                                    throw new ParallelExecutionException(new InterruptedException("Interrupted due to timeout!"));
                                } else {
                                    return bucket.map(input, mapper);
                                }
                            } catch (StoreOperationException ex) {
                                throw new ParallelExecutionException(ex);
                            }
                        }

                    }, new MapCollector<Map<String, Object>, List<Map<String, Object>>>() {

                        @Override
                        public List<Map<String, Object>> collect(List<Map<String, Object>> outputs) {
                            List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
                            for (Map<String, Object> output : outputs) {
                                result.add(output);
                            }
                            return result;
                        }

                    },
                            GlobalExecutor.getQueryExecutor());
                    return result;
                }

            });
            return task.get(mapper.getTimeoutInMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            task.cancel(true);
            cancelled.set(true);
            throw new StoreOperationException(new ErrorMessage(ErrorMessage.INTERNAL_SERVER_ERROR_CODE, "Map cancelled due to long execution time."));
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof StoreOperationException) {
                throw (StoreOperationException) ex.getCause();
            } else {
                throw new StoreOperationException(new ErrorMessage(ErrorMessage.INTERNAL_SERVER_ERROR_CODE, ex.getMessage()));
            }
        } catch (Exception ex) {
            throw new StoreOperationException(new ErrorMessage(ErrorMessage.INTERNAL_SERVER_ERROR_CODE, ex.getMessage()));
        }
    }

    private Map<String, Object> doAggregate(final List<Map<String, Object>> values, final Aggregator aggregator, final long timeout, final Map<String, Object> parameters) throws StoreOperationException {
        Future<Map<String, Object>> task = null;
        try {
            task = GlobalExecutor.getQueryExecutor().submit(new Callable<Map<String, Object>>() {

                @Override
                public Map<String, Object> call() {
                    try {
                        return aggregator.apply(values, parameters);
                    } catch (OperatorException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });
            return task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            task.cancel(true);
            throw new StoreOperationException(new ErrorMessage(ErrorMessage.INTERNAL_SERVER_ERROR_CODE, "Aggregation cancelled due to long execution time."));
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException && ex.getCause().getCause() instanceof OperatorException) {
                throw new StoreOperationException(((OperatorException) ex.getCause().getCause()).getErrorMessage());
            } else {
                throw new StoreOperationException(new ErrorMessage(ErrorMessage.INTERNAL_SERVER_ERROR_CODE, ex.getMessage()));
            }
        } catch (Exception ex) {
            throw new StoreOperationException(new ErrorMessage(ErrorMessage.INTERNAL_SERVER_ERROR_CODE, ex.getMessage()));
        }
    }

}
