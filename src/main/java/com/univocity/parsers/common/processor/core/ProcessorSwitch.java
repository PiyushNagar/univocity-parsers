/*
 * Copyright (c) 2015 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 *
 */
package com.univocity.parsers.common.processor.core;

import com.univocity.parsers.common.*;
import com.univocity.parsers.common.processor.*;

import java.util.*;
import java.util.Map.*;

/**
 * A special {@link RowProcessor} implementation that combines and allows switching among different
 * RowProcessors. Each RowProcessor will have its own {@link ParsingContext}. Concrete implementations of this class
 * are expected to implement the {@link #switchRowProcessor(String[], T)} method and analyze the input row
 * to determine whether or not the current {@link RowProcessor} implementation must be changed to handle a special
 * circumstance (determined by the concrete implementation) such as a different row format.
 *
 * When the row processor is switched, the {@link #rowProcessorSwitched(RowProcessor, RowProcessor)} will be called, and
 * must be overridden, to notify the change to the user.
 */
public abstract class ProcessorSwitch<T extends Context> implements Processor<T>, ColumnOrderDependent {

	private Map<RowProcessor, ParsingContextWrapper> rowProcessors;
	private RowProcessor selectedRowProcessor;
	private ParsingContextWrapper contextForProcessor;

	/**
	 * Analyzes the input to determine whether or not the row processor implementation must be changed
	 *
	 * @param row     a row parsed from the input
	 * @param context the current parsing context (not associated with the current row processor used by this class)
	 *
	 * @return the row processor implementation to use. If it is not the same as the one used by the previous row,
	 * the returned row processor will be used, and the {@link #rowProcessorSwitched(RowProcessor, RowProcessor)} method
	 * will be called.
	 */
	protected abstract RowProcessor switchRowProcessor(String[] row, T context);

	/**
	 * Returns the headers in use by the current row processor implementation, which can vary among row processors.
	 * If {@code null}, the headers parsed by the input, or defined in {@link CommonParserSettings#getHeaders()} will be returned.
	 *
	 * @return the current sequence of headers to use.
	 */
	public String[] getHeaders() {
		return null;
	}

	/**
	 * Returns the indexes in use by the current row processor implementation, which can vary among row processors.
	 * If {@code null} all columns of a given record will be considered.
	 *
	 * @return the current sequence of indexes to use.
	 */
	public int[] getIndexes() {
		return null;
	}

	/**
	 * Notifies a change of row processor implementation. Users are expected to override this method to receive the notification.
	 *
	 * @param from the row processor previously in use
	 * @param to   the new row processor to use to continue processing the input.
	 */
	public void rowProcessorSwitched(RowProcessor from, RowProcessor to) {
	}

	@Override
	public void processStarted(T context) {
		rowProcessors = new HashMap<RowProcessor, ParsingContextWrapper>();
		selectedRowProcessor = NoopRowProcessor.instance;
	}

	@Override
	public final void rowProcessed(String[] row, final T context) {
		RowProcessor processor = switchRowProcessor(row, context);
		if (processor == null) {
			processor = NoopRowProcessor.instance;
		}
		if (processor != selectedRowProcessor) {
			contextForProcessor = rowProcessors.get(processor);

			if (contextForProcessor == null) {
				contextForProcessor = new ParsingContextWrapper(context) {

					private final String[] fieldNames = getHeaders();
					private final int[] indexes = getIndexes();

					@Override
					public String[] headers() {
						return fieldNames == null || fieldNames.length == 0 ? context.headers() : fieldNames;
					}

					@Override
					public int[] extractedFieldIndexes() {
						return indexes == null || indexes.length == 0 ? context.extractedFieldIndexes() : indexes;
					}
				};

				processor.processStarted(contextForProcessor);
				rowProcessors.put(processor, contextForProcessor);
			}

			if (selectedRowProcessor != NoopRowProcessor.instance) {
				rowProcessorSwitched(selectedRowProcessor, processor);
			}
			selectedRowProcessor = processor;
			if(getIndexes() != null){
				int[] indexes = getIndexes();
				String[] tmp = new String[indexes.length];
				for(int i = 0; i < indexes.length; i++){
					int index = indexes[i];
					if(index < row.length){
						tmp[i] = row[index];
					}
				}
				row = tmp;
			}
			selectedRowProcessor.rowProcessed(row, contextForProcessor);
		} else {
			selectedRowProcessor.rowProcessed(row, contextForProcessor);
		}
	}

	@Override
	public void processEnded(T context) {
		rowProcessorSwitched(selectedRowProcessor, null);
		selectedRowProcessor = NoopRowProcessor.instance;
		for (Entry<RowProcessor, ParsingContextWrapper> e : rowProcessors.entrySet()) {
			e.getKey().processEnded(e.getValue());
		}
	}

	public boolean preventColumnReordering() {
		return true;
	}
}