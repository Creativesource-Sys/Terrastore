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

import java.util.Set;
import org.junit.Test;
import terrastore.store.FlushCondition;
import terrastore.store.FlushStrategy;
import terrastore.store.Bucket;
import terrastore.store.FlushCallback;
import terrastore.store.Key;
import terrastore.util.collect.Sets;
import static org.easymock.classextension.EasyMock.*;

/**
 * @author Sergio Bossa
 */
public class SequentialFlushStrategyTest {

    @Test
    public void testSequentiallyFlushAllKeys() {
        FlushCondition condition = createMock(FlushCondition.class);
        FlushCallback callback = createMock(FlushCallback.class);
        Bucket bucket = createMock(Bucket.class);
        Set<Key> keys = Sets.hash(new Key("k1"), new Key("k2"));

        condition.isSatisfied(bucket, new Key("k1"));
        expectLastCall().andReturn(true).once();
        condition.isSatisfied(bucket, new Key("k2"));
        expectLastCall().andReturn(true).once();
        callback.doFlush(new Key("k1"));
        expectLastCall().once();
        callback.doFlush(new Key("k2"));
        expectLastCall().once();

        replay(condition, callback, bucket);

        FlushStrategy strategy = new SequentialFlushStrategy();
        strategy.flush(bucket, keys, condition, callback);

        verify(condition, callback, bucket);
    }

    @Test
    public void testSequentiallyFlushHalfKeys() {
        FlushCondition condition = createMock(FlushCondition.class);
        FlushCallback callback = createMock(FlushCallback.class);
        Bucket bucket = createMock(Bucket.class);
        Set<Key> keys = Sets.hash(new Key("k1"), new Key("k2"));

        condition.isSatisfied(bucket, new Key("k1"));
        expectLastCall().andReturn(true).once();
        condition.isSatisfied(bucket, new Key("k2"));
        expectLastCall().andReturn(false).once();
        callback.doFlush(new Key("k1"));
        expectLastCall().once();

        replay(condition, callback, bucket);

        FlushStrategy strategy = new SequentialFlushStrategy();
        strategy.flush(bucket, keys, condition, callback);

        verify(condition, callback, bucket);
    }

    @Test
    public void testSequentiallyFlushNoKeys() {
        FlushCondition condition = createMock(FlushCondition.class);
        FlushCallback callback = createMock(FlushCallback.class);
        Bucket bucket = createMock(Bucket.class);
        Set<Key> keys = Sets.hash(new Key("k1"), new Key("k2"));

        condition.isSatisfied(bucket, new Key("k1"));
        expectLastCall().andReturn(false).once();
        condition.isSatisfied(bucket, new Key("k2"));
        expectLastCall().andReturn(false).once();

        replay(condition, callback, bucket);

        FlushStrategy strategy = new SequentialFlushStrategy();
        strategy.flush(bucket, keys, condition, callback);

        verify(condition, callback, bucket);
    }
}