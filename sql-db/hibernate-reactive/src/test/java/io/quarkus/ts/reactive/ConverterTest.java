package io.quarkus.ts.reactive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.ts.reactive.database.ISBNConverter;

class ConverterTest {
    private final ISBNConverter converter = new ISBNConverter();

    @Test
    void parse() {
        String source = "978-3-16-148410-0";
        Long result = converter.convertToEntityAttribute(source);
        Assertions.assertEquals(978_3_16_148410_0L, result);
    }

    @Test
    void create() {
        long source = 978_3_16_148410_0L;
        String result = converter.convertToDatabaseColumn(source);
        Assertions.assertEquals("978-3-16-148410-0", result);
    }

    @Test
    void prepended() {
        long source = 3161484100L;
        String result = converter.convertToDatabaseColumn(source);
        Assertions.assertEquals("000-3-16-148410-0", result);
    }

    @Test
    void zero() {
        long source = 0L;
        String result = converter.convertToDatabaseColumn(source);
        Assertions.assertEquals("000-0-00-000000-0", result);
    }
}
