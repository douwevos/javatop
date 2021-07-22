package com.github.douwevos.javatop.thread;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class TailListProducer<T> {

	private TailList<T> list;
	
	public TailListProducer(int samples, int plus) {
		list = new TailList<T>(samples, plus);
	}
	
	public int length() {
		return list.samples;
	}

	
	public void add(T item) {
		list = list.append(item);
	}
	
	public List<T> produce() {
		return list.produce();
	}
	
	static class TailList<T> {
		
		private final T[] list;
		private final int samples;
		private final int plus;
		private int out;
		
		@SuppressWarnings("unchecked")
		public TailList(int samples, int plus) {
			this.samples = samples;
			this.plus = plus;
			Object p[] = new Object[samples+plus];
			list = (T[]) p;
		}
		
		public List<T> produce() {
			int start = out-samples;
			if (start<0) {
				start = 0;
			}
			return new TailSnapshotList<T>(list, start, out);
		}
		
		public TailList<T> append(T item) {
			if (out<list.length) {
				list[out] = item;
				out++;
				return this;
			}
			TailList<T> copy = new TailList<>(samples, plus);
			System.arraycopy(list, plus, copy.list, 0, samples);
			copy.out = samples+1;
			copy.list[samples] = item;
			return copy;
		}
		
	}

	static class TailSnapshotList<T> implements List<T> {

		private final T[] list;
		private final int start;
		private final int end;
		
		public TailSnapshotList(T[] list, int start, int end) {
			this.list = list;
			this.start = start;
			this.end = end;
		}
		
		@Override
		public int size() {
			return end-start;
		}

		@Override
		public boolean isEmpty() {
			return start<=end;
		}

		@Override
		public boolean contains(Object o) {
			for(int s=start; s<end; s++) {
				if (Objects.equals(list[s], o)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {

				int pointer = start;
				
				@Override
				public boolean hasNext() {
					return pointer<end;
				}
				
				@Override
				public T next() {
					return pointer<end ? list[pointer] : null;
				}
				
			};
		}

		@Override
		public Object[] toArray() {
			Object result[] = new Object[end-start];
			int out = 0;
			for(int s=start; s<end; s++) {
				result[out] = list[s];
				out++;
			}
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <S> S[] toArray(S[] a) {
			if (a==null || a.length<(end-start)) {
				Object[] result = toArray();
				return (S[]) result;
			}
			int out = 0;
			for(int s=start; s<end; s++) {
				a[out] = (S) list[s];
				out++;
			}
			return a;
		}

		@Override
		public boolean add(T e) {
			return false;
		}

		@Override
		public boolean remove(Object o) {
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return c.stream().allMatch(s -> contains(s));
		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			return false;
		}

		@Override
		public boolean addAll(int index, Collection<? extends T> c) {
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return false;
		}

		@Override
		public void clear() {
			
		}

		@Override
		public T get(int index) {
			if ((index<0) || (index>=(end-start))) {
				throw new IndexOutOfBoundsException(index);
			}
			return list[index+start];
		}

		@Override
		public T set(int index, T element) {
			return get(index);
		}

		@Override
		public void add(int index, T element) {
		}

		@Override
		public T remove(int index) {
			return null;
		}

		@Override
		public int indexOf(Object o) {
			for(int s=start; s<end; s++) {
				if (Objects.equals(list[s], o)) {
					return s-start;
				}
			}
			return -1;
		}

		@Override
		public int lastIndexOf(Object o) {
			for(int s=end-1; s>=start; s--) {
				if (Objects.equals(list[s], o)) {
					return s-start;
				}
			}
			return -1;
		}

		@Override
		public ListIterator<T> listIterator() {
			return null;
		}

		@Override
		public ListIterator<T> listIterator(int index) {
			return null;
		}

		@Override
		public List<T> subList(int fromIndex, int toIndex) {
			
			fromIndex += start;
			toIndex +=start;
			
			if (fromIndex<start) {
				fromIndex = start;
			}
			if (toIndex>end) {
				toIndex=end;
			}
			if (fromIndex==start && toIndex==end) {
				return this;
			}
			
			if ((fromIndex>=end) || (toIndex<=start)) {
				return Collections.emptyList();
			}
			
			return new TailSnapshotList<T>(list, fromIndex, toIndex);
		}
		
	}

	
	
}
