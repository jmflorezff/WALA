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
package com.ibm.wala.ssa.analysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixpoint.BooleanVariable;
import com.ibm.wala.fixpoint.UnaryOr;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Trace;

/**
 *
 * Eliminate dead assignnments (phis) from an SSA IR.
 * 
 * @author sfink
 */
public class DeadAssignmentElimination {

  private static final boolean DEBUG = false;

  /**
   * eliminate dead phis from an ir
   * @param ir
   */
  public static void perform(IR ir) {
    DefUse DU = new DefUse(ir);
    DeadValueSystem system = new DeadValueSystem(ir, DU);
    system.solve();
    doTransformation(ir,system);
  }

  /**
   * Perform the transformation
   * @param ir IR to transform
   * @param solution dataflow solution for dead assignment elimination
   */
  private static void doTransformation(IR ir, DeadValueSystem solution) {
    ControlFlowGraph cfg = ir.getControlFlowGraph();
    for (Iterator x = cfg.iterateNodes(); x.hasNext();) {
      BasicBlock b = (BasicBlock) x.next();
      if (DEBUG) {
        Trace.println("eliminateDeadPhis: " + b);
      }
      if (b.hasPhi()) {
        HashSet<SSAPhiInstruction> toRemove = HashSetFactory.make(5);
        for (Iterator it = b.iteratePhis(); it.hasNext();) {
          SSAPhiInstruction phi = (SSAPhiInstruction) it.next();
          if (phi != null) {
            int def = phi.getDef();
            if (solution.isDead(def)) {
              if (DEBUG) {
                Trace.println("Will remove phi: " + phi);
              }
              toRemove.add(phi);
            }
          }
        }
        b.removePhis(toRemove);
      }
    }
  }

  /**
   * @author sfink
   *
   * A dataflow system which computes whether or not a value is dead
   */
  private static class DeadValueSystem extends DefaultFixedPointSolver {

    /**
     * Map: value number -> BooleanVariable isLive
     */
    private Map<Integer, BooleanVariable> vars = HashMapFactory.make();

    /**
     * set of value numbers that are trivially dead
     */
    private HashSet<Integer> trivialDead = HashSetFactory.make();

    /**
     * @param ir the IR to analyze
     * @param DU def-use information for the IR
     */
    DeadValueSystem(IR ir, DefUse DU) {
      // create a variable for each potentially dead phi instruction.
      for (Iterator it = ir.iteratePhis(); it.hasNext();) {
        SSAPhiInstruction phi = (SSAPhiInstruction) it.next();
        if (phi == null) {
          continue;
        }
        int def = phi.getDef();
        if (DU.getNumberOfUses(def) == 0) {
          // the phi is certainly dead ... record this with a dataflow fact.
          trivialDead.add(new Integer(def));
        } else {
          boolean maybeDead = true;
          for (Iterator uses = DU.getUses(def); uses.hasNext();) {
            SSAInstruction u = (SSAInstruction) uses.next();
            if (!(u instanceof SSAPhiInstruction)) {
              // certainly not dead
              maybeDead = false;
              break;
            }
          }
          if (maybeDead) {
            // perhaps the phi is dead .. create a variable
            BooleanVariable B = new BooleanVariable(false, def);
            vars.put(new Integer(def), B);
          }
        }
      }

      // Now create dataflow equations; v is live iff any phi that uses v is live
      for (Iterator it = vars.entrySet().iterator(); it.hasNext();) {
        Map.Entry E = (Map.Entry) it.next();
        Integer def = (Integer) E.getKey();
        BooleanVariable B = (BooleanVariable) E.getValue();
        for (Iterator uses = DU.getUses(def.intValue()); uses.hasNext();) {
          SSAPhiInstruction u = (SSAPhiInstruction) uses.next();
          Integer ud = new Integer(u.getDef());
          if (trivialDead.contains(ud)) {
            // do nothing ... u will not keep def live
          } else {
            if (!vars.keySet().contains(ud)) {
              // u is not potentially dead ... certainly v is live.
              // record this.
              B.set(true);
            } else {
              // maybe u is dead?
              // add constraint v is live if u is live.
              BooleanVariable U = vars.get(ud);
              newStatement(B, UnaryOr.instance(), U, true, false);
            }
          }
        }
      }
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.dataflow.DataflowSystem#initializeVariables()
     */
    protected void initializeVariables() {
      //do nothing: all variables are initialized to false (TOP), meaning "not live"
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.dataflow.DataflowSystem#initializeWorkList()
     */
    protected void initializeWorkList() {
      addAllStatementsToWorkList();
    }

    /**
     * @param value
     * @return true iff there are no uses of the given value number
     */
    private boolean isDead(int value) {
      Integer V = new Integer(value);
      if (trivialDead.contains(V)) {
        return true;
      } else {
        BooleanVariable B = vars.get(V);
        if (B == null) {
          return false;
        } else {
          return !B.getValue();
        }
      }
    }

  }
}
