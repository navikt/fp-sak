package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.steg.beregnytelse.BeregneYtelseStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.BeregnYtelseTjeneste;

@CdiDbAwareTest
class BeregneYtelseStegImplTest {
    private static final String ORGNR = "000000000";
    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private FpUttakRepository fpUttakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;

    @Mock
    private BeregnYtelseTjeneste beregnYtelseTjeneste;

    private BeregneYtelseStegImpl steg;

    @BeforeEach
    void setup() {
        steg = new BeregneYtelseStegImpl(behandlingRepository,
                beregningsresultatRepository,
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
    void skalUtførStegForFørstegangsbehandling() {
        // Arrange

        when(beregnYtelseTjeneste.beregnYtelse(ArgumentMatchers.any())).thenReturn(opprettBeregningsresultat());

        var behandling = byggGrunnlag();
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        // Act
        var stegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);

        var beregningsresultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(beregningsresultat).hasValueSatisfying(resultat -> {
            assertThat(resultat).isNotNull();
            assertThat(resultat.getRegelInput()).as("regelInput").isEqualTo("regelInput");
            assertThat(resultat.getRegelSporing()).as("regelSporing").isEqualTo("regelSporing");
        });
    }

    @Test
    void skalSletteBeregningsresultatFPVedTilbakehopp() {
        // Arrange
        var behandling = byggGrunnlag();
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        beregningsresultatRepository.lagre(behandling, opprettBeregningsresultat());

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Assert
        var resultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(resultat).isNotPresent();
    }

    private Behandling byggGrunnlag() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBruker(AKTØR_ID, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build())
                .medDefaultFordeling(LocalDate.now());

        var behandling = lagre(scenario);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        byggUttakPlanResultat(behandling);
        return behandling;
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
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
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
