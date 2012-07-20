/**
 * Copyright (c) 2011, 2012 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   itemis AG (http://www.itemis.eu) - initial API and implementation
 *   Cloudsmith - adaption to DomModel and Contextual formatter
 * 
 */
package org.cloudsmith.xtext.serializer.acceptor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cloudsmith.xtext.dommodel.IDomNode;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.BidiTreeIterator;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parsetree.reconstr.IHiddenTokenHelper;
import org.eclipse.xtext.parsetree.reconstr.impl.NodeIterator;
import org.eclipse.xtext.parsetree.reconstr.impl.TokenUtil;
import org.eclipse.xtext.serializer.acceptor.ISequenceAcceptor;
import org.eclipse.xtext.serializer.acceptor.ISyntacticSequenceAcceptor;
import org.eclipse.xtext.serializer.diagnostic.ISerializationDiagnostic.Acceptor;
import org.eclipse.xtext.serializer.sequencer.IHiddenTokenSequencer;
import org.eclipse.xtext.serializer.sequencer.ISyntacticSequencer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * This is an adapted version of HiddenTokenSequencer that emits implicit white space where it is allowed.
 * Implicit WS is emitted also when an INode model is not present.
 * 
 */
public class HiddenTokenSequencer implements IHiddenTokenSequencer, ISyntacticSequenceAcceptor {

	@Inject
	protected IHiddenTokenSequencerAdvisor advisor;

	@Inject
	protected IHiddenTokenHelper hiddenTokenHelper;

	@Inject
	protected TokenUtil tokenUtil;

	protected ISequenceAcceptor delegate;

	protected INode lastNode;

	protected INode rootNode;

	protected ISyntacticSequencer sequencer;

	protected List<AbstractRule> currentHidden;

	protected List<List<AbstractRule>> hiddenStack = Lists.newArrayList();

	protected List<RuleCall> stack = Lists.newArrayList();

	private List<RuleCall> debugStack = Lists.newArrayList();

	@Override
	public void acceptAssignedCrossRefDatatype(RuleCall rc, String tkn, EObject val, int index, ICompositeNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptAssignedCrossRefDatatype(rc, tkn, val, index, node);
	}

	@Override
	public void acceptAssignedCrossRefEnum(RuleCall rc, String token, EObject value, int index, ICompositeNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptAssignedCrossRefEnum(rc, token, value, index, node);
	}

	@Override
	public void acceptAssignedCrossRefKeyword(Keyword kw, String token, EObject value, int index, ILeafNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptAssignedCrossRefKeyword(kw, token, value, index, node);
	}

	@Override
	public void acceptAssignedCrossRefTerminal(RuleCall rc, String token, EObject value, int index, ILeafNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = node;
		delegate.acceptAssignedCrossRefTerminal(rc, token, value, index, node);
	}

	@Override
	public void acceptAssignedDatatype(RuleCall rc, String token, Object value, int index, ICompositeNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptAssignedDatatype(rc, token, value, index, node);
	}

	@Override
	public void acceptAssignedEnum(RuleCall enumRC, String token, Object value, int index, ICompositeNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptAssignedEnum(enumRC, token, value, index, node);
	}

	@Override
	public void acceptAssignedKeyword(Keyword keyword, String token, Object value, int index, ILeafNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = node;
		delegate.acceptAssignedKeyword(keyword, token, value, index, node);
	}

	@Override
	public void acceptAssignedTerminal(RuleCall terminalRC, String token, Object value, int index, ILeafNode node) {
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = node;
		delegate.acceptAssignedTerminal(terminalRC, token, value, index, node);
	}

	@Override
	public void acceptUnassignedAction(Action action) {
		delegate.acceptUnassignedAction(action);
	}

	@Override
	public void acceptUnassignedDatatype(RuleCall datatypeRC, String token, ICompositeNode node) {
		saveHidden(datatypeRC);
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptUnassignedDatatype(datatypeRC, token, node);
		restoreHidden();
	}

	@Override
	public void acceptUnassignedEnum(RuleCall enumRC, String token, ICompositeNode node) {
		saveHidden(enumRC);
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = getLastLeaf(node);
		delegate.acceptUnassignedEnum(enumRC, token, node);
		restoreHidden();
	}

	@Override
	public void acceptUnassignedKeyword(Keyword keyword, String token, ILeafNode node) {
		saveHidden(keyword);
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = node;
		delegate.acceptUnassignedKeyword(keyword, token, node);
		restoreHidden();
	}

	@Override
	public void acceptUnassignedTerminal(RuleCall terminalRC, String token, ILeafNode node) {
		saveHidden(terminalRC);
		emitHiddenTokens(getHiddenNodesBetween(lastNode, node));
		if(node != null)
			lastNode = node;
		delegate.acceptUnassignedTerminal(terminalRC, token, node);
		restoreHidden();
	}

	private ParserRule closestContainingParserRule(EObject element) {
		while(element != null && element instanceof ParserRule == false)
			element = element.eContainer();
		return (ParserRule) element;
	}

	private String debugStringForHidden(List<AbstractRule> rules) {
		StringBuilder builder = new StringBuilder();
		Iterator<AbstractRule> itor = rules.iterator();
		while(itor.hasNext()) {
			AbstractRule r = itor.next();
			builder.append(r.getName());
			if(itor.hasNext())
				builder.append(", ");
		}
		return builder.toString();
	}

	protected void emitHiddenTokens(List<INode> hiddens /* Set<INode> comments, */) {
		if(hiddens == null)
			return;
		boolean lastNonWhitespace = true;
		AbstractRule ws = hiddenTokenHelper.getWhitespaceRuleFor(null, "");
		for(INode node : hiddens)
			if(tokenUtil.isCommentNode(node)) {
				if(lastNonWhitespace)
					delegate.acceptWhitespace(hiddenTokenHelper.getWhitespaceRuleFor(null, ""), "", null);
				lastNonWhitespace = true;
				// comments.remove(node);
				delegate.acceptComment((AbstractRule) node.getGrammarElement(), node.getText(), (ILeafNode) node);
			}
			else {
				delegate.acceptWhitespace((AbstractRule) node.getGrammarElement(), node.getText(), (ILeafNode) node);
				lastNonWhitespace = false;
			}
		// NOTE: The original implementation has a FIXME note here that whitespace should be determined
		// correctly. (Well, it did not work until a check was added if the ws was hidden or not).

		// Longer explanation:
		// When there is no WS between two elements and no node model the contextual serializer/formatter
		// performs serialization by inserting an IMPLICIT WS.
		// When the node model is created, empty ws nodes are skipped, and thus have to be created (this happens
		// here). The created whitespace node should *NOT* be marked as implicit, since it by virtue of having been
		// parsed is now the source text and should not be subject to formatting (like the IMPLICIT WS always is)
		// when in "preserve whitespace" mode.
		// Finally, what the fix below does is to also check if a missing WS should be emitted based on
		// if whitespace is visible or not.
		// THIS IS PROBABLY STILL NOT ENOUGH, as it may overrule the attempt to treat visible WS as eligible for formatting
		// see isImpliedWhitespace and where it is called.

		if(lastNonWhitespace && currentHidden.contains(ws)) {
			delegate.acceptWhitespace(ws, "", null);
		}
	}

	public boolean enterAssignedAction(Action action, EObject semanticChild, ICompositeNode node) {
		return delegate.enterAssignedAction(action, semanticChild, node);
	}

	public boolean enterAssignedParserRuleCall(RuleCall rc, EObject semanticChild, ICompositeNode node) {
		push(rc);
		return delegate.enterAssignedParserRuleCall(rc, semanticChild, node);
	}

	public void enterUnassignedParserRuleCall(RuleCall rc) {
		push(rc);
		delegate.enterUnassignedParserRuleCall(rc);
	}

	public void finish() {
		if(rootNode != null && rootNode == rootNode.getRootNode()) {
			List<INode> hidden = getRemainingHiddenNodesInContainer(lastNode, rootNode);
			if(!hidden.isEmpty()) {
				emitHiddenTokens(hidden);
				lastNode = rootNode;
			}
		}
		delegate.finish();
		// System.err.println("FINISH : " + stack.size());
		if(stack.size() > 0)
			pop();
	}

	protected Set<INode> getCommentsForEObject(EObject semanticObject, INode node) {
		if(node == null)
			return Collections.emptySet();
		Set<INode> result = Sets.newHashSet();
		BidiTreeIterator<INode> ti = node.getAsTreeIterable().iterator();
		while(ti.hasNext()) {
			INode next = ti.next();
			if(next.getSemanticElement() != null && next.getSemanticElement() != semanticObject) {
				ti.prune();
				continue;
			}
			if(tokenUtil.isCommentNode(next))
				result.add(next);
		}
		return result;
	}

	protected List<INode> getHiddenNodesBetween(INode from, INode to) {
		List<INode> result = getHiddenNodesBetween2(from, to);
		if(result == null) {
			AbstractRule ws = hiddenTokenHelper.getWhitespaceRuleFor(null, "");
			// only emit hidden whitespace, or visible whitespace where this is overridden using
			// isImpliedWhitespace
			boolean implied = currentHidden != null && currentHidden.contains(ws);
			int sz = stack.size();
			implied = isImpliedWhitespace(implied, sz == 0
					? null
					: stack.get(sz - 1), from, to);
			if(implied) {
				delegate.acceptWhitespace(ws, IDomNode.IMPLIED_EMPTY_WHITESPACE, null);
			}
		}
		return result;
	}

	protected List<INode> getHiddenNodesBetween2(INode from, INode to) {
		if(from == null || to == null)
			return null;
		List<INode> out = Lists.newArrayList();
		NodeIterator ni = new NodeIterator(from);
		while(ni.hasNext()) {
			INode next = ni.next();
			if(tokenUtil.isWhitespaceOrCommentNode(next)) {
				out.add(next);
			}
			else if(next.equals(to)) {
				if(next instanceof ICompositeNode &&
						(GrammarUtil.isDatatypeRuleCall(next.getGrammarElement()) ||
								GrammarUtil.isEnumRuleCall(next.getGrammarElement()) || next.getGrammarElement() instanceof CrossReference))
					while(ni.hasNext()) {
						INode next2 = ni.next();
						if(tokenUtil.isWhitespaceOrCommentNode(next2)) {
							out.add(next2);
						}
						else if(next2 instanceof ILeafNode)
							return out;
					}
				else
					return out;
			}
			else if(tokenUtil.isToken(next))
				return null;
		}
		return out;
	}

	protected INode getLastLeaf(INode node) {
		while(node instanceof ICompositeNode)
			node = ((ICompositeNode) node).getLastChild();
		return node;
	}

	protected List<INode> getRemainingHiddenNodesInContainer(INode from, INode root) {
		if(from == null || root == null)
			return Collections.emptyList();
		List<INode> out = Lists.newArrayList();
		NodeIterator ni = new NodeIterator(from);
		while(ni.hasNext()) {
			INode next = ni.next();
			if(next.getTotalOffset() > root.getTotalEndOffset())
				return out;
			else if(tokenUtil.isWhitespaceOrCommentNode(next)) {
				out.add(next);
			}
			else if(tokenUtil.isToken(next))
				return Collections.emptyList();
		}
		return out;
	}

	public void init(EObject context, EObject semanticObject, ISequenceAcceptor sequenceAcceptor, Acceptor errorAcceptor) {
		this.delegate = sequenceAcceptor;
		this.lastNode = NodeModelUtils.findActualNodeFor(semanticObject);
		this.rootNode = lastNode;
		initCurrentHidden(context);
	}

	protected void initCurrentHidden(EObject context) {
		// when called for a specific parser rule, its hidden() spec (if any) is made current
		// otherwise the hidden() spec of the grammar is made current.
		// (There is no real way to calculate the calling chain to a particular starting parser rule)
		//
		if(context instanceof ParserRule) {
			ParserRule pr = (ParserRule) context;
			if(pr.isDefinesHiddenTokens())
				currentHidden = pr.getHiddenTokens();
			else {
				Grammar grammar = GrammarUtil.getGrammar(context);
				currentHidden = grammar.getHiddenTokens();
			}
		}

	}

	/**
	 * This method should be overridden in an implementation where certain visible whitespace
	 * rules should be subject to formatting.
	 * 
	 * @param defaultResult
	 *            - the result to return if the already made decision is ok
	 * @param rc
	 *            - the {@link RuleCall} (or {@link Grammar}) in the call chain that determined what is hidden
	 * @param from
	 *            - the node left of where the ws appears, or null if there is no node model
	 * @param to
	 *            - the node right if where the ws appears, or null if there is no node model
	 * @return true if this WS should be eligible for formatting
	 */
	protected boolean isImpliedWhitespace(boolean defaultResult, EObject rc, INode from, INode to) {
		return defaultResult;
	}

	public void leaveAssignedAction(Action action, EObject semanticChild) {
		delegate.leaveAssignedAction(action, semanticChild);
	}

	public void leaveAssignedParserRuleCall(RuleCall rc, EObject semanticChild) {
		delegate.leaveAssignedParserRuleCall(rc, semanticChild);
		pop();
	}

	public void leaveUnssignedParserRuleCall(RuleCall rc) {
		delegate.leaveUnssignedParserRuleCall(rc);
		pop();
	}

	protected void pop() {
		int sz = stack.size();
		if(sz == 0) {
			// System.err.println("POP of empty stack");
			return;
		}
		RuleCall top = stack.remove(sz - 1);

		// if the rule call on top defines hidden, it pushed on the hidden stack, and state needs to
		// be restored
		if(top.getRule() instanceof ParserRule == false) {
			// System.err.println("POP of non parser rule: " + top.getRule().getName());
			return;
		}
		// System.err.print("POP of: " + top.getRule().getName());
		if(((ParserRule) top.getRule()).isDefinesHiddenTokens()) {
			// RuleCall debugHidden = debugStack.remove(debugStack.size() - 1);
			// if(debugHidden != top) {
			// System.err.print(" UNBALANCED POP (pops hidden from different rule)");
			// }
			// System.err.print(" BEFORE POP [" + debugStringForHidden(currentHidden) + "]");
			currentHidden = hiddenStack.remove(hiddenStack.size() - 1);
			// System.err.print(" AFTER POP[" + debugStringForHidden(currentHidden) + "]");
		}
		// System.err.println(" size = " + (sz - 1));
	}

	protected void push(RuleCall rc) {
		// System.err.print("PUSH call to rule: " + rc.getRule().getName());
		stack.add(rc);

		if(rc.getRule() instanceof ParserRule == false) {
			System.err.println("");
			return;
		}
		ParserRule pr = (ParserRule) rc.getRule();
		if(!pr.isDefinesHiddenTokens()) {
			// System.err.println("");
			return;
		}
		// System.err.print(" BEFORE PUSH [" + debugStringForHidden(currentHidden) + "]");
		// System.err.println(" AFTER PUSH [" + debugStringForHidden(pr.getHiddenTokens()) + "]");
		// if rule defines hidden, remember previous hidden, and set the new as current
		hiddenStack.add(currentHidden);
		// debugStack.add(rc);
		currentHidden = pr.getHiddenTokens();
	}

	private void restoreHidden() {
		if(advisor.shouldSaveRestoreState())
			currentHidden = hiddenStack.remove(hiddenStack.size() - 1);
	}

	/**
	 * Save hidden while performing an operation. Must be paired with a restoreHidden() when done.
	 * 
	 * @param eobj
	 */
	private void saveHidden(EObject eobj) {
		if(!advisor.shouldSaveRestoreState())
			return;

		hiddenStack.add(currentHidden);
		ParserRule r = closestContainingParserRule(eobj);
		if(r != null && r.isDefinesHiddenTokens())
			currentHidden = r.getHiddenTokens();
	}
}