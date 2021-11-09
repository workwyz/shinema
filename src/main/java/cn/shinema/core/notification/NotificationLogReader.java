//   Copyright 2012,2013 Vaughn Vernon
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package cn.shinema.core.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import cn.shinema.core.media.AbstractJSONMediaReader;
import cn.shinema.core.media.Link;
import cn.shinema.core.media.RepresentationReader;

public class NotificationLogReader extends AbstractJSONMediaReader implements List<NotificationReader>, Iterable<NotificationReader> {

	private JsonArray array;

	public NotificationLogReader(String aJSONRepresentation) {
		super(aJSONRepresentation);

		this.array = this.array("notifications");

		if (this.array == null) {
			this.array = new JsonArray();
		}

		if (!this.array.isJsonArray()) {
			throw new IllegalStateException("There are no notifications, and the representation may not be a log.");
		}
	}

	public boolean isArchived() {
		return this.booleanValue("archived");
	}

	public String id() {
		return this.stringValue("id");
	}

	public Iterator<NotificationReader> notifications() {
		return this.iterator();
	}

	public boolean hasNext() {
		return this.next() != null;
	}

	public Link next() {
		return this.linkNamed("linkNext");
	}

	public boolean hasPrevious() {
		return this.previous() != null;
	}

	public Link previous() {
		return this.linkNamed("linkPrevious");
	}

	public boolean hasSelf() {
		return this.self() != null;
	}

	public Link self() {
		return this.linkNamed("linkSelf");
	}

	///////////////////////////////////////////////
	// Iterable and Collection implementations
	///////////////////////////////////////////////

	public Iterator<NotificationReader> iterator() {
		return new NotificationReaderIterator();
	}

	public int size() {
		return this.array.size();
	}

	public boolean isEmpty() {
		return this.size() > 0;
	}

	public boolean contains(Object o) {
		throw new UnsupportedOperationException("Cannot ask contains.");
	}

	public Object[] toArray() {
		List<NotificationReader> readers = new ArrayList<NotificationReader>();

		for (NotificationReader reader : this) {
			readers.add(reader);
		}

		return readers.toArray();
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		return (T[]) this.toArray();
	}

	public boolean add(NotificationReader e) {
		throw new UnsupportedOperationException("Cannot add.");
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Cannot remove.");
	}

	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("Cannot ask containsAll.");
	}

	public boolean addAll(Collection<? extends NotificationReader> c) {
		throw new UnsupportedOperationException("Cannot addAll.");
	}

	public boolean addAll(int index, Collection<? extends NotificationReader> c) {
		throw new UnsupportedOperationException("Cannot addAll.");
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("Cannot removeAll.");
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Cannot retainAll.");
	}

	public void clear() {
		throw new UnsupportedOperationException("Cannot clear.");
	}

	public NotificationReader get(int index) {
		JsonElement element = this.array.get(index);

		NotificationReader reader = new NotificationReader(element.getAsJsonObject());

		return reader;
	}

	public NotificationReader set(int index, NotificationReader element) {
		throw new UnsupportedOperationException("Cannot set.");
	}

	public void add(int index, NotificationReader element) {
		throw new UnsupportedOperationException("Cannot add.");
	}

	public NotificationReader remove(int index) {
		throw new UnsupportedOperationException("Cannot remove.");
	}

	public int indexOf(Object o) {
		throw new UnsupportedOperationException("Cannot ask indexOf.");
	}

	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException("Cannot ask lastIndexOf.");
	}

	public ListIterator<NotificationReader> listIterator() {
		return new NotificationReaderIterator();
	}

	public ListIterator<NotificationReader> listIterator(int index) {
		return new NotificationReaderIterator();
	}

	public List<NotificationReader> subList(int fromIndex, int toIndex) {

		// this implementation is not a typical subList(), but
		// provides only an immutable subset of the orginal list

		List<NotificationReader> readers = new ArrayList<NotificationReader>();

		for (int idx = fromIndex; idx < toIndex; ++idx) {
			NotificationReader reader = this.get(idx);

			readers.add(reader);
		}

		return Collections.unmodifiableList(readers);
	}

	private Link linkNamed(String aLinkName) {
		Link link = null;

		JsonElement linkElement = this.elementFrom(this.representation(), aLinkName);

		if (linkElement.isJsonObject()) {
			RepresentationReader rep = new RepresentationReader(linkElement.getAsJsonObject());

			link = new Link(rep.stringValue("href"), rep.stringValue("rel"), rep.stringValue("title"), rep.stringValue("type"));
		}

		return link;
	}

	private class NotificationReaderIterator implements ListIterator<NotificationReader> {

		private int index;

		NotificationReaderIterator() {
			super();
		}

		public boolean hasNext() {
			return this.nextIndex() < size();
		}

		public NotificationReader next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException("No such next element.");
			}

			NotificationReader reader = get(this.index++);

			return reader;
		}

		public void remove() {
			throw new UnsupportedOperationException("Cannot remove.");
		}

		public boolean hasPrevious() {
			return this.previousIndex() >= 0;
		}

		public NotificationReader previous() {
			if (!this.hasPrevious()) {
				throw new NoSuchElementException("No such previous element.");
			}

			NotificationReader reader = get(--this.index);

			return reader;
		}

		public int nextIndex() {
			return this.index;
		}

		public int previousIndex() {
			return this.index - 1;
		}

		public void set(NotificationReader e) {
			throw new UnsupportedOperationException("Cannot set.");
		}

		public void add(NotificationReader e) {
			throw new UnsupportedOperationException("Cannot add.");
		}
	}
}
