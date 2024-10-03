package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class MedlemskapRepositoryTest extends EntityManagerAwareTest {

    private MedlemskapRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repository = new MedlemskapRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_lagre_og_hente() {
        var behandling = lagBehandling();
        var oppgittDato = LocalDate.now();

        repository.lagreOppgittTilkytning(behandling.getId(),
            new MedlemskapOppgittTilknytningEntitet.Builder().medOppgittDato(oppgittDato).medOppholdNå(true).build());

        var medlemskapAggregat = repository.hentMedlemskap(behandling.getId());
        assertThat(medlemskapAggregat).isPresent();
        assertThat(medlemskapAggregat.get().getOppgittTilknytning()).isPresent();
        assertThat(medlemskapAggregat.get().getOppgittTilknytning().get().getOppgittDato()).isEqualTo(oppgittDato);
        assertThat(medlemskapAggregat.get().getOppgittTilknytning().get().isOppholdNå()).isTrue();
    }

    private Behandling lagBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
