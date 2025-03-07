package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OppgaveTypeTest {

    @Test
    void skal_hente_ut_riktig_kode_for_vur_kons_yte() {
        assertThat(OppgaveType.VUR_KONS_YTE.getKode()).isEqualTo("VUR_KONS_YTE");
    }

    @Test
    void skal_hente_ut_riktig_kode_for_vur() {
        assertThat(OppgaveType.VUR.getKode()).isEqualTo("VUR");
    }

    @Test
    void skal_hente_ut_riktig_navn_for_yte() {
        assertThat(OppgaveType.VUR_KONS_YTE.getNavn()).isEqualTo("Vurder konsekvens for ytelse");
    }

    @Test
    void skal_hente_ut_riktig_navn_for_vur() {
        assertThat(OppgaveType.VUR.getNavn()).isEqualTo("Vurder dokument");
    }

    @Test
    void skal_hente_ut_riktig_kodeverk() {
        assertThat(OppgaveType.VUR_KONS_YTE.getKodeverk()).isEqualTo("OPPGAVE_TYPE");
        assertThat(OppgaveType.VUR.getKodeverk()).isEqualTo("OPPGAVE_TYPE");
    }

    @Test
    void skal_hente_ut_riktig_kodeMap() {
        assertThat(OppgaveType.kodeMap()).containsEntry("VUR_KONS_YTE", OppgaveType.VUR_KONS_YTE);
        assertThat(OppgaveType.kodeMap()).containsEntry("VUR", OppgaveType.VUR);
    }
}
