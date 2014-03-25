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

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import terrastore.store.Bucket;
import terrastore.store.Key;
import terrastore.store.SnapshotManager;
import terrastore.store.SortedSnapshot;

/**
 * @author Sergio Bossa
 */
public class LocalSnapshotManager implements SnapshotManager {

    private ConcurrentMap<String, SortedSnapshot> snapshots;
    private ReentrantLock computationLock;

    public LocalSnapshotManager() {
        this.snapshots = new ConcurrentHashMap<String, SortedSnapshot>();
        this.computationLock = new ReentrantLock(true);
    }

    @Override
    public SortedSnapshot getOrComputeSortedSnapshot(Bucket bucket, Comparator<String> comparator, String name, long timeToLive) {
        String snapshotName = getSnapshotName(bucket, name);
        SortedSnapshot snapshot = snapshots.get(snapshotName);
        boolean expired = false;
        while (snapshot == null || (expired = snapshot.isExpired(timeToLive)) == true) {
            if (snapshot != null && expired) {
                snapshot = tryUpdatingSnapshot(snapshot, bucket);
            } else if (snapshot == null) {
                snapshot = tryComputingSnapshot(snapshotName, bucket, comparator);
            } else {
                break;
            }
            if (snapshot == null) {
                snapshot = waitForSnapshot(snapshotName);
            } else {
                break;
            }
        }
        return snapshot;
    }

    private String getSnapshotName(Bucket bucket, String name) {
        if (name != null && !name.isEmpty()) {
            return bucket.getName() + "-" + name;
        } else {
            return bucket.getName() + "-" + "default";
        }
    }

    private SortedSnapshot tryComputingSnapshot(String snapshotName, Bucket bucket, Comparator<String> comparator) {
        boolean locked = this.computationLock.tryLock();
        if (locked) {
            try {
                Set<Key> keys = bucket.keys();
                SortedSnapshot snapshot = new SortedSnapshot(snapshotName, keys, comparator);
                snapshots.put(snapshotName, snapshot);
                return snapshot;
            } finally {
                computationLock.unlock();
            }
        } else {
            return null;
        }
    }

    private SortedSnapshot tryUpdatingSnapshot(SortedSnapshot snapshot, Bucket bucket) {
        boolean locked = this.computationLock.tryLock();
        if (locked) {
            try {
                snapshot.update(bucket.keys());
                return snapshot;
            } finally {
                computationLock.unlock();
            }
        } else {
            return null;
        }
    }

    private SortedSnapshot waitForSnapshot(String snapshotName) {
        this.computationLock.lock();
        try {
            return snapshots.get(snapshotName);
        } finally {
            computationLock.unlock();
        }
    }
}
