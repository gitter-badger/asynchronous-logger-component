/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.mingshan.logger.async;

import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import me.mingshan.logger.async.api.LogExport;
import me.mingshan.logger.async.common.AsyncLoggerPlugins;
import me.mingshan.logger.async.common.Constants;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事件处理
 *
 * @author mingshan
 */
public class RingBufferLogEventHandler<E> implements
        SequenceReportingEventHandler<RingBufferLogEvent<E>> {
    private static final AtomicInteger COUNT = new AtomicInteger(1);
    private Sequence sequenceCallback;
    private int batchCounter = Constants.NOTIFY_PROGRESS_THRESHOLD;

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(RingBufferLogEvent<E> event, long sequence, boolean endOfBatch) throws Exception {
        final boolean pseudoEndOfBatch = endOfBatch || --batchCounter == 0;

        // Do work...
        AsyncLoggerPlugins<E> asyncLoggerPlugins = AsyncLoggerPlugins.getInstance();
        List<LogExport<E>> logExports = asyncLoggerPlugins.getlogExports();
        for (LogExport<E> logExport : logExports) {
            logExport.export(event.getMessage());
        }
        event.clear();

        // ----
        if (pseudoEndOfBatch) {
            batchCounter = Constants.NOTIFY_PROGRESS_THRESHOLD;
            sequenceCallback.set(sequence);
        }
        System.out.println(COUNT.getAndIncrement());
    }
}
