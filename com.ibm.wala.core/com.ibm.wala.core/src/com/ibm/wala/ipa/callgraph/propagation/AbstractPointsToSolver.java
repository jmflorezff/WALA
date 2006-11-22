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
package com.ibm.wala.ipa.callgraph.propagation;


/**
 * abstract base class for solver for pointer analysis
 * 
 * @author sfink
 */
public abstract class AbstractPointsToSolver implements IPointsToSolver {

  protected final static boolean DEBUG = false;

  private final PropagationSystem system;

  private final PropagationCallGraphBuilder builder;
  
  private final ReflectionHandler reflectionHandler;

  /**
   * @param system
   * @param builder
   */
  public AbstractPointsToSolver(PropagationSystem system, PropagationCallGraphBuilder builder) {
    this.system = system;
    this.builder = builder;
    this.reflectionHandler = new ReflectionHandler(builder);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.IPointsToSolver#solve()
   */
  public abstract void solve();

  protected PropagationCallGraphBuilder getBuilder() {
    return builder;
  }

  protected ReflectionHandler getReflectionHandler() {
    return reflectionHandler;
  }

  protected PropagationSystem getSystem() {
    return system;
  }
}
