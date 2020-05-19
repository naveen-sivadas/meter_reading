package com.naveen.redenergy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests related to SimpleNem12ParserImpl class, covering the possible scenarios during execution.
 * @author Naveen
 *
 */
public class SimpleNem12ParserImplTest {
	private SimpleNem12Parser parser;
	
	/**
	 * Setup the test data.
	 */
	@BeforeEach
	public void setUp() {
		parser = new SimpleNem12ParserImpl();
	}
	/**
	 * Test 1: Test file load and parsing appropriately.
	 * @throws IOException - an error during execution.
	 */
	@Test
	public void parseSimpleNem12Test() throws IOException {
		Collection<MeterRead> meterReads = parser.parseSimpleNem12(new ClassPathResource("ref-data/SimpleNem12.csv").getFile());
		assertThat(meterReads.stream().filter(mr -> mr.getNmi().equals("6123456789")).findFirst().get().getTotalVolume()).isEqualTo("-36.84");
		assertThat(meterReads.stream().filter(mr -> mr.getNmi().equals("6123456789")).findFirst().get().getVolumes().size()).isEqualTo(7);
		assertThat(meterReads.stream().filter(mr -> mr.getNmi().equals("6987654321")).findFirst().get().getTotalVolume()).isEqualTo("14.33");
	}
	
	/**
	 * Test 2: Test IllegalArgumentException throws due to missing start index [100].
	 * @throws IOException - an error during execution.
	 */
	@Test
	public void parseMissingBeginIndexTest() throws IOException {
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			parser.parseSimpleNem12(new ClassPathResource("ref-data/SimpleNemInvalidFirstIndex.csv").getFile());
		});
		assertThat(exception.getMessage()).isEqualTo("Beginning/End of record not found.");
	}
	
	/**
	 * Test 3: Test IllegalArgumentException throws due to missing last index [900]
	 * @throws IOException
	 */
	@Test
	public void parseMissingLastIndexTest() throws IOException {
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			parser.parseSimpleNem12(new ClassPathResource("ref-data/SimpleNemInvalidLastIndex.csv").getFile());
		});
		assertThat(exception.getMessage()).isEqualTo("Beginning/End of record not found.");
	}
	
	/**
	 * Test 4: In case of an erroneous record, log and continue parsing the remaining records without halting the existing flow.
	 * @throws IOException - error in execution.
	 */
	@Test
	public void parseInvalidRecordTest() throws IOException {
		Collection<MeterRead> meterReads = parser.parseSimpleNem12(new ClassPathResource("ref-data/SimpleNemInconsistentRecord.csv").getFile());
		assertThat(meterReads.stream().filter(mr -> mr.getNmi().equals("6123456789")).findFirst().get().getVolumes().size()).isEqualTo(5);
	}
	
	/**
	 * Test 5: Test IllegalArgumentException throws due to incorrect nmi.
	 * @throws IOException - error in execution.
	 */
	@Test
	public void parseInvalidNmiTest() throws IOException {
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			parser.parseSimpleNem12(new ClassPathResource("ref-data/SimpleNemInvalidNmi.csv").getFile());
		});
		assertThat(exception.getMessage()).isEqualTo("Invalid NMI encountered");
	}
	
	/**
	 * Test 6: Test IllegalArgumentException throws due to missing nmi.
	 * @throws IOException - error in execution.
	 */
	@Test
	public void parseMissingNmiTest() throws IOException {
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			parser.parseSimpleNem12(new ClassPathResource("ref-data/SimpleNemMissingNmi.csv").getFile());
		});
		assertThat(exception.getMessage()).isEqualTo("Invalid NMI encountered");
	}
}
