package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BehandlingDokumentEntitetTest {

    @Test
    void feiler_uten_overstyrt_overskrift() {
        var ny = BehandlingDokumentEntitet.Builder.ny();
        ny.medBehandling(123L);
        ny.medOverstyrtBrevFritekst("test");

        assertThrows(NullPointerException.class, ny::build);
    }

    @Test
    void feiler_uten_overstyrt_fritekst() {
        var ny = BehandlingDokumentEntitet.Builder.ny();
        ny.medBehandling(123L);
        ny.medOverstyrtBrevOverskrift("test");

        assertThrows(NullPointerException.class, ny::build);
    }

    @Test
    void feiler_ikke_med_fritekst_overstyrt() {
        var ny = BehandlingDokumentEntitet.Builder.ny();
        ny.medBehandling(123L);
        ny.medOverstyrtBrevOverskrift("oversktift");
        ny.medOverstyrtBrevFritekst("fritekst");

        assertDoesNotThrow(ny::build);
    }

    @Test
    void feiler_ikke_uten_fritekst_overstyrt() {
        var ny = BehandlingDokumentEntitet.Builder.ny();
        ny.medBehandling(123L);

        assertDoesNotThrow(ny::build);
    }
}
