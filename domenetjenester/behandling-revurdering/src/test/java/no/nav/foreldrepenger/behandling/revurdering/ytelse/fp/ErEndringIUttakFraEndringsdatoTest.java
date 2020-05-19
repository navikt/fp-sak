package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsresultatTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.EndringsdatoRevurderingUtlederImpl;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class ErEndringIUttakFraEndringsdatoTest {
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndring;

    @Inject
    private VergeRepository vergeRepository;

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private BeregningsresultatRepository beregningsresultatRepository;
    private FpUttakRepository fpUttakRepository;
    private EndringsdatoRevurderingUtlederImpl endringsdatoRevurderingUtlederImpl = mock(EndringsdatoRevurderingUtlederImpl.class);

    private Behandling behandlingSomSkalRevurderes;
    private Behandling revurdering;
    private ErEndringIUttakFraEndringsdatoImpl erEndringIUttakFraEndringsdato;

    @Before
    public void setUp() {
        fpUttakRepository = repositoryProvider.getFpUttakRepository();
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(), false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        BehandlingskontrollTjenesteImpl behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        RevurderingTjenesteFelles revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        RevurderingTjeneste revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste,
            iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));
        LocalDate endringsdato = LocalDate.now().minusMonths(3);
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        erEndringIUttakFraEndringsdato = new ErEndringIUttakFraEndringsdatoImpl();
    }

    @Test
    public void skal_finne_en_uttaksperiode_etter_endringstidspunkt() {
        LocalDate endringsdato = LocalDate.now();

        UttakResultatEntitet.Builder uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(revurdering.getBehandlingsresultat());
        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(6)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato.minusDays(5), endringsdato.minusDays(1)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato, endringsdato.plusDays(5)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        UttakResultatEntitet uttakResultat = uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder).build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultat.getGjeldendePerioder());

        var holder = new UttakResultatHolderImpl(Optional.of(map(uttakResultat)), null);
        var uttaksperioder = UttakResultatHolderImpl.finnUttaksperioderEtterEndringsdato(endringsdato, holder);

        // Assert
        assertThat(uttaksperioder).hasSize(1);

    }

    @Test
    public void skal_finne_to_uttaksperiode_etter_endringstidspunkt() {
        LocalDate endringsdato = LocalDate.now();

        UttakResultatEntitet.Builder uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(revurdering.getBehandlingsresultat());
        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(6)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato.minusDays(5), endringsdato.minusDays(1)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato, endringsdato.plusDays(5)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder,
            new LocalDateInterval(endringsdato.plusDays(10), endringsdato.plusDays(50)),
            false, PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT, true, false, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
        UttakResultatEntitet uttakResultat = uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder).build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultat.getGjeldendePerioder());

        var holder = new UttakResultatHolderImpl(Optional.of(map(uttakResultat)), null);
        var uttaksperioder = UttakResultatHolderImpl.finnUttaksperioderEtterEndringsdato(endringsdato, holder);

        // Assert
        assertThat(uttaksperioder).hasSize(2);

    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_perioder_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))), StønadskontoType.FORELDREPENGER);

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20))), StønadskontoType.FEDREKVOTE);

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder,  originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_aktiviteter_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), Collections.nCopies(2, 100), Collections.nCopies(2, 100), List.of(new Trekkdager(2), new Trekkdager(10)), Collections.nCopies(2, StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder,  originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_trekkdager_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(10)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder,  originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_arbeidsprosent_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_utbetatlingsgrad_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(50), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_stønadskonto_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))), StønadskontoType.FORELDREPENGER);

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))), StønadskontoType.FEDREKVOTE);

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_resultatType_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))), List.of(PeriodeResultatType.AVSLÅTT));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))), List.of(PeriodeResultatType.INNVILGET));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_samtidig_uttak_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false),
            StønadskontoType.FORELDREPENGER);

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(true),
            StønadskontoType.FORELDREPENGER);

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_gradering_utfall_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_dersom_begge_uttak_mangler() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();

        // Act
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, new UttakResultatHolderImpl( Optional.empty(), null),new UttakResultatHolderImpl( Optional.empty(), null));

        // Assert
        assertThat(endringIUttak).isFalse();
    }


    @Test
    public void skal_gi_endring_i_uttak_dersom_uttakene_har_forskjellig_antall_aktiviteter() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), Collections.emptyList());

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false),List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), Collections.nCopies(2, 100), Collections.nCopies(2, 100), List.of(new Trekkdager(2), new Trekkdager(10)), Collections.nCopies(2, StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_dersom_uttakene_har_like_aktiviteter_men_forskjellig_uttakskonto() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));


        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FELLESPERIODE));
        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_dersom_uttakene_har_samme_antall_perioder_men_med_ulik_fom_og_tom() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FELLESPERIODE));


        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato.plusDays(1), endringsdato.plusDays(11))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FELLESPERIODE));
        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_dersom_kun_et_av_uttakene_har_periode_med_flerbarnsdager() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(true), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));


        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }


    @Test
    public void skal_ikkje_gi_endring_i_uttak_om_det_ikkje_er_avvik_etter_endringstidspunktet() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderImpl(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = erEndringIUttakFraEndringsdato.vurder(endringsdato, revurderingHolder, originalBehandlingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    private void lagUttakPeriodeMedPeriodeAktivitet(UttakResultatPerioderEntitet uttakResultatPerioder, LocalDateInterval periode, boolean samtidigUttak, PeriodeResultatType periodeResultatType,
                                                    PeriodeResultatÅrsak periodeResultatÅrsak, boolean graderingInnvilget, boolean erFlerbarnsdager, List<Integer> andelIArbeid, List<Integer> utbetalingsgrad, List<Trekkdager> trekkdager, List<StønadskontoType> stønadskontoTyper) {
        UttakResultatPeriodeEntitet uttakResultatPeriode = byggPeriode(periode.getFomDato(), periode.getTomDato(), samtidigUttak, periodeResultatType, periodeResultatÅrsak, graderingInnvilget, erFlerbarnsdager);

        int antallAktiviteter = stønadskontoTyper.size();
        for (int i = 0; i < antallAktiviteter; i++) {
            UttakResultatPeriodeAktivitetEntitet periodeAktivitet = lagPeriodeAktivitet(stønadskontoTyper.get(i), uttakResultatPeriode, trekkdager.get(i),
                andelIArbeid.get(i), utbetalingsgrad.get(i));
            uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);
        }
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
    }

    private UttakResultatPeriodeAktivitetEntitet lagPeriodeAktivitet(StønadskontoType stønadskontoType, UttakResultatPeriodeEntitet uttakResultatPeriode,
                                                                     Trekkdager trekkdager, int andelIArbeid, int utbetalingsgrad) {
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        return UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(BigDecimal.valueOf(andelIArbeid))
            .medUtbetalingsgrad(new BigDecimal(utbetalingsgrad))
            .build();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder, List<PeriodeResultatType> periodeResultatTyper) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false), Collections.nCopies(perioder.size(), false),
            periodeResultatTyper, List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
    }


    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder, StønadskontoType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false), Collections.nCopies(perioder.size(), false),
            Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET), Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT), Collections.nCopies(perioder.size(), true), List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(stønadskontoType));
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder, List<Boolean> samtidigUttak, StønadskontoType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, samtidigUttak, Collections.nCopies(perioder.size(), false), Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET),
            Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT), samtidigUttak, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(stønadskontoType));
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder, List<Boolean> samtidigUttak,
                                                                   List<Boolean> erFlerbarnsdager, List<PeriodeResultatType> periodeResultatTyper, List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                   List<Boolean> graderingInnvilget, List<Integer> andelIArbeid,
                                                                   List<Integer> utbetalingsgrad, List<Trekkdager> trekkdager, List<StønadskontoType> stønadskontoTyper) {
        UttakResultatEntitet.Builder uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(behandling.getBehandlingsresultat());
        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        int antallPerioder = perioder.size();
        for (int i = 0; i < antallPerioder; i++) {
            lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder, perioder.get(i),
                samtidigUttak.get(i), periodeResultatTyper.get(i), periodeResultatÅrsak.get(i), graderingInnvilget.get(i), erFlerbarnsdager.get(i), andelIArbeid, utbetalingsgrad, trekkdager, stønadskontoTyper);
        }
        UttakResultatEntitet uttakResultat = uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder).build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultat.getGjeldendePerioder());
        return uttakResultat;

    }

    private UttakResultatPeriodeEntitet byggPeriode(LocalDate fom, LocalDate tom, boolean samtidigUttak, PeriodeResultatType periodeResultatType, PeriodeResultatÅrsak periodeResultatÅrsak, boolean graderingInnvilget, boolean erFlerbarnsdager) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .medFlerbarnsdager(erFlerbarnsdager)
            .build();
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate endringsdato) {
        BeregningsresultatEntitet originaltresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, true, ORGNR);
        BeregningsresultatEntitet revurderingsresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(behandlingSomSkalRevurderes, originaltresultat);
    }
}

