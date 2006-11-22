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

import java.util.Collection;

import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *
 */
public class SSASwitchInstruction extends SSAInstruction {
  private final int val;
  private final int defaultLabel;
  private final int[] casesAndLabels;
  SSASwitchInstruction(int val, int defaultLabel, int[] casesAndLabels) {
    super();
    this.val = val;
    this.defaultLabel = defaultLabel;
    this.casesAndLabels =  casesAndLabels;
  }
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return
      new SSASwitchInstruction(
        uses==null? val: uses[0],
	defaultLabel,
	casesAndLabels);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "switch " + getValueString(symbolTable, d, val);
  }
  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitSwitch(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j <= 1);
    return val;
  }

  public int[] getTargets() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  public int getTarget(int caseValue) {
     for(int i = 0; i < casesAndLabels.length; i+= 2)
       if (caseValue == casesAndLabels[i])
       return casesAndLabels[i+1];
 
     return defaultLabel;
   }
 
  public int getDefault() { return defaultLabel; }
 
  public int[] getCasesAndLabels() { return casesAndLabels; }

  public int hashCode() {
    return val * 1663 ^ 3499;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  public boolean isFallThrough() {
    return false;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return null;
  }
}
