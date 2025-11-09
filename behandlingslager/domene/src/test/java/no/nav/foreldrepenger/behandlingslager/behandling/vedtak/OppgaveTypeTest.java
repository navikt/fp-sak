package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OppgaveTypeTest {

    @Test
    void skal_hente_ut_riktig_kode_for_vur_kons_yte() {
        assertThat(OppgaveType.VUR_KONSEKVENS.getKode()).isEqualTo("VUR_KONSEKVENS");
    }

    @Test
    void skal_hente_ut_riktig_kode_for_vur() {
        assertThat(OppgaveType.VUR_DOKUMENT.getKode()).isEqualTo("VUR_DOKUMENT");
    }

    @Test
    void skal_hente_ut_riktig_navn_for_yte() {
        assertThat(OppgaveType.VUR_KONSEKVENS.getNavn()).isEqualTo("Vurder konsekvens for ytelse");
    }

    @Test
    void skal_hente_ut_riktig_navn_for_vur() {
        assertThat(OppgaveType.VUR_DOKUMENT.getNavn()).isEqualTo("Vurder dokument");
    }
}
