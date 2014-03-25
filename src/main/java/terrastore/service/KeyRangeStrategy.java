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
package terrastore.service;

import java.util.Set;

import terrastore.router.Router;
import terrastore.store.Key;
import terrastore.store.features.Range;
import terrastore.util.collect.parallel.ParallelExecutionException;

/**
 * 
 * 
 * @author Sven Johansson
 */
public interface KeyRangeStrategy {

    Set<Key> getKeyRangeForBucket(Router router, String bucket, Range range) throws ParallelExecutionException;

}
