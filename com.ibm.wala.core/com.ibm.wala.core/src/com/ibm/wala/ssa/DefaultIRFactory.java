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
package com.ibm.wala.ssa;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethodWrapper;
import com.ibm.wala.classLoader.ShrikeIRFactory;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.summaries.SyntheticIRFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author Julian Dolby
 *
 */
public class DefaultIRFactory implements IRFactory {
  private final ShrikeIRFactory shrikeFactory = new ShrikeIRFactory();

  private final SyntheticIRFactory syntheticFactory = new SyntheticIRFactory();

  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.IRFactory#makeCFG(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.Context, com.ibm.wala.ipa.cha.ClassHierarchy, com.ibm.wala.util.warnings.WarningSet)
   */
  public ControlFlowGraph makeCFG(IMethod method, Context C, ClassHierarchy cha, WarningSet warnings) {
    if (method.isSynthetic()) {
      return syntheticFactory.makeCFG(method, C, cha, warnings);
    } else if (method instanceof ShrikeCTMethodWrapper) {
      return shrikeFactory.makeCFG(method, C, cha, warnings);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.IRFactory#makeIR(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.Context, com.ibm.wala.ipa.cha.ClassHierarchy, com.ibm.wala.ssa.SSAOptions, com.ibm.wala.util.warnings.WarningSet)
   */
  public IR makeIR(IMethod method, Context C, ClassHierarchy cha, SSAOptions options, WarningSet warnings) {
    if (method.isSynthetic()) {
      return syntheticFactory.makeIR(method, C, cha, options, warnings);
    } else if (method instanceof ShrikeCTMethodWrapper) {
      return shrikeFactory.makeIR(method, C, cha, options, warnings);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

}
