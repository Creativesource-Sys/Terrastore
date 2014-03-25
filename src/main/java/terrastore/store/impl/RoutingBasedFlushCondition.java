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

import terrastore.store.FlushCondition;
import terrastore.communication.Node;
import terrastore.router.MissingRouteException;
import terrastore.router.Router;
import terrastore.store.Bucket;
import terrastore.store.Key;

/**
 * Flush condition based on the routing path: keys whose routing path doesn't belong
 * to the local node (meaning they belong to remote nodes), will be locally flushed.
 *
 * @author Sergio Bossa
 */
public class RoutingBasedFlushCondition implements FlushCondition {

    private final Router router;

    public RoutingBasedFlushCondition(Router router) {
        this.router = router;
    }

    @Override
    public boolean isSatisfied(Bucket bucket, Key key) {
        try {
            Node localNode = router.routeToLocalNode();
            Node actual = router.routeToNodeFor(bucket.getName(), key);
            return !actual.equals(localNode);
        } catch (MissingRouteException ex) {
            return true;
        }
    }
}
