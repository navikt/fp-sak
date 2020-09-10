package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagUttakResultatPlanTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class HarEtablertYtelseImplTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndring;

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private SvangerskapspengerUttakResultatRepository uttakRepository;

    private Behandling behandlingSomSkalRevurderes;
    private RevurderingBehandlingsresultatutleder resultatUtleder;

    @Before
    public void setUp() {
        uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        resultatUtleder = new RevurderingBehandlingsresultatutleder(repositoryProvider, null,
            null, null,null, null);
    }

    @Test
    public void skal_gi_etablert_ytelse_med_tom_for_innvilget_periode_etter_dagens_dato() {
        // Arrange
        LocalDate dagensDato = LocalDate.now();

        SvangerskapspengerUttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            Collections.singletonList(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.plusDays(10)))
        );

        // Act
        boolean etablertYtelse = resultatUtleder.harEtablertYtelse( null,true,
            new UttakResultatHolderSVP(Optional.of(uttakResultatOriginal), null));

        // Assert
        assertThat(etablertYtelse).isTrue();
    }

    @Test
    public void skal_ikke_gi_etablert_ytelse_hvis_finnesInnvilgetIkkeOpphørtVedtak_er_false() {
        // Arrange
        LocalDate dagensDato = LocalDate.now();

        SvangerskapspengerUttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            Collections.singletonList(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.plusDays(10)))
        );
        boolean finnesInnvilgetIkkeOpphørtVedtak = false;
        // Act
        boolean etablertYtelse = resultatUtleder.harEtablertYtelse(null, finnesInnvilgetIkkeOpphørtVedtak,
            new UttakResultatHolderSVP( Optional.of(uttakResultatOriginal), null));

        // Assert
        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_gi_etablert_ytelse_med_tom_for_innvilget_periode_på_dagens_dato() {
        // Arrange
        LocalDate dagensDato = LocalDate.now();

        SvangerskapspengerUttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            Collections.singletonList(new LocalDateInterval(dagensDato.minusDays(10), dagensDato))
        );
        // Act
        boolean etablertYtelse = resultatUtleder.harEtablertYtelse(null, true,
            new UttakResultatHolderSVP( Optional.of(uttakResultatOriginal), null));

        // Assert
        assertThat(etablertYtelse).isTrue();
    }

    @Test
    public void skal_ikkje_gi_etablert_ytelse_med_tom_for_avslått_periode_etter_dagens_dato() {
        // Arrange
        LocalDate dagensDato = LocalDate.now();

        SvangerskapspengerUttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            Collections.singletonList(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.plusDays(5)))
        );
        // Act
        boolean etablertYtelse = resultatUtleder.harEtablertYtelse(null, false,
            new UttakResultatHolderSVP( Optional.of(uttakResultatOriginal), null));

        // Assert
        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_ikkje_gi_etablert_ytelse_med_tom_for_innvilget_periode_før_dagens_dato() {
        // Arrange
        LocalDate dagensDato = LocalDate.now();

        SvangerskapspengerUttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            Collections.singletonList(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.minusDays(5)))
        );
        // Act
        boolean etablertYtelse = resultatUtleder.harEtablertYtelse(null, true,
            new UttakResultatHolderSVP( Optional.of(uttakResultatOriginal), null));

        // Assert
        assertThat(etablertYtelse).isFalse();
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder) {
        SvangerskapspengerUttakResultatEntitet uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(behandling, perioder,
            Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET),
            Collections.nCopies(perioder.size(), PeriodeIkkeOppfyltÅrsak.INGEN),
            Collections.nCopies(perioder.size(), 100));
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;

    }
}

