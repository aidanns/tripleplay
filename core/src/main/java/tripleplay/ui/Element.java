//
// Triple Play - utilities for use in PlayN-based games
// Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.ui;

import pythagoras.f.Dimension;
import pythagoras.f.IDimension;
import pythagoras.f.IPoint;
import pythagoras.f.IRectangle;
import pythagoras.f.MathUtil;
import pythagoras.f.Point;
import pythagoras.f.Rectangle;
import pythagoras.f.Transform;

import react.Signal;
import react.SignalView;
import react.Slot;

import playn.core.GroupLayer;
import playn.core.PlayN;

/**
 * The root of the interface element hierarchy. See {@link Widget} for the root of all interactive
 * elements, and {@link Elements} for the root of all grouping elements.
 *
 * @param T used as a "self" type; when subclassing {@code Element}, T must be the type of the
 * subclass.
 */
public abstract class Element<T extends Element<T>>
{
    /** The layer associated with this element. */
    public final GroupLayer layer = createLayer();

    /**
     * Returns this element's x offset relative to its parent.
     */
    public float x () {
        return layer.transform().tx();
    }

    /**
     * Returns this element's y offset relative to its parent.
     */
    public float y () {
        return layer.transform().ty();
    }

    /**
     * Returns the width and height of this element's bounds.
     */
    public IDimension size () {
        return _size;
    }

    /**
     * Writes the location of this element (relative to its parent) into the supplied point.
     * @return {@code loc} for convenience.
     */
    public IPoint location (Point loc) {
        Transform transform = layer.transform();
        return loc.set(transform.tx(), transform.ty());
    }

    /**
     * Writes the current bounds of this element into the supplied bounds.
     * @return {@code bounds} for convenience.
     */
    public IRectangle bounds (Rectangle bounds) {
        Transform transform = layer.transform();
        bounds.setBounds(transform.tx(), transform.ty(), _size.width, _size.height);
        return bounds;
    }

    /**
     * Returns the parent of this element, or null.
     */
    public Elements<?> parent () {
        return _parent;
    }

    /**
     * Returns a signal that will dispatch when this element is added or removed from the
     * hierarchy. The emitted value is true if the element was just added to the hierarchy, false
     * if removed.
     */
    public SignalView<Boolean> hierarchyChanged () {
        if (_hierarchyChanged == null) _hierarchyChanged = Signal.create();
        return _hierarchyChanged;
    }

    /**
     * Returns the styles configured on this element.
     */
    public Styles styles () {
        return _styles;
    }

    /**
     * Configures the styles for this element. Any previously configured styles are overwritten.
     * @return this element for convenient call chaining.
     */
    public T setStyles (Styles styles) {
        _styles = styles;
        clearLayoutData();
        invalidate();
        return asT();
    }

    /**
     * Configures styles for this element (in the DEFAULT mode). Any previously configured styles
     * are overwritten.
     * @return this element for convenient call chaining.
     */
    public T setStyles (Style.Binding<?>... styles) {
        return setStyles(Styles.make(styles));
    }

    /**
     * Adds the supplied styles to this element. Where the new styles overlap with existing styles,
     * the new styles are preferred, but non-overlapping old styles are preserved.
     * @return this element for convenient call chaining.
     */
    public T addStyles (Styles styles) {
        _styles = _styles.merge(styles);
        clearLayoutData();
        invalidate();
        return asT();
    }

    /**
     * Adds the supplied styles to this element (in the DEFAULT mode). Where the new styles overlap
     * with existing styles, the new styles are preferred, but non-overlapping old styles are
     * preserved.
     * @return this element for convenient call chaining.
     */
    public T addStyles (Style.Binding<?>... styles) {
        return addStyles(Styles.make(styles));
    }

    /**
     * Returns <code>this</code> cast to <code>T</code>.
     */
    @SuppressWarnings({"unchecked", "cast"}) protected T asT () {
        return (T)this;
    }

    /**
     * Returns whether this element is enabled.
     */
    public boolean isEnabled () {
        return isSet(Flag.ENABLED);
    }

    /**
     * Enables or disables this element. Disabled elements are not interactive and are usually
     * rendered so as to communicate this state to the user.
     */
    public T setEnabled (boolean enabled) {
        if (enabled != isEnabled()) {
            set(Flag.ENABLED, enabled);
            clearLayoutData();
            invalidate();
        }
        return asT();
    }

    /**
     * Returns a slot which can be used to wire the enabled status of this element to a {@link
     * react.Signal} or {@link react.Value}.
     */
    public Slot<Boolean> enabledSlot () {
        return new Slot<Boolean>() {
            public void onEmit (Boolean value) {
                setEnabled(value);
            }
        };
    }

    /**
     * Returns whether this element is visible.
     */
    public boolean isVisible () {
        return isSet(Flag.VISIBLE);
    }

    /**
     * Configures whether this element is visible. An invisible element is not rendered and
     * consumes no space in a group.
     */
    public T setVisible (boolean visible) {
        if (visible != isVisible()) {
            set(Flag.VISIBLE, visible);
            layer.setVisible(visible);
            invalidate();
        }
        return asT();
    }

    /**
     * Returns a slot which can be used to wire the visible status of this element to a {@link
     * react.Signal} or {@link react.Value}.
     */
    public Slot<Boolean> visibleSlot () {
        return new Slot<Boolean>() {
            public void onEmit (Boolean value) {
                setVisible(value);
            }
        };
    }

    /**
     * Returns true only if this element and all its parents' {#isVisible()} return true.
     */
    public boolean isShowing () {
        Elements<?> parent;
        return isVisible() && ((parent = parent()) != null) && parent.isShowing();
    }

    /**
     * Returns the layout constraint configured on this element, or null.
     */
    public Layout.Constraint constraint () {
        return _constraint;
    }

    /**
     * Configures the layout constraint on this element.
     * @return this element for call chaining.
     */
    public T setConstraint (Layout.Constraint constraint) {
        if (constraint != null) constraint.setElement(this);
        _constraint = constraint;
        invalidate();
        return asT();
    }

    /**
     * Returns true if this element is part of an interface heirarchy.
     */
    public boolean isAdded () {
        return root() != null;
    }

    /**
     * Returns the class of this element for use in computing its style. By default this is the
     * actual class, but you may wish to, for example, extend {@link Label} with some customization
     * and override this method to return {@code Label.class} so that your extension has the same
     * styles as Label applied to it.
     */
    protected Class<?> getStyleClass () {
        return getClass();
    }

    /**
     * Called when this element (or its parent element) was added to the interface hierarchy.
     */
    protected void wasAdded (Elements<?> parent) {
        _parent = parent;
        if (_hierarchyChanged != null) _hierarchyChanged.emit(Boolean.TRUE);
    }

    /**
     * Called when this element (or its parent element) was removed from the interface hierarchy.
     */
    protected void wasRemoved () {
        _parent = null;
        if (_bginst != null) {
            _bginst.destroy();
            _bginst = null;
        }
        if (_hierarchyChanged != null) _hierarchyChanged.emit(Boolean.FALSE);
    }

    /**
     * Returns true if the supplied, element-relative, coordinates are inside our bounds.
     */
    protected boolean contains (float x, float y) {
        return !(x < 0 || x > _size.width || y < 0 || y > _size.height);
    }

    /**
     * Returns whether this element is selected. This is only applicable for elements that maintain
     * a selected state, but is used when computing styles for all elements (it is assumed that an
     * element that maintains no selected state will always return false from this method).
     * Elements that do maintain a selected state should override this method and expose it as
     * public.
     */
    protected boolean isSelected () {
        return isSet(Flag.SELECTED);
    }

    /**
     * An element should call this method when it knows that it has changed in such a way that
     * requires it to recreate its visualization.
     */
    protected void invalidate () {
        // note that our preferred size and background are no longer valid
        _preferredSize = null;

        if (isSet(Flag.VALID)) {
            set(Flag.VALID, false);
            // invalidate our parent if we've got one
            if (_parent != null) {
                _parent.invalidate();
            }
        }
    }

    /**
     * Does whatever this element needs to validate itself. This may involve recomputing
     * visualizations, or laying out children, or anything else.
     */
    protected void validate () {
        if (!isSet(Flag.VALID)) {
            layout();
            set(Flag.VALID, true);
        }
    }

    /**
     * Returns the root of this element's hierarchy, or null if the element is not currently added
     * to a hierarchy.
     */
    protected Root root () {
        return (_parent == null) ? null : _parent.root();
    }

    /**
     * Returns whether the specified flag is set.
     */
    protected boolean isSet (Flag flag) {
        return (flag.mask & _flags) != 0;
    }

    /**
     * Sets or clears the specified flag.
     */
    protected void set (Flag flag, boolean on) {
        if (on) {
            _flags |= flag.mask;
        } else {
            _flags &= ~flag.mask;
        }
    }

    /**
     * Returns this element's preferred size, potentially recomputing it if needed.
     *
     * @param hintX if non-zero, an indication that the element will be constrained in the x
     * direction to the specified width.
     * @param hintY if non-zero, an indication that the element will be constrained in the y
     * direction to the specified height.
     */
    protected IDimension preferredSize (float hintX, float hintY) {
        if (_preferredSize == null) {
            _preferredSize = computeSize(hintX, hintY);
            if (_constraint != null) _constraint.adjustPreferredSize(_preferredSize, hintX, hintY);
        }
        return _preferredSize;
    }

    /**
     * Configures the location of this element, relative to its parent.
     */
    protected void setLocation (float x, float y) {
        layer.transform().setTranslation(MathUtil.ifloor(x), MathUtil.ifloor(y));
    }

    /**
     * Configures the size of this widget.
     */
    protected T setSize (float width, float height) {
        if (_size.width == width && _size.height == height) return asT(); // NOOP
        _size.setSize(width, height);
        // if we have a cached preferred size and this size differs from it, we need to clear our
        // layout data as it may contain computations specific to our preferred size
        if (_preferredSize != null && !_size.equals(_preferredSize)) clearLayoutData();
        invalidate();
        return asT();
    }

    /**
     * Resolves the value for the supplied style. See {@link Styles#resolveStyle} for the gritty
     * details.
     */
    protected <V> V resolveStyle (Style<V> style) {
        return Styles.resolveStyle(this, style);
    }

    /**
     * Recomputes this element's preferred size.
     *
     * @param hintX if non-zero, an indication that the element will be constrained in the x
     * direction to the specified width.
     * @param hintY if non-zero, an indication that the element will be constrained in the y
     * direction to the specified height.
     */
    protected Dimension computeSize (float hintX, float hintY) {
        LayoutData ldata = _ldata = createLayoutData(hintX, hintY);
        Dimension size = ldata.computeSize(hintX - ldata.bg.width(), hintY - ldata.bg.height());
        return ldata.bg.addInsets(size);
    }

    /**
     * Handles common element layout (background), then calls {@link LayoutData#layout} to do the
     * actual layout.
     */
    protected void layout () {
        if (!isVisible()) return;

        float width = _size.width, height = _size.height;
        LayoutData ldata = (_ldata != null) ? _ldata : createLayoutData(width, height);

        // prepare our background
        if (_bginst != null) _bginst.destroy();
        if (width > 0 && height > 0) {
            _bginst = ldata.bg.instantiate(_size);
            _bginst.addTo(layer);
        }

        // do our actual layout
        ldata.layout(ldata.bg.left, ldata.bg.top,
                     width - ldata.bg.width(), height - ldata.bg.height());

        // finally clear our cached layout data
        clearLayoutData();
    }

    /**
     * Creates the layout data record used by this element. This record temporarily holds resolved
     * style information between the time that an element has its preferred size computed, and the
     * time that the element is subsequently laid out. Note: {@code hintX} and {@code hintY} <em>do
     * not</em> yet have the background insets subtracted from them, because the creation of the
     * LayoutData is what resolves the background in the first place.
     */
    protected abstract LayoutData createLayoutData (float hintX, float hintY);

    /**
     * Clears out cached layout data. This can be called by methods that change the configuration
     * of the element when they know it will render pre-computed layout info invalid.
     */
    protected void clearLayoutData () {
        _ldata = null;
    }

    /**
     * Creates the layer to be used by this element. Subclasses may override to use a clipped one.
     */
    protected GroupLayer createLayer () {
        return PlayN.graphics().createGroupLayer();
    }

    protected abstract class LayoutData {
        public final Background bg = resolveStyle(Style.BACKGROUND);

        /**
         * Computes this element's preferred size, given the supplied hints. The background insets
         * will be automatically added to the returned size.
         */
        public abstract Dimension computeSize (float hintX, float hintY);

        /**
         * Rebuilds this element's visualization. Called when this element's size has changed. In
         * the case of groups, this will relayout its children, in the case of widgets, this will
         * rerender the widget.
         */
        public void layout (float left, float top, float width, float height) {
            // noop!
        }
    }

    protected int _flags = Flag.VISIBLE.mask | Flag.ENABLED.mask;
    protected Elements<?> _parent;
    protected Dimension _preferredSize;
    protected Dimension _size = new Dimension();
    protected Styles _styles = Styles.none();
    protected Layout.Constraint _constraint;
    protected Signal<Boolean> _hierarchyChanged;

    protected LayoutData _ldata;
    protected Background.Instance _bginst;

    protected static enum Flag {
        VALID(1 << 0), ENABLED(1 << 1), VISIBLE(1 << 2), SELECTED(1 << 3);

        public final int mask;

        Flag (int mask) {
            this.mask = mask;
        }
    };
}
