package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
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
    public void skal_lagre_verge_grunnlag() {
        Behandling behandling = opprettBehandling();

        NavBruker bruker = NavBruker.opprettNyNB(AktørId.dummy());

        VergeBuilder vergeBuilder = new VergeBuilder()
                .medVergeType(VergeType.BARN)
                .medBruker(bruker);

        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        var resultat = vergeRepository.hentAggregat(behandling.getId());
        assertThat(resultat).isPresent();
    }

    private Behandling opprettBehandling() {
        Fagsak fagsak = opprettFagsak();
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        NavBruker bruker = NavBruker.opprettNyNB(AktørId.dummy());

        // Opprett fagsak
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, new Saksnummer("1000"));
        repository.lagre(bruker);
        repository.lagre(fagsak);
        repository.flush();
        return fagsak;
    }
}
