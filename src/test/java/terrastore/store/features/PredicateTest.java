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
package terrastore.store.features;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sergio Bossa
 */
public class PredicateTest {

    @Test
    public void testPredicate() {
        Predicate predicate = new Predicate("type:expression");
        assertEquals("type", predicate.getConditionType());
        assertEquals("expression", predicate.getConditionExpression());
        assertFalse(predicate.isEmpty());
    }

    @Test
    public void testEmptyPredicate() {
        Predicate predicate = new Predicate(null);
        assertTrue(predicate.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedPredicate() {
        Predicate predicate = new Predicate("name-expression");
    }
}
