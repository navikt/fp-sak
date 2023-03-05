package no.nav.foreldrepenger.batch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.feil.UnknownArgumentsReceivedVLBatchException;

class EmptyBatchArgumentsTest {

    @Test
    void skal_kaste_exception_ved_for_mange_argumenter() {
        Map<String, String> map = new HashMap<>();
        map.put("asdf", "asdf");
        assertThrows(UnknownArgumentsReceivedVLBatchException.class, () -> new EmptyBatchArguments(map));
    }

    @Test
    void skal_ikke_kaste_exception_ved_ingen_argumenter() {
        assertTrue(new EmptyBatchArguments(new HashMap<>()).isValid());
    }
}
