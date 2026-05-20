package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class BehandlingDokumentEntitetTest {

    @Test
    void bygger_uten_fritekst() {
        var ny = BehandlingDokumentEntitet.Builder.ny();
        ny.medBehandling(123L);

        assertDoesNotThrow(ny::build);
    }

    @Test
    void bygger_med_html_fritekst() {
        var ny = BehandlingDokumentEntitet.Builder.ny();
        ny.medBehandling(123L);
        ny.medOverstyrtBrevFritekstHtml("<p>html</p>");

        assertDoesNotThrow(ny::build);
    }
}
