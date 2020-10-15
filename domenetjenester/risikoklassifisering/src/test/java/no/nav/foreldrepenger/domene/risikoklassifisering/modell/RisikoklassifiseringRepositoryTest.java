package no.nav.foreldrepenger.domene.risikoklassifisering.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class RisikoklassifiseringRepositoryTest {
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final RisikoklassifiseringRepository risikorepository = new RisikoklassifiseringRepository(entityManager);

    private Repository repository = repoRule.getRepository();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Test
    public void skal_lagre_og_hente_klassifisering() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.IKKE_KLASSIFISERT);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);
    }

    @Test
    public void skal_ikke_hente_klassifisering_gitt_ugyldig_behandlingId() {
        // Act
        Optional<RisikoklassifiseringEntitet> risikoklassifiseringEntitet = risikorepository.hentRisikoklassifiseringForBehandling(123);

        // Assert
        assertThat(risikoklassifiseringEntitet).isNotPresent();
    }

    @Test
    public void skal_deakivere_gammelt_grunnlag_når_det_eksiterer() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.IKKE_KLASSIFISERT);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Arrange
        RisikoklassifiseringEntitet nyRisikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY);

        // Act
        risikorepository.lagreRisikoklassifisering(nyRisikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        assertThat(nyPersistertKlassifisering.get()).isEqualTo(nyRisikoklassifiseringEntitet);
    }

    @Test
    public void skal_oppdatere_klassifisering_med_vurdering_fra_saksbehandler_når_gammel_klassifisering_ikke_var_vurdert() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INNVIRKNING, behandling.getId());
        Optional<RisikoklassifiseringEntitet> nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        RisikoklassifiseringEntitet entitet = nyPersistertKlassifisering.get();
        assertThat(entitet.getKontrollresultat()).isEqualTo(risikoklassifiseringEntitet.getKontrollresultat());
        assertThat(entitet.getBehandlingId()).isEqualTo(risikoklassifiseringEntitet.getBehandlingId());
        assertThat(entitet.getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INNVIRKNING);
    }

    @Test
    public void skal_oppdatere_klassifisering_med_vurdering_fra_saksbehandler_når_gammel_klassifisering_var_vurdert() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY, FaresignalVurdering.INNVIRKNING);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INGEN_INNVIRKNING, behandling.getId());
        Optional<RisikoklassifiseringEntitet> nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        RisikoklassifiseringEntitet entitet = nyPersistertKlassifisering.get();
        assertThat(entitet.getKontrollresultat()).isEqualTo(risikoklassifiseringEntitet.getKontrollresultat());
        assertThat(entitet.getBehandlingId()).isEqualTo(risikoklassifiseringEntitet.getBehandlingId());
        assertThat(entitet.getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INGEN_INNVIRKNING);
    }

    @Test( expected = IllegalStateException.class)
    public void skal_feile_under_oppdatering_når_gammelt_grunnlag_ikke_finnes() {
        // Arrange
        Behandling behandling = opprettBehandling();

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INGEN_INNVIRKNING, behandling.getId());
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Long behandlingId, Kontrollresultat kontrollresultat, FaresignalVurdering faresignalvurdering) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).medFaresignalVurdering(faresignalvurdering).buildFor(behandlingId);
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Long behandlingId, Kontrollresultat kontrollresultat) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).buildFor(behandlingId);
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
