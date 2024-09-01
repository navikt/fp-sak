package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.BERØRT_BEHANDLING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.RelevanteArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

class EndringsdatoRevurderingUtlederTest {

    private static final LocalDate MANUELT_SATT_FØRSTE_UTTAKSDATO = FØDSELSDATO.plusDays(1);
    private static final LocalDate OMSORGSOVERTAKELSEDATO = FØDSELSDATO.plusDays(10);
    private static final LocalDate ANKOMSTDATO = FØDSELSDATO.plusDays(11);

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final UttakBeregningsandelTjenesteTestUtil uttakBeregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
    private final DekningsgradTjeneste dekningsgradTjeneste = new DekningsgradTjeneste(ytelsesFordelingRepository);
    private final UttakRevurderingTestUtil testUtil = new UttakRevurderingTestUtil(repositoryProvider, iayTjeneste);
    private final StønadskontoSaldoTjeneste saldoTjeneste = mock(StønadskontoSaldoTjeneste.class);
    private final EndringsdatoRevurderingUtleder utleder = new EndringsdatoRevurderingUtleder(
        repositoryProvider, mock(BehandlingRepository.class), dekningsgradTjeneste,
        new RelevanteArbeidsforholdTjeneste(repositoryProvider.getFpUttakRepository()), saldoTjeneste);

    @Test
    void skal_utlede_at_endringsdatoen_er_første_uttaksdato_til_startdato_for_uttak_når_dekningsgrad_er_endret() {
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(
            MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1), MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var revurdering = testUtil.opprettRevurdering(AktørId.dummy(), RE_OPPLYSNINGER_OM_DØD, opprinneligUttak,
            new OppgittFordelingEntitet(List.of(), true));
        var entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            MANUELT_SATT_FØRSTE_UTTAKSDATO).build();

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId()).medAvklarteDatoer(entitet);
        ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());

        endreDekningsgrad(revurdering);

        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(opprinneligPeriode.getFom());
    }

    private void endreDekningsgrad(Behandling revurdering) {
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var gjeldendeDekningsgrad = ytelseFordelingTjeneste.hentAggregat(revurdering.getId()).getGjeldendeDekningsgrad();
        ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(revurdering.getId(), gjeldendeDekningsgrad.isÅtti() ? Dekningsgrad._100 : Dekningsgrad._80);
    }

    @Test
    void skal_utlede_at_endringsdatoen_er_første_uttaksdato_til_startdato_for_uttak_når_dekningsgrad_er_endret_hvis_endringssøknad() {
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(
            MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1), MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var fordeling = List.of(OppgittPeriodeBuilder.ny()
            .medPeriode(VirkedagUtil.fomVirkedag(LocalDate.now()), VirkedagUtil.fomVirkedag(LocalDate.now()))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build());
        var revurdering = testUtil.opprettRevurdering(AktørId.dummy(), RE_ENDRING_FRA_BRUKER, opprinneligUttak,
            new OppgittFordelingEntitet(fordeling, true));
        var entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            MANUELT_SATT_FØRSTE_UTTAKSDATO).build();

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
            .medAvklarteDatoer(entitet);
        ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());

        endreDekningsgrad(revurdering);

        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(opprinneligPeriode.getFom());
    }

    @Test
    void skal_utlede_at_endringsdatoen_er_startdato_ny_sak_dersom_ny_stønadsperiode() {
        var baselineDato = Virkedager.justerHelgTilMandag(LocalDate.now());
        var startdatoNySak = baselineDato.plusWeeks(12);
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(
            baselineDato, baselineDato.plusWeeks(15)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var revurdering = testUtil.opprettRevurdering(AktørId.dummy(), OPPHØR_YTELSE_NYTT_BARN, opprinneligUttak,
            new OppgittFordelingEntitet(List.of(), true));

        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering, startdatoNySak));

        assertThat(endringsdato).isEqualTo(startdatoNySak);
    }

    @Test
    void skal_utlede_at_endringsdatoen_er_tom_dvs_start_uttak_dersom_ny_stønadsperiode_begynner_etter() {
        var baselineDato = Virkedager.justerHelgTilMandag(LocalDate.now());
        var startdatoNySak = baselineDato.plusWeeks(16);
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(
            baselineDato, baselineDato.plusWeeks(15)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var revurdering = testUtil.opprettRevurdering(AktørId.dummy(), OPPHØR_YTELSE_NYTT_BARN, opprinneligUttak,
            new OppgittFordelingEntitet(List.of(), true));

        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering, startdatoNySak));

        assertThat(endringsdato).isEqualTo(baselineDato);
    }

    @Test // #1.1
    void skal_utlede_at_endringsdato_er_fødselsdato_når_fødsel_har_forekommet_før_første_uttaksdato() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        var bekreftetHendelse = FamilieHendelse.forFødsel(FØDSELSDATO, FØDSELSDATO, List.of(new Barn()), 1);
        var ref = BehandlingReferanse.fra(revurdering);
        var familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var originalSøknadshendelse = FamilieHendelse.forFødsel(FØDSELSDATO, null, List.of(new Barn()), 1);
        var originalBehandling = new OriginalBehandling(revurdering.getOriginalBehandlingId().get(),
            new FamilieHendelser().medSøknadHendelse(originalSøknadshendelse));
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(originalBehandling);

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(ref, ytelsespesifiktGrunnlag));

        // Assert
        assertThat(endringsdato).isEqualTo(FØDSELSDATO);
    }

    private UttakInput lagInput(BehandlingReferanse ref, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        return new UttakInput(ref, null, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(
            uttakBeregningsandelTjeneste.hentStatuser());
    }

    @Test // #1.2
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtak_når_fødsel_har_forekommet_etter_første_uttaksdato() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        var bekreftetHendelse = FamilieHendelse.forFødsel(FØDSELSDATO, FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK,
            List.of(new Barn()), 1);
        var ref = BehandlingReferanse.fra(revurdering);
        var familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var originalSøknadshendelse = FamilieHendelse.forFødsel(FØDSELSDATO, null, List.of(new Barn()), 1);
        var originalBehandling = new OriginalBehandling(revurdering.getOriginalBehandlingId().get(),
            new FamilieHendelser().medSøknadHendelse(originalSøknadshendelse));
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(originalBehandling);

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(ref, ytelsespesifiktGrunnlag));

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #2
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_søknad_når_endringssøknad_er_mottatt() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_ENDRING_FRA_BRUKER);
        testUtil.byggOgLagreOppgittFordelingForMorFPFF(revurdering);

        // Act
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF);
    }

    @Test // #2
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_søknad_når_endringssøknad_er_mottatt_selv_om_mottatt_dato_før_vedtaksdato_på_original_behandling() {
        var originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        originalScenario.medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .medOppgittDekningsgrad(Dekningsgrad._100);

        var originalBehandling = originalScenario.lagre(repositoryProvider);

        var originaltUttak = new UttakResultatPerioderEntitet();
        originaltUttak.leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(LocalDate.now(),
            LocalDate.now().plusWeeks(1).minusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build());
        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(originalBehandling.getId(), originaltUttak);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        revurderingScenario.medDefaultInntektArbeidYtelse();
        revurderingScenario.medDefaultOppgittDekningsgrad();
        revurderingScenario.medOriginalBehandling(originalBehandling, RE_ENDRING_FRA_BRUKER);
        var nyOppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(VirkedagUtil.fomVirkedag(LocalDate.now().plusWeeks(1)), VirkedagUtil.tomVirkedag(LocalDate.now().plusWeeks(2)))
            .build();

        revurderingScenario.medFordeling(
            new OppgittFordelingEntitet(List.of(nyOppgittPeriode), true))
            .medOppgittDekningsgrad(Dekningsgrad._100);

        var revurdering = lagre(revurderingScenario);

        // Act
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(nyOppgittPeriode.getFom());
    }

    @Test // #2
    void skal_utlede_at_endringsdato_er_siste_uttaksdato_pluss_1_virkedag_fra_original_behandling_når_første_uttaksdato_fra_søknad_er_senere() {
        var originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var originalOppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        originalScenario.medFordeling(
            new OppgittFordelingEntitet(List.of(originalOppgittPeriode), true));
        var originalBehandling = originalScenario.lagre(repositoryProvider);

        var originaltUttak = new UttakResultatPerioderEntitet();
        originaltUttak.leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(LocalDate.now(),
            LocalDate.now().plusWeeks(1).minusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build());
        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(originalBehandling.getId(), originaltUttak);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        revurderingScenario.medDefaultInntektArbeidYtelse();
        revurderingScenario.medOriginalBehandling(originalBehandling, RE_ENDRING_FRA_BRUKER);
        var nyOppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.now().plusWeeks(3), LocalDate.now().plusWeeks(4))
            .build();

        revurderingScenario.medFordeling(
            new OppgittFordelingEntitet(List.of(nyOppgittPeriode), true));

        var revurdering = lagre(revurderingScenario);

        // Act
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(Virkedager.plusVirkedager(originalOppgittPeriode.getTom(), 1));
    }

    @Test // #3
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtak_når_revurdering_er_manuelt_opprettet() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_HENDELSE_FØDSEL))
            .medBehandlingManueltOpprettet(true);

        // Act
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #4.1
    void skal_utlede_at_endringsdato_på_mors_berørte_behandling_er_lik_fars_første_uttaksdag() {
        // Arrange førstegangsbehandling mor
        var behandling = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_MOR,
            testUtil.uttaksresultatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK), testUtil.søknadsAggregatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK));

        // Arrange førstegangsbehandling far
        var fomFar = VirkedagUtil.fomVirkedag(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(11));
        var behandlingFar = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_FAR,
            testUtil.uttaksresultatBerørtSak(fomFar), testUtil.søknadsAggregatBerørtSak(fomFar), behandling.getFagsak());

        // Arrange berørt behandling mor
        var revurderingBerørtSak = testUtil.opprettRevurderingBerørtSak(AKTØR_ID_MOR, BERØRT_BEHANDLING,
            behandling);
        var revurderingÅrsak = BehandlingÅrsak.builder(BERØRT_BEHANDLING)
            .medOriginalBehandlingId(revurderingBerørtSak.getOriginalBehandlingId().get());
        revurderingÅrsak.buildFor(revurderingBerørtSak);

        var familieHendelser = new FamilieHendelser().medSøknadHendelse(
            FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1));
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medErBerørtBehandling(true)
            .medOriginalBehandling(new OriginalBehandling(behandling.getId(), familieHendelser))
            .medFamilieHendelser(familieHendelser)
            .medAnnenpart(new Annenpart(behandlingFar.getId(), FØDSELSDATO.atStartOfDay()));
        var iayGrunnlag = iayTjeneste.hentGrunnlag(revurderingBerørtSak.getId());
        var input = new UttakInput(BehandlingReferanse.fra(revurderingBerørtSak), null, iayGrunnlag,
            fpGrunnlag).medBehandlingÅrsaker(Set.of(BERØRT_BEHANDLING));

        // Act
        var endringsdatoMor = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdatoMor).isEqualTo(fomFar);
    }

    @Test // #4.2
    void skal_utlede_at_endringsdato_på_mors_berørte_behandling_er_første_uttaksdag_fra_vedtaket_når_fars_endringsdato_er_tidligere() {
        // Arrange førstegangsbehandling mor
        var behandling = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_MOR,
            testUtil.uttaksresultatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK), testUtil.søknadsAggregatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK));

        // Arrange førstegangsbehandling far
        var fomFar = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.minusDays(2L);
        var behandlingFar = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_FAR,
            testUtil.uttaksresultatBerørtSak(fomFar), testUtil.søknadsAggregatBerørtSak(fomFar), behandling.getFagsak());

        // Arrange berørt behandling mor
        var revurderingBerørtSak = testUtil.opprettRevurderingBerørtSak(AKTØR_ID_MOR, BERØRT_BEHANDLING,
            behandling);
        var revurderingÅrsak = BehandlingÅrsak.builder(BERØRT_BEHANDLING)
            .medOriginalBehandlingId(revurderingBerørtSak.getOriginalBehandlingId().get());
        revurderingÅrsak.buildFor(revurderingBerørtSak);

        var familieHendelser = new FamilieHendelser().medSøknadHendelse(
            FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1));
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medErBerørtBehandling(true)
            .medOriginalBehandling(new OriginalBehandling(behandling.getId(), familieHendelser))
            .medFamilieHendelser(familieHendelser)
            .medAnnenpart(new Annenpart(behandlingFar.getId(), FØDSELSDATO.atStartOfDay()));
        var iayGrunnlag = iayTjeneste.hentGrunnlag(revurderingBerørtSak.getId());
        var input = new UttakInput(BehandlingReferanse.fra(revurderingBerørtSak), null, iayGrunnlag,
            fpGrunnlag).medBehandlingÅrsaker(Set.of(BERØRT_BEHANDLING));

        // Act
        var endringsdatoMor = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdatoMor).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #5
    void skal_utlede_at_endringsdato_er_manuelt_satt_første_uttaksdato() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        var entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            MANUELT_SATT_FØRSTE_UTTAKSDATO).build();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
            .medAvklarteDatoer(entitet);

        ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(MANUELT_SATT_FØRSTE_UTTAKSDATO);
    }

    @Test // #2 + #5
    void skal_utlede_at_endringsdato_er_første_uttaksdag_fra_søknad_når_denne_er_tidligere_enn_manuelt_satt_første_uttaksdato() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_ENDRING_FRA_BRUKER);
        testUtil.byggOgLagreOppgittFordelingForMorFPFF(revurdering);
        var entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            MANUELT_SATT_FØRSTE_UTTAKSDATO).build();

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId()).medAvklarteDatoer(entitet);
        ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());

        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1);

        // Act
        var input = lagInput(revurdering, familieHendelse).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF);
    }

    @Test // #6
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtaket_når_inntektsmelding_endrer_uttak() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRET_INNTEKTSMELDING);
        testUtil.opprettInntektsmelding(revurdering);

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #7
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtaket_ved_opplysninger_om_død() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        var bekreftetHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1);
        var ref = BehandlingReferanse.fra(revurdering);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familiehendelser)
            .medDødsfall(true)
            .medOriginalBehandling(new OriginalBehandling(revurdering.getOriginalBehandlingId().orElseThrow(),
                new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse)));
        var input = new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(bekreftetHendelse.getFamilieHendelseDato()).build(), iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(
            uttakBeregningsandelTjeneste.hentStatuser());

        // Act
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtaket_når_endring_i_ytelse_ikke_fører_til_endring_i_grunnlaget() {
        // Arrange
        var startdatoEndringssøknad = VirkedagUtil.fomVirkedag(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(10));
        var revurdering = testUtil.opprettEndringssøknadRevurdering(AKTØR_ID_MOR, startdatoEndringssøknad,
            RE_ENDRING_FRA_BRUKER);
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));

        // Act
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(startdatoEndringssøknad);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_i_endring_dersom_endringer_i_ytelse_stammer_fra_samme_fagsak() {

        var endringFom = VirkedagUtil.fomVirkedag(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(11));

        // Arrange
        var revurdering = testUtil.opprettRevurdering(RE_ENDRING_FRA_BRUKER);
        testUtil.byggOgLagreOppgittFordelingMedPeriode(revurdering, endringFom,
            FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(50), UttakPeriodeType.FELLESPERIODE);

        leggTilFpsakYtelse(revurdering);
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));

        // Act
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(endringFom);
    }

    @Test // Adopsjon.1
    void skal_utlede_at_endringsdato_er_omsorgsovertakelsedato_ved_adopsjon_uten_ankomstdato() {
        // Arrange
        var revurdering = testUtil.opprettRevurderingAdopsjon();
        var ref = BehandlingReferanse.fra(revurdering);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(OMSORGSOVERTAKELSEDATO, List.of(new Barn()),
            1, null, false);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            familieHendelser)
            .medOriginalBehandling(
                new OriginalBehandling(revurdering.getOriginalBehandlingId().get(), familieHendelser));
        var uttakInput = new UttakInput(ref, null, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(
            uttakBeregningsandelTjeneste.hentStatuser());

        // Act
        var endringsdato = utleder.utledEndringsdato(uttakInput);

        // Assert
        assertThat(endringsdato).isEqualTo(OMSORGSOVERTAKELSEDATO);
    }

    @Test // Adopsjon.2
    void skal_utlede_at_endringsdato_er_ankomstdato_ved_adopsjon_når_ankomstdatoen_er_satt() {
        // Arrange
        var revurdering = testUtil.opprettRevurderingAdopsjon();
        var ref = BehandlingReferanse.fra(revurdering);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(OMSORGSOVERTAKELSEDATO, List.of(new Barn()),
            1, ANKOMSTDATO, false);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            familieHendelser)
            .medOriginalBehandling(
                new OriginalBehandling(revurdering.getOriginalBehandlingId().get(), familieHendelser));
        var uttakInput = new UttakInput(ref, null, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(
            uttakBeregningsandelTjeneste.hentStatuser());

        // Act
        var endringsdato = utleder.utledEndringsdato(uttakInput);

        // Assert
        assertThat(endringsdato).isEqualTo(ANKOMSTDATO);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_forrige_behandling_når_uten_uttaksresultat() {
        // Arrange
        var revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_HENDELSE_FØDSEL);

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_dato_i_vedtak_hvis_endringssøknad_men_beregningsandel_er_fjernet() {
        var fomOpprinneligUttak = FØDSELSDATO;
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(fomOpprinneligUttak,
            fomOpprinneligUttak.plusWeeks(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER).build();
        var uttakAktivitet1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.FRILANS).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet1).medArbeidsprosent(
            BigDecimal.TEN).build();
        var uttakAktivitet2 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet2).medArbeidsprosent(
            BigDecimal.TEN).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var søknadsFom = FØDSELSDATO.plusWeeks(1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(søknadsFom, søknadsFom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var nyFordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);
        var revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRING_FRA_BRUKER, opprinneligUttak,
            nyFordeling);
        uttakBeregningsandelTjeneste.leggTilSelvNæringdrivende(Arbeidsgiver.virksomhet("123"));

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØDSELSDATO);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_dato_i_vedtak_hvis_endringssøknad_men_beregningsandel_er_lagt_til() {
        var fomOpprinneligUttak = FØDSELSDATO;
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(fomOpprinneligUttak,
            fomOpprinneligUttak.plusWeeks(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER).build();
        var uttakAktivitet1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.FRILANS).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet1).medArbeidsprosent(
            BigDecimal.TEN).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var søknadsFom = FØDSELSDATO.plusWeeks(1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(søknadsFom, søknadsFom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var nyFordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);
        var revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRING_FRA_BRUKER, opprinneligUttak, nyFordeling);
        uttakBeregningsandelTjeneste.leggTilSelvNæringdrivende(Arbeidsgiver.virksomhet("123"));
        uttakBeregningsandelTjeneste.leggTilFrilans();

        // Act
        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØDSELSDATO);
    }

    @Test
    void arbeidsforholdref_null_object_skal_ikke_sette_endringsdato_første_dag_uttak() {
        var fomOpprinneligUttak = FØDSELSDATO;
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(fomOpprinneligUttak,
            fomOpprinneligUttak.plusWeeks(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var uttakAktivitet1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(arbeidsgiver, null).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet1).medArbeidsprosent(
            BigDecimal.TEN).build();
        var opprinneligUttak = List.of(opprinneligPeriode);
        var søknadsFom = FØDSELSDATO.plusWeeks(1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(søknadsFom, søknadsFom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var nyFordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);
        var revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRING_FRA_BRUKER, opprinneligUttak, nyFordeling);
        uttakBeregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, InternArbeidsforholdRef.nullRef());
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));

        // Act
        var endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(søknadsFom);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_gjeldende_vedtak_når_alle_perioder_er_tapt_til_annenpart() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morUttak = new UttakResultatPerioderEntitet();
        var morFom = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK;
        var morTom = morFom.plusDays(10);
        var morPeriode = new UttakResultatPeriodeEntitet.Builder(morFom,
            morTom).medResultatType(PeriodeResultatType.AVSLÅTT,
            PeriodeResultatÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK).build();
        morUttak.leggTilPeriode(morPeriode);
        morScenario.medUttak(morUttak);
        var førstegangsbehandling = morScenario.lagre(repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        revurderingScenario.medOriginalBehandling(førstegangsbehandling, RE_OPPLYSNINGER_OM_FORDELING);

        var revurdering = revurderingScenario.lagre(repositoryProvider);
        leggTilAktørArbeid(revurdering);
        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_gjeldende_vedtak_arbeidsforhold_har_en_aktivitet_i_første_uttaksperiode_har_startdato_etter_første_uttaksperiode() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morUttak = new UttakResultatPerioderEntitet();
        var morFom = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK;
        var morTom = morFom.plusDays(10);
        var morPeriode = new UttakResultatPeriodeEntitet.Builder(morFom, morTom).medResultatType(
            PeriodeResultatType.AVSLÅTT,
            PeriodeResultatÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK).build();
        var arbeidsgiver1 = Arbeidsgiver.virksomhet("123");
        var aktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(morPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(arbeidsgiver1, null)
                .build()).medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        var nyArbeidsgiver = Arbeidsgiver.virksomhet("456");
        var aktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(morPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(nyArbeidsgiver, null)
                .build()).medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        morPeriode.leggTilAktivitet(aktivitet1);
        morPeriode.leggTilAktivitet(aktivitet2);
        morUttak.leggTilPeriode(morPeriode);
        morScenario.medDefaultOppgittDekningsgrad().medUttak(morUttak);
        var førstegangsbehandling = morScenario.lagre(repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad();
        uttakBeregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver1, null);
        uttakBeregningsandelTjeneste.leggTilOrdinærtArbeid(nyArbeidsgiver, null);
        revurderingScenario.medOriginalBehandling(førstegangsbehandling, RE_ENDRING_FRA_BRUKER);
        var oppgittPeriodeEndringssøknad = OppgittPeriodeBuilder.ny()
            .medPeriode(morFom.plusDays(2), morTom)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        revurderingScenario.medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeEndringssøknad), true));

        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var startdatoNyttArbeidsforhold = morFom.plusDays(2);

        var iayBuilder = iayTjeneste.opprettBuilderForRegister(revurdering.getId());
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(revurdering.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(
            lagYrkesaktivitet(arbeidsgiver1, DatoIntervallEntitet.fraOgMed(morFom)));
        aktørArbeidBuilder.leggTilYrkesaktivitet(
            lagYrkesaktivitet(nyArbeidsgiver, DatoIntervallEntitet.fraOgMed(startdatoNyttArbeidsforhold)));
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(revurdering.getId(), iayBuilder);

        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    void endringsdato_skal_være_mors_første_dag_hvis_far_endrer_stønadskonto() {
        var behandling = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_MOR,
            testUtil.uttaksresultatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK), testUtil.søknadsAggregatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK));
        var førsteGangUttak = repositoryProvider.getFpUttakRepository().hentUttakResultat(behandling.getId());
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(),
            Stønadskontoberegning.builder().medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(230).build()).build(),
            førsteGangUttak.getOpprinneligPerioder());

        var fomFar = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusMonths(1);
        var behandlingFar = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_FAR,
            testUtil.uttaksresultatBerørtSak(fomFar), testUtil.søknadsAggregatBerørtSak(fomFar), behandling.getFagsak());
        var farUttak = repositoryProvider.getFpUttakRepository().hentUttakResultat(behandlingFar.getId());
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandlingFar.getId(),
            Stønadskontoberegning.builder().medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build()).build(),
            farUttak.getOpprinneligPerioder());

        var revurderingBerørtSak = testUtil.opprettRevurderingBerørtSak(AKTØR_ID_MOR, BERØRT_BEHANDLING,
            behandling);
        var revurderingÅrsak = BehandlingÅrsak.builder(BERØRT_BEHANDLING)
            .medOriginalBehandlingId(revurderingBerørtSak.getOriginalBehandlingId().get());
        revurderingÅrsak.buildFor(revurderingBerørtSak);

        var familieHendelser = new FamilieHendelser().medSøknadHendelse(
            FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1));
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medErBerørtBehandling(true)
            .medOriginalBehandling(new OriginalBehandling(behandling.getId(), familieHendelser))
            .medFamilieHendelser(familieHendelser)
            .medAnnenpart(new Annenpart(behandlingFar.getId(), FØDSELSDATO.atStartOfDay()));
        var iayGrunnlag = iayTjeneste.hentGrunnlag(revurderingBerørtSak.getId());
        var input = new UttakInput(BehandlingReferanse.fra(revurderingBerørtSak), null, iayGrunnlag,
            fpGrunnlag).medBehandlingÅrsaker(Set.of(BERØRT_BEHANDLING));

        var endringsdatoMor = utleder.utledEndringsdato(input);

        assertThat(endringsdatoMor).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtak_når_revurdering_er_pga_feil_praksis_utsettelse() {
        var revurdering = testUtil.opprettRevurdering(FEIL_PRAKSIS_UTSETTELSE);
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(FEIL_PRAKSIS_UTSETTELSE));

        var endringsdato = utleder.utledEndringsdato(input);

        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtak_når_revurdering_er_pga_feil_praksis_utsettelse_og_endringssøknad_mottatt() {
        var revurdering = testUtil.opprettRevurdering(FEIL_PRAKSIS_UTSETTELSE);
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(FEIL_PRAKSIS_UTSETTELSE, RE_ENDRING_FRA_BRUKER));

        var endringsdato = utleder.utledEndringsdato(input);

        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    private YrkesaktivitetBuilder lagYrkesaktivitet(Arbeidsgiver arbeidsgiver, DatoIntervallEntitet periode) {
        var aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(periode)
            .medProsentsats(BigDecimal.valueOf(50))
            .medSisteLønnsendringsdato(periode.getFomDato());
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder().medPeriode(periode);
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);
    }

    private UttakInput lagInput(Behandling behandling) {
        var årsaker = behandling.getBehandlingÅrsaker()
            .stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .collect(Collectors.toSet());
        return lagInput(behandling,
            FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1)).medBehandlingÅrsaker(årsaker);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelse bekreftetHendelse) {
        var ref = BehandlingReferanse.fra(behandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(new OriginalBehandling(behandling.getOriginalBehandlingId().get(),
                new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse)));
        return new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(bekreftetHendelse.getFamilieHendelseDato()).build(), iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(
            uttakBeregningsandelTjeneste.hentStatuser());
    }

    private UttakInput lagInput(Behandling behandling, LocalDate startdatoNySak) {
        var årsaker = behandling.getBehandlingÅrsaker()
            .stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .collect(Collectors.toSet());
        return lagInput(behandling,
            FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1), startdatoNySak).medBehandlingÅrsaker(årsaker);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelse bekreftetHendelse, LocalDate startdatoNySak) {
        var ref = BehandlingReferanse.fra(behandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(new OriginalBehandling(behandling.getOriginalBehandlingId().get(),
                new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse)))
            .medNesteSakGrunnlag(NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medSaksnummer(new Saksnummer("1234")).medBehandlingId(behandling.getId())
                .medStartdato(startdatoNySak).medHendelsedato(startdatoNySak.plusWeeks(3)).build());
        return new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(bekreftetHendelse.getFamilieHendelseDato()).build(), iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(
            uttakBeregningsandelTjeneste.hentStatuser());
    }

    private void leggTilAktørArbeid(Behandling revurdering) {
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(revurdering.getId());
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(
            AKTØR_ID_MOR);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.person(AKTØR_ID_MOR));
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(revurdering.getId(), iayBuilder);
    }

    private void leggTilFpsakYtelse(Behandling revurdering) {
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(revurdering.getId());
        var aktørYtelseBuilder = iayBuilder.getAktørYtelseBuilder(
            AKTØR_ID_MOR);
        var ytelselseBuilder = aktørYtelseBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK,
            RelatertYtelseType.FORELDREPENGER, new Saksnummer("01"));
        ytelselseBuilder.tilbakestillAnvisteYtelser();
        var ytelse = ytelselseBuilder.medKilde(Fagsystem.FPSAK)
            .medYtelseType(RelatertYtelseType.FORELDREPENGER)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medSaksnummer(revurdering.getFagsak().getSaksnummer())
            .medPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(6)));
        aktørYtelseBuilder.leggTilYtelse(ytelse);
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(revurdering.getId(), iayBuilder);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);
    }

}
