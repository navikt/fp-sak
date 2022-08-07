package no.nav.foreldrepenger.behandlingslager.uttak;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

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
        var uttaksperiodegrense = new Uttaksperiodegrense(LocalDate.now());

        // Act
        var behandlingId = behandlingsresultat.getBehandlingId();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        // Assert
        var uttaksperiodegrenseFør = uttaksperiodegrenseRepository.hent(behandlingId);
        assertThat(uttaksperiodegrense).isEqualTo(uttaksperiodegrenseFør);

        // Act
        uttaksperiodegrenseRepository.ryddUttaksperiodegrense(behandlingId);

        // Assert
        assertThat(uttaksperiodegrenseRepository.hentHvisEksisterer(behandlingId)).isEmpty();
    }

    @Test
    public void ikke_lagre_duplikate_aktive_hvis_ingen_endring() {
        var behandlingsresultat = lagBehandlingMedResultat();
        var uttaksperiodegrense1 = new Uttaksperiodegrense(LocalDate.now());
        var uttaksperiodegrense2 = new Uttaksperiodegrense(uttaksperiodegrense1.getMottattDato());

        var behandlingId = behandlingsresultat.getBehandlingId();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense1);
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense2);

        // Kaster exception ved duplikat
        assertThatCode(() -> uttaksperiodegrenseRepository.hent(behandlingId)).doesNotThrowAnyException();
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
