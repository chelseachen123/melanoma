package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A min priority queue of distinct elements of type `KeyType` associated with (extrinsic) integer
 * priorities, implemented using a binary heap paired with a hash table.
 */
public class HeapMinQueue<KeyType> implements MinQueue<KeyType> {

    /**
     * Pairs an element `key` with its associated priority `priority`.
     */
    private record Entry<KeyType>(KeyType key, int priority) {
        // Note: This is equivalent to declaring a static nested class with fields `key` and
        //  `priority` and a corresponding constructor and observers, overriding `equals()` and
        //  `hashCode()` to depend on the fields, and overriding `toString()` to print their values.
        // https://docs.oracle.com/en/java/javase/17/language/records.html
    }

    /**
     * Associates each element in the queue with its index in `heap`.  Satisfies
     * `heap.get(index.get(e)).key().equals(e)` if `e` is an element in the queue. Only maps
     * elements that are in the queue (`index.size() == heap.size()`).
     */
    private final Map<KeyType, Integer> index;

    /**
     * Sequence representing a min-heap of element-priority pairs.  Satisfies
     * `heap.get(i).priority() >= heap.get((i-1)/2).priority()` for all `i` in `[1..heap.size()]`.
     */
    private final ArrayList<Entry<KeyType>> heap;

    /**
     * Assert that our class invariant is satisfied.  Returns true if it is (or if assertions are
     * disabled).
     */
    private boolean checkInvariant() {
        for (int i = 1; i < heap.size(); ++i) {
            int p = (i - 1) / 2;
            assert heap.get(i).priority() >= heap.get(p).priority();
            assert index.get(heap.get(i).key()) == i;
        }
        assert index.size() == heap.size();
        return true;
    }

    /**
     * Create an empty queue.
     */
    public HeapMinQueue() {
        index = new HashMap<>();
        heap = new ArrayList<>();
        assert checkInvariant();
    }

    /**
     * Return whether this queue contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Return the number of elements contained in this queue.
     */
    @Override
    public int size() {
        return heap.size();
    }

    /**
     * Return an element associated with the smallest priority in this queue.  This is the same
     * element that would be removed by a call to `remove()` (assuming no mutations in between).
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType get() {
        // Propagate exception from `List::getFirst()` if empty.
        return heap.getFirst().key();
    }

    /**
     * Return the minimum priority associated with an element in this queue.  Throws
     * NoSuchElementException if this queue is empty.
     */
    @Override
    public int minPriority() {
        return heap.getFirst().priority();
    }

    /**
     * If `key` is already contained in this queue, change its associated priority to `priority`.
     * Otherwise, add it to this queue with that priority.
     */
    @Override
    public void addOrUpdate(KeyType key, int priority) {
        if (!index.containsKey(key)) {
            add(key, priority);
        } else {
            update(key, priority);
        }
    }

    /**
     * Remove and return the element associated with the smallest priority in this queue.  If
     * multiple elements are tied for the smallest priority, an arbitrary one will be removed.
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        // Return first element from `index` and `heap`.
        KeyType returnKey = heap.get(0).key();

        // Swap first and last element of heap.
        swap(0, heap.size() - 1);

        heap.remove((int) index.get(returnKey));
        // Remove element with key `returnKey`.
        index.remove(returnKey);

        // Bubble down.
        if (!heap.isEmpty()) {
            bubbleDown(0);
        }

        checkInvariant();
        return returnKey;
    }

    /**
     * Remove all elements from this queue (making it empty).
     */
    @Override
    public void clear() {
        index.clear();
        heap.clear();
        assert checkInvariant();
    }

    /**
     * Swap the Entries at indices `i` and `j` in `heap`, updating `index` accordingly.  Requires `0
     * <= i,j < heap.size()`.
     */
    private void swap(int i, int j) {
        assert i >= 0 && i < heap.size();
        assert j >= 0 && j < heap.size();
        Entry<KeyType> temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);

        index.put(heap.get(i).key(), i);
        index.put(heap.get(j).key(), j);
    }

    /**
     * Add element `key` to this queue, associated with priority `priority`.  Requires `key` is not
     * contained in this queue.
     */
    private void add(KeyType key, int priority) {
        assert !index.containsKey(key);

        heap.add(new Entry<KeyType>(key, priority));
        index.put(key, heap.size() - 1);
        bubbleUp(heap.size() - 1);

        assert checkInvariant();
    }

    /**
     * Change the priority associated with element `key` to `priority`.  Requires that `key` is
     * contained in this queue.
     */
    private void update(KeyType key, int priority) {
        assert index.containsKey(key);

        int oldPriority = heap.get(index.get(key)).priority();
        heap.set(index.get(key), new Entry<KeyType>(key, priority));
        if (priority > oldPriority) {
            bubbleDown(index.get(key));
        } else {
            bubbleUp(index.get(key));
        }

        assert checkInvariant();
    }

    /**
     * Bubbles up the element with index `index` of the min heap by repeatedly swapping with its
     * parent until the class invariant is satisfied.
     * Requires 0 <= `index` <= heap.size() - 1.
     */
    private void bubbleUp(int index) {
        assert 0 <= index;
        assert index <= heap.size() - 1;
        boolean done = false;
        while (index != 0 && !done) {
            // Swap lesser child of parent with parent if priority is less than that of parent.
            int parentIndex = (index - 1) / 2;
            if (heap.get(index).priority() < heap.get(parentIndex).priority()) {
                int leftChildIndex = parentIndex * 2 + 1;
                // If there is not a right child, let right child be left child.
                int rightChildIndex = leftChildIndex + (leftChildIndex < heap.size() - 1 ? 1 : 0);
                if (heap.get(rightChildIndex).priority() < heap.get(leftChildIndex).priority()) {
                    swap(parentIndex, rightChildIndex);
                } else {
                    swap(parentIndex, leftChildIndex);
                }
                index = parentIndex;
            } else {
                done = true;
            }
        }
        checkInvariant();
    }

    /**
     * Bubbles down the element with index `index` by repeatedly swapping with child with lesser
     * priority until the class invariant is satisfied.
     *
     * Requires 0 <= `index` <= heap.size() - 1.
     */
    private void bubbleDown(int index) {
        assert 0 <= index;
        assert index <= heap.size() - 1;
        int leftChildIndex = index * 2 + 1;
        // If there is not a right child, let right child be left child.
        int rightChildIndex = leftChildIndex + (leftChildIndex < heap.size() - 1 ? 1 : 0);
        // Swap only if there are children to swap with.
        boolean done = false;
        while (leftChildIndex <= heap.size() - 1 && !done) {
            // Swap only if `index` has greater priority than at least one child.
            if (heap.get(index).priority() > Math.min(heap.get(leftChildIndex).priority(),
            heap.get(rightChildIndex).priority())) {
                // Swap with the child with less priority, choosing the left child if tied.
                if (heap.get(rightChildIndex).priority() < heap.get(leftChildIndex).priority()) {
                    swap(index, rightChildIndex);
                    index = rightChildIndex;
                } else {
                    swap(index, leftChildIndex);
                    index = leftChildIndex;
                }
            } else {
                done = true;
            }
            leftChildIndex = index * 2 + 1;
            // If there is not a right child, let right child be left child.
            rightChildIndex = leftChildIndex + (leftChildIndex < heap.size() - 1 ? 1 : 0);
        }
        checkInvariant();
    }
}
