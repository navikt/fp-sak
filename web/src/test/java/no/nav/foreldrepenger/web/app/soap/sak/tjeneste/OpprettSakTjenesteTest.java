package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.opprettgsak.OpprettGSakTjeneste;
import no.nav.foreldrepenger.web.RepositoryAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
@ExtendWith(MockitoExtension.class)
public class OpprettSakTjenesteTest extends RepositoryAwareTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    @Mock
    private BrukerTjeneste brukerTjeneste;

    private OpprettSakTjeneste opprettSakTjeneste;

    private AktørId aktørId = AktørId.dummy();

    private PersoninfoSpråk personinfo;

    @BeforeEach
    public void setUp() {
        personinfo = new PersoninfoSpråk(aktørId, Språkkode.NB);
        lenient().when(personinfoAdapter.hentForetrukketSpråk(any(AktørId.class))).thenReturn(personinfo);

        // Mock BersonTjeneste
        NavBruker navBruker = NavBruker.opprettNy(personinfo.getAktørId(), personinfo.getForetrukketSpråk());
        when(brukerTjeneste.hentEllerOpprettFraAktorId(any(AktørId.class), any(Språkkode.class))).thenReturn(navBruker);

        var fagsakTjeneste = new FagsakTjeneste(fagsakRepository, søknadRepository, null);
        var opprettGSakTjeneste = new OpprettGSakTjeneste(null);
        this.opprettSakTjeneste = new OpprettSakTjeneste(personinfoAdapter, fagsakTjeneste, opprettGSakTjeneste, brukerTjeneste, null);
    }

    @Test
    public void opprett_sak_for_foreldrepenger() {
        var fagsak = opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.FORELDREPENGER, new JournalpostId(1001L));

        assertThat(fagsak.getYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void opprett_sak_for_svangerskapspenger() {
        var fagsak = opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.SVANGERSKAPSPENGER, new JournalpostId(1001L));

        assertThat(fagsak.getYtelseType()).isEqualTo(FagsakYtelseType.SVANGERSKAPSPENGER);
    }

}
