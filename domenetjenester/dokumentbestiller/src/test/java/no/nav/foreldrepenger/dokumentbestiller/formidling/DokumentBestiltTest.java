package no.nav.foreldrepenger.dokumentbestiller.formidling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class DokumentBestiltTest {

    @Mock
    private HistorikkRepository historikkRepository;

    private DokumentBestilt dokumentBestilt;

    @BeforeEach
    void setUp() {
        dokumentBestilt = new DokumentBestilt(historikkRepository);
    }

    @Test
    void historikk_innslag_med_fritekst_mal() {
        var saksbehandler = HistorikkAktør.SAKSBEHANDLER;

        var malBrukt = DokumentMalType.FRITEKSTBREV;
        var journalførSom = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var bestilling = lagBestilling(malBrukt, journalførSom);

        dokumentBestilt.opprettHistorikkinnslag(saksbehandler, dummyBehandling(), bestilling);

        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository).lagre(captor.capture());
        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getAktør()).isEqualTo(saksbehandler);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkinnslag.getHistorikkinnslagDeler().getFirst().getBegrunnelse()).isPresent();
        assertThat(historikkinnslag.getHistorikkinnslagDeler().getFirst().getBegrunnelse().get()).containsSubsequence(journalførSom.getNavn(), malBrukt.getNavn());
    }

    @Test
    void historikk_innslag_uten_fritekst_mal() {
        var saksbehandler = HistorikkAktør.SAKSBEHANDLER;
        var malBrukt = DokumentMalType.ENGANGSSTØNAD_INNVILGELSE;

        var bestilling = lagBestilling(malBrukt, null);

        dokumentBestilt.opprettHistorikkinnslag(saksbehandler, dummyBehandling(), bestilling);

        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository).lagre(captor.capture());
        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getAktør()).isEqualTo(saksbehandler);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkinnslag.getHistorikkinnslagDeler().getFirst().getBegrunnelse()).isPresent();
        assertThat(historikkinnslag.getHistorikkinnslagDeler().getFirst().getBegrunnelse().get()).contains(malBrukt.getNavn());
    }

    private static Behandling dummyBehandling() {
        return Behandling.forFørstegangssøknad(Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AktørId.dummy(), Språkkode.NB)))
            .build();
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
