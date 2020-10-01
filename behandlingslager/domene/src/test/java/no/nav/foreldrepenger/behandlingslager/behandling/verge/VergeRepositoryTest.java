package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class VergeRepositoryTest {

    private static final LocalDate GYLDIG_FOM = LocalDate.now().minusYears(1);
    private static final LocalDate GYLDIG_TOM = LocalDate.now().plusYears(3);
    private static final String ORGANISASJONSNUMMER = "987654321";
    private static final String VERGE_ORGNAVN = "Advokat Advokatsen";

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();

    private VergeRepository vergeRepository = new VergeRepository(repositoryRule.getEntityManager(),
            new BehandlingLåsRepository(repositoryRule.getEntityManager()));
    private FagsakRepository fagsakRepository = new FagsakRepository(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = new BehandlingRepository(repositoryRule.getEntityManager());

    @Test
    public void skal_lagre_og_hente_ut_vergeinformasjon() {
        // Arrange
        NavBruker bruker = opprettBruker();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker);
        fagsakRepository.opprettNy(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        VergeOrganisasjonEntitet vergeOrganisasjon = new VergeOrganisasjonBuilder()
                .medNavn(VERGE_ORGNAVN)
                .medOrganisasjonsnummer(ORGANISASJONSNUMMER)
                .build();
        VergeBuilder vergeBuilder = new VergeBuilder()
                .medVergeType(VergeType.BARN)
                .gyldigPeriode(GYLDIG_FOM, GYLDIG_TOM)
                .medVergeOrganisasjon(vergeOrganisasjon);

        // Act
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        // Assert
        Optional<VergeAggregat> vergeAggregat = vergeRepository.hentAggregat(behandling.getId());
        assertThat(vergeAggregat.isPresent()).isTrue();
        assertThat(vergeAggregat.get().getVerge().isPresent()).isTrue();
        VergeEntitet verge = vergeAggregat.get().getVerge().get();
        assertThat(verge.getGyldigFom()).isEqualTo(GYLDIG_FOM);
        assertThat(verge.getGyldigTom()).isEqualTo(GYLDIG_TOM);
        assertThat(verge.getVergeType()).isEqualTo(VergeType.BARN);
        assertThat(verge.getVergeOrganisasjon().isPresent()).isTrue();
        VergeOrganisasjonEntitet vergeOrg = verge.getVergeOrganisasjon().get();
        assertThat(vergeOrg.getVerge()).isEqualTo(verge);
        assertThat(vergeOrg.getOrganisasjonsnummer()).isEqualTo(ORGANISASJONSNUMMER);
        assertThat(vergeOrg.getNavn()).isEqualTo(VERGE_ORGNAVN);
    }

    @Test
    public void skal_kopiere_vergegrunnlag_fra_tidligere_behandling() {
        // Arrange
        NavBruker bruker = opprettBruker();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker);
        fagsakRepository.opprettNy(fagsak);
        Behandling gammelBehandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(gammelBehandling, behandlingRepository.taSkriveLås(gammelBehandling));
        Behandling nyBehandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, behandlingRepository.taSkriveLås(nyBehandling));

        VergeOrganisasjonEntitet vergeOrganisasjon = new VergeOrganisasjonBuilder()
                .medNavn(VERGE_ORGNAVN)
                .medOrganisasjonsnummer(ORGANISASJONSNUMMER)
                .build();
        VergeBuilder vergeBuilder = new VergeBuilder()
                .medVergeType(VergeType.BARN)
                .gyldigPeriode(GYLDIG_FOM, GYLDIG_TOM)
                .medVergeOrganisasjon(vergeOrganisasjon);
        vergeRepository.lagreOgFlush(gammelBehandling.getId(), vergeBuilder);

        // Act
        vergeRepository.kopierGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling.getId());

        // Assert
        Optional<VergeAggregat> vergeAggregat = vergeRepository.hentAggregat(nyBehandling.getId());
        assertThat(vergeAggregat.isPresent()).isTrue();
        assertThat(vergeAggregat.get().getVerge().isPresent()).isTrue();
        VergeEntitet verge = vergeAggregat.get().getVerge().get();
        assertThat(verge).isEqualTo(vergeRepository.hentAggregat(gammelBehandling.getId()).get().getVerge().get());
        assertThat(verge.getVergeOrganisasjon().isPresent()).isTrue();
    }

    private NavBruker opprettBruker() {
        Personinfo personinfo = new Personinfo.Builder()
                .medNavn("Mindreårig Jente")
                .medAktørId(AktørId.dummy())
                .medFødselsdato(LocalDate.now().minusYears(15))
                .medLandkode(Landkoder.NOR)
                .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
                .medPersonIdent(new PersonIdent("12345678901"))
                .medForetrukketSpråk(Språkkode.NB)
                .build();
        NavBruker navBruker = NavBruker.opprettNy(personinfo);
        repositoryRule.getRepository().lagre(navBruker);
        return navBruker;
    }
}
