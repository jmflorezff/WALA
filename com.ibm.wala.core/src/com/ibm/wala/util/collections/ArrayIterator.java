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
package com.ibm.wala.util.collections;

import java.util.Iterator;

/**
 *
 * an Iterator of array elements
 * 
 * @author unknown
 */
public class ArrayIterator<T> implements Iterator<T> {

  /**
   * The index of the next array element to return
   */
  protected int _cnt;
  
  /**
   * The index of the last array element to return
   */
  protected final int last;

  /**
   * The array source for the iterator
   */
  protected final T[] _elts;
  
  
  /**
   * @param elts the array which should be iterated over
   */
  public ArrayIterator(T[] elts) {
    this(elts, 0);
  }

  /**
   * @param elts the array which should be iterated over
   * @param start the first array index to return 
   */
  public ArrayIterator(T[] elts, int start) {
    _elts = elts;
    _cnt = start;
    last = _elts.length - 1;
  }
  
  /**
   * @param elts the array which should be iterated over
   * @param start the first array index to return 
   */
  public ArrayIterator(T[] elts, int start, int last) {
    _elts = elts;
    _cnt = start;
    this.last = last;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return _cnt <= last;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public T next() {
    return _elts[_cnt++];
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
