package no.nav.foreldrepenger.behandlingslager.uttak;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class UttaksperiodegrenseRepositoryTest extends EntityManagerAwareTest {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        uttaksperiodegrenseRepository = new UttaksperiodegrenseRepository(entityManager);
    }

    @Test
    public void skal_lagre_og_sette_uttaksperiodegrense_inaktiv() {
        // Arrange
        var behandlingsresultat = lagBehandlingMedResultat();
        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
            .medMottattDato(LocalDate.now())
            .medFørsteLovligeUttaksdag((LocalDate.now().minusDays(5)))
            .build();

        // Act
        var behandlingId = behandlingsresultat.getBehandlingId();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        // Assert
        Uttaksperiodegrense uttaksperiodegrenseFør = uttaksperiodegrenseRepository.hent(behandlingId);
        assertThat(uttaksperiodegrense).isEqualTo(uttaksperiodegrenseFør);

        // Act
        uttaksperiodegrenseRepository.ryddUttaksperiodegrense(behandlingId);

        // Assert
        assertThat(uttaksperiodegrenseRepository.hentHvisEksisterer(behandlingId)).isEmpty();
    }

    private Behandlingsresultat lagBehandlingMedResultat() {
        var entityManager = getEntityManager();
        var behandling = new BasicBehandlingBuilder(entityManager)
            .opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        new BehandlingsresultatRepository(entityManager).lagre(behandling.getId(), behandlingsresultat);
        return behandlingsresultat;
    }
}
