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

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsresultatTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.EndringsdatoRevurderingUtleder;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ExtendWith(JpaExtension.class)
class ErEndringIUttakTest {
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;

    private BehandlingskontrollServiceProvider serviceProvider;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningRevurderingTestUtil revurderingTestUtil;
    private RevurderingEndring revurderingEndring;
    private VergeRepository vergeRepository;

    private BeregningsresultatRepository beregningsresultatRepository;
    private FpUttakRepository fpUttakRepository;
    private final EndringsdatoRevurderingUtleder datoRevurderingUtlederImpl = mock(EndringsdatoRevurderingUtleder.class);
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        fpUttakRepository = new FpUttakRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        serviceProvider = new BehandlingskontrollServiceProvider(entityManager, new BehandlingModellRepository(), null);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        revurderingTestUtil = new BeregningRevurderingTestUtil(repositoryProvider);
        vergeRepository = new VergeRepository(entityManager, new BehandlingLåsRepository(entityManager));
        revurderingEndring = new no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingEndring();
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        behandlingRevurderingTjeneste = new BehandlingRevurderingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
    }

    private Behandling opprettRevurdering(Behandling behandlingSomSkalRevurderes) {
        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider, behandlingRevurderingTjeneste);
        RevurderingTjeneste revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, grunnlagRepositoryProvider,
            behandlingskontrollTjeneste, iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        var dato = LocalDate.now().minusMonths(3);
        when(datoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(dato);
        return revurderingTjeneste.opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            new OrganisasjonsEnhet("1234", "Test"));
    }

    private Behandling opprettFørstegangsbehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now()).medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        var behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository()
            .lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(), false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);
        return behandlingSomSkalRevurderes;
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_perioder() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            UttakPeriodeType.FORELDREPENGER);

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato, dato.plusDays(10)), new LocalDateInterval(dato.plusDays(11), dato.plusDays(20))),
            UttakPeriodeType.FEDREKVOTE);

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_aktiviteter() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
            Collections.nCopies(2, 100), Collections.nCopies(2, 100), List.of(new Trekkdager(2), new Trekkdager(10)),
            Collections.nCopies(2, UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_trekkdager_i_aktivitet() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(10)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_null_trekkdager_ulik_konto() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false),
            List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(UttakPeriodeType.UDEFINERT));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false),
            List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void case_fra_prod_bør_gi_ingen_endring() {
        // Arrange
        var dato = LocalDate.of(2020, 4, 9);
        var orig1 = new LocalDateInterval(dato, LocalDate.of(2020, 4, 30));
        var orig2 = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 9, 10));
        var ny1a = new LocalDateInterval(dato, LocalDate.of(2020, 4, 11));
        var ny1b = new LocalDateInterval(LocalDate.of(2020, 4, 12), LocalDate.of(2020, 4, 30));
        var ny2a = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 9, 8));
        var ny2b = new LocalDateInterval(LocalDate.of(2020, 9, 9), LocalDate.of(2020, 9, 10));

        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatPlanBuilderOrig = new UttakResultatEntitet.Builder(førstegangsbehandling.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(List.of(orig1, orig2), List.of(false, false), List.of(false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false), List.of(100, 0), List.of(0, 100), List.of(Trekkdager.ZERO, new Trekkdager(95)),
            List.of(UttakPeriodeType.UDEFINERT, UttakPeriodeType.FEDREKVOTE), List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT), false,
            uttakResultatPlanBuilderOrig);
        var uttakResultatOriginal = uttakResultatPlanBuilderOrig.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(førstegangsbehandling.getId(), uttakResultatOriginal.getGjeldendePerioder());

        var uttakResultatPlanBuilderRev = new UttakResultatEntitet.Builder(revurdering.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(List.of(ny1a, ny1b, ny2a, ny2b), List.of(false, false, false, false),
            List.of(false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false, false, false), List.of(100, 100, 0, 0), List.of(0, 0, 100, 100),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, new Trekkdager(93), new Trekkdager(2)),
            List.of(UttakPeriodeType.UDEFINERT, UttakPeriodeType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT), false,
            uttakResultatPlanBuilderRev);
        var uttakResultatRevurdering = uttakResultatPlanBuilderRev.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatRevurdering.getGjeldendePerioder());

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void case_fra_prod_bør_gi_endring_avslag() {
        // Arrange
        var dato = LocalDate.of(2020, 3, 31);
        var orig1 = new LocalDateInterval(dato, LocalDate.of(2020, 4, 27));
        var orig2 = new LocalDateInterval(LocalDate.of(2020, 4, 28), LocalDate.of(2020, 4, 30));
        var orig3 = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 6, 3));
        var ny1 = new LocalDateInterval(dato, LocalDate.of(2020, 4, 23));
        var ny2 = new LocalDateInterval(LocalDate.of(2020, 4, 24), LocalDate.of(2020, 4, 24));
        var ny3 = new LocalDateInterval(LocalDate.of(2020, 4, 25), LocalDate.of(2020, 4, 30));
        var ny4 = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 1));
        var ny5 = new LocalDateInterval(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 6, 3));

        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatPlanBuilderOrig = new UttakResultatEntitet.Builder(førstegangsbehandling.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(List.of(orig1, orig2, orig3), List.of(false, false, false), List.of(false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT), List.of(false, false, false),
            List.of(0, 0, 0), List.of(0, 0), List.of(Trekkdager.ZERO, Trekkdager.ZERO),
            List.of(UttakPeriodeType.UDEFINERT, UttakPeriodeType.UDEFINERT),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER), false, uttakResultatPlanBuilderOrig);
        var uttakResultatOriginal = uttakResultatPlanBuilderOrig.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(førstegangsbehandling.getId(), uttakResultatOriginal.getGjeldendePerioder());

        var uttakResultatPlanBuilderRev = new UttakResultatEntitet.Builder(revurdering.getBehandlingsresultat());
        lagUttakResultatPlanForBehandlingLogging(List.of(ny1, ny2, ny3, ny4, ny5), List.of(false, false, false, false, false),
            List.of(false, false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET,
                PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT,
                PeriodeResultatÅrsak.UKJENT), List.of(false, false, false, false, false), List.of(0, 0, 0), List.of(0, 0, 0),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO),
            List.of(UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER,
                OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER), false, uttakResultatPlanBuilderRev);
        lagUttakResultatPlanForBehandlingLogging(List.of(ny1, ny2, ny3, ny4, ny5), List.of(false, false, false, false, false),
            List.of(false, false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT, PeriodeResultatType.INNVILGET,
                PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT,
                PeriodeResultatÅrsak.UKJENT), List.of(false, false, false, false, false), List.of(0, 0, 0), List.of(0, 0, 0),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO),
            List.of(UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE),
            List.of(OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.UDEFINERT, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER,
                OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER), true, uttakResultatPlanBuilderRev);
        var uttakResultatRevurdering = uttakResultatPlanBuilderRev.build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatRevurdering.getOpprinneligPerioder());
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(revurdering.getId(), uttakResultatRevurdering.getOverstyrtPerioder());

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_arbeidsprosent_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_utbetatlingsgrad_i_aktivitet() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(50), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_stønadskonto() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            UttakPeriodeType.FORELDREPENGER);

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            UttakPeriodeType.FEDREKVOTE);

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_resultatType() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(PeriodeResultatType.AVSLÅTT));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(PeriodeResultatType.INNVILGET));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_samtidig_uttak() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), UttakPeriodeType.FORELDREPENGER);

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(true), UttakPeriodeType.FORELDREPENGER);

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_gradering_utfall_i_aktivitet() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_dersom_begge_uttak_mangler() {
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.empty(), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.empty(), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);
        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_forskjellig_antall_aktiviteter() {
        // Arrange
        var dato = LocalDate.now();

        var førstegangsbehandling = opprettFørstegangsbehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), Collections.emptyList());

        var revurdering = opprettRevurdering(førstegangsbehandling);
        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
            Collections.nCopies(2, 100), Collections.nCopies(2, 100), List.of(new Trekkdager(2), new Trekkdager(10)),
            Collections.nCopies(2, UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_like_aktiviteter_men_forskjellig_uttakskonto() {
        // Arrange
        var dato = LocalDate.now();

        var førstegangsbehandling = opprettFørstegangsbehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var revurdering = opprettRevurdering(førstegangsbehandling);
        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_samme_antall_perioder_men_med_ulik_fom_og_tom() {
        // Arrange
        var dato = LocalDate.now();

        var førstegangsbehandling = opprettFørstegangsbehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));

        var revurdering = opprettRevurdering(førstegangsbehandling);
        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering,
            List.of(new LocalDateInterval(dato.plusDays(1), dato.plusDays(11))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_kun_et_av_uttakene_har_periode_med_flerbarnsdager() {
        // Arrange
        var dato = LocalDate.now();

        var førstegangsbehandling = opprettFørstegangsbehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(true), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var revurdering = opprettRevurdering(førstegangsbehandling);
        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));
        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_ikkje_gi_endring_i_uttak_om_det_ikkje_er_avvik() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikUttaksplan(revurderingHolder) || originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_endring_kontodager() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var kontoOriginal = new Stønadskontoberegning.Builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(280).build())
            .build();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoOriginal);

        var kontoRevurdering = new Stønadskontoberegning.Builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(291).build())
            .build();

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoRevurdering);


        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_endring_minsterett() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var kontoOriginal = new Stønadskontoberegning.Builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoOriginal);

        var kontoRevurdering = new Stønadskontoberegning.Builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(50).build())
            .build();

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoRevurdering);


        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_om_det_er_konto_minsterett_uendret() {
        // Arrange
        var dato = LocalDate.now();
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(dato, førstegangsbehandling, revurdering);

        var kontoOriginal = new Stønadskontoberegning.Builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();

        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(førstegangsbehandling, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoOriginal);

        var kontoRevurdering = new Stønadskontoberegning.Builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();

        var uttakResultatRevurdering = lagUttakResultatPlanForBehandling(revurdering, List.of(new LocalDateInterval(dato, dato.plusDays(10))),
            List.of(false), List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoRevurdering);


        // Act
        var revurderingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatRevurdering)), null);
        var originalBehandlingHolder = new UttakResultatHolderFP(Optional.of(map(uttakResultatOriginal)), null);
        var endringIUttak = originalBehandlingHolder.harUlikKontoEllerMinsterett(revurderingHolder);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    private void lagUttakPeriodeMedPeriodeAktivitet(UttakResultatPerioderEntitet uttakResultatPerioder,
                                                    LocalDateInterval periode,
                                                    boolean samtidigUttak,
                                                    PeriodeResultatType periodeResultatType,
                                                    PeriodeResultatÅrsak periodeResultatÅrsak,
                                                    boolean graderingInnvilget,
                                                    boolean erFlerbarnsdager,
                                                    List<Integer> andelIArbeid,
                                                    List<Integer> utbetalingsgrad,
                                                    List<Trekkdager> trekkdager,
                                                    List<UttakPeriodeType> stønadskontoTyper) {
        var uttakResultatPeriode = byggPeriode(periode.getFomDato(), periode.getTomDato(), samtidigUttak, periodeResultatType, periodeResultatÅrsak,
            graderingInnvilget, erFlerbarnsdager);

        var antallAktiviteter = stønadskontoTyper.size();
        for (var i = 0; i < antallAktiviteter; i++) {
            var periodeAktivitet = lagPeriodeAktivitet(stønadskontoTyper.get(i), uttakResultatPeriode, trekkdager.get(i), andelIArbeid.get(i),
                utbetalingsgrad.get(i));
            uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);
        }
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
    }

    private void lagUttakPeriodeUtenPeriodeAktivitet(UttakResultatPerioderEntitet uttakResultatPerioder,
                                                     LocalDateInterval periode,
                                                     boolean samtidigUttak,
                                                     PeriodeResultatType periodeResultatType,
                                                     PeriodeResultatÅrsak periodeResultatÅrsak,
                                                     boolean graderingInnvilget,
                                                     boolean erFlerbarnsdager,
                                                     OppholdÅrsak oppholdÅrsak) {
        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(periode.getFomDato(), periode.getTomDato()).medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .medFlerbarnsdager(erFlerbarnsdager)
            .medOppholdÅrsak(oppholdÅrsak)
            .build();
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
    }

    private UttakResultatPeriodeAktivitetEntitet lagPeriodeAktivitet(UttakPeriodeType stønadskontoType,
                                                                     UttakResultatPeriodeEntitet uttakResultatPeriode,
                                                                     Trekkdager trekkdager,
                                                                     int andelIArbeid,
                                                                     int utbetalingsgrad) {
        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        return UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(BigDecimal.valueOf(andelIArbeid))
            .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad))
            .build();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   List<PeriodeResultatType> periodeResultatTyper) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false),
            Collections.nCopies(perioder.size(), false), periodeResultatTyper, List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
            List.of(100), List.of(Trekkdager.ZERO), List.of(UttakPeriodeType.FORELDREPENGER));
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   UttakPeriodeType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false),
            Collections.nCopies(perioder.size(), false), Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET),
            Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT), Collections.nCopies(perioder.size(), true), List.of(100), List.of(100),
            List.of(new Trekkdager(1)), List.of(stønadskontoType));
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   List<Boolean> samtidigUttak,
                                                                   UttakPeriodeType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, samtidigUttak, Collections.nCopies(perioder.size(), false),
            Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET), Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT),
            samtidigUttak, List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(stønadskontoType));
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   List<Boolean> samtidigUttak,
                                                                   List<Boolean> erFlerbarnsdager,
                                                                   List<PeriodeResultatType> periodeResultatTyper,
                                                                   List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                   List<Boolean> graderingInnvilget,
                                                                   List<Integer> andelIArbeid,
                                                                   List<Integer> utbetalingsgrad,
                                                                   List<Trekkdager> trekkdager,
                                                                   List<UttakPeriodeType> stønadskontoTyper) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, samtidigUttak, erFlerbarnsdager, periodeResultatTyper,
            periodeResultatÅrsak, graderingInnvilget, andelIArbeid, utbetalingsgrad, trekkdager, stønadskontoTyper, null);
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   List<Boolean> samtidigUttak,
                                                                   List<Boolean> erFlerbarnsdager,
                                                                   List<PeriodeResultatType> periodeResultatTyper,
                                                                   List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                   List<Boolean> graderingInnvilget,
                                                                   List<Integer> andelIArbeid,
                                                                   List<Integer> utbetalingsgrad,
                                                                   List<Trekkdager> trekkdager,
                                                                   List<UttakPeriodeType> stønadskontoTyper,
                                                                   Stønadskontoberegning stønadskontoberegning) {
        var uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(behandling.getBehandlingsresultat());
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        var antallPerioder = perioder.size();
        for (var i = 0; i < antallPerioder; i++) {
            lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder, perioder.get(i), samtidigUttak.get(i), periodeResultatTyper.get(i),
                periodeResultatÅrsak.get(i), graderingInnvilget.get(i), erFlerbarnsdager.get(i), andelIArbeid, utbetalingsgrad, trekkdager,
                stønadskontoTyper);
        }
        var uttakResultat = uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder).medStønadskontoberegning(stønadskontoberegning).build();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultat.getGjeldendePerioder());
        return uttakResultat;

    }

    private void lagUttakResultatPlanForBehandlingLogging(List<LocalDateInterval> perioder,
                                                          List<Boolean> samtidigUttak,
                                                          List<Boolean> erFlerbarnsdager,
                                                          List<PeriodeResultatType> periodeResultatTyper,
                                                          List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                          List<Boolean> graderingInnvilget,
                                                          List<Integer> andelIArbeid,
                                                          List<Integer> utbetalingsgrad,
                                                          List<Trekkdager> trekkdager,
                                                          List<UttakPeriodeType> stønadskontoTyper,
                                                          List<OppholdÅrsak> oppholdÅrsaker,
                                                          boolean overstyrt,
                                                          UttakResultatEntitet.Builder uttakResultatPlanBuilder) {
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        var antallPerioder = perioder.size();
        for (var i = 0; i < antallPerioder; i++) {
            if (oppholdÅrsaker.get(i) != OppholdÅrsak.UDEFINERT) {
                lagUttakPeriodeUtenPeriodeAktivitet(uttakResultatPerioder, perioder.get(i), samtidigUttak.get(i), periodeResultatTyper.get(i),
                    periodeResultatÅrsak.get(i), graderingInnvilget.get(i), erFlerbarnsdager.get(i), oppholdÅrsaker.get(i));
            } else {
                lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder, perioder.get(i), samtidigUttak.get(i), periodeResultatTyper.get(i),
                    periodeResultatÅrsak.get(i), graderingInnvilget.get(i), erFlerbarnsdager.get(i), List.of(andelIArbeid.get(i)),
                    List.of(utbetalingsgrad.get(i)), List.of(trekkdager.get(i)), List.of(stønadskontoTyper.get(i)));
            }
        }
        if (!overstyrt) {
            uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder);
        } else {
            uttakResultatPlanBuilder.medOverstyrtPerioder(uttakResultatPerioder);
        }
    }

    private UttakResultatPeriodeEntitet byggPeriode(LocalDate fom,
                                                    LocalDate tom,
                                                    boolean samtidigUttak,
                                                    PeriodeResultatType periodeResultatType,
                                                    PeriodeResultatÅrsak periodeResultatÅrsak,
                                                    boolean graderingInnvilget,
                                                    boolean erFlerbarnsdager) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom).medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .medFlerbarnsdager(erFlerbarnsdager)
            .build();
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate dato, Behandling førstegangsbehandling, Behandling revurdering) {
        var originaltresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(dato, true, ORGNR);
        var revurderingsresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(dato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(førstegangsbehandling, originaltresultat);
    }
}
