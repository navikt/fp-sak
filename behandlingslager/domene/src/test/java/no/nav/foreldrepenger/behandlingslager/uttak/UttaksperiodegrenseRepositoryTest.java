package no.nav.foreldrepenger.behandlingslager.uttak;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class UttaksperiodegrenseRepositoryTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private final UttaksperiodegrenseRepository uttaksperiodegrenseRepository = new UttaksperiodegrenseRepository(repoRule.getEntityManager());
    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() {
        var behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        new BehandlingsresultatRepository(repoRule.getEntityManager()).lagre(behandling.getId(), behandlingsresultat);
    }

    @Test
    public void skal_lagre_og_sette_uttaksperiodegrense_inaktiv() {
        // Arrange
        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
            .medMottattDato(LocalDate.now())
            .medFørsteLovligeUttaksdag((LocalDate.now().minusDays(5)))
            .build();

        // Act
        uttaksperiodegrenseRepository.lagre(behandlingsresultat.getBehandlingId(), uttaksperiodegrense);

        // Assert
        Uttaksperiodegrense uttaksperiodegrenseFør = uttaksperiodegrenseRepository.hent(behandlingsresultat.getBehandlingId());
        assertThat(uttaksperiodegrense).isEqualTo(uttaksperiodegrenseFør);

        // Act
        uttaksperiodegrenseRepository.ryddUttaksperiodegrense(behandlingsresultat.getBehandlingId());

        // Assert
        assertThat(uttaksperiodegrenseRepository.hentHvisEksisterer(behandlingsresultat.getBehandlingId())).isEmpty();
    }
}
