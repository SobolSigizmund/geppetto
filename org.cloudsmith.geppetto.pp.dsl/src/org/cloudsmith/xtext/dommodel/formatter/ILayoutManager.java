/**
 * Copyright (c) 2012 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Cloudsmith
 * 
 */
package org.cloudsmith.xtext.dommodel.formatter;

import org.cloudsmith.xtext.dommodel.IDomNode;
import org.cloudsmith.xtext.dommodel.formatter.css.DomCSS;
import org.cloudsmith.xtext.dommodel.formatter.css.StyleSet;
import org.cloudsmith.xtext.formatting.ILineSeparatorInformation;
import org.cloudsmith.xtext.textflow.ITextFlow;
import org.eclipse.xtext.formatting.IIndentationInformation;
import org.eclipse.xtext.serializer.diagnostic.ISerializationDiagnostic.Acceptor;
import org.eclipse.xtext.util.ITextRegion;

/**
 * An ILayoutManager is responsible for providing textual output to an {@link ITextFlow}.
 * 
 */
public interface ILayoutManager {

	public interface ILayoutContext {
		/**
		 * 
		 * @return the style sheet to use
		 */
		public DomCSS getCSS();

		/**
		 * 
		 * @return where any errors should be emitted
		 */
		public Acceptor getErrorAcceptor();

		/**
		 * 
		 * @return the indentation information to use
		 */
		public IIndentationInformation getIndentationInformation();

		/**
		 * 
		 * @return the line separator information to use
		 */
		public ILineSeparatorInformation getLineSeparatorInformation();

		/**
		 * 
		 * @return the text region to format (or null for "everything").
		 */
		public ITextRegion getRegionToFormat();

		/**
		 * 
		 * @return true if existing (non implied) white space should be preserved
		 */
		public boolean isWhitespacePreservation();
	}

	/**
	 * Called after all children of a composite have been processed. This call is always performed (even if format of the composite
	 * returned true).
	 * 
	 * @param styleSet
	 * @param node
	 * @param output
	 * @param context
	 */
	public void afterComposite(StyleSet styleSet, IDomNode node, ITextFlow output, ILayoutContext context);

	/**
	 * Called before format of a composite.
	 * 
	 * @param styleSet
	 * @param node
	 * @param output
	 * @param context
	 */
	public void beforeComposite(StyleSet styleSet, IDomNode node, ITextFlow output, ILayoutContext context);

	/**
	 * Formats the dom node and produces output in the flow.
	 * 
	 * @param dom
	 * @param flow
	 * @param context
	 * @return true if the layout manager processed all children of the given node
	 */
	public boolean format(IDomNode dom, ITextFlow flow, ILayoutContext context);

	/**
	 * Formats the dom node and produces output in the flow. The given style set is the styles that
	 * should be applied to the given dom node. (This is typically the result of collecting the style from
	 * the css passed in the layout context).
	 * 
	 * @param styleset
	 * @param dom
	 * @param flow
	 * @param context
	 * @return true if the layout manager processed all children of the given node
	 */
	public boolean format(StyleSet styleset, IDomNode dom, ITextFlow flow, ILayoutContext context);
}