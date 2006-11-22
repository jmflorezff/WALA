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

package com.ibm.wala.dataflow.ssa;

import java.util.Iterator;

import com.ibm.wala.fixedpoint.impl.AbstractOperator;
import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixedpoint.impl.NullaryOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 *
 * This class performs intraprocedural propagation over an SSA form
 * 
 * @author sfink
 */
public abstract class SSAInference extends DefaultFixedPointSolver {
  static final boolean DEBUG = false;

  /**
   * The governing SSA form
   */
  private IR ir;

  /**
   * The governing symbol table
   */
  private SymbolTable symbolTable;

  /**
   * Dataflow variables, one for each value in the symbol table.
   */
  private IVariable[] vars;

  public interface OperatorFactory {
    /**
     * Get the dataflow operator induced by an instruction in SSA form.
     * @param instruction
     * @return dataflow operator for the instruction, or null if the
     * instruction is not applicable to the dataflow system.
     */
    AbstractOperator get(SSAInstruction instruction);
  }

  public interface VariableFactory {
    /**
     * Make the variable for a given value number.
     * @return a newly created dataflow variable, or null if not applicable.
     */
    public IVariable makeVariable(int valueNumber);
  }

  /**
   * initializer for SSA Inference equations.
   */
  protected void init(IR ir, VariableFactory varFactory, OperatorFactory opFactory) {

    this.ir = ir;
    this.symbolTable = ir.getSymbolTable();

    createVariables(varFactory);
    createEquations(opFactory);
  }

  /**
   * Method createEquations.
   * TODO: optimize more.
   * @param opFactory
   */
  private void createEquations(OperatorFactory opFactory) {
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction s = instructions[i];
      makeEquationForInstruction(opFactory, s);
    }
    for (Iterator it = ir.iteratePhis(); it.hasNext();) {
      SSAInstruction s = (SSAInstruction) it.next();
      makeEquationForInstruction(opFactory, s);
    }
    for (Iterator it = ir.iteratePis(); it.hasNext();) {
      SSAInstruction s = (SSAInstruction) it.next();
      makeEquationForInstruction(opFactory, s);
    }
    for (Iterator it = ir.iterateCatchInstructions(); it.hasNext();) {
      SSAInstruction s = (SSAInstruction) it.next();
      makeEquationForInstruction(opFactory, s);
    }
  }

  /**
   * Create a dataflow equation induced by a given instruction
   */
  private void makeEquationForInstruction(OperatorFactory opFactory, SSAInstruction s) {
    if (s != null && s.hasDef()) {
      AbstractOperator op = opFactory.get(s);
      if (op != null) {
        IVariable def = getVariable(s.getDef());
        if (op instanceof NullaryOperator) {
          newStatement(def, (NullaryOperator)op, false, false);
        } else {
          int n = s.getNumberOfUses();
          IVariable[] uses = new IVariable[n];
          for (int j = 0; j < n; j++) {
            if (s.getUse(j) > -1) {
              uses[j] = getVariable(s.getUse(j));
              if (Assertions.verifyAssertions) {
                Assertions._assert(uses[j] != null);
              }
            }
          }
          newStatement(def, op, uses, false, false);
        }
      }
    }
  }

  /**
   * Create a dataflow variable for each value number 
   */
  private void createVariables(VariableFactory factory) {
    vars = new IVariable[symbolTable.getMaxValueNumber() + 1];
    for (int i = 1; i < vars.length; i++) {
      vars[i] = factory.makeVariable(i);
    }

  }

  /**
   * @param valueNumber
   * @return the dataflow variable representing the value number,
   * or null if none found.
   */
  protected IVariable getVariable(int valueNumber) {
    if (DEBUG) {
      Trace.println("getVariable for " + valueNumber + " returns " + vars[valueNumber]);
    }
    if (Assertions.verifyAssertions) {
      if (vars == null) {
        Assertions._assert(vars != null, "null vars array");
      }
    }
    return vars[valueNumber];
  }
  /** 
   * Return a string representation of the system 
   * @return a string representation of the system 
   */
  public String toString() {
    StringBuffer result = new StringBuffer("Type inference : \n");
    for (int i = 0; i < vars.length; i++) {
      result.append("v").append(i).append("  ").append(vars[i]).append("\n");
    }
    return result.toString();
  }
}
