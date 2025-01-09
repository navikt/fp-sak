package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagOld;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

class HistorikkV2AdapterTest {

    private static final UUID BEHANDLING_UUID = UUID.randomUUID();

    @Test
    void malType5MappingTest() {
        var historikkinnslag = new HistorikkinnslagOld();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setOpprettetAv("KLARA");

        var del = HistorikkinnslagDel.builder();

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.SKJERMLENKE)
            .medTilVerdi(SkjermlenkeType.UTTAK)
            .build(del);

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.HENDELSE)
            .medNavn(HistorikkinnslagType.FAKTA_ENDRET)
            .build(del);

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(new TestKodeverk("Aktivitet", "AKTIVITET"))
            .medNavnVerdi("Sjørøver")
            .medFraVerdi("fraVerdi")
            .medTilVerdi("tilVerdi")
            .medSekvensNr(1)
            .build(del);

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.OPPLYSNINGER)
            .medNavn(new TestKodeverk("Antall barn", "ANTALL_BARN"))
            .medTilVerdi("1")
            .medSekvensNr(2)
            .build(del);

        historikkinnslag.setHistorikkinnslagDeler(List.of(del.build()));


        var historikkinnslagDtoV2 = HistorikkV2Adapter.map(historikkinnslag, BEHANDLING_UUID, List.of(), null);

        assertThat(historikkinnslagDtoV2).isNotNull();
    }

    @Test
    void malType10TrekkDagerTest() {
        var historikkinnslag = new HistorikkinnslagOld();
        historikkinnslag.setType(HistorikkinnslagType.FASTSATT_UTTAK);
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setOpprettetAv("ELISE");

        var del = HistorikkinnslagDel.builder();

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.SKJERMLENKE)
            .medTilVerdi(SkjermlenkeType.UTTAK)
            .build(del);

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.HENDELSE)
            .medNavn(HistorikkinnslagType.FASTSATT_UTTAK)
            .build(del);

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(new TestKodeverk("Trekkdager", "UTTAK_TREKKDAGER"))
            .medNavnVerdi("trekk")
            .medFraVerdi("20.0")
            .medTilVerdi("41.0")
            .medSekvensNr(1)
            .build(del);

        historikkinnslag.setHistorikkinnslagDeler(List.of(del.build()));

        var historikkinnslagDtoV2 = HistorikkV2Adapter.map(historikkinnslag, BEHANDLING_UUID, List.of(), null);

        assertThat(historikkinnslagDtoV2).isNotNull();
        assertThat(historikkinnslagDtoV2.linjer())
            .extracting(HistorikkinnslagDtoV2.Linje::tekst)
            .contains("__Trekkdager__ er endret fra 20.0 til __41.0__");
    }

    private record TestKodeverk(String navn, String kode) implements Kodeverdi {
        @Override
        public String getKode() {
            return kode;
        }

        @Override
        public String getKodeverk() {
            return "kodeverk";
        }

        @Override
        public String getNavn() {
            return navn;
        }
    }
}
