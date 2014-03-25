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
package terrastore.util.collect;

import terrastore.util.collect.support.KeyExtractor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergio Bossa
 */
public class Maps {

    public static <K, V> Map<K, V> hash(K[] keys, V[] values) {
        Map<K, V> result = new HashMap<K, V>();
        Maps.<K, V>fill(result, keys, values);
        return result;
    }

    public static <K, V> Map<K, V> linked(K[] keys, V[] values) {
        Map<K, V> result = new LinkedHashMap<K, V>();
        Maps.<K, V>fill(result, keys, values);
        return result;
    }

    public static <K, V> Map<K, V> serializing(Map<K, V> source) {
        return new SerializingMap<K, V>(source);
    }

    public static <K, V> Map<K, V> union(List<Map<K, V>> maps) {
        return new UnionMap<K, V>(maps);
    }

    public static <K, V> Map<K, V> composite(Set<K> keys, List<Map<K, V>> values) {
        return new CompositeMap<K, V>(keys, values);
    }

    public static <K, V> Map<K, V> slice(Map<K, V> map, Set<K> keys) {
        return new SlicedMap<K, V>(map, keys);
    }

    public static <K, V> void fill(Map<K, V> map, K[] keys, V[] values) {
        if (keys.length == values.length) {
            int index = 0;
            for (K key : keys) {
                map.put(key, values[index++]);
            }
        } else {
            throw new IllegalArgumentException("Both keys and values arrays must have same length!");
        }
    }

    public static <K, V> void fill(Map<K, V> map, KeyExtractor<K, V> keyExtractor, V[] values) {
        for (V value : values) {
            map.put(keyExtractor.extractFrom(value), value);
        }
    }
}
