package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VergeGrunnlagEntitetTest extends EntityManagerAwareTest {
    private Repository repository;
    private VergeRepository vergeRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void init() {
        var repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        repository = new Repository(getEntityManager());
        vergeRepository = new VergeRepository(getEntityManager(), repositoryProvider.getBehandlingLåsRepository());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Test
    public void skal_lagre_verge_grunnlag() throws Exception {
        Behandling behandling = opprettBehandling();

        Personinfo.Builder builder = new Personinfo.Builder();
        builder.medAktørId(AktørId.dummy())
                .medPersonIdent(new PersonIdent("12345678901"))
                .medNavn("Kari Verge")
                .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                .medForetrukketSpråk(Språkkode.NB)
                .medNavBrukerKjønn(KVINNE);

        Personinfo personinfo = builder.build();
        NavBruker bruker = NavBruker.opprettNy(personinfo);

        VergeBuilder vergeBuilder = new VergeBuilder()
                .medVergeType(VergeType.BARN)
                .medBruker(bruker);

        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        List<VergeGrunnlagEntitet> resultat = repository.hentAlle(VergeGrunnlagEntitet.class);
        assertThat(resultat).hasSize(1);
    }

    private Behandling opprettBehandling() {
        Fagsak fagsak = opprettFagsak();
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        NavBruker bruker = NavBruker.opprettNy(
                new Personinfo.Builder()
                        .medAktørId(AktørId.dummy())
                        .medPersonIdent(new PersonIdent("12345678901"))
                        .medNavn("Kari Nordmann")
                        .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                        .medForetrukketSpråk(Språkkode.NB)
                        .medNavBrukerKjønn(KVINNE)
                        .build());

        // Opprett fagsak
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, new Saksnummer("1000"));
        repository.lagre(bruker);
        repository.lagre(fagsak);
        repository.flush();
        return fagsak;
    }
}
