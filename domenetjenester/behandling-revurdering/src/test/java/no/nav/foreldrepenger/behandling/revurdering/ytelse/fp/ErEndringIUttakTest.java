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
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
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
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.EndringsdatoRevurderingUtlederImpl;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class ErEndringIUttakTest {
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
    private EndringsdatoRevurderingUtlederImpl datoRevurderingUtlederImpl = mock(EndringsdatoRevurderingUtlederImpl.class);

    private Behandling behandlingSomSkalRevurderes;
    private Behandling revurdering;

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
        LocalDate dato = LocalDate.now().minusMonths(3);
        when(datoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(dato);
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_perioder() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))), StønadskontoType.FORELDREPENGER);

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10)),
                new LocalDateInterval(dato.plusDays(11), dato.plusDays(20))), StønadskontoType.FEDREKVOTE);

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_aktiviteter() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), Collections.nCopies(2, 100), Collections.nCopies(2, 100), List.of(new Trekkdager(2), new Trekkdager(10)), Collections.nCopies(2, StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_trekkdager_i_aktivitet() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(10)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_ikke_gi_endring_i_uttak_null_trekkdager_ulik_konto() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.UDEFINERT));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }


    @Test
    public void case_fra_prod_bør_gi_ingen_endring() {
        // Arrange
        LocalDate dato = LocalDate.of(2020,4,9);
        LocalDateInterval orig1 = new LocalDateInterval(dato, LocalDate.of(2020,4,30));
        LocalDateInterval orig2 = new LocalDateInterval(LocalDate.of(2020,5,1), LocalDate.of(2020,9,10));
        LocalDateInterval ny1a = new LocalDateInterval(dato, LocalDate.of(2020,4,11));
        LocalDateInterval ny1b = new LocalDateInterval(LocalDate.of(2020,4,12), LocalDate.of(2020,4,30));
        LocalDateInterval ny2a = new LocalDateInterval(LocalDate.of(2020,5,1), LocalDate.of(2020,9,8));
        LocalDateInterval ny2b = new LocalDateInterval(LocalDate.of(2020,9,9), LocalDate.of(2020,9,10));

        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet.Builder uttakResultatPlanBuilderOrig = new UttakResultatEntitet.Builder(behandlingSomSkalRevurderes.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(behandlingSomSkalRevurderes,
            List.of(orig1, orig2),
            List.of(false, false), List.of(false, false), List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT), List.of(false, false), List.of(100, 0), List.of(0, 100), List.of(Trekkdager.ZERO, new Trekkdager(95)),
            List.of(StønadskontoType.UDEFINERT, StønadskontoType.FEDREKVOTE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT), false, uttakResultatPlanBuilderOrig);
        UttakResultatEntitet uttakResultatOriginal = uttakResultatPlanBuilderOrig.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingSomSkalRevurderes.getId(), uttakResultatOriginal.getGjeldendePerioder());

        UttakResultatEntitet.Builder uttakResultatPlanBuilderRev = new UttakResultatEntitet.Builder(revurdering.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(revurdering,
            List.of(ny1a, ny1b, ny2a, ny2b),
            List.of(false, false, false, false), List.of(false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false, false, false), List.of(100, 100, 0, 0), List.of(0, 0, 100, 100),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, new Trekkdager(93), new Trekkdager(2)),
            List.of(StønadskontoType.UDEFINERT, StønadskontoType.FEDREKVOTE, StønadskontoType.FEDREKVOTE, StønadskontoType.FEDREKVOTE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT), false, uttakResultatPlanBuilderRev);
        UttakResultatEntitet uttakResultatRevurdering = uttakResultatPlanBuilderRev.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatRevurdering.getGjeldendePerioder());

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    public void case_fra_prod_bør_gi_endring_avslag() {
        // Arrange
        LocalDate dato = LocalDate.of(2020,3,31);
        LocalDateInterval orig1 = new LocalDateInterval(dato, LocalDate.of(2020,4,27));
        LocalDateInterval orig2 = new LocalDateInterval(LocalDate.of(2020,4,28), LocalDate.of(2020,4,30));
        LocalDateInterval orig3 = new LocalDateInterval(LocalDate.of(2020,5,1), LocalDate.of(2020,6,3));
        LocalDateInterval ny1 = new LocalDateInterval(dato, LocalDate.of(2020,4,23));
        LocalDateInterval ny2 = new LocalDateInterval(LocalDate.of(2020,4,24), LocalDate.of(2020,4,24));
        LocalDateInterval ny3 = new LocalDateInterval(LocalDate.of(2020,4,25), LocalDate.of(2020,4,30));
        LocalDateInterval ny4 = new LocalDateInterval(LocalDate.of(2020,5,1), LocalDate.of(2020,5,1));
        LocalDateInterval ny5 = new LocalDateInterval(LocalDate.of(2020,5,2), LocalDate.of(2020,6,3));

        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet.Builder uttakResultatPlanBuilderOrig = new UttakResultatEntitet.Builder(behandlingSomSkalRevurderes.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(behandlingSomSkalRevurderes,
            List.of(orig1, orig2, orig3),
            List.of(false, false, false), List.of(false, false, false), List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT), List.of(false, false, false), List.of(0, 0, 0),
            List.of(0, 0), List.of(Trekkdager.ZERO, Trekkdager.ZERO), List.of(StønadskontoType.UDEFINERT, StønadskontoType.UDEFINERT),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER), false, uttakResultatPlanBuilderOrig);
        UttakResultatEntitet uttakResultatOriginal = uttakResultatPlanBuilderOrig.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingSomSkalRevurderes.getId(), uttakResultatOriginal.getGjeldendePerioder());

        UttakResultatEntitet.Builder uttakResultatPlanBuilderRev = new UttakResultatEntitet.Builder(revurdering.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(revurdering,
            List.of(ny1, ny2, ny3, ny4, ny5),
            List.of(false, false, false, false, false), List.of(false, false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false, false, false, false), List.of(0, 0, 0), List.of(0, 0, 0),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO ),
            List.of(StønadskontoType.FELLESPERIODE, StønadskontoType.FELLESPERIODE, StønadskontoType.FELLESPERIODE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER), false, uttakResultatPlanBuilderRev);
        lagUttakResultatPlanForBehandlingLogging(revurdering,
            List.of(ny1, ny2, ny3, ny4, ny5),
            List.of(false, false, false, false, false), List.of(false, false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false, false, false, false), List.of(0, 0, 0), List.of(0, 0, 0),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO ),
            List.of(StønadskontoType.FELLESPERIODE, StønadskontoType.FELLESPERIODE, StønadskontoType.FELLESPERIODE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER), true, uttakResultatPlanBuilderRev);
        UttakResultatEntitet uttakResultatRevurdering = uttakResultatPlanBuilderRev.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatRevurdering.getOpprinneligPerioder());
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(revurdering.getId(), uttakResultatRevurdering.getOverstyrtPerioder());

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_arbeidsprosent_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_utbetatlingsgrad_i_aktivitet() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(50), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_stønadskonto() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))), StønadskontoType.FORELDREPENGER);

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))), StønadskontoType.FEDREKVOTE);

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_resultatType() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(PeriodeResultatType.AVSLÅTT));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(PeriodeResultatType.INNVILGET));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_samtidig_uttak() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false),
            StønadskontoType.FORELDREPENGER);

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(true),
            StønadskontoType.FORELDREPENGER);

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_om_det_er_avvik_i_gradering_utfall_i_aktivitet() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_dersom_begge_uttak_mangler() {
        // Act
        boolean endringIUttak = new UttakResultatHolderFP( Optional.empty(), null).harUlikUttaksplan(new UttakResultatHolderFP( Optional.empty(), null));

        // Assert
        assertThat(endringIUttak).isFalse();
    }


    @Test
    public void skal_gi_endring_i_uttak_dersom_uttakene_har_forskjellig_antall_aktiviteter() {
        // Arrange
        LocalDate dato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), Collections.emptyList());

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false),List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), Collections.nCopies(2, 100), Collections.nCopies(2, 100), List.of(new Trekkdager(2), new Trekkdager(10)), Collections.nCopies(2, StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_dersom_uttakene_har_like_aktiviteter_men_forskjellig_uttakskonto() {
        // Arrange
        LocalDate dato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));


        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FELLESPERIODE));
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_dersom_uttakene_har_samme_antall_perioder_men_med_ulik_fom_og_tom() {
        // Arrange
        LocalDate dato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FELLESPERIODE));


        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato.plusDays(1), dato.plusDays(11))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FELLESPERIODE));
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    public void skal_gi_endring_i_uttak_dersom_kun_et_av_uttakene_har_periode_med_flerbarnsdager() {
        // Arrange
        LocalDate dato = LocalDate.now();

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(true), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));


        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }


    @Test
    public void skal_ikkje_gi_endring_i_uttak_om_det_ikkje_er_avvik() {
        // Arrange
        LocalDate dato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato);

        UttakResultatEntitet uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        UttakResultatEntitet uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        boolean endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder);

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

    private void lagUttakPeriodeUtenPeriodeAktivitet(UttakResultatPerioderEntitet uttakResultatPerioder, LocalDateInterval periode, boolean samtidigUttak, PeriodeResultatType periodeResultatType,
                                                     PeriodeResultatÅrsak periodeResultatÅrsak, boolean graderingInnvilget, boolean erFlerbarnsdager, OppholdÅrsak oppholdÅrsak) {
        UttakResultatPeriodeEntitet uttakResultatPeriode  = new UttakResultatPeriodeEntitet.Builder(periode.getFomDato(), periode.getTomDato())
            .medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .medFlerbarnsdager(erFlerbarnsdager)
            .medOppholdÅrsak(oppholdÅrsak)
            .build();
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
            .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad))
            .build();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder, List<PeriodeResultatType> periodeResultatTyper) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false), Collections.nCopies(perioder.size(), false),
            periodeResultatTyper, List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(StønadskontoType.FORELDREPENGER));
    }


    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling, List<LocalDateInterval> perioder, StønadskontoType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false), Collections.nCopies(perioder.size(), false),
            Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET), Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT), Collections.nCopies(perioder.size(), true), List.of(100), List.of(100), List.of(new Trekkdager(1)), List.of(stønadskontoType));
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

    private void lagUttakResultatPlanForBehandlingLogging(Behandling behandling, List<LocalDateInterval> perioder, List<Boolean> samtidigUttak,
                                                                   List<Boolean> erFlerbarnsdager, List<PeriodeResultatType> periodeResultatTyper, List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                   List<Boolean> graderingInnvilget, List<Integer> andelIArbeid,
                                                                   List<Integer> utbetalingsgrad, List<Trekkdager> trekkdager, List<StønadskontoType> stønadskontoTyper,
                                                                          List<OppholdÅrsak> oppholdÅrsaker, boolean overstyrt, UttakResultatEntitet.Builder uttakResultatPlanBuilder) {
        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        int antallPerioder = perioder.size();
        for (int i = 0; i < antallPerioder; i++) {
            if (oppholdÅrsaker.get(i) != OppholdÅrsak.UDEFINERT) {
                lagUttakPeriodeUtenPeriodeAktivitet(uttakResultatPerioder, perioder.get(i),
                    samtidigUttak.get(i), periodeResultatTyper.get(i), periodeResultatÅrsak.get(i), graderingInnvilget.get(i), erFlerbarnsdager.get(i),
                    oppholdÅrsaker.get(i));
            } else {
                lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder, perioder.get(i),
                    samtidigUttak.get(i), periodeResultatTyper.get(i), periodeResultatÅrsak.get(i), graderingInnvilget.get(i), erFlerbarnsdager.get(i),
                    List.of(andelIArbeid.get(i)), List.of(utbetalingsgrad.get(i)), List.of(trekkdager.get(i)), List.of(stønadskontoTyper.get(i)));
            }
        }
        if (!overstyrt)
            uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder);
        else
            uttakResultatPlanBuilder.medOverstyrtPerioder(uttakResultatPerioder);
    }

    private UttakResultatPeriodeEntitet byggPeriode(LocalDate fom, LocalDate tom, boolean samtidigUttak, PeriodeResultatType periodeResultatType, PeriodeResultatÅrsak periodeResultatÅrsak, boolean graderingInnvilget, boolean erFlerbarnsdager) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .medFlerbarnsdager(erFlerbarnsdager)
            .build();
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate dato) {
        BeregningsresultatEntitet originaltresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(dato, true, ORGNR);
        BeregningsresultatEntitet revurderingsresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(dato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(behandlingSomSkalRevurderes, originaltresultat);
    }
}

