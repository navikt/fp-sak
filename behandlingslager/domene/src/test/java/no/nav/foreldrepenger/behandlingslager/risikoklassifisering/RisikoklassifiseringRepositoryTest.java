package no.nav.foreldrepenger.behandlingslager.risikoklassifisering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
public class RisikoklassifiseringRepositoryTest {

    private RisikoklassifiseringRepository risikorepository;
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        this.entityManager = entityManager;
        behandlingRepository = new BehandlingRepository(entityManager);
        risikorepository = new RisikoklassifiseringRepository(entityManager);
    }

    @Test
    public void skal_lagre_og_hente_klassifisering() {
        // Arrange
        var behandling = opprettBehandling();
        var risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.IKKE_KLASSIFISERT);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        var persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);
    }

    @Test
    public void skal_ikke_hente_klassifisering_gitt_ugyldig_behandlingId() {
        // Act
        var risikoklassifiseringEntitet = risikorepository.hentRisikoklassifiseringForBehandling(123);

        // Assert
        assertThat(risikoklassifiseringEntitet).isNotPresent();
    }

    @Test
    public void skal_deakivere_gammelt_grunnlag_når_det_eksiterer() {
        // Arrange
        var behandling = opprettBehandling();
        var risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.IKKE_KLASSIFISERT);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        var persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Arrange
        var nyRisikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY);

        // Act
        risikorepository.lagreRisikoklassifisering(nyRisikoklassifiseringEntitet, behandling.getId());
        var nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        assertThat(nyPersistertKlassifisering.get()).isEqualTo(nyRisikoklassifiseringEntitet);
    }

    @Test
    public void skal_oppdatere_klassifisering_med_vurdering_fra_saksbehandler_når_gammel_klassifisering_ikke_var_vurdert() {
        // Arrange
        var behandling = opprettBehandling();
        var risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        var persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INNVILGET_REDUSERT, behandling.getId());
        var nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        var entitet = nyPersistertKlassifisering.get();
        assertThat(entitet.getKontrollresultat()).isEqualTo(risikoklassifiseringEntitet.getKontrollresultat());
        assertThat(entitet.getBehandlingId()).isEqualTo(risikoklassifiseringEntitet.getBehandlingId());
        assertThat(entitet.getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INNVILGET_REDUSERT);
    }

    @Test
    public void skal_oppdatere_klassifisering_med_vurdering_fra_saksbehandler_når_gammel_klassifisering_var_vurdert() {
        // Arrange
        var behandling = opprettBehandling();
        var risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY, FaresignalVurdering.INNVIRKNING);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        var persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INGEN_INNVIRKNING, behandling.getId());
        var nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        var entitet = nyPersistertKlassifisering.get();
        assertThat(entitet.getKontrollresultat()).isEqualTo(risikoklassifiseringEntitet.getKontrollresultat());
        assertThat(entitet.getBehandlingId()).isEqualTo(risikoklassifiseringEntitet.getBehandlingId());
        assertThat(entitet.getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INGEN_INNVIRKNING);
    }

    @Test
    public void skal_feile_under_oppdatering_når_gammelt_grunnlag_ikke_finnes() {
        // Arrange
        var behandling = opprettBehandling();

        // Act
        assertThrows(IllegalStateException.class,
            () -> risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INGEN_INNVIRKNING, behandling.getId()));
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Long behandlingId, Kontrollresultat kontrollresultat, FaresignalVurdering faresignalvurdering) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).medFaresignalVurdering(faresignalvurdering).buildFor(behandlingId);
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Long behandlingId, Kontrollresultat kontrollresultat) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).buildFor(behandlingId);
    }

    private Behandling opprettBehandling() {
        var fagsak = opprettFagsak();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        var bruker = NavBruker.opprettNyNB(AktørId.dummy());

        // Opprett fagsak
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, new Saksnummer("1000"));
        entityManager.persist(bruker);
        entityManager.persist(fagsak);
        entityManager.flush();
        return fagsak;
    }

}
