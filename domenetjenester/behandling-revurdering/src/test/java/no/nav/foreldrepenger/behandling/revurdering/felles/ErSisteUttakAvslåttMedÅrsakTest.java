package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingTjenesteImpl;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.UttakResultatHolderFP;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.EndringsdatoRevurderingUtlederImpl;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class ErSisteUttakAvslåttMedÅrsakTest {
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = KUNSTIG_ORG;

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndring;

    @Inject
    private VergeRepository vergeRepository;

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(
        repoRule.getEntityManager());
    private FpUttakRepository fpUttakRepository;
    private EndringsdatoRevurderingUtlederImpl endringsdatoRevurderingUtlederImpl = mock(
        EndringsdatoRevurderingUtlederImpl.class);

    private Behandling revurdering;

    @Before
    public void setUp() {
        fpUttakRepository = repositoryProvider.getFpUttakRepository();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE,
            BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        Behandling behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository()
            .lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(),
                false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider
        );
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        var revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste,
            iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));
        LocalDate endringsdato = LocalDate.now().minusMonths(3);
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
    }

    @Test
    public void skal_teste_at_alle_opphørsårsaker_gir_opphør_på_behandlingen() {
        IkkeOppfyltÅrsak.opphørsAvslagÅrsaker().forEach(opphørsårsak -> {
            // Arrange
            UttakResultatEntitet uttakresultatRevurdering = lagUttaksplanMedIkkeOppfyltÅrsak(
                (IkkeOppfyltÅrsak) opphørsårsak);

            // Act

            var holder = new UttakResultatHolderFP(
                Optional.of(ForeldrepengerUttakTjeneste.map(uttakresultatRevurdering)), null);
            boolean harOpphørsårsak = holder.kontrollerErSisteUttakAvslåttMedÅrsak();

            // Assert
            assertThat(harOpphørsårsak).isTrue();
        });
    }

    @Test
    public void skal_sjekke_at_siste_periode_ikke_gir_opphør_når_det_ikke_er_avslått_med_opphørsårsak() {
        // Arrange
        UttakResultatEntitet uttakresultatRevurdering = lagUttaksplanMedIkkeOppfyltÅrsak(
            IkkeOppfyltÅrsak.UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT);

        // Act
        var holder = new UttakResultatHolderFP(Optional.of(ForeldrepengerUttakTjeneste.map(uttakresultatRevurdering)),
            null);
        boolean harOpphørsårsak = holder.kontrollerErSisteUttakAvslåttMedÅrsak();

        // Assert
        assertThat(harOpphørsårsak).isFalse();
    }


    private UttakResultatEntitet lagUttaksplanMedIkkeOppfyltÅrsak(IkkeOppfyltÅrsak årsak) {
        LocalDate fra = LocalDate.now();
        return lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(fra, fra.plusDays(10))),
            List.of(false), List.of(PeriodeResultatType.AVSLÅTT),
            List.of(årsak), List.of(false), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(StønadskontoType.FORELDREPENGER)
        );
    }

    private void lagUttakPeriodeMedPeriodeAktivitet(UttakResultatPerioderEntitet uttakResultatPerioder,
                                                    LocalDateInterval periode,
                                                    boolean samtidigUttak,
                                                    PeriodeResultatType periodeResultatType,
                                                    PeriodeResultatÅrsak periodeResultatÅrsak,
                                                    boolean graderingInnvilget,
                                                    List<Integer> andelIArbeid,
                                                    List<Integer> utbetalingsgrad,
                                                    List<Trekkdager> trekkdager,
                                                    List<StønadskontoType> stønadskontoTyper) {
        UttakResultatPeriodeEntitet uttakResultatPeriode = byggPeriode(periode.getFomDato(), periode.getTomDato(),
            samtidigUttak, periodeResultatType, periodeResultatÅrsak, graderingInnvilget);

        int antallAktiviteter = stønadskontoTyper.size();
        for (int i = 0; i < antallAktiviteter; i++) {
            UttakResultatPeriodeAktivitetEntitet periodeAktivitet = lagPeriodeAktivitet(stønadskontoTyper.get(i),
                uttakResultatPeriode, trekkdager.get(i),
                andelIArbeid.get(i), utbetalingsgrad.get(i));
            uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);
        }
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
    }

    private UttakResultatPeriodeAktivitetEntitet lagPeriodeAktivitet(StønadskontoType stønadskontoType,
                                                                     UttakResultatPeriodeEntitet uttakResultatPeriode,
                                                                     Trekkdager trekkdager,
                                                                     int andelIArbeid,
                                                                     int utbetalingsgrad) {
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        return UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(BigDecimal.valueOf(andelIArbeid))
            .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad))
            .build();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   List<Boolean> samtidigUttak,
                                                                   List<PeriodeResultatType> periodeResultatTyper,
                                                                   List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                   List<Boolean> graderingInnvilget,
                                                                   List<Integer> andelIArbeid,
                                                                   List<Integer> utbetalingsgrad,
                                                                   List<Trekkdager> trekkdager,
                                                                   List<StønadskontoType> stønadskontoTyper) {
        UttakResultatEntitet.Builder uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(
            behandling.getBehandlingsresultat());
        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        int antallPerioder = perioder.size();
        for (int i = 0; i < antallPerioder; i++) {
            lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder, perioder.get(i),
                samtidigUttak.get(i), periodeResultatTyper.get(i), periodeResultatÅrsak.get(i),
                graderingInnvilget.get(i), andelIArbeid, utbetalingsgrad, trekkdager, stønadskontoTyper);
        }
        UttakResultatEntitet uttakResultat = uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder)
            .build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(),
            uttakResultat.getGjeldendePerioder());
        return uttakResultat;

    }

    private UttakResultatPeriodeEntitet byggPeriode(LocalDate fom,
                                                    LocalDate tom,
                                                    boolean samtidigUttak,
                                                    PeriodeResultatType periodeResultatType,
                                                    PeriodeResultatÅrsak periodeResultatÅrsak,
                                                    boolean graderingInnvilget) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .build();
    }
}
