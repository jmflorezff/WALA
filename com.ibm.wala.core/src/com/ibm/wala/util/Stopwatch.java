/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util;

/**
 * 
 * Basic class to time events. The resolution is one millisecond.
 * 
 * @author sfink
 * @author dgrove
 */
public class Stopwatch extends com.ibm.wala.util.perf.Stopwatch {

  private String name;

  private long startMemory;

  private long endMemory;

  public Stopwatch(String name) {
    super();
    this.name = name;
  }

  public final void start() {
    System.gc();
    Runtime r = Runtime.getRuntime();
    startMemory = r.totalMemory() - r.freeMemory();
    super.start();
  }

  public final void stop() {
    super.stop();
    System.gc();
    Runtime r = Runtime.getRuntime();
    endMemory = r.totalMemory() - r.freeMemory();
  }

  public final String report() {
    String result = "";
    if (getCount() > 0) {
      result += "Stopwatch: " + name + " " + getElapsedMillis() + " ms" + "\n";

    }
    if (getCount() == 1) {
      result += "       Footprint at entry: " + (float) startMemory / 1000000 + " MB\n";
      result += "        Footprint at exit: " + (float) endMemory / 1000000 + " MB\n";
      result += "                    Delta: " + (float) (endMemory - startMemory) / 1000000 + " MB\n";
    }
    return result;
  }

  /**
   * @return memory at the end of the phase, in MB
   */
  public float getEndMemory() {
    return (float) endMemory / 1000000;
  }

  /**
   * @return memory at the end of the phase, in MB
   */
  public float getStartMemory() {
    return (float) startMemory / 1000000;
  }

  /**
   * @return getEndMemory() - getStartMemory()
   */
  public float getFootprint() {
    return getEndMemory() - getStartMemory();
  }

  /**
   * Returns the name for this timer.
   */
  public String getName() {
    return name;
  }

}
