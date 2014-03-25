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
package terrastore.cluster.ensemble.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import terrastore.cluster.ensemble.EnsembleConfiguration;

/**
 * @author Amir Moulavi
 */
public class FuzzyInferenceEngineTest {

    private FuzzyInferenceEngine fuzzy;
    private long previousPeriodLength;
    private int viewChanges;
    private long result;
    private EnsembleConfiguration.DiscoveryConfiguration conf;

    @Before
    public void set_up() {
        fuzzy = new FuzzyInferenceEngine();
        conf = new EnsembleConfiguration.DiscoveryConfiguration();
        conf.setType("adaptive");
        conf.setBaseline(30000L);
        conf.setIncrement(10000L);
        conf.setLimit(60000L);
    }

    @Test
    public void no_view_change() {
        given(view_changes_percentage(0), previous_period_length(40));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(40 + 10);
    }

    @Test
    public void no_view_change_with_upbound_limit() {
        given(view_changes_percentage(0), previous_period_length(60));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(60);
    }

    @Test
    public void very_high_view_changes_and_very_frequent_period() {
        given(view_changes_percentage(80), previous_period_length(10));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(5);
    }

    @Test
    public void very_high_view_changes_and_frequent_period() {
        given(view_changes_percentage(80), previous_period_length(21));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(10);
    }

    @Test
    public void very_high_view_changes_and_less_frequent_period() {
        given(view_changes_percentage(80), previous_period_length(45));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(15);
    }

    @Test
    public void high_view_changes_and_very_frequent_period() {
        given(view_changes_percentage(50), previous_period_length(10));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(20);
    }

    @Test
    public void high_view_changes_and_frequent_period() {
        given(view_changes_percentage(50), previous_period_length(25));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(25);
    }

    @Test
    public void high_view_changes_and_less_frequent_period() {
        given(view_changes_percentage(50), previous_period_length(80));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(30);
    }

    @Test
    public void low_view_changes_and_very_frequent_period() {
        given(view_changes_percentage(20), previous_period_length(10));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(35);
    }

    @Test
    public void low_view_changes_and_frequent_period() {
        given(view_changes_percentage(20), previous_period_length(30));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(40);
    }

    @Test
    public void low_view_changes_and_less_frequent_period() {
        given(view_changes_percentage(20), previous_period_length(90));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(45);
    }

    @Test
    public void very_low_view_changes_and_very_frequent_period() {
        given(view_changes_percentage(5), previous_period_length(10));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(50);
    }

    @Test
    public void very_low_view_changes_and_frequent_period() {
        given(view_changes_percentage(5), previous_period_length(35));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(55);
    }

    @Test
    public void very_low_view_changes_and_less_frequent_period() {
        given(view_changes_percentage(5), previous_period_length(65));

        when_the_fuzzy_inference_engine_estimates();

        then_the_estimated_next_preiod_length_is(60);
    }

    private void given(int viewChanges, long previousPeriodLength) {
        this.viewChanges = viewChanges;
        this.previousPeriodLength = previousPeriodLength;
    }

    private void when_the_fuzzy_inference_engine_estimates() {
        result = fuzzy.estimateNextPeriodLength(viewChanges, previousPeriodLength, conf);
    }

    private void then_the_estimated_next_preiod_length_is(int estimatedPeriodLength) {
        Assert.assertTrue("Wrong estimated value: expected [" + estimatedPeriodLength * 1000 + "], but it was [" + result + "]", estimatedPeriodLength * 1000 == result);
    }

    private int view_changes_percentage(int percentage) {
        return percentage;
    }

    private long previous_period_length(int period) {
        return period * 1000;
    }

}
