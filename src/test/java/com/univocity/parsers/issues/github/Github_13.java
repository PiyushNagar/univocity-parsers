/*******************************************************************************
 * Copyright 2015 uniVocity Software Pty Ltd
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
package com.univocity.parsers.issues.github;

import static org.testng.Assert.*;

import java.io.*;
import java.math.*;
import java.util.*;

import org.testng.annotations.*;

import com.univocity.parsers.annotations.*;
import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;
import com.univocity.parsers.fixed.*;

/**
 *
 * From: https://github.com/uniVocity/univocity-parsers/issues/13
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:parsers@univocity.com">parsers@univocity.com</a>
 *
 */
public class Github_13 {

	enum ClientType {
		PERSONAL(2),
		BUSINESS(1);

		int typeCode;

		ClientType(int typeCode) {
			this.typeCode = typeCode;
		}
	}

	static final String CSV_INPUT = ""
			+ "Client,1,Foo\n"
			+ "Account,23234,HSBC,123433-000,HSBCAUS\n"
			+ "Account,11234,HSBC,222343-130,HSBCCAD\n"
			+ "Client,2,BAR\n"
			+ "Account,1234,CITI,213343-130,CITICAD\n";

	static final String FIXED_INPUT = ""
			+ "N#123123 1888858    58888548\n"
			+ "111222       3000FOO                               10\n"
			+ "333444       2000BAR                               60\n"
			+ "N#123124 1888844    58888544\n"
			+ "311222       3500FOO                               30\n";

	@Test
	public void processMultiRowFormatCsv() {
		final ObjectRowListProcessor clientProcessor = new ObjectRowListProcessor();
		clientProcessor.convertIndexes(Conversions.toEnum(ClientType.class, "typeCode", EnumSelector.CUSTOM_FIELD)).set(1);

		final ObjectRowListProcessor accountProcessor = new ObjectRowListProcessor();
		accountProcessor.convertFields(Conversions.toBigDecimal()).set("balance");

		InputValueSwitch valueSwitch = new InputValueSwitch(0) {
			@Override
			public void rowProcessorSwitched(RowProcessor from, RowProcessor to) {
				if (from == accountProcessor) {
					clientProcessor.getRows().addAll(accountProcessor.getRows());
					accountProcessor.getRows().clear();
				}
			}
		};
		valueSwitch.addSwitchForValue("Client", clientProcessor);
		valueSwitch.addSwitchForValue("Account", accountProcessor, "type", "balance", "bank", "account", "swift");

		CsvParserSettings settings = new CsvParserSettings();
		settings.setRowProcessor(valueSwitch);

		CsvParser parser = new CsvParser(settings);
		parser.parse(new StringReader(CSV_INPUT));

		List<Object[]> rows = clientProcessor.getRows();
		assertEquals(rows.size(), 5);
		assertEquals(rows.get(0)[1], ClientType.BUSINESS);
		assertEquals(rows.get(1)[1], new BigDecimal("23234"));
		assertEquals(rows.get(2)[1], new BigDecimal("11234"));
		assertEquals(rows.get(3)[1], ClientType.PERSONAL);
		assertEquals(rows.get(4)[1], new BigDecimal("1234"));
	}

	@Test
	public void writeMultiRowFormatCsv() {
		final ObjectRowWriterProcessor clientProcessor = new ObjectRowWriterProcessor();
		clientProcessor.convertIndexes(Conversions.toEnum(ClientType.class, "typeCode", EnumSelector.CUSTOM_FIELD)).set(1);

		final ObjectRowWriterProcessor accountProcessor = new ObjectRowWriterProcessor();
		accountProcessor.convertFields(Conversions.toBigDecimal()).set("balance");

		OutputValueSwitch writerSwitch = new OutputValueSwitch();
		writerSwitch.addSwitchForValue("Account", accountProcessor, "type", "balance", "bank", "account", "swift");
		writerSwitch.addSwitchForValue("Client", clientProcessor);

		CsvWriterSettings settings = new CsvWriterSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setHeaderWritingEnabled(false);
		settings.setRowWriterProcessor(writerSwitch);

		StringWriter out = new StringWriter();
		CsvWriter writer = new CsvWriter(out, settings);

		List<Object[]> inputRows = new ArrayList<Object[]>();
		inputRows.add(new Object[] { "Client", ClientType.BUSINESS, "Foo" });
		inputRows.add(new Object[] { "Account", new BigDecimal(23234), "HSBC", "123433-000", "HSBCAUS" });
		inputRows.add(new Object[] { "Account", new BigDecimal(11234), "HSBC", "222343-130", "HSBCCAD" });
		inputRows.add(new Object[] { "Client", ClientType.PERSONAL, "BAR" });
		inputRows.add(new Object[] { "Account", new BigDecimal(1234), "CITI", "213343-130", "CITICAD" });

		writer.processRecordsAndClose(inputRows);

		assertEquals(out.toString(), CSV_INPUT);
	}

	@Test
	public void processMultiRowFormatFixedWidth() {

		FixedWidthFieldLengths itemLengths = new FixedWidthFieldLengths(13, 4, 34, 2);
		FixedWidthParserSettings settings = new FixedWidthParserSettings(itemLengths);
		settings.addFormatForLookahead("N#", new FixedWidthFieldLengths(9, 11, 8)); //receipt lengths

		FixedWidthParser parser = new FixedWidthParser(settings);

		List<String[]> rows = parser.parseAll(new StringReader(FIXED_INPUT));
		assertEquals(rows.size(), 5);
		assertEquals(rows.get(0), new String[] { "N#123123", "1888858", "58888548" });
		assertEquals(rows.get(1), new String[] { "111222", "3000", "FOO", "10" });
		assertEquals(rows.get(2), new String[] { "333444", "2000", "BAR", "60" });
		assertEquals(rows.get(3), new String[] { "N#123124", "1888844", "58888544" });
		assertEquals(rows.get(4), new String[] { "311222", "3500", "FOO", "30" });
	}

	@Test
	public void processLookbehindMultiRowFormatFixedWidth() {
		FixedWidthFieldLengths itemLengths = new FixedWidthFieldLengths(9, 11, 8);
		FixedWidthParserSettings settings = new FixedWidthParserSettings(itemLengths);
		settings.getFormat().setLineSeparator("\n");
		settings.addFormatForLookbehind("N#", new FixedWidthFieldLengths(13, 4, 34, 2));

		FixedWidthParser parser = new FixedWidthParser(settings);

		List<String[]> rows = parser.parseAll(new StringReader(FIXED_INPUT));
		assertEquals(rows.size(), 5);
		assertEquals(rows.get(0), new String[] { "N#123123", "1888858", "58888548" });
		assertEquals(rows.get(1), new String[] { "111222", "3000", "FOO", "10" });
		assertEquals(rows.get(2), new String[] { "333444", "2000", "BAR", "60" });
		assertEquals(rows.get(3), new String[] { "N#123124", "1888844", "58888544" });
		assertEquals(rows.get(4), new String[] { "311222", "3500", "FOO", "30" });
	}

	@Test
	public void processLookbehindAndAhead() {
		FixedWidthParserSettings settings = new FixedWidthParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.addFormatForLookahead("N#", new FixedWidthFieldLengths(9, 11, 8));
		settings.addFormatForLookbehind("N#", new FixedWidthFieldLengths(13, 4, 34, 2));
		settings.addFormatForLookahead("111", new FixedWidthFieldLengths(13, 4, 34, 2));
		settings.addFormatForLookbehind("111", new FixedWidthFieldLengths(3, 10, 4, 34, 2));

		FixedWidthParser parser = new FixedWidthParser(settings);

		List<String[]> rows = parser.parseAll(new StringReader(FIXED_INPUT));
		assertEquals(rows.size(), 5);
		assertEquals(rows.get(0), new String[] { "N#123123", "1888858", "58888548" });
		assertEquals(rows.get(1), new String[] { "111222", "3000", "FOO", "10" });
		assertEquals(rows.get(2), new String[] { "333", "444", "2000", "BAR", "60" });
		assertEquals(rows.get(3), new String[] { "N#123124", "1888844", "58888544" });
		assertEquals(rows.get(4), new String[] { "311222", "3500", "FOO", "30" });
	}

	@Test
	public void writeMultiRowFormatFixedWidth() {
		FixedWidthFieldLengths itemLengths = new FixedWidthFieldLengths(13, 4, 34, 2);
		FixedWidthWriterSettings settings = new FixedWidthWriterSettings(itemLengths);
		settings.getFormat().setLineSeparator("\n");
		settings.addFormatForLookahead("N#", new FixedWidthFieldLengths(9, 11, 8)); //receipt lengths

		StringWriter out = new StringWriter();
		FixedWidthWriter writer = new FixedWidthWriter(out, settings);

		List<Object[]> inputRows = new ArrayList<Object[]>();
		inputRows.add(new Object[] { "N#123123", "1888858", "58888548" });
		inputRows.add(new Object[] { "111222", 3000, "FOO", 10 });
		inputRows.add(new Object[] { "333444", 2000, "BAR", 60 });
		inputRows.add(new Object[] { "N#123124", "1888844", "58888544" });
		inputRows.add(new Object[] { "311222", 3500, "FOO", 30 });

		writer.writeRowsAndClose(inputRows);

		assertEquals(out.toString(), FIXED_INPUT);
	}

	@Test
	public void writeLookbehindMultiRowFormatFixedWidth() {
		FixedWidthFieldLengths itemLengths = new FixedWidthFieldLengths(9, 11, 8);
		FixedWidthWriterSettings settings = new FixedWidthWriterSettings(itemLengths);

		settings.getFormat().setLineSeparator("\n");
		settings.addFormatForLookbehind("N#", new FixedWidthFieldLengths(13, 4, 34, 2));

		StringWriter out = new StringWriter();
		FixedWidthWriter writer = new FixedWidthWriter(out, settings);

		List<Object[]> inputRows = new ArrayList<Object[]>();
		inputRows.add(new Object[] { "N#123123", "1888858", "58888548" });
		inputRows.add(new Object[] { "111222", 3000, "FOO", 10 });
		inputRows.add(new Object[] { "333444", 2000, "BAR", 60 });
		inputRows.add(new Object[] { "N#123124", "1888844", "58888544" });
		inputRows.add(new Object[] { "311222", 3500, "FOO", 30 });

		writer.writeRowsAndClose(inputRows);

		assertEquals(out.toString(), FIXED_INPUT);
	}

	@Test
	public void writeLookbehindAndAhead() {
		FixedWidthWriterSettings settings = new FixedWidthWriterSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.addFormatForLookahead("N#", new FixedWidthFieldLengths(9, 11, 8));
		settings.addFormatForLookbehind("N#", new FixedWidthFieldLengths(13, 4, 34, 2));
		settings.addFormatForLookahead("111", new FixedWidthFieldLengths(13, 4, 34, 2));
		settings.addFormatForLookbehind("111", new FixedWidthFieldLengths(3, 10, 4, 34, 2));

		StringWriter out = new StringWriter();
		FixedWidthWriter writer = new FixedWidthWriter(out, settings);

		List<Object[]> inputRows = new ArrayList<Object[]>();
		inputRows.add(new Object[] { "N#123123", "1888858", "58888548" });
		inputRows.add(new Object[] { "111222", 3000, "FOO", 10 });
		inputRows.add(new Object[] { 333, 444, 2000, "BAR", 60 });
		inputRows.add(new Object[] { "N#123124", "1888844", "58888544" });
		inputRows.add(new Object[] { "311222", 3500, "FOO", 30 });

		writer.writeRowsAndClose(inputRows);

		assertEquals(out.toString(), FIXED_INPUT);
	}

	public static class Client {
		@EnumOptions(customElement = "typeCode", selectors = { EnumSelector.CUSTOM_FIELD })
		@Parsed(index = 1)
		private ClientType type;

		@Parsed(index = 2)
		private String name;

		@Nested(identityValue = "Account", identityIndex = 0, instanceOf = ArrayList.class, componentType = ClientAccount.class)
		private List<ClientAccount> accounts;
	}

	public static class ClientAccount {
		@Parsed(index = 1)
		private BigDecimal balance;
		@Parsed(index = 2)
		private String bank;
		@Parsed(index = 3)
		private String number;
		@Parsed(index = 4)
		private String swift;
	}

	@Test(enabled = false)
	public void parseCsvToBeanWithList() {
		final BeanListProcessor<Client> clientProcessor = new BeanListProcessor<Client>(Client.class);

		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setRowProcessor(clientProcessor);

		CsvParser parser = new CsvParser(settings);
		parser.parse(new StringReader(CSV_INPUT));

		List<Client> rows = clientProcessor.getBeans();
		assertEquals(rows.size(), 2);
		assertEquals(rows.get(0).accounts.size(), 2);
		assertEquals(rows.get(0).type, ClientType.BUSINESS);
		assertEquals(rows.get(0).name, "Foo");
		assertEquals(rows.get(0).accounts.get(0).balance, new BigDecimal("23234"));
		assertEquals(rows.get(0).accounts.get(0).bank, "HSBC");
		assertEquals(rows.get(0).accounts.get(0).number, "123433-000");
		assertEquals(rows.get(0).accounts.get(0).swift, "HSBCAUS");
		assertEquals(rows.get(0).accounts.get(1).balance, new BigDecimal("11234"));
		assertEquals(rows.get(0).accounts.get(1).bank, "HSBC");
		assertEquals(rows.get(0).accounts.get(1).number, "222343-130");
		assertEquals(rows.get(0).accounts.get(1).swift, "HSBCCAD");
		assertEquals(rows.get(1).accounts.size(), 1);
		assertEquals(rows.get(1).type, ClientType.PERSONAL);
		assertEquals(rows.get(1).name, "BAR");
		assertEquals(rows.get(1).accounts.get(0).balance, new BigDecimal("1234"));
		assertEquals(rows.get(1).accounts.get(0).bank, "CITI");
		assertEquals(rows.get(1).accounts.get(0).number, "213343-130");
		assertEquals(rows.get(1).accounts.get(0).swift, "CITICAD");

	}

	@Test(enabled = false)
	public void writeBeanWithListToCsv() {
		fail("Not implemented");
	}

	public static class Receipt {
		private String number;
		private String storeCode;
		private String customerCode;

		private Map<String, SaleItem> items;
	}

	public static class SaleItem {
		private BigDecimal unitPrice;
		private String description;
		private int units;
	}

	@Test(enabled = false)
	public void parseFixedWidthToBeanWithList() {
		fail("Not implemented");
	}

	@Test(enabled = false)
	public void writeBeanWithListToFixedWidth() {
		fail("Not implemented");
	}
}