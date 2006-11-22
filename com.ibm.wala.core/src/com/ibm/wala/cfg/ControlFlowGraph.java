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
package com.ibm.wala.cfg;

import java.util.Collection;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.BitVector;

/**
 * An interface that is common to the Shrike and SSA CFG implementations.
 * 
 * @author cahoon
 * @author sfink
 */
public interface ControlFlowGraph extends NumberedGraph<IBasicBlock> {

  /**
   * Return the entry basic block in the CFG
   */
  public IBasicBlock entry();

  /**
   * @return the synthetic exit block for the cfg
   */
  public IBasicBlock exit();

  /**
   * @return the indices of the catch blocks, as a bit vector
   */
  public BitVector getCatchBlocks();

  /**
   * @param index
   *          an instruction index
   * @return the basic block which contains this instruction.
   */
  public IBasicBlock getBlockForInstruction(int index);

  /**
   * @return the instructions of this CFG, as an array.
   */
  IInstruction[] getInstructions();

  /**
   * @param index
   *          an instruction index
   * @return the program counter (bytecode index) corresponding to that
   *         instruction
   */
  public int getProgramCounter(int index);

  /**
   * @return the Method this CFG represents
   */
  public IMethod getMethod();

  /**
   * The order of blocks returned should be arbitrary but deterministic.
   * 
   * @param b
   * @return the basic blocks which may be reached from b via exceptional
   *         control flow
   */
  public Collection<IBasicBlock> getExceptionalSuccessors(IBasicBlock b);

  /**
   * The order of blocks returned should be arbitrary but deterministic.
   * @param b
   * @return the basic blocks which may be reached from b via normal control
   *         flow
   */
  public Collection<IBasicBlock> getNormalSuccessors(IBasicBlock b);
  
  /**
   * The order of blocks returned should be arbitrary but deterministic.
   * 
   * @param b
   * @return the basic blocks from which b may be reached via exceptional
   *         control flow
   */
  public Collection<IBasicBlock> getExceptionalPredecessors(IBasicBlock b);

  /**
   * The order of blocks returned should be arbitrary but deterministic.
   * 
   * @param b
   * @return the basic blocks from which b may be reached via normal
   *         control flow
   */
  public Collection<IBasicBlock> getNormalPredecessors(IBasicBlock b);
}
