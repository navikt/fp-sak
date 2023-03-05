package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class OpprettSakTjenesteTest {

    @Mock
    private NavBrukerTjeneste brukerTjeneste;

    private OpprettSakTjeneste opprettSakTjeneste;

    private final AktørId aktørId = AktørId.dummy();

    @BeforeEach
    public void setUp(EntityManager entityManager) {

        // Mock BersonTjeneste
        var navBruker = NavBruker.opprettNy(aktørId, Språkkode.NB);
        when(brukerTjeneste.hentEllerOpprettFraAktørId(any(AktørId.class))).thenReturn(navBruker);

        var fagsakTjeneste = new FagsakTjeneste(new FagsakRepository(entityManager),
            new SøknadRepository(entityManager, new BehandlingRepository(entityManager)), null);
        this.opprettSakTjeneste = new OpprettSakTjeneste(fagsakTjeneste, brukerTjeneste);
    }

    @Test
    void opprett_sak_for_foreldrepenger() {
        var fagsak = opprettSakTjeneste.opprettSakVL(aktørId, FagsakYtelseType.FORELDREPENGER,
            new JournalpostId(1001L));

        assertThat(fagsak.getYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    void opprett_sak_for_svangerskapspenger() {
        var fagsak = opprettSakTjeneste.opprettSakVL(aktørId, FagsakYtelseType.SVANGERSKAPSPENGER,
            new JournalpostId(1001L));

        assertThat(fagsak.getYtelseType()).isEqualTo(FagsakYtelseType.SVANGERSKAPSPENGER);
    }

}
