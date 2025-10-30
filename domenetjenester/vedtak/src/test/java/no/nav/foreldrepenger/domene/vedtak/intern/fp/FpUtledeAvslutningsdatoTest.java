package no.nav.foreldrepenger.domene.vedtak.intern.fp;


import static no.nav.foreldrepenger.domene.vedtak.intern.fp.FpUtledeAvslutningsdato.PADDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.fp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FpUtledeAvslutningsdatoTest {

    private FpUtledeAvslutningsdato fpUtledeAvslutningsdato;
    private static final int SØKNADSFRIST_I_MÅNEDER = 3;

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private FpUttakRepository fpUttakRepository;

    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    @Mock
    private SaldoUtregning saldoUtregning;

    @Mock
    private UttakInputTjeneste uttakInputTjeneste;


    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        when(stønadskontoSaldoTjeneste.finnSaldoUtregning(any(UttakInput.class))).thenReturn(saldoUtregning);

        var maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(fpUttakRepository,
            stønadskontoSaldoTjeneste);

        var fagsakLåsRepository = mock(FagsakLåsRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);

        fpUtledeAvslutningsdato = new FpUtledeAvslutningsdato(repositoryProvider,
            stønadskontoSaldoTjeneste, uttakInputTjeneste, maksDatoUttakTjeneste, fagsakRelasjonTjeneste);

        behandling = lagBehandling();
        fagsak = behandling.getFagsak();

    }

    @Test
    void avslutningsdatoHvorIngenFamiliehendelseEllerAvslutningsdato() {
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        var fødselsdato = LocalDate.now().minusDays(5);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));

        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, new ForeldrepengerGrunnlag()));

        var forventetAvslutningsdato = LocalDate.now().plusDays(1);

        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isEqualTo(forventetAvslutningsdato);
    }

    @Test
    void avslutningsdatoHvorAlleBarnaDøde() {
        // Arrange
        var fødselsdato = LocalDate.now().minusDays(5);
        var dødsdato = fødselsdato.plusWeeks(1);
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));

        var familieHendelseDødeBarn = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(new Barn(dødsdato)), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medSøknadHendelse(familieHendelseDødeBarn));
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));

        var forventetAvslutningsdatoMin = dødsdato.plusDays(1)
            .plusWeeks(UttakParametre.ukerTilgjengeligEtterDødsfall(LocalDate.now()))
            .plusMonths(SØKNADSFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth());
        var forventetAvslutningsdatoMax = forventetAvslutningsdatoMin.plusDays(PADDING-1);

        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isBetween(forventetAvslutningsdatoMin, forventetAvslutningsdatoMax);
    }

    @Test
    void avslutningsdatoHvorDetIkkeErUttak() {

        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        var fødselsdato = LocalDate.now().minusDays(5);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));

        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse));
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));

        var forventetAvslutningsdato = fødselsdato.plusYears(UttakParametre.årMaksimalStønadsperiode(LocalDate.now()));
        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isEqualTo(forventetAvslutningsdato);

    }
    @Test
    void avslutningsdatoVedOpphørOgIkkeKobletTilAnnenPart() {
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        var fødselsdato = LocalDate.now().minusDays(5);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak))
            .thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), null, null, Dekningsgrad._80, null)));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.OPPHØR));

        var periodeStartDato = LocalDate.now().minusDays(10);
        var periodeAvsluttetDato = LocalDate.now().plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse));
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(0);
        when(stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning)).thenReturn(0);

        var forventetAvslutningsdatoMin = VirkedagUtil.tomVirkedag(periodeAvsluttetDato).plusDays(1).plusMonths(SØKNADSFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth());
        var forventetAvslutningsdatoMax = forventetAvslutningsdatoMin.plusDays(PADDING-1);
        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isBetween(forventetAvslutningsdatoMin, forventetAvslutningsdatoMax);
    }

    @Test
    void avslutningsdatoVedOpphørOgErKobletTilAnnenPart() {
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        var fødselsdato = LocalDate.now().minusDays(5);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak))
            .thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), behandling.getFagsak(), null, Dekningsgrad._80, null)));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.OPPHØR));

        var periodeStartDato = LocalDate.now().minusDays(10);
        var periodeAvsluttetDato = LocalDate.now().plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse));
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(0);
        when(stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning)).thenReturn(0);

        var forventetAvslutningsdatoMin = VirkedagUtil.tomVirkedag(periodeAvsluttetDato).plusDays(1).plusMonths(SØKNADSFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth());
        var forventetAvslutningsdatoMax = forventetAvslutningsdatoMin.plusDays(PADDING-1);
        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isBetween(forventetAvslutningsdatoMin, forventetAvslutningsdatoMax);
    }

    @Test
    void avslutningsdatoHvorStønadsdagerErOppbrukt() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        var fødselsdato = LocalDate.now().minusDays(5);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));

        var periodeStartDato = LocalDate.now().minusDays(10);
        var periodeAvsluttetDato = LocalDate.now().plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse));
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(0);
        when(stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning)).thenReturn(0);

        var forventetAvslutningsdatoMin = VirkedagUtil.tomVirkedag(periodeAvsluttetDato).plusDays(1).plusMonths(SØKNADSFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth());
        var forventetAvslutningsdatoMax = forventetAvslutningsdatoMin.plusDays(PADDING-1);
        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isBetween(forventetAvslutningsdatoMin, forventetAvslutningsdatoMax);
    }

    @Test
    void avslutningsdatoHvorDetErStønadsdagerIgjen() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET));

        var dato = LocalDate.now();
        var fødselsdato = dato.minusDays(5);
        var periodeStartDato = dato.minusDays(10);
        var periodeAvsluttetDato = dato.plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(periodeStartDato, periodeAvsluttetDato));


        var familieHendelse2 = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse2));
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));

        var totalRest = 3;
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(1);
        when(stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning)).thenReturn(totalRest); //summen for de tre stønadskotoene

        var forventetAvslutningsdato = fødselsdato.plusYears(3);

        // Assert and act
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isEqualTo(forventetAvslutningsdato);
    }

    @Test
    void avslutningsdatoNårNyttBarn() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET));

        var dato = LocalDate.now();
        var fødselsdato = dato.minusMonths(20);
        var fødselsdatoNyttBarn = dato.plusWeeks(2);
        var periodeAvsluttetDato = dato.plusDays(20);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(fødselsdato, periodeAvsluttetDato));


        var familieHendelse2 = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse2))
            .medNesteSakGrunnlag(NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medSaksnummer(new Saksnummer("1234")).medBehandlingId(1234L)
                .medStartdato(fødselsdatoNyttBarn).medHendelsedato(fødselsdatoNyttBarn.plusWeeks(3)).build());
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));

        var trekkdager = new Trekkdager(0);
        when(saldoUtregning.restSaldoEtterNesteStønadsperiode()).thenReturn(trekkdager);

        var forventetAvslutningsdatoMin = fødselsdatoNyttBarn.plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.lastDayOfMonth());
        var forventetAvslutningsdatoMax = forventetAvslutningsdatoMin.plusDays(PADDING-1);
        // Act and assert
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isBetween(forventetAvslutningsdatoMin, forventetAvslutningsdatoMax);
    }

    @Test
    void avslutningsdatoNårNyttBarnMedToTetteOgRestdagerIgjenr() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET));

        var dato = LocalDate.now();
        var fødselsdato = dato.minusMonths(10);
        var fødselsdatoNyttBarn = dato.plusWeeks(2);
        var periodeAvsluttetDato = dato.plusDays(20);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(fødselsdato, periodeAvsluttetDato));


        var familieHendelse2 = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse2))
            .medNesteSakGrunnlag(NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medSaksnummer(new Saksnummer("1234")).medBehandlingId(1234L)
                .medStartdato(fødselsdatoNyttBarn).medHendelsedato(fødselsdatoNyttBarn.plusWeeks(3)).build());
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(fødselsdato))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, ytelsespesifiktGrunnlag));

        var totalRest = 22;
        var trekkdager = new Trekkdager(22);
        when(saldoUtregning.restSaldoEtterNesteStønadsperiode()).thenReturn(trekkdager);
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(22);
        when(stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning)).thenReturn(totalRest); //summen for de tre stønadskotoene

        var forventetAvslutningsdato = fødselsdato.plusYears(3);

        // Assert and act
        assertThat(fpUtledeAvslutningsdato.utledAvslutningsdato(fagsak.getId(), fagsakRelasjon)).isEqualTo(forventetAvslutningsdato);
    }


    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        return scenario.lagMocked();
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.ENDRING_I_UTTAK)
            .buildFor(behandling));
    }

    private Optional<UttakResultatEntitet> lagUttakResultat(LocalDate fom, LocalDate tom) {
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build();
        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        var behandlingsresultat = new Behandlingsresultat.Builder().build();

        return Optional.of(
            new UttakResultatEntitet.Builder(behandlingsresultat).medOpprinneligPerioder(perioder).build());
    }
}
