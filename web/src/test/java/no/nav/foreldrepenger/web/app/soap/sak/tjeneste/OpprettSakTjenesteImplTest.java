package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.produksjonsstyring.opprettgsak.OpprettGSakTjeneste;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
@ExtendWith(MockitoExtension.class)
public class OpprettSakTjenesteImplTest extends EntityManagerAwareTest {

    @Mock
    private TpsTjeneste tpsTjenesteMock;

    @Mock
    private BrukerTjeneste brukerTjenesteMock;

    private OpprettSakTjeneste opprettSakTjeneste;

    private AktørId aktørId = AktørId.dummy();

    private Personinfo personinfo;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        personinfo = new Personinfo.Builder()
                .medPersonIdent(PersonIdent.fra("12345612345"))
                .medNavn("Kari Nordmann")
                .medFødselsdato(LocalDate.of(1980, 1, 1))
                .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
                .medAktørId(aktørId)
                .medForetrukketSpråk(Språkkode.NB).build();
        lenient().when(tpsTjenesteMock.hentBrukerForAktør(any(AktørId.class))).thenReturn(Optional.of(personinfo));

        // Mock BersonTjeneste
        NavBruker navBruker = NavBruker.opprettNy(personinfo);
        when(brukerTjenesteMock.hentEllerOpprettFraAktorId(any(Personinfo.class))).thenReturn(navBruker);

        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        var opprettGSakTjeneste = new OpprettGSakTjeneste(null);
        this.opprettSakTjeneste = new OpprettSakTjeneste(tpsTjenesteMock, fagsakTjeneste, opprettGSakTjeneste, brukerTjenesteMock, null);
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
