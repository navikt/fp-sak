package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.produksjonsstyring.opprettgsak.OpprettGSakTjeneste;

public class OpprettSakTjenesteImplTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Mock
    private TpsTjeneste tpsTjenesteMock;

    @Mock
    private BrukerTjeneste brukerTjenesteMock;

    private OpprettSakTjeneste opprettSakTjeneste;

    private AktørId aktørId = AktørId.dummy();

    private Personinfo personinfo;

    @Before
    public void setUp() {
        //Mock TpsTjeneste
        personinfo = new Personinfo.Builder()
            .medPersonIdent(PersonIdent.fra("12345612345"))
            .medNavn("Kari Nordmann")
            .medFødselsdato(LocalDate.of(1980, 1, 1))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medAktørId(aktørId)
            .medForetrukketSpråk(Språkkode.nb).build();
        when(tpsTjenesteMock.hentBrukerForAktør(any(AktørId.class))).thenReturn(Optional.of(personinfo));

        //Mock BersonTjeneste
        NavBruker navBruker = NavBruker.opprettNy(personinfo);
        when(brukerTjenesteMock.hentEllerOpprettFraAktorId(any(Personinfo.class))).thenReturn(navBruker);

        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        var opprettGSakTjeneste = new OpprettGSakTjeneste(null, null);
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
