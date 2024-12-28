package selector;

import java.awt.Point;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {
        return new PolyLine(super.lastPoint(), p);
    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {
        super.selection.add(new PolyLine(super.lastPoint(), p));
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    @Override
    public void movePoint(int index, Point newPos) {
        // Confirm that we have a closed selection and that `index` is valid
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }

        Point newPosCopy = new Point(newPos);

        ListIterator<PolyLine> iter = selection.listIterator();
        while (iter.hasNext()) {
            if (index == 0 && iter.nextIndex() == 0) {
                PolyLine oldAfter = iter.next();
                PolyLine newAfter = new PolyLine(newPosCopy, new Point(oldAfter.xs()[1],
                        oldAfter.ys()[1]));
                iter.set(newAfter);

                while (iter.hasNext()) {
                    iter.next();
                }
                PolyLine oldBefore = iter.previous();
                PolyLine newBefore = new PolyLine(new Point(oldBefore.xs()[0], oldBefore.ys()[0]),
                        newPosCopy);
                iter.set(newBefore);
            } else if (iter.nextIndex() == index) {
                PolyLine oldBefore = iter.previous();
                PolyLine newBefore = new PolyLine(new Point(oldBefore.xs()[0], oldBefore.ys()[0]),
                        newPosCopy);
                iter.set(newBefore);

                PolyLine oldAfter = iter.next();
                oldAfter = iter.next();
                PolyLine newAfter = new PolyLine(newPosCopy, new Point(oldAfter.xs()[1],
                        oldAfter.ys()[1]));
                iter.set(newAfter);
            } else {
                iter.next();
            }
        }
        start = selection.getFirst().start();
        propSupport.firePropertyChange("selection", null, selection());
    }
}
