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
package com.ibm.wala.util.intset;

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;

/** 
 * A bit set is a set of elements, each of which corresponds to a unique
 * integer from [0,MAX].  
 *
 * @author by Stephen Fink
 */
public final class BitSet<T> {

  /**
   * The backing bit vector that determines set membership.
   */
  private BitVector vector;

  /**
   * The bijection between integer to object. 
   */
  private OrdinalSetMapping<T> map;

  /**
   * Constructor: create an empty set corresponding to a given mapping
   */
  public BitSet(OrdinalSetMapping<T> map) {
    int length = map.getMappingSize();
    vector = new BitVector(length);
    this.map = map;
  }

  public BitSet(BitSet<T> B) {
    this(B.map);
    addAll(B);
  }

  /**
   * Add all elements in bitset B to this bit set
   */
  public void addAll(BitSet B) {
    vector.or(B.vector);
  }

  /**
   * Add all bits in BitVector B to this bit set
   */
  public void addAll(BitVector B) {
    vector.or(B);
  }

  /**
   * Add an object to this bit set.
   */
  public void add(T o) {
    int n = map.getMappedIndex(o);
    vector.set(n);
  }

  /**
   * Remove an object from this bit set.
   * @param o the object to remove
   */
  public void clear(T o) {
    int n = map.getMappedIndex(o);
    vector.clear(n);
  }

  /**
   * Does this set contain a certain object?
   */
  public boolean contains(T o) {
    int n = map.getMappedIndex(o);
    return vector.get(n);
  }

  /**
   * @return a String representation
   */
  public String toString() {
    return vector.toString();
  }

  /**
   * Method copy.  Copies the bits in the bit vector, but only assigns the object map.  No need to create
   * a new object/bit bijection object.
   */
  public void copyBits(BitSet<T> other) {
    vector.copyBits(other.vector);
    map = other.map;
  }

  /**
   * Does this object hold the same bits as other?
   */
  public boolean sameBits(BitSet other) {
    //		if (Assertions.verifyAssertions) {
    //      Assertions._assert(map.equals(other.map));
    //    }
    return vector.equals(other.vector);
  }

  /**
   * Method iterator.
   * Not very efficient.
   * @return Iterator
   */
  public Iterator iterator() {
    return new Iterator() {
      private int next = -1;
      {
        for (int i = 0; i < vector.length(); i++) {
          if (vector.get(i)) {
            next = i;
            break;
          }
        }
      }
      public boolean hasNext() {
        return (next != -1);
      }
      public Object next() {
        Object result = map.getMappedObject(next);
        int start = next + 1;
        next = -1;
        for (int i = start; i < vector.length(); i++) {
          if (vector.get(i)) {
            next = i;
            break;
          }
        }
        return result;
      }
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /**
   * Method size.
   * @return int
   */
  public int size() {
    return vector.populationCount();
  }

  public int length() {
    return vector.length();
  }

  /**
   * Set all the bits to 0.
   */
  public void clearAll() {
    vector.clearAll();
  }

  /**
   * Set all the bits to 1.
   */
  public void setAll() {
    vector.setAll();
  }

  /**
   * Perform intersection of two bitsets
   * @param other the other bitset in the operation
   */
  public void intersect(BitSet other) {
    vector.and(other.vector);
  }

  /**
   * Perform the difference of two bit sets
   * @param other
   */
  public void difference(BitSet other) {
    //		if (Assertions.verifyAssertions) {
    //			Assertions._assert(map.equals(other.map));
    //		}
    vector.and(BitVector.not(other.vector));
  }

  /**
   */
  public boolean isEmpty() {
    return size() == 0;
  }

}
