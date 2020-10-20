package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class SøknadRepositoryTest extends EntityManagerAwareTest {

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
    public void skal_finne_endringssøknad_for_behandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, behandlingRepository.taSkriveLås(behandling2));

        FamilieHendelseBuilder fhBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        fhBuilder.medFødselsDato(LocalDate.now()).medAntallBarn(1);
        familieHendelseRepository.lagre(behandling, fhBuilder);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), behandling2.getId());

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        SøknadEntitet søknad2 = opprettSøknad(true);
        søknadRepository.lagreOgFlush(behandling2, søknad2);

        // Act
        Optional<SøknadEntitet> endringssøknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        Optional<SøknadEntitet> endringssøknad2 = søknadRepository.hentSøknadHvisEksisterer(behandling2.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad2).isPresent();
        assertThat(endringssøknad.get()).isNotEqualTo(endringssøknad2.get());
    }

    @Test
    public void skal_ikke_finne_endringssøknad_for_behandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        FamilieHendelseBuilder fhBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        fhBuilder.medFødselsDato(LocalDate.now()).medAntallBarn(1);
        familieHendelseRepository.lagre(behandling, fhBuilder);

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Act
        Optional<SøknadEntitet> endringssøknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad.get().erEndringssøknad()).isFalse();
    }

    @Test
    public void skal_kopiere_søknadsgrunnlaget_fra_behandling1_til_behandling2() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling1, behandlingRepository.taSkriveLås(behandling1));
        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling1, søknad);

        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, behandlingRepository.taSkriveLås(behandling2));

        // Act
        søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling1, behandling2);

        // Assert
        Optional<SøknadEntitet> søknadEntitet = søknadRepository.hentSøknadHvisEksisterer(behandling2.getId());
        assertThat(søknadEntitet).isPresent();
    }

    private SøknadEntitet opprettSøknad(boolean erEndringssøknad) {
        return new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now().minusDays(1))
            .medErEndringssøknad(erEndringssøknad)
            .build();
    }

}
