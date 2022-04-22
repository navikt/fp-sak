package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagUttakResultatPlanTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class RevurderingBehandlingsresultatutlederHarEtablertYtelseTest {

    private BeregningRevurderingTestUtil revurderingTestUtil;

    private BehandlingRepositoryProvider repositoryProvider;
    private SvangerskapspengerUttakResultatRepository uttakRepository;

    private RevurderingBehandlingsresultatutleder resultatUtleder;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        revurderingTestUtil = new BeregningRevurderingTestUtil(repositoryProvider);
        uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        resultatUtleder = new RevurderingBehandlingsresultatutleder(repositoryProvider,
            new SvangerskapspengerUttakResultatRepository(entityManager), null,
                null, null, null);
    }

    private Behandling opprettBehandling() {
        var behandlingSomSkalRevurderes = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
                .lagre(repositoryProvider);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        return behandlingSomSkalRevurderes;
    }

    @Test
    public void skal_gi_etablert_ytelse_med_tom_for_innvilget_periode_etter_dagens_dato() {
        var behandlingSomSkalRevurderes = opprettBehandling();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(
                behandlingSomSkalRevurderes,
                Collections.singletonList(
                        new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))));

        var etablertYtelse = resultatUtleder.harEtablertYtelse(null, true,
                new UttakResultatHolderSVP(Optional.of(uttakResultatOriginal), null));

        assertThat(etablertYtelse).isTrue();
    }

    @Test
    public void skal_ikke_gi_etablert_ytelse_hvis_finnesInnvilgetIkkeOpphørtVedtak_er_false() {
        var behandlingSomSkalRevurderes = opprettBehandling();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(
                behandlingSomSkalRevurderes,
                Collections.singletonList(
                        new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))));
        var finnesInnvilgetIkkeOpphørtVedtak = false;
        var etablertYtelse = resultatUtleder.harEtablertYtelse(null, finnesInnvilgetIkkeOpphørtVedtak,
                new UttakResultatHolderSVP(Optional.of(uttakResultatOriginal), null));

        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_gi_etablert_ytelse_med_tom_for_innvilget_periode_på_dagens_dato() {
        var behandlingSomSkalRevurderes = opprettBehandling();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(
                behandlingSomSkalRevurderes,
                Collections.singletonList(new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now())));
        var etablertYtelse = resultatUtleder.harEtablertYtelse(null, true,
                new UttakResultatHolderSVP(Optional.of(uttakResultatOriginal), null));

        assertThat(etablertYtelse).isTrue();
    }

    @Test
    public void skal_ikkje_gi_etablert_ytelse_med_tom_for_avslått_periode_etter_dagens_dato() {
        var behandlingSomSkalRevurderes = opprettBehandling();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(
                behandlingSomSkalRevurderes,
                Collections.singletonList(new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().plusDays(5))));
        var etablertYtelse = resultatUtleder.harEtablertYtelse(null, false,
                new UttakResultatHolderSVP(Optional.of(uttakResultatOriginal), null));

        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_ikkje_gi_etablert_ytelse_med_tom_for_innvilget_periode_før_dagens_dato() {
        var behandlingSomSkalRevurderes = opprettBehandling();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(
                behandlingSomSkalRevurderes,
                Collections.singletonList(
                        new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))));
        var etablertYtelse = resultatUtleder.harEtablertYtelse(null, true,
                new UttakResultatHolderSVP(Optional.of(uttakResultatOriginal), null));

        assertThat(etablertYtelse).isFalse();
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
            List<LocalDateInterval> perioder) {
        var uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(
                behandling, perioder,
                Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET),
                Collections.nCopies(perioder.size(), PeriodeIkkeOppfyltÅrsak.INGEN),
                Collections.nCopies(perioder.size(), 100));
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;

    }
}
