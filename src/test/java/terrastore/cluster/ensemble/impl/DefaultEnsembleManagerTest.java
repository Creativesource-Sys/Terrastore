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

import org.easymock.EasyMock;
import org.junit.Test;
import terrastore.communication.NodeConfiguration;
import terrastore.cluster.ensemble.EnsembleManager;
import terrastore.cluster.ensemble.EnsembleConfiguration;
import terrastore.cluster.ensemble.EnsembleScheduler;
import terrastore.communication.Cluster;
import terrastore.communication.Node;
import terrastore.communication.RemoteNodeFactory;
import terrastore.communication.protocol.MembershipCommand;
import terrastore.router.Router;
import terrastore.util.collect.Maps;
import terrastore.util.collect.Sets;
import static org.easymock.classextension.EasyMock.*;

/**
 * @author Sergio Bossa
 */
public class DefaultEnsembleManagerTest {

    @Test
    public void testJoinAndUpdate() throws Exception {
        Cluster cluster = new Cluster("cluster", false);

        EnsembleConfiguration.DiscoveryConfiguration discoveryConfiguration = createMock(EnsembleConfiguration.DiscoveryConfiguration.class);
        makeThreadSafe(discoveryConfiguration, true);
        discoveryConfiguration.getType();
        expectLastCall().andReturn("fixed").anyTimes();
        EnsembleConfiguration ensembleConfiguration = createMock(EnsembleConfiguration.class);
        makeThreadSafe(ensembleConfiguration, true);
        ensembleConfiguration.getDiscovery();
        expectLastCall().andReturn(discoveryConfiguration).anyTimes();
        EnsembleScheduler scheduler = createMock(EnsembleScheduler.class);
        makeThreadSafe(scheduler, true);
        scheduler.schedule(same(cluster), EasyMock.<EnsembleManager>anyObject(), same(ensembleConfiguration));
        expectLastCall().once();
        scheduler.shutdown();
        expectLastCall().once();
        //
        Node seed = createMock(Node.class);
        makeThreadSafe(seed, true);
        seed.connect();
        expectLastCall().once();
        seed.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andReturn(new View("cluster", Sets.hash(new View.Member(new NodeConfiguration("discovered", "localhost", 6000, "localhost", 8080)))));
        seed.disconnect();
        expectLastCall().once();
        //
        Node discoveredNode = createMock(Node.class);
        makeThreadSafe(discoveredNode, true);
        discoveredNode.connect();
        expectLastCall().once();
        discoveredNode.disconnect();
        expectLastCall().once();
        //
        RemoteNodeFactory nodeFactory = createMock(RemoteNodeFactory.class);
        makeThreadSafe(nodeFactory, true);
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("localhost:6000", "localhost", 6000, "", -1)));
        expectLastCall().andReturn(seed).once();
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("discovered", "localhost", 6000, "localhost", 8080)));
        expectLastCall().andReturn(discoveredNode).once();
        //
        Router router = createMock(Router.class);
        makeThreadSafe(router, true);
        router.addRouteTo(cluster, discoveredNode);
        expectLastCall().once();

        replay(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode, nodeFactory, router);

        DefaultEnsembleManager ensemble = new DefaultEnsembleManager(Maps.hash(new String[]{"fixed"}, new EnsembleScheduler[]{scheduler}), router, nodeFactory);
        try {
            ensemble.join(cluster, "localhost:6000", ensembleConfiguration);
            ensemble.update(cluster);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            ensemble.shutdown();
            verify(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode, nodeFactory, router);
        }
    }

    @Test
    public void testJoinAndUpdateTwoTimesWithNewConnectedNode() throws Exception {
        Cluster cluster = new Cluster("cluster", false);

        EnsembleConfiguration.DiscoveryConfiguration discoveryConfiguration = createMock(EnsembleConfiguration.DiscoveryConfiguration.class);
        makeThreadSafe(discoveryConfiguration, true);
        discoveryConfiguration.getType();
        expectLastCall().andReturn("fixed").anyTimes();
        EnsembleConfiguration ensembleConfiguration = createMock(EnsembleConfiguration.class);
        makeThreadSafe(ensembleConfiguration, true);
        ensembleConfiguration.getDiscovery();
        expectLastCall().andReturn(discoveryConfiguration).anyTimes();
        EnsembleScheduler scheduler = createMock(EnsembleScheduler.class);
        makeThreadSafe(scheduler, true);
        scheduler.schedule(same(cluster), EasyMock.<EnsembleManager>anyObject(), same(ensembleConfiguration));
        expectLastCall().once();
        scheduler.shutdown();
        expectLastCall().once();
        //
        Node seed = createMock(Node.class);
        makeThreadSafe(seed, true);
        seed.connect();
        expectLastCall().once();
        seed.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andReturn(new View("cluster", Sets.hash(new View.Member(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)))));
        seed.disconnect();
        expectLastCall().once();
        //
        Node discoveredNode1 = createMock(Node.class);
        makeThreadSafe(discoveredNode1, true);
        discoveredNode1.connect();
        expectLastCall().once();
        discoveredNode1.disconnect();
        expectLastCall().once();
        discoveredNode1.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andReturn(new View("cluster", Sets.hash(new View.Member(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)), new View.Member(new NodeConfiguration("discovered2", "localhost", 6001, "localhost", 8080)))));
        //
        Node discoveredNode2 = createMock(Node.class);
        makeThreadSafe(discoveredNode2, true);
        discoveredNode2.connect();
        expectLastCall().once();
        discoveredNode2.disconnect();
        expectLastCall().once();
        //
        RemoteNodeFactory nodeFactory = createMock(RemoteNodeFactory.class);
        makeThreadSafe(nodeFactory, true);
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("localhost:6000", "localhost", 6000, "", -1)));
        expectLastCall().andReturn(seed).once();
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)));
        expectLastCall().andReturn(discoveredNode1).once();
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("discovered2", "localhost", 6001, "localhost", 8080)));
        expectLastCall().andReturn(discoveredNode2).once();
        //
        Router router = createMock(Router.class);
        makeThreadSafe(router, true);
        router.addRouteTo(cluster, discoveredNode1);
        expectLastCall().once();
        router.addRouteTo(cluster, discoveredNode2);
        expectLastCall().once();

        replay(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode1, discoveredNode2, nodeFactory, router);

        DefaultEnsembleManager ensemble = new DefaultEnsembleManager(Maps.hash(new String[]{"fixed"}, new EnsembleScheduler[]{scheduler}), router, nodeFactory);
        try {
            ensemble.join(cluster, "localhost:6000", ensembleConfiguration);
            ensemble.update(cluster);
            ensemble.update(cluster);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            ensemble.shutdown();
            verify(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode1, discoveredNode2, nodeFactory, router);
        }
    }

    @Test
    public void testJoinAndUpdateTwoTimesWithSameNodeDisconnectingAndConnectingAgain() throws Exception {
        Cluster cluster = new Cluster("cluster", false);

        EnsembleConfiguration.DiscoveryConfiguration discoveryConfiguration = createMock(EnsembleConfiguration.DiscoveryConfiguration.class);
        makeThreadSafe(discoveryConfiguration, true);
        discoveryConfiguration.getType();
        expectLastCall().andReturn("fixed").anyTimes();
        EnsembleConfiguration ensembleConfiguration = createMock(EnsembleConfiguration.class);
        makeThreadSafe(ensembleConfiguration, true);
        ensembleConfiguration.getDiscovery();
        expectLastCall().andReturn(discoveryConfiguration).anyTimes();
        EnsembleScheduler scheduler = createMock(EnsembleScheduler.class);
        makeThreadSafe(scheduler, true);
        scheduler.schedule(same(cluster), EasyMock.<EnsembleManager>anyObject(), same(ensembleConfiguration));
        expectLastCall().once();
        scheduler.shutdown();
        expectLastCall().once();
        //
        Node seed = createMock(Node.class);
        makeThreadSafe(seed, true);
        seed.connect();
        expectLastCall().times(2);
        seed.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andReturn(new View("cluster", Sets.hash(new View.Member(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080))))).times(2);
        seed.disconnect();
        expectLastCall().times(2);
        //
        Node discoveredNode1 = createMock(Node.class);
        makeThreadSafe(discoveredNode1, true);
        discoveredNode1.connect();
        expectLastCall().times(2);
        discoveredNode1.disconnect();
        expectLastCall().times(2);
        discoveredNode1.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andThrow(new RuntimeException());
        //
        RemoteNodeFactory nodeFactory = createMock(RemoteNodeFactory.class);
        makeThreadSafe(nodeFactory, true);
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("localhost:6000", "localhost", 6000, "", -1)));
        expectLastCall().andReturn(seed).once();
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)));
        expectLastCall().andReturn(discoveredNode1).times(2);
        //
        Router router = createMock(Router.class);
        makeThreadSafe(router, true);
        router.addRouteTo(cluster, discoveredNode1);
        expectLastCall().times(2);
        router.removeRouteTo(cluster, discoveredNode1);
        expectLastCall().once();

        replay(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode1, nodeFactory, router);

        DefaultEnsembleManager ensemble = new DefaultEnsembleManager(Maps.hash(new String[]{"fixed"}, new EnsembleScheduler[]{scheduler}), router, nodeFactory);
        try {
            ensemble.join(cluster, "localhost:6000", ensembleConfiguration);
            ensemble.update(cluster);
            ensemble.update(cluster);
            ensemble.update(cluster);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            ensemble.shutdown();
            verify(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode1, nodeFactory, router);
        }
    }

    @Test
    public void testJoinAndUpdateTwoTimesWithDisconnectedNode() throws Exception {
        Cluster cluster = new Cluster("cluster", false);

        EnsembleConfiguration.DiscoveryConfiguration discoveryConfiguration = createMock(EnsembleConfiguration.DiscoveryConfiguration.class);
        makeThreadSafe(discoveryConfiguration, true);
        discoveryConfiguration.getType();
        expectLastCall().andReturn("fixed").anyTimes();
        EnsembleConfiguration ensembleConfiguration = createMock(EnsembleConfiguration.class);
        makeThreadSafe(ensembleConfiguration, true);
        ensembleConfiguration.getDiscovery();
        expectLastCall().andReturn(discoveryConfiguration).anyTimes();
        EnsembleScheduler scheduler = createMock(EnsembleScheduler.class);
        makeThreadSafe(scheduler, true);
        scheduler.schedule(same(cluster), EasyMock.<EnsembleManager>anyObject(), same(ensembleConfiguration));
        expectLastCall().once();
        scheduler.shutdown();
        expectLastCall().once();
        //
        Node seed = createMock(Node.class);
        makeThreadSafe(seed, true);
        seed.connect();
        expectLastCall().once();
        seed.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andReturn(new View("cluster", Sets.hash(new View.Member(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)), new View.Member(new NodeConfiguration("discovered2", "localhost", 6001, "localhost", 8080)))));
        seed.disconnect();
        expectLastCall().once();
        //
        Node discoveredNode1 = createMock(Node.class);
        makeThreadSafe(discoveredNode1, true);
        discoveredNode1.getName();
        expectLastCall().andReturn("discovered1").once();
        discoveredNode1.connect();
        expectLastCall().once();
        discoveredNode1.disconnect();
        expectLastCall().once();
        discoveredNode1.send(EasyMock.<MembershipCommand>anyObject());
        expectLastCall().andReturn(new View("cluster", Sets.hash(new View.Member(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)))));
        //
        Node discoveredNode2 = createMock(Node.class);
        makeThreadSafe(discoveredNode2, true);
        discoveredNode2.getName();
        expectLastCall().andReturn("discovered2").once();
        discoveredNode2.connect();
        expectLastCall().once();
        discoveredNode2.disconnect();
        expectLastCall().once();
        //
        RemoteNodeFactory nodeFactory = createMock(RemoteNodeFactory.class);
        makeThreadSafe(nodeFactory, true);
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("localhost:6000", "localhost", 6000, "", -1)));
        expectLastCall().andReturn(seed).once();
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("discovered1", "localhost", 6000, "localhost", 8080)));
        expectLastCall().andReturn(discoveredNode1).once();
        nodeFactory.makeRemoteNode(eq(new NodeConfiguration("discovered2", "localhost", 6001, "localhost", 8080)));
        expectLastCall().andReturn(discoveredNode2).once();
        //
        Router router = createMock(Router.class);
        makeThreadSafe(router, true);
        router.addRouteTo(cluster, discoveredNode1);
        expectLastCall().once();
        router.addRouteTo(cluster, discoveredNode2);
        expectLastCall().once();
        router.removeRouteTo(cluster, discoveredNode2);
        expectLastCall().once();

        replay(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode1, discoveredNode2, nodeFactory, router);

        DefaultEnsembleManager ensemble = new DefaultEnsembleManager(Maps.hash(new String[]{"fixed"}, new EnsembleScheduler[]{scheduler}), router, nodeFactory);
        try {
            ensemble.join(cluster, "localhost:6000", ensembleConfiguration);
            ensemble.update(cluster);
            ensemble.update(cluster);
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            ensemble.shutdown();
            verify(discoveryConfiguration, ensembleConfiguration, scheduler, seed, discoveredNode1, discoveredNode2, nodeFactory, router);
        }
    }

    @Test
    public void testUpdateDoesNothingIfNoJoin() throws Exception {
        Cluster cluster = new Cluster("cluster", false);

        EnsembleScheduler scheduler = createMock(EnsembleScheduler.class);
        makeThreadSafe(scheduler, true);
        RemoteNodeFactory nodeFactory = createMock(RemoteNodeFactory.class);
        makeThreadSafe(nodeFactory, true);
        //
        Router router = createMock(Router.class);
        makeThreadSafe(router, true);

        replay(scheduler, nodeFactory, router);

        DefaultEnsembleManager ensemble = new DefaultEnsembleManager(Maps.hash(new String[]{"fixed"}, new EnsembleScheduler[]{scheduler}), router, nodeFactory);
        try {
            ensemble.update(cluster);
        } finally {
            verify(scheduler, nodeFactory, router);
        }
    }
}
