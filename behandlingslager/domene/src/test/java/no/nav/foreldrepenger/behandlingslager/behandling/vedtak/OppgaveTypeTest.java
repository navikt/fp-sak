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

    @Test
    void skal_hente_ut_riktig_kodeverk() {
        assertThat(OppgaveType.VUR_KONSEKVENS.getKodeverk()).isEqualTo("OPPGAVE_TYPE");
        assertThat(OppgaveType.VUR_DOKUMENT.getKodeverk()).isEqualTo("OPPGAVE_TYPE");
    }

    @Test
    void skal_hente_ut_riktig_kodeMap() {
        assertThat(OppgaveType.kodeMap()).containsEntry("VUR_KONSEKVENS", OppgaveType.VUR_KONSEKVENS);
        assertThat(OppgaveType.kodeMap()).containsEntry("VUR_DOKUMENT", OppgaveType.VUR_DOKUMENT);
    }

    @Test
    void skal_mappe_til_riktig_oppgaveType_for_vur_kons_yte() {
        assertThat(OppgaveType.fraKode("VUR_KONS_YTE")).isEqualTo(OppgaveType.VUR_KONSEKVENS);
    }

    @Test
    void skal_mappe_til_riktig_oppgaveType_for_vur_og_vur_vl() {
        assertThat(OppgaveType.fraKode("VUR")).isEqualTo(OppgaveType.VUR_DOKUMENT);
        assertThat(OppgaveType.fraKode("VUR_VL")).isEqualTo(OppgaveType.VUR_DOKUMENT);
    }
}
