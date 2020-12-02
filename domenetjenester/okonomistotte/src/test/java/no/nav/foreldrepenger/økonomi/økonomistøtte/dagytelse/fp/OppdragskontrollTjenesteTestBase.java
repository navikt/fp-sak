package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactory;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactoryProvider;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OpprettBehandlingForOppdrag;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollManagerFactoryDagYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.SjekkOmDetFinnesTilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter.BehandlingTilOppdragMapperTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter.MapBehandlingVedtak;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.endring.OppdragskontrollEndring;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.førstegangsoppdrag.OppdragskontrollFørstegang;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.OppdragskontrollOpphør;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.OpprettOpphørIEndringsoppdrag;
import no.nav.vedtak.felles.testutilities.db.Repository;

@CdiDbAwareTest
public abstract class OppdragskontrollTjenesteTestBase {

    static final String TYPE_SATS_FP_FERIEPG = "ENG";
    static final String TYPE_SATS_FP_YTELSE = "DAG";
    static final String ARBEIDSFORHOLD_ID = "999999999";
    static final String ARBEIDSFORHOLD_ID_2 = "123456789";
    static final String ARBEIDSFORHOLD_ID_3 = "789123456";
    static final String ARBEIDSFORHOLD_ID_4 = "654321987";
    static final Long AKTØR_ID = 1234567891234L;

    static final LocalDate DAGENS_DATO = LocalDate.now();
    static final int I_ÅR = DAGENS_DATO.getYear();
    static final List<Integer> FERIEPENGEÅR_LISTE = List.of(DAGENS_DATO.plusYears(1).getYear(),
        DAGENS_DATO.plusYears(2).getYear());
    protected Repository repository;
    protected ØkonomioppdragRepository økonomioppdragRepository;
    protected BehandlingRepositoryProvider repositoryProvider;
    protected BehandlingRepository behandlingRepository;
    protected BeregningsresultatRepository beregningsresultatRepository;
    protected FpUttakRepository fpUttakRepository;
    protected FamilieHendelseRepository familieHendelseRepository;
    protected OppdragskontrollTjeneste oppdragskontrollTjeneste;
    protected TilbakekrevingRepository tilbakekrevingRepository;

    Behandling behandling;
    Fagsak fagsak;
    PersonIdent personIdent = PersonIdent.fra("12345678901");
    BehandlingVedtak behVedtak;

    protected String virksomhet = ARBEIDSFORHOLD_ID;
    protected String virksomhet2 = ARBEIDSFORHOLD_ID_2;
    protected String virksomhet3 = ARBEIDSFORHOLD_ID_3;
    protected String virksomhet4 = ARBEIDSFORHOLD_ID_4;
    private EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setUp() {
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        fpUttakRepository = new FpUttakRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);

        PersoninfoAdapter personinfoAdapterMock = mock(PersoninfoAdapter.class);

        HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering = new HentOppdragMedPositivKvittering(
            økonomioppdragRepository);
        SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse = new SjekkOmDetFinnesTilkjentYtelse(
            beregningsresultatRepository);
        MapBehandlingVedtak mapBehandlingVedtakFP = new MapBehandlingVedtak(
            repositoryProvider.getBehandlingVedtakRepository());
        BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjenesteFP = new BehandlingTilOppdragMapperTjeneste(
            hentOppdragMedPositivKvittering, mapBehandlingVedtakFP, personinfoAdapterMock, tilbakekrevingRepository,
            beregningsresultatRepository, familieHendelseRepository, sjekkOmDetFinnesTilkjentYtelse);
        OppdragskontrollFørstegang oppdragskontrollFørstegangFP = new OppdragskontrollFørstegang(
            behandlingTilOppdragMapperTjenesteFP);
        OppdragskontrollOpphør oppdragskontrollOpphørFP = new OppdragskontrollOpphør(behandlingTilOppdragMapperTjenesteFP);
        OpprettOpphørIEndringsoppdrag opprettOpphørIEndringsoppdragBruker = new OpprettOpphørIEndringsoppdrag(
            oppdragskontrollOpphørFP);
        OppdragskontrollEndring oppdragskontrollEndringFP = new OppdragskontrollEndring(
            behandlingTilOppdragMapperTjenesteFP, opprettOpphørIEndringsoppdragBruker);
        OppdragskontrollManagerFactory oppdragskontrollManagerFactory = new OppdragskontrollManagerFactoryDagYtelse(
            oppdragskontrollFørstegangFP, oppdragskontrollEndringFP, oppdragskontrollOpphørFP,
            sjekkOmDetFinnesTilkjentYtelse);

        OppdragskontrollManagerFactoryProvider factoryProviderMock = mock(OppdragskontrollManagerFactoryProvider.class);
        lenient().when(factoryProviderMock.getTjeneste(any(FagsakYtelseType.class)))
            .thenReturn(oppdragskontrollManagerFactory);

        oppdragskontrollTjeneste = new OppdragskontrollTjenesteImpl(repositoryProvider, økonomioppdragRepository, factoryProviderMock);

        behandling = opprettOgLagreBehandling(FamilieYtelseType.FØDSEL);

        lenient().when(personinfoAdapterMock.hentFnrForAktør(any(AktørId.class))).thenReturn(personIdent);
    }

    protected Behandling opprettOgLagreBehandlingFPForSammeFagsak(Fagsak fagsak) {
        Behandling behandlingFP = Behandling.forFørstegangssøknad(fagsak).build();
        final FamilieHendelseBuilder hendelse = familieHendelseRepository.opprettBuilderFor(behandlingFP);
        hendelse.medTerminbekreftelse(hendelse.getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now().plusDays(40))
            .medUtstedtDato(LocalDate.now().minusDays(7))
            .medNavnPå("Navn"));
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandlingFP);
        behandlingRepository.lagre(behandlingFP, lås);
        familieHendelseRepository.lagre(behandlingFP, hendelse);
        Behandlingsresultat.builderForInngangsvilkår()
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .medRettenTil(RettenTil.HAR_RETT_TIL_FP)
            .medVedtaksbrev(Vedtaksbrev.INGEN)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandlingFP);
        behandlingRepository.lagre(getBehandlingsresultat(behandlingFP).getVilkårResultat(), lås);
        repository.lagre(getBehandlingsresultat(behandlingFP));
        behVedtak = OpprettBehandlingForOppdrag.opprettBehandlingVedtak(behandlingFP,
            getBehandlingsresultat(behandlingFP), VedtakResultatType.INNVILGET);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behVedtak, lås);
        repository.flush();
        return behandlingFP;
    }

    protected Behandling opprettOgLagreBehandling(FamilieYtelseType familieYtelseType) {
        AbstractTestScenario<?> scenario;
        if (FamilieYtelseType.SVANGERSKAPSPENGER.equals(familieYtelseType)) {
            scenario = scenarioSvangerskapspenger();
        } else {
            scenario = scenarioForeldrepenger(familieYtelseType);
        }
        behandling = scenario.lagre(repositoryProvider);
        fagsak = scenario.getFagsak();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat.builderForInngangsvilkår()
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .medRettenTil(RettenTil.HAR_RETT_TIL_FP)
            .medVedtaksbrev(Vedtaksbrev.INGEN)
            .buildFor(behandling);

        behandlingRepository.lagre(getBehandlingsresultat(behandling).getVilkårResultat(), lås);
        repository.lagre(getBehandlingsresultat(behandling));

        behVedtak = OpprettBehandlingForOppdrag.opprettBehandlingVedtak(behandling, getBehandlingsresultat(behandling),
            VedtakResultatType.INNVILGET);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behVedtak, lås);

        repository.flush();

        return behandling;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private ScenarioMorSøkerForeldrepenger scenarioForeldrepenger(FamilieYtelseType familieYtelseType) {
        if (FamilieYtelseType.FØDSEL.equals(familieYtelseType)) {
            ScenarioMorSøkerForeldrepenger scenarioFødsel = ScenarioMorSøkerForeldrepenger.forFødsel();
            scenarioFødsel.medSøknadHendelse()
                .medTerminbekreftelse(scenarioFødsel.medSøknadHendelse()
                    .getTerminbekreftelseBuilder()
                    .medTermindato(LocalDate.now().plusMonths(1)))
                .medAntallBarn(1);
            scenarioFødsel.medBekreftetHendelse()
                .medTerminbekreftelse(scenarioFødsel.medBekreftetHendelse()
                    .getTerminbekreftelseBuilder()
                    .medTermindato(LocalDate.now().plusMonths(1)))
                .medAntallBarn(1);
            return scenarioFødsel;
        }
        ScenarioMorSøkerForeldrepenger scenarioAdopsjon = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenarioAdopsjon.medSøknadHendelse()
            .medAdopsjon(scenarioAdopsjon.medSøknadHendelse()
                .getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now())
                .medAnkomstDato(LocalDate.now()));
        return scenarioAdopsjon;
    }

    private ScenarioMorSøkerSvangerskapspenger scenarioSvangerskapspenger() {
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(
                scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(LocalDate.now().plusMonths(1)))
            .medAntallBarn(1);
        return scenario;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFP() {
        return buildBeregningsresultatFP(false);
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFP(boolean medFeriepenger) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        BeregningsresultatAndel andelBruker = buildBeregningsresultatAndel(brPeriode1, true, 1500,
            BigDecimal.valueOf(80), virksomhet);
        BeregningsresultatAndel andelArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 500,
            BigDecimal.valueOf(100), virksomhet);

        BeregningsresultatPeriode brPeriode3 = buildBeregningsresultatPeriode(beregningsresultat, 16, 22);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(80), virksomhet3);
        BeregningsresultatAndel andelArbeidsforhold3 = buildBeregningsresultatAndel(brPeriode3, false, 2160,
            BigDecimal.valueOf(80), virksomhet3);

        BeregningsresultatPeriode brPeriode4 = buildBeregningsresultatPeriode(beregningsresultat, 23, 30);
        buildBeregningsresultatAndel(brPeriode4, true, 2160, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode4, false, 0, BigDecimal.valueOf(80), virksomhet3);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet2);
        BeregningsresultatAndel andelArbeidsforhold2 = buildBeregningsresultatAndel(brPeriode2, false, 450,
            BigDecimal.valueOf(100), virksomhet2);

        if (medFeriepenger) {
            BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 20000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold, 15000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold2, 20000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold3, 20000L,
                List.of(DAGENS_DATO, DAGENS_DATO.plusYears(1)));
        }

        return beregningsresultat;
    }

    protected BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                   Boolean brukerErMottaker,
                                                                   int dagsats,
                                                                   BigDecimal utbetalingsgrad,
                                                                   String virksomhetOrgnr) {
        return buildBeregningsresultatAndel(beregningsresultatPeriode, brukerErMottaker, dagsats, utbetalingsgrad,
            virksomhetOrgnr, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
    }

    protected BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                   Boolean brukerErMottaker,
                                                                   int dagsats,
                                                                   BigDecimal utbetalingsgrad,
                                                                   String virksomhetOrgnr,
                                                                   AktivitetStatus aktivitetStatus,
                                                                   Inntektskategori inntektskategori) {
        BeregningsresultatAndel.Builder andelBuilder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(brukerErMottaker);
        if (!AktivitetStatus.FRILANSER.equals(aktivitetStatus) && virksomhetOrgnr != null) {
            andelBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr));
        }
        if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus) && virksomhetOrgnr == null) {
            andelBuilder.medArbeidsgiver(Arbeidsgiver.person(new AktørId(AKTØR_ID)));
        }
        return andelBuilder.medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(utbetalingsgrad)
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(inntektskategori)
            .build(beregningsresultatPeriode);
    }

    protected BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                       int fom,
                                                                       int tom) {
        return buildBeregningsresultatPeriode(beregningsresultat, DAGENS_DATO.plusDays(fom), DAGENS_DATO.plusDays(tom));
    }

    protected BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                       LocalDate fom,
                                                                       LocalDate tom) {

        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    protected BeregningsresultatFeriepenger buildBeregningsresultatFeriepenger(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(DAGENS_DATO.plusDays(1))
            .medFeriepengerPeriodeTom(DAGENS_DATO.plusDays(29))
            .medFeriepengerRegelInput("clob1")
            .medFeriepengerRegelSporing("clob2")
            .build(beregningsresultat);
    }

    protected void buildBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepenger beregningsresultatFeriepenger,
                                                          BeregningsresultatAndel andel,
                                                          Long årsBeløp,
                                                          List<LocalDate> opptjeningsårList) {
        for (LocalDate opptjeningsår : opptjeningsårList) {
            buildBeregningsresultatFeriepengerPrÅr(beregningsresultatFeriepenger, andel, årsBeløp, opptjeningsår);
        }
    }

    protected void buildBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepenger beregningsresultatFeriepenger,
                                                          BeregningsresultatAndel andel,
                                                          Long årsBeløp,
                                                          LocalDate opptjeningsår) {
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(opptjeningsår)
            .medÅrsbeløp(årsBeløp)
            .build(beregningsresultatFeriepenger, andel);
    }

    protected Behandling opprettOgLagreRevurdering(Behandling originalBehandling,
                                                   VedtakResultatType resultat,
                                                   boolean gjelderOpphør,
                                                   boolean gjelderEndring) {

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL)
                .medOriginalBehandlingId(originalBehandling.getId()))
            .build();

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);
        repositoryProvider.getFamilieHendelseRepository()
            .kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
        OpprettBehandlingForOppdrag.genererBehandlingOgResultatFP(revurdering);
        behandlingRepository.lagre(getBehandlingsresultat(revurdering).getVilkårResultat(), behandlingLås);
        if (gjelderOpphør) {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(revurdering);
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medBehandlingResultatType(BehandlingResultatType.OPPHØR);
        } else if (gjelderEndring) {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(revurdering);
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medBehandlingResultatType(BehandlingResultatType.FORELDREPENGER_ENDRET);
        } else {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(revurdering);
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        }
        repository.lagre(getBehandlingsresultat(revurdering));

        BehandlingVedtak behandlingVedtak = OpprettBehandlingForOppdrag.opprettBehandlingVedtak(revurdering,
            getBehandlingsresultat(revurdering), resultat);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, behandlingLås);
        repository.flush();

        return revurdering;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatMedFlereInntektskategoriFP(boolean medFeriepenger) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        BeregningsresultatAndel andelBruker = buildBeregningsresultatAndel(brPeriode1, true, 1500,
            BigDecimal.valueOf(80), virksomhet);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet2,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatAndel andelArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 500,
            BigDecimal.valueOf(100), virksomhet);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet2);
        BeregningsresultatAndel andelArbeidsforhold2 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), virksomhet2);

        if (medFeriepenger) {
            BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 20000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold, 15000L, List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsforhold2, 20000L, List.of(DAGENS_DATO));
        }

        return beregningsresultat;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatMedFlereAndelerSomArbeidsgiver() {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet2,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(brPeriode1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode1, false, 1000, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet2);
        buildBeregningsresultatAndel(brPeriode2, false, 400, BigDecimal.valueOf(100), virksomhet2);

        return beregningsresultat;
    }

    protected void verifiserOppdrag110_ENDR(Oppdragskontroll oppdragskontroll,
                                            List<Oppdrag110> originaltOpp110Liste,
                                            boolean medFeriepenger) {
        List<Oppdrag110> nyOppdr110Liste = oppdragskontroll.getOppdrag110Liste();
        for (Oppdrag110 oppdr110Revurd : nyOppdr110Liste) {
            Optional<Refusjonsinfo156> refusjonsinfo156 = oppdr110Revurd.getOppdragslinje150Liste()
                .stream()
                .map(Oppdragslinje150::getRefusjonsinfo156)
                .filter(Objects::nonNull)
                .filter(r -> r.getRefunderesId()
                    .equals(OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_4)))
                .findFirst();
            if (refusjonsinfo156.isPresent()) {
                Oppdrag110 opp110 = refusjonsinfo156.get().getOppdragslinje150().getOppdrag110();
                assertThat(opp110.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.NY.name());
            } else {
                assertThat(oppdr110Revurd.getKodeEndring()).isEqualTo(ØkonomiKodeFagområde.FP.name()
                    .equals(
                        oppdr110Revurd.getKodeFagomrade()) ? ØkonomiKodeEndring.ENDR.name() : ØkonomiKodeEndring.UEND.name());
            }
            assertThat(oppdr110Revurd.getOppdragslinje150Liste()).isNotEmpty();
            boolean nyMottaker = erMottakerNy(oppdr110Revurd);
            if (!nyMottaker) {
                assertThat(originaltOpp110Liste).anySatisfy(
                    oppdrag110 -> assertThat(oppdrag110.getFagsystemId()).isEqualTo(oppdr110Revurd.getFagsystemId()));
            }
        }
        if (medFeriepenger) {
            List<Oppdragslinje150> opp150List = nyOppdr110Liste.stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .collect(Collectors.toList());
            assertThat(opp150List).anySatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(
                ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()));
            assertThat(opp150List).anySatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(
                ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()));
        }
    }

    private boolean erMottakerNy(Oppdrag110 oppdr110Revurd) {
        return ØkonomiKodeEndring.NY.name().equals(oppdr110Revurd.getKodeEndring());
    }

    protected BeregningsresultatEntitet buildBeregningsresultatBrukerFP(LocalDate endringsdato,
                                                                        int dagsatsBruker,
                                                                        int dagsatsArbeidsgiver,
                                                                        LocalDate... perioder) {
        return buildBeregningsresultatBrukerFP(endringsdato, List.of(dagsatsBruker), List.of(dagsatsArbeidsgiver),
            perioder);
    }

    protected BeregningsresultatEntitet buildBeregningsresultatBrukerFP(LocalDate endringsdato,
                                                                        List<Integer> dagsatsBruker,
                                                                        List<Integer> dagsatsArbeidsgiver,
                                                                        LocalDate... perioder) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();

        BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        for (int i = 0; i < dagsatsBruker.size(); i++) {
            var fom = perioder[i * 2];
            var tom = perioder[i * 2 + 1];
            BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat, fom, tom);
            BeregningsresultatAndel andelBruker = buildBeregningsresultatAndel(brPeriode, true, dagsatsBruker.get(i),
                BigDecimal.valueOf(100), virksomhet);
            if (dagsatsArbeidsgiver.get(i) != 0) {
                BeregningsresultatAndel andelArbeidsgiver = buildBeregningsresultatAndel(brPeriode, false,
                    dagsatsArbeidsgiver.get(i), BigDecimal.valueOf(100), virksomhet);
                buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver, 5000L, List.of(DAGENS_DATO));
            }
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 5000L, List.of(DAGENS_DATO));
        }

        return beregningsresultat;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatMedFlereInntektskategoriFP(boolean sammeKlasseKodeForFlereAndeler,
                                                                                          AktivitetStatus aktivitetStatus,
                                                                                          Inntektskategori inntektskategori) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        BeregningsresultatAndel andelBruker = buildBeregningsresultatAndel(brPeriode1, true, 1500,
            BigDecimal.valueOf(80), virksomhet, aktivitetStatus, inntektskategori);
        if (sammeKlasseKodeForFlereAndeler) {
            buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet2,
                AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        }
        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 8, 15);
        BeregningsresultatAndel andelArbeidsgiver2 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), virksomhet2);

        BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker, 20000L, List.of(DAGENS_DATO));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver2, 20000L, List.of(DAGENS_DATO));

        return beregningsresultat;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatEntenForBrukerEllerArbgvr(boolean erBrukerMottaker,
                                                                                         boolean medFeriepenger) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        BeregningsresultatAndel andel1 = buildBeregningsresultatAndel(brPeriode1, erBrukerMottaker, 1500,
            BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode2, erBrukerMottaker, 1500, BigDecimal.valueOf(100), virksomhet);
        if (medFeriepenger) {
            BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andel1, 20000L, List.of(DAGENS_DATO));
        }

        return beregningsresultat;
    }

    private BeregningsresultatEntitet buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(Optional<LocalDate> endringsdatoOpt,
                                                                                                     boolean erOpptjentOverFlereÅr,
                                                                                                     Long årsbeløp1,
                                                                                                     Long årsbeløp2) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdatoOpt.ifPresent(builder::medEndringsdato);
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        BeregningsresultatAndel andel1 = buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(100),
            virksomhet);
        BeregningsresultatAndel andel2 = buildBeregningsresultatAndel(brPeriode1, false, 1300, BigDecimal.valueOf(100),
            virksomhet);
        BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(beregningsresultat);
        oppsettFeriepenger(erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2, andel1, feriepenger);
        oppsettFeriepenger(erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2, andel2, feriepenger);

        return beregningsresultat;
    }

    private void oppsettFeriepenger(boolean erOpptjentOverFlereÅr,
                                    Long årsbeløp1,
                                    Long årsbeløp2,
                                    BeregningsresultatAndel andel1,
                                    BeregningsresultatFeriepenger feriepenger) {
        List<LocalDate> opptjeningsårListe;
        if (erOpptjentOverFlereÅr) {
            opptjeningsårListe = List.of(DAGENS_DATO, DAGENS_DATO.plusYears(1));
        } else if (årsbeløp1 > 0 || årsbeløp2 > 0) {
            opptjeningsårListe = årsbeløp2 > 0 ? List.of(DAGENS_DATO.plusYears(1)) : List.of(DAGENS_DATO);
        } else {
            opptjeningsårListe = Collections.emptyList();
        }
        List<Long> årsbeløpListe = List.of(årsbeløp1, årsbeløp2);
        int size = opptjeningsårListe.size();
        for (int i = 0; i < årsbeløpListe.size(); i++) {
            Long årsbeløp = årsbeløpListe.get(i);
            if (årsbeløp > 0) {
                LocalDate opptjeningsår = size == 2 ? opptjeningsårListe.get(i) : opptjeningsårListe.get(0);
                buildBeregningsresultatFeriepengerPrÅr(feriepenger, andel1, årsbeløp, opptjeningsår);
            }
        }
    }

    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingFP(boolean medFeriepenger,
                                                                             LocalDate endringsdato) {
        BeregningsresultatEntitet beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet);
        BeregningsresultatAndel andelRevurderingArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 500,
            BigDecimal.valueOf(100), virksomhet);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 8, 15);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet4);
        BeregningsresultatAndel andelRevurderingArbeidsforhold4 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), virksomhet4);

        BeregningsresultatPeriode brPeriode3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 16, 22);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode3, false, 2160, BigDecimal.valueOf(80), virksomhet3);

        BeregningsresultatPeriode brPeriode4 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 23, 29);
        buildBeregningsresultatAndel(brPeriode4, true, 2160, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode4, false, 0, BigDecimal.valueOf(80), virksomhet3);

        if (medFeriepenger) {
            BeregningsresultatFeriepenger feriepengerRevurdering = buildBeregningsresultatFeriepenger(
                beregningsresultatRevurderingFP);
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold, 15000L,
                List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold4, 15000L,
                List.of(DAGENS_DATO));
        }
        return beregningsresultatRevurderingFP;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingFP(AktivitetStatus aktivitetStatus,
                                                                             Inntektskategori inntektskategori,
                                                                             LocalDate endringsdato) {

        return buildBeregningsresultatRevurderingFP(aktivitetStatus, inntektskategori, virksomhet, virksomhet4,
            endringsdato, true);
    }

    /**
     * Lag to perioder, hver med en andel med oppgitt {@link AktivitetStatus} og {@link Inntektskategori}.
     *
     * @param aktivitetStatus
     * @param inntektskategori
     * @param førsteVirksomhetOrgnr
     * @param andreVirksomhetOrgnr
     * @param endringsdato
     * @param medFeriepenger
     * @return
     */
    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingFP(AktivitetStatus aktivitetStatus,
                                                                             Inntektskategori inntektskategori,
                                                                             String førsteVirksomhetOrgnr,
                                                                             String andreVirksomhetOrgnr,
                                                                             LocalDate endringsdato,
                                                                             boolean medFeriepenger) {
        BeregningsresultatEntitet beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1600, BigDecimal.valueOf(80), førsteVirksomhetOrgnr,
            aktivitetStatus, inntektskategori);
        BeregningsresultatAndel andelRevurderingArbeidsforhold = buildBeregningsresultatAndel(brPeriode1, false, 400,
            BigDecimal.valueOf(100), førsteVirksomhetOrgnr, aktivitetStatus, inntektskategori);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 8, 20);
        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), andreVirksomhetOrgnr,
            aktivitetStatus, inntektskategori);
        BeregningsresultatAndel andelRevurderingArbeidsforhold4 = buildBeregningsresultatAndel(brPeriode2, false, 400,
            BigDecimal.valueOf(100), andreVirksomhetOrgnr, aktivitetStatus, inntektskategori);

        if (medFeriepenger) {
            BeregningsresultatFeriepenger feriepengerRevurdering = buildBeregningsresultatFeriepenger(
                beregningsresultatRevurderingFP);
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold, 15000L,
                List.of(DAGENS_DATO));
            buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsforhold4, 15000L,
                List.of(DAGENS_DATO));
        }

        return beregningsresultatRevurderingFP;
    }

    /**
     * Lag to perioder.
     * Periode 1: Lag andel for {@link AktivitetStatus#ARBEIDSTAKER}.
     * Periode 2: Lag andel for {@link AktivitetStatus#ARBEIDSTAKER} og en annen oppgitt {@link AktivitetStatus} og {@link Inntektskategori}.
     *
     * @param aktivitetStatus  en {@link AktivitetStatus}
     * @param inntektskategori en {@link Inntektskategori}
     * @param endringsdato     en endringsdato
     * @return Beregningsresultat
     */
    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus aktivitetStatus,
                                                                                                     Inntektskategori inntektskategori,
                                                                                                     LocalDate endringsdato) {
        BeregningsresultatEntitet beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1600, BigDecimal.valueOf(80), virksomhet4);

        BeregningsresultatAndel andelRevurderingArbeidsiver = buildBeregningsresultatAndel(brPeriode1, false, 400,
            BigDecimal.valueOf(100), virksomhet4, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 8, 15);

        buildBeregningsresultatAndel(brPeriode2, true, 1600, BigDecimal.valueOf(80), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(brPeriode2, true, 1500, BigDecimal.valueOf(80), virksomhet4, aktivitetStatus,
            inntektskategori);

        BeregningsresultatFeriepenger feriepengerRevurdering = buildBeregningsresultatFeriepenger(
            beregningsresultatRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingArbeidsiver, 16000L,
            List.of(DAGENS_DATO));

        return beregningsresultatRevurderingFP;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(boolean erBrukerMottaker,
                                                                                                    boolean medFeriepenger,
                                                                                                    LocalDate endringsdato) {
        BeregningsresultatEntitet beregningsresultatRevurderingFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .medEndringsdato(endringsdato)
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 8);
        BeregningsresultatAndel andel1 = buildBeregningsresultatAndel(brPeriode1, erBrukerMottaker, 2000,
            BigDecimal.valueOf(100), virksomhet);
        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 9, 20);
        buildBeregningsresultatAndel(brPeriode2, erBrukerMottaker, 1500, BigDecimal.valueOf(100), virksomhet);
        if (medFeriepenger) {
            BeregningsresultatFeriepenger feriepenger = buildBeregningsresultatFeriepenger(
                beregningsresultatRevurderingFP);
            buildBeregningsresultatFeriepengerPrÅr(feriepenger, andel1, 21000L, List.of(DAGENS_DATO));
        }

        return beregningsresultatRevurderingFP;
    }

    protected BeregningsresultatEntitet buildBeregningsresultatFP(Optional<LocalDate> endringsdato) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdato.ifPresent(builder::medEndringsdato);
        return builder.build();
    }

    protected Oppdragskontroll opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                                           Long årsbeløp1,
                                                                                           Long årsbeløp2) {
        return opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(erOpptjentOverFlereÅr, true, årsbeløp1,
            årsbeløp2);
    }

    protected Oppdragskontroll opprettBeregningsresultatOgFørstegangsoppdragForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                                           boolean gjelderFødsel,
                                                                                           Long årsbeløp1,
                                                                                           Long årsbeløp2) {
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(
            Optional.empty(), erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2);
        if (gjelderFødsel) {
            beregningsresultatRepository.lagre(behandling, beregningsresultat);
            return OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);
        }
        Behandling behandlingAdopsjon = opprettOgLagreBehandling(FamilieYtelseType.ADOPSJON);
        beregningsresultatRepository.lagre(behandlingAdopsjon, beregningsresultat);
        return OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandlingAdopsjon);
    }

    protected Behandling oppsettBeregningsresultatFPRevurderingForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                              Long årsbeløp1,
                                                                              Long årsbeløp2) {
        return oppsettBeregningsresultatFPRevurderingForFeriepenger(erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2,
            behandling);
    }

    protected Behandling oppsettBeregningsresultatFPRevurderingForFeriepenger(boolean erOpptjentOverFlereÅr,
                                                                              Long årsbeløp1,
                                                                              Long årsbeløp2,
                                                                              Behandling behandling) {
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate endringsdato = DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFPForVerifiseringAvOpp150MedFeriepenger(
            Optional.of(endringsdato), erOpptjentOverFlereÅr, årsbeløp1, årsbeløp2);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);
        return revurdering;
    }

    protected UttakResultatPerioderEntitet buildUttakResultatPerioderEntitet() {
        UttakResultatPerioderEntitet opprinneligPerioder = new UttakResultatPerioderEntitet();

        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(DAGENS_DATO.minusMonths(1),
            DAGENS_DATO).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(
            Arbeidsgiver.virksomhet(virksomhet), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(
            periode, uttakAktivitet).medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(new Trekkdager(1))
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .build();
        periode.leggTilAktivitet(periodeAktivitet);
        opprinneligPerioder.leggTilPeriode(periode);

        return opprinneligPerioder;
    }

    static List<Oppdragslinje150> getOppdragslinje150Feriepenger(Oppdragskontroll oppdrag) {
        return oppdrag.getOppdrag110Liste()
            .stream()
            .map(OppdragskontrollTjenesteTestBase::getOppdragslinje150Feriepenger)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    static List<Oppdragslinje150> getOppdragslinje150Feriepenger(Oppdrag110 oppdrag110) {
        return oppdrag110.getOppdragslinje150Liste()
            .stream()
            .filter(opp150 -> ØkonomiKodeKlassifik.fraKode(opp150.getKodeKlassifik()).gjelderFerie())
            .collect(Collectors.toList());
    }
}
