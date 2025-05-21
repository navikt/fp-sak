package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class SøknadRepositoryTest extends EntityManagerAwareTest {

    private SøknadRepository søknadRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        søknadRepository = new SøknadRepository(entityManager, behandlingRepository);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
    }

    @Test
    void skal_finne_endringssøknad_for_behandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, behandlingRepository.taSkriveLås(behandling2));

        var fhBuilder = familieHendelseRepository.opprettBuilderForSøknad(behandling.getId());
        fhBuilder.medFødselsDato(LocalDate.now()).medAntallBarn(1);
        familieHendelseRepository.lagreSøknadHendelse(behandling.getId(), fhBuilder);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), behandling2.getId());

        var søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        var søknad2 = opprettSøknad(true);
        søknadRepository.lagreOgFlush(behandling2, søknad2);

        // Act
        var endringssøknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        var endringssøknad2 = søknadRepository.hentSøknadHvisEksisterer(behandling2.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad2).isPresent();
        assertThat(endringssøknad.get()).isNotEqualTo(endringssøknad2.get());
    }

    @Test
    void skal_ikke_finne_endringssøknad_for_behandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var fhBuilder = familieHendelseRepository.opprettBuilderForSøknad(behandling.getId());
        fhBuilder.medFødselsDato(LocalDate.now()).medAntallBarn(1);
        familieHendelseRepository.lagreSøknadHendelse(behandling.getId(), fhBuilder);

        var søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Act
        var endringssøknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad.get().erEndringssøknad()).isFalse();
    }

    @Test
    void skal_kopiere_søknadsgrunnlaget_fra_behandling1_til_behandling2() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        var behandling1 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling1, behandlingRepository.taSkriveLås(behandling1));
        var søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling1, søknad);

        var behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, behandlingRepository.taSkriveLås(behandling2));

        // Act
        søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling1, behandling2);

        // Assert
        var søknadEntitet = søknadRepository.hentSøknadHvisEksisterer(behandling2.getId());
        assertThat(søknadEntitet).isPresent();
    }

    private SøknadEntitet opprettSøknad(boolean erEndringssøknad) {
        return new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now().minusDays(1))
            .medErEndringssøknad(erEndringssøknad)
            .build();
    }

}
