/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
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

package ch.ksfx.services.spidering;

import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ParsingRunner
{
    private ThreadPoolExecutor threadPoolExecutor;
    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);

    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;

    public ParsingRunner(SystemLogger systemLogger, ServiceProvider serviceProvider)
    {
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;

        this.threadPoolExecutor = new ThreadPoolExecutor(2, 100, 100, TimeUnit.SECONDS, queue);
    }

    public void runParsing(Spidering spidering)
    {
        ParsingRun parsingRun = new ParsingRun(systemLogger, spidering, serviceProvider);
        RunningSpideringCache.runningParsings.put(spidering.getId(), parsingRun);

        threadPoolExecutor.execute(parsingRun);
    }

    public boolean isParsingRunning(Spidering spidering)
    {
        return RunningSpideringCache.runningParsings.containsKey(spidering.getId());
    }

    public void terminateParsing(Spidering spidering)
    {
        if (RunningSpideringCache.runningParsings.containsKey(spidering.getId())) {
            RunningSpideringCache.runningParsings.get(spidering.getId()).terminateParsing();
        }
    }
}
