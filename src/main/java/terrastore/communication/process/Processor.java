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
package terrastore.communication.process;

import terrastore.communication.ProcessingException;
import terrastore.communication.protocol.Command;
import terrastore.communication.protocol.Response;

/**
 * @author Sergio Bossa
 */
public interface Processor {

    public void start();

    public void pause();

    public void resume();

    public void stop();

    public boolean isPaused();

    public <R> R process(Command<R> command, CommandHandler<R> commandHandler) throws ProcessingException;

    public <R> void process(Command<R> command, CommandHandler<R> commandHandler, CompletionHandler<R, ProcessingException> completionHandler);
}
