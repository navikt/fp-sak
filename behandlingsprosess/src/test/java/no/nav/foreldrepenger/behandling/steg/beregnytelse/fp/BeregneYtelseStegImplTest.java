package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.steg.beregnytelse.BeregneYtelseStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.BeregnYtelseTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.util.Tuple;

@CdiDbAwareTest
public class BeregneYtelseStegImplTest {
    private static final String ORGNR = "000000000";
    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Inject
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private FpUttakRepository fpUttakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;

    @FagsakYtelseTypeRef("FP")
    @Inject
    private FinnEndringsdatoBeregningsresultatTjeneste finnEndringsdatoBeregningsresultatTjeneste;

    @Mock
    private BeregnYtelseTjeneste beregnYtelseTjeneste;
    @Mock
    private BeregnFeriepengerTjeneste beregnFeriepengerTjeneste;

    private BeregneYtelseStegImpl steg;

    @BeforeEach
    void setup() {
        steg = new BeregneYtelseStegImpl(behandlingRepository,
                beregningsresultatRepository,
                new UnitTestLookupInstanceImpl<>(beregnFeriepengerTjeneste),
                new UnitTestLookupInstanceImpl<>(finnEndringsdatoBeregningsresultatTjeneste),
                beregnYtelseTjeneste);
    }

    private BeregningsresultatEntitet opprettBeregningsresultat() {
        return BeregningsresultatEntitet.builder()
                .medRegelInput("regelInput")
                .medRegelSporing("regelSporing")
                .build();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalUtførStegForFørstegangsbehandling() {
        // Arrange

        when(beregnYtelseTjeneste.beregnYtelse(ArgumentMatchers.any())).thenReturn(opprettBeregningsresultat());

        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(true, true);
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();

        // Act
        BehandleStegResultat stegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<BeregningsresultatEntitet> beregningsresultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(beregningsresultat).hasValueSatisfying(resultat -> {
            assertThat(resultat).isNotNull();
            assertThat(resultat.getRegelInput()).as("regelInput").isEqualTo("regelInput");
            assertThat(resultat.getRegelSporing()).as("regelSporing").isEqualTo("regelSporing");
        });
    }

    @Test
    public void skalSletteBeregningsresultatFPVedTilbakehopp() {
        // Arrange
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(true, true);
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();
        beregningsresultatRepository.lagre(behandling, opprettBeregningsresultat());

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Assert
        Optional<BeregningsresultatEntitet> resultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(resultat).isNotPresent();
    }

    private Tuple<Behandling, BehandlingskontrollKontekst> byggGrunnlag(boolean medBeregningsgrunnlag, boolean medUttaksPlanResultat) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBruker(AKTØR_ID, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build())
                .medDefaultFordeling(LocalDate.now());

        var behandling = lagre(scenario);

        if (medBeregningsgrunnlag) {
            medBeregningsgrunnlag(behandling);
        }

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        if (medUttaksPlanResultat) {
            byggUttakPlanResultat(behandling);
        }
        return new Tuple<>(behandling, kontekst);
    }

    private void medBeregningsgrunnlag(Behandling behandling) {
        var beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(LocalDate.now())
                .medGrunnbeløp(BigDecimal.valueOf(90000));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagBuilder.build();
        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPRETTET);
    }

    private void byggUttakPlanResultat(Behandling behandling) {
        var periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();

        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
                .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), InternArbeidsforholdRef.nyRef())
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
                .medTrekkonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
                .medTrekkdager(new Trekkdager(15))
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .build();

        periode.leggTilAktivitet(periodeAktivitet);

        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), perioder);
    }
}
