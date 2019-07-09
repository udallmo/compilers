/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Spring 2019 (1195)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.common.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.parboiled.common.ImmutableList;

import ece351.util.Examinable;
import ece351.util.Examiner;

/**
 * An expression with multiple children. Must be commutative.
 */
public abstract class NaryExpr extends Expr {

	public final ImmutableList<Expr> children;

	public NaryExpr(final Expr... exprs) {
		Arrays.sort(exprs);
		ImmutableList<Expr> c = ImmutableList.of();
		for (final Expr e : exprs) {
			c = c.append(e);
		}
    	this.children = c;
	}
	
	public NaryExpr(final List<Expr> children) {
		final ArrayList<Expr> a = new ArrayList<Expr>(children);
		Collections.sort(a);
		this.children = ImmutableList.copyOf(a);
	}

	/**
	 * Each subclass must implement this factory method to return
	 * a new object of its own type. 
	 */
	public abstract NaryExpr newNaryExpr(final List<Expr> children);

	/**
	 * Construct a new NaryExpr (of the appropriate subtype) with 
	 * one extra child.
	 * @param e the child to append
	 * @return a new NaryExpr
	 */
	public NaryExpr append(final Expr e) {
		return newNaryExpr(children.append(e));
	}

	/**
	 * Construct a new NaryExpr (of the appropriate subtype) with 
	 * the extra children.
	 * @param list the children to append
	 * @return a new NaryExpr
	 */
	public NaryExpr appendAll(final List<Expr> list) {
		final List<Expr> a = new ArrayList<Expr>(children.size() + list.size());
		a.addAll(children);
		a.addAll(list);
		return newNaryExpr(a);
	}

	/**
	 * Check the representation invariants.
	 */
	public boolean repOk() {
		// programming sanity
		assert this.children != null;
		// should not have a single child: indicates a bug in simplification
		assert this.children.size() > 1 : "should have more than one child, probably a bug in simplification";
		// check that children is sorted
		int i = 0;
		for (int j = 1; j < this.children.size(); i++, j++) {
			final Expr x = this.children.get(i);
			assert x != null : "null children not allowed in NaryExpr";
			final Expr y = this.children.get(j);
			assert y != null : "null children not allowed in NaryExpr";
			assert x.compareTo(y) <= 0 : "NaryExpr.children must be sorted";
		}
        // Note: children might contain duplicates --- not checking for that
        // ... maybe should check for duplicate children ...
		// no problems found
		return true;
	}

	/**
	 * The name of the operator represented by the subclass.
	 * To be implemented by each subclass.
	 */
	public abstract String operator();
	
	/**
	 * The complementary operation: NaryAnd returns NaryOr, and vice versa.
	 */
	abstract protected Class<? extends NaryExpr> getThatClass();
	

	/**
     * e op x = e for absorbing element e and operator op.
     * @return
     */
	public abstract ConstantExpr getAbsorbingElement();

    /**
     * e op x = x for identity element e and operator op.
     * @return
     */
	public abstract ConstantExpr getIdentityElement();


	@Override 
    public final String toString() {
    	final StringBuilder b = new StringBuilder();
    	b.append("(");
    	int count = 0;
    	for (final Expr c : children) {
    		b.append(c);
    		if (++count  < children.size()) {
    			b.append(" ");
    			b.append(operator());
    			b.append(" ");
    		}
    		
    	}
    	b.append(")");
    	return b.toString();
    }


	@Override
	public final int hashCode() {
		return 17 + children.hashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		if (!(obj instanceof Examinable)) return false;
		return examine(Examiner.Equals, (Examinable)obj);
	}
	
	@Override
	public final boolean isomorphic(final Examinable obj) {
		return examine(Examiner.Isomorphic, obj);
	}
	
	private boolean examine(final Examiner e, final Examinable obj) {
		// basics
		if (obj == null) return false;
		if (!this.getClass().equals(obj.getClass())) return false;
		final NaryExpr that = (NaryExpr) obj;
		
		// if the number of children are different, consider them not equivalent
		// since the n-ary expressions have the same number of children and they are sorted, just iterate and check
		// supposed to be sorted, but might not be (because repOk might not pass)
		// if they are not the same elements in the same order return false
		// no significant differences found, return true
		if (this.children.size() != that.children.size()) {
			return false;
		}
		
		for (int i = 0; i < this.children.size(); i++) {
			if (!e.examine(this.children.get(i), that.children.get(i))) {
				return false;
			}
		}
		
		return true;
//throw new ece351.util.Todo351Exception();
	}

	
	@Override
	protected final Expr simplifyOnce() {
		assert repOk();
		final Expr result = 
				simplifyChildren().
				mergeGrandchildren().
				foldIdentityElements().
				foldAbsorbingElements().
				foldComplements().
				removeDuplicates().
				simpleAbsorption().
				subsetAbsorption().
				singletonify();
		assert result.repOk();
		return result;
	}
	
	/**
	 * Call simplify() on each of the children.
	 */
	private NaryExpr simplifyChildren() {
		// note: we do not assert repOk() here because the rep might not be ok
		// the result might contain duplicate children, and the children
		// might be out of order
		List<Expr> list = new ArrayList<Expr>();
		
		for (Expr child : children) {
			list.add(child.simplify());
		}
		
		return newNaryExpr(list);
//		return this; // TODO: replace this stub
	}

	
	private NaryExpr mergeGrandchildren() {
		
		// extract children to merge using filter (because they are the same type as us)
			// if no children to merge, then return this (i.e., no change)
			// use filter to get the other children, which will be kept in the result unchanged
			// merge in the grandchildren
			// assert result.repOk():  this operation should always leave the AST in a legal state
		
		NaryExpr match = this.filter(this.getClass(), true);
		
		if (match.children.size() < 1) { 
			return this; 
		}

		NaryExpr newChildren = this.filter(this.getClass(), false);
		ArrayList<Expr> list = new ArrayList<Expr>();

		for (Expr child : match.children) {
			list.addAll(((NaryExpr)child).children);
		}
		
		newChildren = newChildren.appendAll(list);
		
//		assert newChildren.repOk();
		return newChildren;
//		return this; // TODO: replace this stub
	}


    private NaryExpr foldIdentityElements() {
    	// if we have only one child stop now and return self
    	// we have multiple children, remove the identity elements
    		// all children were identity elements, so now our working list is empty
    		// return a new list with a single identity element
    		// normal return
    	if (this.children.size() == 1) {
			return this;
		}

		NaryExpr result = this.filter(this.getIdentityElement(), Examiner.Equals, false);

		if (result.children.size() < 1) {
			result = result.append(getIdentityElement());
		}

		// normal return
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
		return result; // TODO: replace this stub
    }

    private NaryExpr foldAbsorbingElements() {
		// absorbing element: 0.x=0 and 1+x=1
			// absorbing element is present: return it
			// not so fast! what is the return type of this method? why does it have to be that way?
			// no absorbing element present, do nothing
    	
    	if(this.contains(this.getAbsorbingElement(), Examiner.Equals)) {
    		return newNaryExpr(ImmutableList.of((Expr)this.getAbsorbingElement()));
    	}

		return this; // TODO: replace this stub
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
	}

	private NaryExpr foldComplements() {
		// collapse complements
		// !x . x . ... = 0 and !x + x + ... = 1
		// x op !x = absorbing element
		// find all negations
		NaryExpr negations = this.filter(NotExpr.class, true);

		for (Expr notExpr : negations.children) {
			if (this.contains(((NotExpr)notExpr).expr, Examiner.Equals)) {
				return newNaryExpr(ImmutableList.of((Expr)this.getAbsorbingElement()));
			}
		}

		// no complements to fold
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
		return this; // TODO: replace this stub
	}

	private NaryExpr removeDuplicates() {
		ArrayList<Expr> list = new ArrayList<Expr>();
		
		for(int i = 0; i < this.children.size(); i++) {
			if(!list.contains(this.children.get(i))) {
				list.add(this.children.get(i));
			}
		}
		
		// remove duplicate children: x.x=x and x+x=x
		// since children are sorted this is fairly easy
			// no changes
			// removed some duplicates
		if (list.size() != this.children.size()) {
			return newNaryExpr(list);
		}
		
		return this; // TODO: replace this stub
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
	}

	private NaryExpr simpleAbsorption() {
		// (x.y) + x ... = x ...
    	if(this.getClass().equals(NaryOrExpr.class))
    	{
    		for(Expr expr: this.children)
    		{
    			if(expr.getClass().equals(NaryAndExpr.class))
    			{
    				NaryExpr result = this.filter(expr, Examiner.Equivalent, false);
    				for(Expr e: ((NaryAndExpr) expr).children) {
    					if(result.children.contains(e))
    					{
    						return result;
    					}
    				}
    			}
    		}
    	}
    	else if(this.getClass().equals(NaryAndExpr.class))
    	{
    		for(Expr expr: this.children)
    		{
    			if(expr.getClass().equals(NaryOrExpr.class))
    			{
    				NaryExpr result = this.filter(expr, Examiner.Equivalent, false);
    				for(Expr e: ((NaryOrExpr) expr).children) {
    					if(result.children.contains(e))
    					{
    						return result;
    					}
    				}
    			}
    		}
    	}
    	return this;
//		return this; // TODO: replace this stub
    	// do not assert repOk(): this operation might leave the AST in an illegal state (with only one child)
	}

	private NaryExpr subsetAbsorption() {
		// check if there are any conjunctions that are supersets of others
		// e.g., ( a . b . c ) + ( a . b ) = a . b
		
		NaryExpr result = this;
    	NaryExpr children = this;
    	
    	if(this.getClass().equals(NaryOrExpr.class))
    	{
    		result = this.filter(NaryAndExpr.class, true);
    		
    		for(int i = 0; i < result.children.size(); i++)
    		{
    			for(int j = i + 1; j < result.children.size(); j++) {
    				if(((NaryExpr)this.children.get(i)).children.containsAll(((NaryExpr)this.children.get(j)).children))
    				{
    					children = this.filter(result.children.get(i), Examiner.Equivalent, false);
    				}
    			}
    		}
    		return children;
    	}
    	else if(this.getClass().equals(NaryAndExpr.class))
    	{
    		result = this.filter(NaryOrExpr.class, true);
    		
    		for(int i = 0; i < result.children.size(); i++)
    		{
    			for(int j = i + 1; j < result.children.size(); j++) {
       				if(((NaryExpr)this.children.get(i)).children.containsAll(((NaryExpr)this.children.get(j)).children))
    				{
    					children = this.filter(result.children.get(i), Examiner.Equivalent, false);
    				}
    			}
    		}
    		return children;
    	}
		return this;
		
//		return this; // TODO: replace this stub
    	// do not assert repOk(): this operation might leave the AST in an illegal state (with only one child)
	}

	/**
	 * If there is only one child, return it (the containing NaryExpr is unnecessary).
	 */
	private Expr singletonify() {
		// if we have only one child, return it
		// having only one child is an illegal state for an NaryExpr
			// multiple children; nothing to do; return self
		if (this.children.size() ==1 ) {
			return this.children.get(0);
		}
		
		return this; // TODO: replace this stub
	}

	/**
	 * Return a new NaryExpr with only the children of a certain type, 
	 * or excluding children of a certain type.
	 * @param filter
	 * @param shouldMatchFilter
	 * @return
	 */
	public final NaryExpr filter(final Class<? extends Expr> filter, final boolean shouldMatchFilter) {
		ImmutableList<Expr> l = ImmutableList.of();
		for (final Expr child : children) {
			if (child.getClass().equals(filter)) {
				if (shouldMatchFilter) {
					l = l.append(child);
				}
			} else {
				if (!shouldMatchFilter) {
					l = l.append(child);
				}
			}
		}
		return newNaryExpr(l);
	}

	public final NaryExpr filter(final Expr filter, final Examiner examiner, final boolean shouldMatchFilter) {
		ImmutableList<Expr> l = ImmutableList.of();
		for (final Expr child : children) {
			if (examiner.examine(child, filter)) {
				if (shouldMatchFilter) {
					l = l.append(child);
				}
			} else {
				if (!shouldMatchFilter) {
					l = l.append(child);
				}
			}
		}
		return newNaryExpr(l);
	}

	public final NaryExpr removeAll(final List<Expr> toRemove, final Examiner examiner) {
		NaryExpr result = this;
		for (final Expr e : toRemove) {
			result = result.filter(e, examiner, false);
		}
		return result;
	}

	public final boolean contains(final Expr expr, final Examiner examiner) {
		for (final Expr child : children) {
			if (examiner.examine(child, expr)) {
				return true;
			}
		}
		return false;
	}

}
