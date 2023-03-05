package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class TilbakekrevingRepositoryTest extends EntityManagerAwareTest {

    private TilbakekrevingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TilbakekrevingRepository(getEntityManager());
    }

    @Test
    void skal_gi_empty_når_det_hentes_for_behandling_som_ikke_har_tilbakekrevingsvalg() {
        var behandling = Mockito.mock(Behandling.class);
        when(behandling.getId()).thenReturn(2L);
        assertThat(repository.hent(2L)).isEmpty();
    }

    @Test
    void skal_lagre_og_hente_tilbakekrevingsvalg() {
        var behandling = opprettBehandling();

        var valg = TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, "Varsel");
        repository.lagre(behandling, valg);

        var lagretResultat = repository.hent(behandling.getId());
        assertThat(lagretResultat).isPresent();
        var resultat = lagretResultat.get();
        assertThat(resultat.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
        assertThat(resultat.getErTilbakekrevingVilkårOppfylt()).isNull();
        assertThat(resultat.getGrunnerTilReduksjon()).isNull();
    }

    @Test
    void skal_oppdatere_tilbakekrevingsvalg() {
        var behandling = opprettBehandling();

        var valg1 = TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, "Varseltekst");
        repository.lagre(behandling, valg1);

        var valg2 = TilbakekrevingValg.medMulighetForInntrekk(true, false, TilbakekrevingVidereBehandling.INNTREKK);
        repository.lagre(behandling, valg2);

        var lagretResultat = repository.hent(behandling.getId());
        assertThat(lagretResultat).isPresent();
        var resultat = lagretResultat.get();
        assertThat(resultat.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK);
        assertThat(resultat.getErTilbakekrevingVilkårOppfylt()).isTrue();
        assertThat(resultat.getGrunnerTilReduksjon()).isFalse();
    }

    @Test
    void lagrer_tilbakekreving_inntrekk() {
        // Arrange
        var behandling = opprettBehandling();

        // Act
        repository.lagre(behandling, true);
        var tilbakekrevingInntrekkEntitet = repository.hentTilbakekrevingInntrekk(behandling.getId());

        // Assert
        assertThat(tilbakekrevingInntrekkEntitet).hasValueSatisfying(TilbakekrevingInntrekkEntitet::isAvslåttInntrekk);
    }

    @Test
    void oppdaterer_tilbakekreving_inntrekk() {
        // Arrange
        var behandling = opprettBehandling();

        // Act
        repository.lagre(behandling, true);
        repository.lagre(behandling, false);
        var tilbakekrevingInntrekkEntitet = repository.hentTilbakekrevingInntrekk(behandling.getId());

        // Assert
        assertThat(tilbakekrevingInntrekkEntitet).isPresent();
        assertThat(tilbakekrevingInntrekkEntitet.get().isAvslåttInntrekk()).isFalse();
    }

    private Behandling opprettBehandling() {
        return new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

}
