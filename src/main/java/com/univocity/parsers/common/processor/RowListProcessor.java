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
 * A convenience {@link RowProcessor} implementation for storing all rows parsed into a list.
 * A typical use case of this class will be:
 *
 * <hr><blockquote><pre>
 *
 * parserSettings.setRowProcessor(new RowListProcessor());
 * parser.parse(reader); // will invoke the {@link RowListProcessor#rowProcessed(String[], ParsingContext)} method for each parsed record.
 *
 * String[] headers = rowProcessor.getHeaders();
 * List&lt;String[]&gt; rows = rowProcessor.getRows();
 *
 * </pre></blockquote><hr>
 *
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:parsers@univocity.com">parsers@univocity.com</a>
 *
 */
public class RowListProcessor extends ListProcessor<ParsingContext> implements RowProcessor{

}
