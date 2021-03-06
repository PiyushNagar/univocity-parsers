/*******************************************************************************
 * Copyright 2014 uniVocity Software Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.univocity.parsers.common.processor;

import com.univocity.parsers.common.*;
import com.univocity.parsers.common.processor.core.*;

/**
 *
 * A convenience {@link BeanProcessor} implementation for storing all java objects generated form the parsed input into a list.
 * A typical use case of this class will be:
 *
 * <hr><blockquote><pre>{@code
 *
 * parserSettings.setRowProcessor(new BeanListProcessor(MyObject.class));
 * parser.parse(reader); // will invoke the {@link BeanListProcessor#beanProcessed(Object, ParsingContext)} method for each generated object.
 *
 * List&lt;T&gt; beans = rowProcessor.getBeans();
 * }</pre></blockquote><hr>
 *
 * @param <T> the annotated class type.
 *
 * @see BeanProcessor
 * @see RowProcessor
 * @see AbstractParser
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:parsers@univocity.com">parsers@univocity.com</a>
 *
 */
public class BeanListProcessor<T> extends TypedBeanListProcessor<T, ParsingContext> implements RowProcessor {

	/**
	 * Creates a processor that stores java beans of a given type into a list
	 * @param beanType the class with its attributes mapped to fields of records parsed by an {@link AbstractParser} or written by an {@link AbstractWriter}.
	 */
	public BeanListProcessor(Class<T> beanType) {
		super(beanType);
	}


}
