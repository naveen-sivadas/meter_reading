package com.naveen.redenergy.domain;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.naveen.redenergy.units.EnergyUnit;
import com.naveen.redenergy.units.Quality;

/**
 * Implements a SimpleNem12Parser used to parse data pertaining to meter reading such as NMI, Date, Volume and the respective units.
 * 
 * @author Naveen.
 *
 */
public class SimpleNem12ParserImpl implements SimpleNem12Parser {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNem12ParserImpl.class);
	private static final String METER_READ_BLOCK_OFFSET = "200";
	private static final String VOLUME_READ_OFFSET = "300";
	private static final String INITIAL_OFSFET = "100";
	private static final String FINAL_OFSFET = "900";
	private static final String DELIMITER = ",";
	private static final String DATE_FORMATTER_PATERN = "yyyyMMdd";
	private short recordBoundaries = 0;
	private final List<MeterRead> meterRecordsList = new ArrayList<>();

	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
		try (Stream<String> stream = Files.lines(Paths.get(simpleNem12File.getAbsolutePath()))) {
			stream.forEach(this::processRecord);
		} catch (IOException exception) {
			LOGGER.error("Program execution failed.", exception);
		}

		// Check if the beginning and end of records exists!
		if (recordBoundaries != 2) {
			meterRecordsList.clear();
			throw new IllegalArgumentException("Beginning/End of record not found.");
		}

		return meterRecordsList;
	}

	/**
	 * Used to process the record based on their type and create subsequent entries as Meter reading record and Volumes record.
	 * @param meterReading - raw string that is used to split into individual records.
	 */
	private void processRecord(String meterReading) {
		String[] split = meterReading.split(DELIMITER);
		switch (split[0]) {
		case INITIAL_OFSFET:
			recordBoundaries += 1;
			break;
		case METER_READ_BLOCK_OFFSET:
			createMeterRecord(split[1], split[2]);
			break;
		case VOLUME_READ_OFFSET:
			createVolumeRecord(split[1], split[2], split[3]);
			break;
		case FINAL_OFSFET:
			recordBoundaries += 1;
			break;
		default:
			LOGGER.info("Invalid Record Type encountered", split[0]);
		}

	}

	/**
	 * Used to create a Volume record using date, meter reading and quality.
	 * @param date - the date field mapped to each record.
	 * @param reading - the meter reading.
	 * @param quality - indicates whether absolute or estimate.
	 */
	private void createVolumeRecord(String date, String reading, String quality) {
		try {
			LocalDate readingDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_FORMATTER_PATERN));
			meterRecordsList.get(meterRecordsList.size() - 1).appendVolume(readingDate,
					new MeterVolume(new BigDecimal(reading), Quality.valueOf(quality)));
		} catch (DateTimeParseException exception) {
			LOGGER.info("Error parsing date in record, the record " + date + " will not be processed", exception);
		}

	}

	/**
	 * Used to create a meter record passing nmi and unit as part of the constructor. Will throw an Exception if invalid nmi is encountered.
	 * @param nmi - id for each meter reading.
	 * @param unit - the energy unit.
	 */
	private void createMeterRecord(String nmi, String unit) {
		if(StringUtils.isEmpty(nmi) || nmi.length()<10) {
			throw new IllegalArgumentException("Invalid NMI encountered");
		}
		meterRecordsList.add(new MeterRead(nmi, EnergyUnit.valueOf(unit)));
	}
}
