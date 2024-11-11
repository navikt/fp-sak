package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

class HistorikkV2AdapterTest {

    private static final UUID BEHANDLING_UUID = UUID.randomUUID();

    @Test
    void malType5MappingTest() {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setOpprettetAv("KLARA");
        new HistorikkInnslagTekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UTTAK)
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medEndretFelt(HistorikkEndretFeltType.AKTIVITET, "Sjørøver", "fraVerdi", "tilVerdi")
            .medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_FOM, LocalDate.now().minusMonths(1))
            .medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_TOM, LocalDate.now().plusDays(1))
            .build(historikkinnslag);

        var historikkinnslagDtoV2 = HistorikkV2Adapter.map(historikkinnslag, BEHANDLING_UUID, List.of(), null);

        assertThat(historikkinnslagDtoV2).isNotNull();
    }

    @Test
    void malType10TrekkDagerTest() {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FASTSATT_UTTAK);
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setOpprettetAv("ELISE");
        new HistorikkInnslagTekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UTTAK)
            .medHendelse(HistorikkinnslagType.FASTSATT_UTTAK)
            .medEndretFelt(HistorikkEndretFeltType.UTTAK_TREKKDAGER, "trekk", "20.0", "41.0")
            .build(historikkinnslag);

        var historikkinnslagDtoV2 = HistorikkV2Adapter.map(historikkinnslag, BEHANDLING_UUID, List.of(), null);

        assertThat(historikkinnslagDtoV2).isNotNull();
        assertThat(historikkinnslagDtoV2.body()).contains("__Trekkdager__ er endret fra 20.0 til __41.0__");
    }
}
