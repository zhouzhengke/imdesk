package com.company.imticket.service.domain.ticket;

import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.TicketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketNoGeneratorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TicketNoGenerator ticketNoGenerator;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ======================== Format validation ========================

    @Test
    void generate_shouldReturnCorrectFormat() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        String ticketNo = ticketNoGenerator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expected = "IM-" + expectedDatePart + "-0001";

        assertEquals(expected, ticketNo);
        assertTrue(ticketNo.matches("IM-\\d{8}-\\d{4}"));
    }

    @Test
    void generate_shouldPadSequenceToFourDigits() {
        when(valueOperations.increment(anyString())).thenReturn(42L);

        String ticketNo = ticketNoGenerator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expected = "IM-" + expectedDatePart + "-0042";

        assertEquals(expected, ticketNo);
        assertTrue(ticketNo.matches("IM-\\d{8}-\\d{4}"));
    }

    @Test
    void generate_shouldHandleLargeSequenceNumbers() {
        when(valueOperations.increment(anyString())).thenReturn(9999L);

        String ticketNo = ticketNoGenerator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expected = "IM-" + expectedDatePart + "-9999";

        assertEquals(expected, ticketNo);
        assertTrue(ticketNo.matches("IM-\\d{8}-\\d{4}"));
    }

    // ======================== Sequence increment ========================

    @Test
    void generate_secondCallShouldReturnHigherNumber() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);

        String first = ticketNoGenerator.generate();
        String second = ticketNoGenerator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertEquals("IM-" + expectedDatePart + "-0001", first);
        assertEquals("IM-" + expectedDatePart + "-0002", second);
    }

    @Test
    void generate_multipleCallsShouldAllIncrement() {
        when(valueOperations.increment(anyString())).thenReturn(5L, 6L, 7L);

        String first = ticketNoGenerator.generate();
        String second = ticketNoGenerator.generate();
        String third = ticketNoGenerator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertEquals("IM-" + expectedDatePart + "-0005", first);
        assertEquals("IM-" + expectedDatePart + "-0006", second);
        assertEquals("IM-" + expectedDatePart + "-0007", third);

        verify(valueOperations, times(3)).increment(anyString());
    }

    // ======================== Redis key verification ========================

    @Test
    void generate_shouldUseCorrectRedisKey() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        ticketNoGenerator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expectedKey = "im:ticket:seq:" + expectedDatePart;

        verify(valueOperations).increment(expectedKey);
    }

    @Test
    void generate_shouldCallOpsForValueExactlyOncePerGeneration() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L, 3L);

        ticketNoGenerator.generate();
        ticketNoGenerator.generate();
        ticketNoGenerator.generate();

        verify(redisTemplate, times(3)).opsForValue();
    }

    // ======================== TTL on new keys ========================

    @Test
    void generate_NewKey_shouldSetTTL() {
        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expectedKey = "im:ticket:seq:" + expectedDatePart;
        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        ticketNoGenerator.generate();

        verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofHours(48)));
    }

    // ======================== Overflow handling ========================

    @Test
    void generate_SequenceExceeds9999_shouldThrowTicketException() {
        when(valueOperations.increment(anyString())).thenReturn(10000L);

        TicketException ex = assertThrows(TicketException.class, () -> ticketNoGenerator.generate());
        assertEquals(BizErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("10000"));
    }
}
