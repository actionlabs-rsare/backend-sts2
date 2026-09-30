package com.ap.sts.stockholders;

import com.ap.sts.stockholders.StockholderDtos.StockholderInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MDM-002 required fields. TIN, nationality, gender and email are required by the BRD; gender and
 * email are the additions the UX screens lacked (conflict UX-4, resolved at Gate 2).
 */
class StockholderValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> invalidFields(StockholderInput input) {
        return validator.validate(input).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void rejectsMissingTinNationalityGenderEmail() {
        StockholderInput empty = new StockholderInput(
                "  ", null, "  ", "  ", null, "  ", null, null, null, null, null);

        Set<String> fields = invalidFields(empty);

        assertTrue(fields.containsAll(Set.of("name", "type", "tin", "nationality", "gender", "email")),
                "expected every required MDM-002 field to be reported, got " + fields);
    }

    @Test
    void rejectsMalformedEmail() {
        StockholderInput badEmail = new StockholderInput(
                "Luna, Antonio (SAMPLE)", StockholderType.Individual, "777-888-999-000",
                "Filipino", Gender.Male, "not-an-email", null, null, null, null, null);

        assertEquals(Set.of("email"), invalidFields(badEmail));
    }

    @Test
    void rejectsOverlongName() {
        StockholderInput longName = new StockholderInput(
                "x".repeat(201), StockholderType.Individual, "777-888-999-000",
                "Filipino", Gender.Male, "a.luna@example.test", null, null, null, null, null);

        assertEquals(Set.of("name"), invalidFields(longName));
    }

    @Test
    void acceptsACompleteRecord() {
        StockholderInput complete = new StockholderInput(
                "Santiago, Maria Clara (SAMPLE)", StockholderType.Individual, "123-456-789-000",
                "Filipino", Gender.Female, "mc.santiago@example.test", "+63 900 000 0001",
                "Cebu City, PH", null, null, true);

        assertTrue(invalidFields(complete).isEmpty());
    }

    @Test
    void acceptsACorporateRecord() {
        StockholderInput corporate = new StockholderInput(
                "Rizal Holdings Corp. (SAMPLE)", StockholderType.Corporate, "222-333-444-000",
                "Filipino", Gender.PreferNotToSay, "corpsec@rizalholdings.example.test", null,
                "Makati City, PH", CorporationType.Domestic, null, null);

        assertTrue(invalidFields(corporate).isEmpty());
    }
}
