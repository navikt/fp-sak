package no.nav.foreldrepenger.datavarehus.xml;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.fp.DvhPersonopplysningXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.fp.OppdragXmlTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class DvhVedtakXmlTjenesteForeldrepengerTest {

    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    private static final PersonIdent FNR_MOR = new PersonIdent("12345678901");
    private static final Saksnummer SAKSNUMMER = new Saksnummer("12345");
    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();
    private static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    private static final String ANSVARLIG_SAKSBEHANDLER = "fornavn etternavn";
    private static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);
    private static final LocalDate FØRSTE_UTTAKSDATO_OPPGITT = LocalDate.now().minusDays(20);
    private static final Long OPPDRAG_FAGSYSTEM_ID = 44L;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final BigDecimal MÅNEDSBELØP_TILSTØTENDE_YTELSE = BigDecimal.valueOf(10000L);
    private static LocalDateTime VEDTAK_DATO = LocalDateTime.parse("2017-10-11T08:00");
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(repoRule.getEntityManager());
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private Repository repository = repoRule.getRepository();

    @Mock
    private TpsTjeneste tpsTjeneste;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjeneste;

    private DvhVedtakXmlTjeneste dvhVedtakXmlTjenesteFP;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    private DvhPersonopplysningXmlTjenesteImpl dvhPersonopplysningXmlTjenesteImpl;
    private VedtakXmlTjeneste vedtakXmlTjeneste;

    @Inject
    private ØkonomioppdragRepository økonomioppdragRepository;

    private HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering;

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private MedlemskapRepository medlemskapRepository;

    @Inject
    private VirksomhetRepository virksomhetRepository;

    @Inject
    private KodeverkRepository kodeverkRepository;

    @Before
    public void oppsett() {
        hentOppdragMedPositivKvittering = new HentOppdragMedPositivKvittering(økonomioppdragRepository);

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);

        var poXmlFelles = new PersonopplysningXmlFelles(tpsTjeneste, kodeverkRepository);

        dvhPersonopplysningXmlTjenesteImpl = new DvhPersonopplysningXmlTjenesteImpl(poXmlFelles,
            familieHendelseRepository,
            vergeRepository,
            medlemskapRepository,
            virksomhetRepository,
            personopplysningTjeneste,
            iayTjeneste,
            ytelseFordelingTjeneste);

        vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        OppdragXmlTjenesteImpl oppdragXmlTjenesteImpl = new OppdragXmlTjenesteImpl(hentOppdragMedPositivKvittering);

        dvhVedtakXmlTjenesteFP = new DvhVedtakXmlTjeneste(repositoryProvider,
            vedtakXmlTjeneste,
            new UnitTestLookupInstanceImpl<>(dvhPersonopplysningXmlTjenesteImpl),
            new UnitTestLookupInstanceImpl<>(oppdragXmlTjenesteImpl),
            behandlingsresultatXmlTjeneste,
            skjæringstidspunktTjeneste);

        when(tpsTjeneste.hentFnr(any(AktørId.class))).thenReturn(Optional.of(FNR_MOR));
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_vedtaks_xml_med_oppdrag() {
        Behandling behandling = byggBehandlingMedVedtak();

        Long delytelseId = 65L;
        String delytelseXmlElement = String.format("delytelseId>%s</", delytelseId);
        String fagsystemIdXmlElement = String.format("fagsystemId>%s</", OPPDRAG_FAGSYSTEM_ID);
        String aktørIdElement = String.format("aktoerId>%s", ANNEN_PART_AKTØR_ID.getId());
        buildOppdragskontroll(behandling.getId(), delytelseId);

        String avkortetXmlElement = "avkortet>";

        // Act
        String xml = dvhVedtakXmlTjenesteFP.opprettDvhVedtakXml(behandling.getId());

        // Assert
        assertNotNull(xml);
        assertThat(xml).contains(avkortetXmlElement);
        assertThat(xml).contains(aktørIdElement);
        assertThat(xml).contains(fagsystemIdXmlElement);
        assertThat(xml).contains(delytelseXmlElement);
    }

    private Behandling byggBehandlingMedVedtak() {
        String selvstendigNæringsdrivendeOrgnr = KUNSTIG_ORG;

        YtelseHjelperTester ytelseHjelper = new YtelseHjelperTester();
        ytelseHjelper.medArbeidsForhold("55L")
            .medUttakFom(LocalDate.now().minusDays(6).minusYears(2)).medUttakTom(LocalDate.now().plusDays(1).minusMonths(10));

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
        scenario.medDefaultOppgittDekningsgrad();

        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID).medNavn("Anne N. Forelder").medType(SøknadAnnenPartType.FAR);
        scenario.medSøknadHendelse()
            .medFødselsDato(FØDSELSDATO_BARN);

        scenario.medFordeling(opprettOppgittFordeling());
        opprettPeriodeAleneomsorg(scenario);

        opprettVirksomhet(selvstendigNæringsdrivendeOrgnr);

        Behandling behandling = lagre(scenario);
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedVilkårResultatForBehandling(behandling);

        byggYtelse(behandling, RelatertYtelseType.SYKEPENGER, new BigDecimal(90), Arbeidskategori.ARBEIDSTAKER, selvstendigNæringsdrivendeOrgnr);

        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandling.getBehandlingsresultat())
            .medFørsteLovligeUttaksdag(LocalDate.now())
            .medMottattDato(LocalDate.now())
            .build();
        repositoryProvider.getUttakRepository().lagreUttaksperiodegrense(behandling.getId(), uttaksperiodegrense);

        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusDays(11))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPerioderEntitet uttakResultatPerioder1 = new UttakResultatPerioderEntitet();
        uttakResultatPerioder1.leggTilPeriode(periode);

        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder1);

        opprettStønadskontoer(behandling);
        lagBeregningsgrunnlag(behandling);

        BeregningsresultatEntitet beregningsresultat = lagBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        Arbeidsgiver arbeidsgiverVirksomhet = opprettOgLagreVirksomhet(ytelseHjelper);
        UttakResultatEntitet uttakResultatEntitet = opprettUttak(true, behandling, ytelseHjelper.uttakFom, ytelseHjelper.uttakTom, arbeidsgiverVirksomhet);
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatEntitet.getGjeldendePerioder());

        BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Arbeidsgiver opprettOgLagreVirksomhet(YtelseHjelperTester ytelseHjelper) {
        return Arbeidsgiver.virksomhet(ytelseHjelper.arbeidsForholdId);
    }

    private UttakResultatEntitet opprettUttak(boolean innvilget, Behandling behandling, LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver) {
        UttakResultatEntitet.Builder uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(behandling.getBehandlingsresultat());

        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(innvilget ? PeriodeResultatType.INNVILGET : PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(new BigDecimal(100))
            .medUtbetalingsprosent(new BigDecimal(100))
            .medErSøktGradering(true)
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);

        return uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder)
            .build();
    }

    private void opprettPeriodeAleneomsorg(ScenarioMorSøkerForeldrepenger scenario) {
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        PerioderAleneOmsorgEntitet perioderAleneOmsorg = new PerioderAleneOmsorgEntitet();
        perioderAleneOmsorg.leggTil(new PeriodeAleneOmsorgEntitet(LocalDate.now(), LocalDate.now().plusDays(10)));
        perioderAleneOmsorg.leggTil(new PeriodeAleneOmsorgEntitet(LocalDate.now().plusDays(11), LocalDate.now().plusDays(22)));

        scenario.medPeriodeMedAleneomsorg(perioderAleneOmsorg);
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP() {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(FØRSTE_UTTAKSDATO_OPPGITT, FØRSTE_UTTAKSDATO_OPPGITT.plusWeeks(2))
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(123)
            .medDagsatsFraBg(123)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private Behandlingsresultat opprettBehandlingsresultatMedVilkårResultatForBehandling(Behandling behandling) {

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);
        VilkårResultat vilkårResultat = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.INNVILGET)
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .buildFor(behandlingsresultat);
        repository.lagre(vilkårResultat);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandling.setBehandlingresultat(behandlingsresultat);

        return behandlingsresultat;
    }

    private void opprettStønadskontoer(Behandling behandling) {
        Stønadskonto foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        Stønadskonto mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskonto fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(50)
            .build();
        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(mødrekvote).medStønadskonto(fellesperiode).medStønadskonto(foreldrepengerFørFødsel).build();

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private Virksomhet opprettVirksomhet(String orgnr) {
        Optional<Virksomhet> optional = repositoryProvider.getVirksomhetRepository().hent(orgnr);
        if (optional.isPresent()) {
            return optional.get();
        }
        Virksomhet virksomhet = new VirksomhetEntitet.Builder()
            .medOrgnr(orgnr)
            .oppdatertOpplysningerNå()
            .build();
        repositoryProvider.getVirksomhetRepository().lagre(virksomhet);
        return virksomhet;
    }

    private OppgittFordelingEntitet opprettOppgittFordeling() {
        OppgittPeriodeBuilder periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(FØRSTE_UTTAKSDATO_OPPGITT, FØRSTE_UTTAKSDATO_OPPGITT.plusWeeks(2))
            .medArbeidsgiver(opprettOgLagreArbeidsgiver());

        return new OppgittFordelingEntitet(singletonList(periode.build()), true);
    }

    private Arbeidsgiver opprettOgLagreArbeidsgiver() {
        return Arbeidsgiver.virksomhet(ORGNR);
    }

    private void buildOppdragskontroll(Long behandlingId, Long delytelseId) {
        Oppdragskontroll oppdrag = Oppdragskontroll.builder()
            .medBehandlingId(behandlingId)
            .medSaksnummer(SAKSNUMMER)
            .medVenterKvittering(false)
            .medProsessTaskId(56L)
            .build();

        Avstemming115 avstemming115 = buildAvstemming115();
        Oppdrag110 oppdrag110 = buildOppdrag110(oppdrag, avstemming115);
        buildOppdragslinje150(oppdrag110, delytelseId);
        buildOppdragKvittering(oppdrag110);

        økonomioppdragRepository.lagre(oppdrag);
    }

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110, Long delytelseId) {

        Oppdragslinje150 oppdragslinje150 = Oppdragslinje150.builder()
            .medKodeEndringLinje("ENDR")
            .medKodeStatusLinje("OPPH")
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(delytelseId)
            .medKodeKlassifik("FPENFOD-OP")
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(61122L)
            .medFradragTillegg(TfradragTillegg.F.value())
            .medTypeSats(ØkonomiTypeSats.UKE.name())
            .medBrukKjoreplan("B")
            .medSaksbehId("F2365245")
            .medUtbetalesTilId("123456789")
            .medOppdrag110(oppdrag110)
            .medHenvisning(43L)
            .medRefDelytelseId(1L)
            .build();

        Refusjonsinfo156.builder().medOppdragslinje150(oppdragslinje150).medRefunderesId("123").medMaksDato(LocalDate.now()).medDatoFom(LocalDate.now())
            .build();

        return oppdragslinje150;
    }

    private OppdragKvittering buildOppdragKvittering(Oppdrag110 oppdrag110) {
        return OppdragKvittering.builder()
            .medOppdrag110(oppdrag110)
            .medAlvorlighetsgrad("1")
            .build();
    }

    private Avstemming115 buildAvstemming115() {
        return Avstemming115.builder()
            .medKodekomponent(Fagsystem.FPSAK.getOffisiellKode())
            .medNokkelAvstemming("nokkelAvstemming")
            .medTidspnktMelding("tidspnktMelding")
            .build();
    }

    private Oppdrag110 buildOppdrag110(Oppdragskontroll oppdragskontroll, Avstemming115 avstemming115) {
        return Oppdrag110.builder()
            .medKodeAksjon(ØkonomiKodeAksjon.TRE.getKodeAksjon())
            .medKodeEndring(ØkonomiKodeEndring.NY.name())
            .medKodeFagomrade(ØkonomiKodeFagområde.REFUTG.name())
            .medFagSystemId(OPPDRAG_FAGSYSTEM_ID)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.DAG.getUtbetFrekvens())
            .medOppdragGjelderId("12345678901")
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId("J5624215")
            .medAvstemming115(avstemming115)
            .medOppdragskontroll(oppdragskontroll)
            .build();
    }

    private void byggYtelse(Behandling behandling, RelatertYtelseType relatertYtelseType, BigDecimal prosent, Arbeidskategori arbeidskategori,
                            String virksomhetOrgnr) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder ytelserBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(BRUKER_AKTØR_ID);

        YtelseBuilder ytelseBuilder = ytelserBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, relatertYtelseType, SAKSNUMMER)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusDays(6), SKJÆRINGSTIDSPUNKT.plusDays(8)))
            .medKilde(Fagsystem.INFOTRYGD);

        YtelseStørrelse ytelseStørrelse = YtelseStørrelseBuilder.ny()
            .medBeløp(MÅNEDSBELØP_TILSTØTENDE_YTELSE)
            .medHyppighet(InntektPeriodeType.MÅNEDLIG)
            .medVirksomhet(virksomhetOrgnr)
            .build();
        YtelseGrunnlagBuilder ytelseGrunnlagBuilder = ytelseBuilder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori)
            .medYtelseStørrelse(ytelseStørrelse);

        if (RelatertYtelseType.FORELDREPENGER.equals(relatertYtelseType)) {
            ytelseGrunnlagBuilder.medDekningsgradProsent(prosent);
        }
        if (RelatertYtelseType.SYKEPENGER.equals(relatertYtelseType)) {
            ytelseGrunnlagBuilder.medInntektsgrunnlagProsent(prosent);
        }
        YtelseGrunnlag ytelseGrunnlag = ytelseGrunnlagBuilder
            .build();
        ytelseBuilder.medYtelseGrunnlag(ytelseGrunnlag);

        ytelserBuilder.leggTilYtelse(ytelseBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(ytelserBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

    }

    private static BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, SKJÆRINGSTIDSPUNKT);
    }

    class YtelseHjelperTester {
        private LocalDate uttakFom;
        private LocalDate uttakTom;
        private String arbeidsForholdId;

        YtelseHjelperTester medUttakFom(LocalDate fom) {
            this.uttakFom = fom;
            return this;
        }

        YtelseHjelperTester medUttakTom(LocalDate tom) {
            this.uttakTom = tom;
            return this;
        }

        YtelseHjelperTester medArbeidsForhold(String arbeidsForholdId) {
            this.arbeidsForholdId = arbeidsForholdId;
            return this;
        }

    }

    private void lagBeregningsgrunnlag(Behandling behandling) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();
        BeregningsgrunnlagPeriode bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.FRILANSER)
            .medAvkortetPrÅr(BigDecimal.TEN)
            .medAvkortetBrukersAndelPrÅr(BigDecimal.TEN)
            .medAvkortetRefusjonPrÅr(BigDecimal.ZERO)
            .medRedusertPrÅr(BigDecimal.TEN)
            .medRedusertBrukersAndelPrÅr(BigDecimal.TEN)
            .medRedusertRefusjonPrÅr(BigDecimal.ZERO)
            .build(bgPeriode);

        beregningsgrunnlagRepository.lagre(behandling.getId(), bg, BeregningsgrunnlagTilstand.FASTSATT);
    }
}
