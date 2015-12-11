/*
 * Copyright 2015 Luca Baggi, Marco Mezzanotte
 * 
 * This file is part of ADPF.
 *
 *  ADPF is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ADPF is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ADPF.  If not, see <http://www.gnu.org/licenses/>.
 */


package it.polimi.geinterface.concurrency;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.util.Log;

/**
 * A thin wrapper around a thread pool executor that only exposes partially what the executor is
 * doing. This is so that we don't make a mistake somewhere along the way and jack something up.
 *
 * @author Matthew A. Johnston (warmwaffles)
 */
public class Scheduler {
    private PausableThreadPoolExecutor executor;
    private LinkedBlockingQueue<Runnable> queue;

    public Scheduler() {
        int processors = Runtime.getRuntime().availableProcessors();
        queue = new LinkedBlockingQueue<Runnable>();
        executor = new PausableThreadPoolExecutor(processors, 10, 10, TimeUnit.SECONDS, queue);
    }

    public void schedule(Runnable runnable) {
        executor.execute(runnable);
        
    }

    public void pause() {
    	Log.d("Scheduler", "PAUSED!");
        executor.pause();
    }

    public void resume() {
    	Log.d("Scheduler", "RESUMED!");
        executor.resume();
    }

    public void clear() {
        queue.clear();
    }
    
    public void stop(){
    	executor.shutdownNow();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
