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
package terrastore.communication.protocol;

import java.io.IOException;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import terrastore.communication.CommunicationException;
import terrastore.communication.Node;
import terrastore.communication.ProcessingException;
import terrastore.router.MissingRouteException;
import terrastore.router.Router;
import terrastore.store.Bucket;
import terrastore.store.Key;
import terrastore.store.Store;
import terrastore.store.StoreOperationException;
import terrastore.store.Value;
import terrastore.util.io.MsgPackUtils;

/**
 * @author Sergio Bossa
 * @author Adriano Santos
 */
public class MergeCommand extends AbstractCommand<Value> {

    private String bucketName;
    private Key key;
    private Value value;

    public MergeCommand(String bucketName, Key key, Value value) {
        this.bucketName = bucketName;
        this.key = key;
        this.value = value;
        }

    public MergeCommand() {
    }

    @Override
    public Response<Value> executeOn(Router router) throws CommunicationException, MissingRouteException, ProcessingException {
        Node node = router.routeToNodeFor(bucketName, key);
        return new ValueResponse(id, node.<Value>send(this));
    }

    public Response<Value> executeOn(Store store) throws StoreOperationException {
        Bucket bucket = store.getOrCreate(bucketName);
        if (bucket != null) {
            return new ValueResponse(id, bucket.merge(key, value));
        } else {
            return new ValueResponse(id, null);
        }
    }

    @Override
    protected void doSerialize(Packer packer) throws IOException {
        MsgPackUtils.packString(packer, bucketName);
        MsgPackUtils.packKey(packer, key);
        MsgPackUtils.packValue(packer, value);
    }

    @Override
    protected void doDeserialize(Unpacker unpacker) throws IOException, MessageTypeException {
        bucketName = MsgPackUtils.unpackString(unpacker);
        key = MsgPackUtils.unpackKey(unpacker);
        value = MsgPackUtils.unpackValue(unpacker);
    }
}
