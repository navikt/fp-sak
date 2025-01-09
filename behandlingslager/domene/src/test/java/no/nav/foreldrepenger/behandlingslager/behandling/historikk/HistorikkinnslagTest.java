package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

class HistorikkinnslagTest {

    @Test
    void fjernerLeadingLinjeskift() {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medFagsakId(1L)
            .medBehandlingId(1L)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT)
            .addLinje("test")
            .build();

        assertThat(historikkinnslag.getLinjer()).allMatch(l -> l.getType() == HistorikkinnslagLinjeType.TEKST);
    }

    @Test
    void fjernerTrailingLinjeskift() {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medFagsakId(1L)
            .medBehandlingId(1L)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje("test")
            .addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT)
            .build();

        assertThat(historikkinnslag.getLinjer()).allMatch(l -> l.getType() == HistorikkinnslagLinjeType.TEKST);
    }

    @Test
    void fjernerIkkeLinjeskiftIMellomTekstlinjer() {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medFagsakId(1L)
            .medBehandlingId(1L)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje("test")
            .addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT)
            .addLinje("test")
            .build();

        assertThat(historikkinnslag.getLinjer()).hasSize(3);
        assertThat(historikkinnslag.getLinjer().get(1).getType()).isEqualTo(HistorikkinnslagLinjeType.LINJESKIFT);
    }
}
