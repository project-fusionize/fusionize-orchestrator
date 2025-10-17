package dev.fusionize.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationExceptionTest {
    @Test
    void getCode() {
        try{
            throw new ApplicationException(new Exception("(te123) test exception"));
        } catch (ApplicationException e) {
            assertEquals("te123", e.getCode());
            assertEquals("test exception", e.getError());

        }

        try{
            throw new ApplicationException("(te124) another test",new Exception("some test exception"));
        } catch (ApplicationException e) {
            assertEquals("te124", e.getCode());
            assertEquals("another test", e.getError());

        }

        try{
            throw new ApplicationException("(testa124.5) another test 2",new Exception("some test exception"));
        } catch (ApplicationException e) {
            assertEquals("testa124.5", e.getCode());
            assertEquals("another test 2", e.getError());

        }
    }
}