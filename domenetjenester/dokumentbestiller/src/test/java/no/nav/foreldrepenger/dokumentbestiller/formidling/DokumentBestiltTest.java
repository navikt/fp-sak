package no.nav.foreldrepenger.dokumentbestiller.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class DokumentBestiltTest {

    @Test
    void historikk_innslag_med_fritekst_mal() {
        var saksbehandler = HistorikkAktør.SAKSBEHANDLER;

        var malBrukt = DokumentMalType.FRITEKSTBREV;
        var journalførSom = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var bestilling = lagBestilling(malBrukt, journalførSom);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        var behandlingRepositoryProvider = scenario.mockBehandlingRepositoryProvider();
        var dokumentBestilt = new DokumentBestilt(behandlingRepositoryProvider.getHistorikkinnslag2Repository());
        dokumentBestilt.opprettHistorikkinnslag(saksbehandler, behandling, bestilling);

        var historikkinnslag = behandlingRepositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();

        assertThat(historikkinnslag.getAktør()).isEqualTo(saksbehandler);
        assertThat(historikkinnslag.getTittel()).isEqualTo("Brev er bestilt");
        assertThat(historikkinnslag.getLinjer()).hasSize(1);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).containsSubsequence(journalførSom.getNavn(), malBrukt.getNavn());
    }

    @Test
    void historikk_innslag_uten_fritekst_mal() {
        var saksbehandler = HistorikkAktør.SAKSBEHANDLER;
        var malBrukt = DokumentMalType.ENGANGSSTØNAD_INNVILGELSE;

        var bestilling = lagBestilling(malBrukt, null);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();
        var behandlingRepositoryProvider = scenario.mockBehandlingRepositoryProvider();
        var dokumentBestilt = new DokumentBestilt(behandlingRepositoryProvider.getHistorikkinnslag2Repository());
        dokumentBestilt.opprettHistorikkinnslag(saksbehandler, behandling, bestilling);

        var historikkinnslag = behandlingRepositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();

        assertThat(historikkinnslag.getAktør()).isEqualTo(saksbehandler);
        assertThat(historikkinnslag.getTittel()).isEqualTo("Brev er bestilt");
        assertThat(historikkinnslag.getLinjer()).hasSize(1);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).containsSubsequence(malBrukt.getNavn());
    }

    private DokumentBestilling lagBestilling(DokumentMalType dokumentMal, DokumentMalType journalførSomMal) {
        return DokumentBestilling.builder()
            .medBehandlingUuid(UUID.randomUUID())
            .medSaksnummer(new Saksnummer("9999"))
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSomMal)
            .medFritekst("test")
            .build();
    }
}
