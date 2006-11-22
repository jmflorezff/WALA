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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * 
 * A function which gens a vector of outgoing dataflow facts
 * 
 * @author sfink
 */
public class VectorGenFlowFunction implements IReversibleFlowFunction {

  private final SparseIntSet gen;
  
  /**
   * @param gen the intset of facts which are gen'ned by this flow function
   */
  private VectorGenFlowFunction(SparseIntSet gen) {
    this.gen = gen;
    if (Assertions.verifyAssertions) {
      Assertions._assert(gen.contains(0));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.transactions.IFlowFunction#eval(int)
   */
  public SparseIntSet getTargets(int i) {
    return (i == 0) ? gen : gen.contains(i) ? null : SparseIntSet.singleton(i);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.transactions.IFlowFunction#eval(int)
   */
  public SparseIntSet getSources(int i) {
    return (gen.contains(i)) ? SparseIntSet.singleton(0) : SparseIntSet.singleton(i);
  }

  /**
   * @param gen the intset of facts which should be gen'ed by a function
   * @return an instance of a flow function which gens these facts
   */
  public static VectorGenFlowFunction make(SparseIntSet gen) {
    return new VectorGenFlowFunction(gen);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "VectorGen: " + gen;
  }
}