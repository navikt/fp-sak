package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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
import no.nav.foreldrepenger.domene.typer.AktørId;

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
        var opprinneligDokumentMal = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        dokumentBestilt.opprettHistorikkinnslag(saksbehandler, dummyBehandling(), malBrukt, opprinneligDokumentMal);

        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository).lagre(captor.capture());
        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getAktør()).isEqualTo(saksbehandler);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkinnslag.getHistorikkinnslagDeler().getFirst().getBegrunnelse()).isPresent();
        assertThat(historikkinnslag.getHistorikkinnslagDeler().getFirst().getBegrunnelse().get()).containsSubsequence(opprinneligDokumentMal.getNavn(), malBrukt.getNavn());
    }

    @Test
    void historikk_innslag_uten_fritekst_mal() {
        var saksbehandler = HistorikkAktør.SAKSBEHANDLER;
        var malBrukt = DokumentMalType.ENGANGSSTØNAD_INNVILGELSE;

        dokumentBestilt.opprettHistorikkinnslag(saksbehandler, dummyBehandling(), malBrukt, null);

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
}
