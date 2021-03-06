package com.cliffc.aa.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
  
// ArrayList with saner syntax
public class Ary<E> implements Iterable<E> {
  public E[] _es;
  public int _len;
  public Ary(E[] es) { this(es,es.length); }
  public Ary(E[] es, int len) { _es=es; _len=len; }
  public Ary(Class<E> clazz) { this((E[]) Array.newInstance(clazz, 1),0); }

  /** @return list is empty */
  public boolean isEmpty() { return _len==0; }
  /** @return active list length */
  public int len() { return _len; }
  /** @param i element index
   *  @return element being returned; throws if OOB */
  public E at( int i ) {
    range_check(i);
    return _es[i];
  }
  /** @param i element index
   *  @return element being returned, or null if OOB */
  public E atX( int i ) {
    return i < _len ? _es[i] : null;
  }
  /** @return last element */
  public E last( ) {
    range_check(0);
    return _es[_len-1];
  }
  
  /** @return remove and return last element */
  public E pop( ) {
    range_check(0);
    return _es[--_len];
  }
  
  /** Add element in amortized constant time
   *  @param e Element to add at end of list
   *  @return 'this' for flow-coding */
  public Ary<E> add( E e ) {
    if( _len >= _es.length ) _es = Arrays.copyOf(_es,Math.max(1,_es.length<<1));
    _es[_len++] = e;
    return this;
  }

  /** Fast, constant-time, element removal.  Does not preserve order
   *  @param i element to be removed
   *  @return element removed */
  public E del( int i ) {
    range_check(i);
    E tmp = _es[i];
    _es[i]=_es[--_len];
    return tmp;
  }

  /** Slow, linear-time, element removal.  Preserves order.
   *  @param i element to be removed 
   *  @return element removed */
  public E remove( int i ) {
    range_check(i);
    E e = _es[i];
    System.arraycopy(_es,i+1,_es,i,(--_len)-i);
    return e;
  }
  
  /** Remove all elements */
  public void clear( ) { _len=0; }

  // Extend and set
  public E setX( int i, E e ) {
    while( i>= _es.length ) _es = Arrays.copyOf(_es,_es.length<<1);
    if( i >= _len ) _len = i+1;
    return (_es[i] = e);
  }
  
  public E set( int i, E e ) {
    range_check(i);
    return (_es[i] = e);
  }
  
  public Ary<E> set_as( E e ) { _es[0] = e; _len=1; return this; }
  public Ary<E> set_len( int len ) {
    if( len > _len ) throw new RuntimeException("unimpl");
    _len = len;
    while( _es.length > (len<<1) ) // Shrink if hugely too large
      _es = Arrays.copyOf(_es,_es.length>>1);
    Arrays.fill(_es,len,_es.length,null);
    return this;
  }
  
  /** @param c Collection to be added */
  public void addAll( Collection<? extends E> c ) { for( E e : c ) add(e); }
    
  /** @param es Array to be added */
  public <F extends E> Ary<E> addAll( F[] es ) {
    if( es.length==0 ) return this;
    while( _len+es.length > _es.length ) _es = Arrays.copyOf(_es,_es.length<<1);
    System.arraycopy(es,0,_es,_len,es.length);
    _len += es.length;
    return this;
  }
    
  /** @param c Collection to be added */
  public void addAll( Ary<? extends E> c ) {
    if( c._len==0 ) return;
    while( _len+c._len > _es.length ) _es = Arrays.copyOf(_es,_es.length<<1);
    System.arraycopy(c._es,0,_es,_len,c._len);
    _len += c._len;
  }
  
  /** @return compact array version, using the internal base array where possible. */
  public E[] asAry() { return _len==_es.length ? _es : Arrays.copyOf(_es,_len); }

  /** @param f function to apply to each element.  Updates in-place. */
  public Ary<E> map_update( Function<E,E> f ) { for( int i = 0; i<_len; i++ ) _es[i] = f.apply(_es[i]); return this; }
  /** @param P filter out elements failing to pass the predicate; updates in
   *  place and shuffles list. 
   *  @return this, for flow-coding */
  public Ary<E> filter_update( Predicate<E> P ) {
    for( int i=0; i<_len; i++ )
      if( !P.test(_es[i]) )
        del(i--);
    return this;
  }
  /** Sorts in-place 
   *  @param c Comparator to sort by */
  public void sort_update(Comparator<? super E> c ) { Arrays.sort(_es, 0, _len, c);  }
  /** Find the first matching element using ==, or -1 if none.  Note that
   *  most del calls shuffle the list, so the first element might be random.
   *  @param e Element to find
   *  @return index of first matching element, or -1 if none */
  public int find( E e ) {
    for( int i=0; i<_len; i++ )  if( _es[i]==e )  return i;
    return -1;
  }
  /** Find the first element matching predicate P, or -1 if none.  Note that
   *  most del calls shuffle the list, so the first element might be random.
   *  @param P Predicate to match
   *  @return index of first matching element, or -1 if none */
  public int find( Predicate<E> P ) {
    for( int i=0; i<_len; i++ )  if( P.test(_es[i]) )  return i;
    return -1;
  }

  /** Merge-Or.  Merge 2 sorted Arys, tossing out duplicates.  Return a new
   *  sorted Ary with the merged list.  Undefined if the original arrays are
   *  not sorted.  Error if they are not of the same type.  Elements must
   *  implement Comparable.
   *  @param a0 Sorted Ary to merge
   *  @param a1 Sorted Ary to merge
   *  @return A new sorted merged Ary
   */
  public static <X extends Comparable<X>> Ary<X> merge_or( Ary<X> a0, Ary<X> a1 ) {
    int i=0, j=0;
    Ary<X> res = new Ary<>(Arrays.copyOf(a0._es,a0._len+a1._len),0);

    while( i<a0._len && j<a1._len ) {
      X x = a0._es[i];
      X y = a1._es[j];
      int cmp = x.compareTo(y);
      if( cmp<0 )      { res.add(x); i++;      }
      else if( cmp>0 ) { res.add(y);      j++; }
      else             { res.add(x); i++; j++; }
    }
    while( i<a0._len ) res.add(a0._es[i++]);
    while( j<a1._len ) res.add(a1._es[j++]);
    return res;
  }
  /** Merge-And.  Merge 2 sorted Arys, keeping only duplicates.  Return a new
   *  sorted Ary with the merged list.  Undefined if the original arrays are
   *  not sorted.  Error if they are not of the same type.  Elements must
   *  implement Comparable.
   *  @param a0 Sorted Ary to merge
   *  @param a1 Sorted Ary to merge
   *  @return A new sorted merged Ary
   */
  public static <X extends Comparable<X>> Ary<X> merge_and( Ary<X> a0, Ary<X> a1 ) {
    int i=0, j=0;
    Ary<X> res = new Ary<>(Arrays.copyOf(a0._es,Math.min(a0._len,a1._len)),0);
    while( i<a0._len && j<a1._len ) {
      X x = a0._es[i];
      X y = a1._es[j];
      int cmp = x.compareTo(y);
      if( cmp<0 )      { i++;      }
      else if( cmp>0 ) {      j++; }
      else { res.add(x); i++; j++; }
    }
    return res;
  }
  
  /** @return an iterator */
  @Override public Iterator<E> iterator() { return new Iter(); }
  private class Iter implements Iterator<E> {
    int _i=0;
    @Override public boolean hasNext() { return _i<_len; }
    @Override public E next() { return _es[_i++]; }
  }
  
  @Override public String toString() {
    SB sb = new SB().p('{');
    for( int i=0; i<_len; i++ ) {
      if( i>0 ) sb.p(',');
      if( _es[i] != null ) sb.p(_es[i].toString());
    }
    return sb.p('}').toString();
  }

  private void range_check( int i ) {
    if( i < 0 || i>=_len )
      throw new ArrayIndexOutOfBoundsException(""+i+" >= "+_len);
  }

  @Override public boolean equals( Object o ) {
    if( this==o ) return true;
    if( !(o instanceof Ary) ) return false;
    Ary ary = (Ary)o;
    if( _len != ary._len ) return false;
    if( _es == ary._es ) return true;
    for( int i=0; i<_len; i++ )
      if( !(_es[i]==null ? (ary._es[i] == null) : _es[i].equals(ary._es[i])) )
        return false;
    return true;
  }
  @Override public int hashCode( ) {
    int sum=_len;
    for( int i=0; i<_len; i++ )
      sum += _es[i]==null ? 0 : _es[i].hashCode();
    return sum;
  }
}
