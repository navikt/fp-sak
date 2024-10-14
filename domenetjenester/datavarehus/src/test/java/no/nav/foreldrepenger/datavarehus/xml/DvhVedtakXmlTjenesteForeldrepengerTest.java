package no.nav.foreldrepenger.datavarehus.xml;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
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
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.datavarehus.xml.fp.DvhPersonopplysningXmlTjenesteImpl;
import no.nav.foreldrepenger.datavarehus.xml.fp.OppdragXmlTjenesteImpl;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjenesteInMemory;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@CdiDbAwareTest
class DvhVedtakXmlTjenesteForeldrepengerTest {

    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
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
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BeregningTjenesteInMemory beregningTjenesteInMemory;

    private EntityManager entityManager;

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjeneste;

    private DvhVedtakXmlTjeneste dvhVedtakXmlTjenesteFP;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private ØkonomioppdragRepository økonomioppdragRepository;

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private MedlemskapRepository medlemskapRepository;

    @Inject
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    @Inject
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @BeforeEach
    public void oppsett(EntityManager em) {
        entityManager = em;
        var hentOppdragMedPositivKvittering = new HentOppdragMedPositivKvittering(økonomioppdragRepository);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);

        var dvhPersonopplysningXmlTjenesteImpl = new DvhPersonopplysningXmlTjenesteImpl(poXmlFelles, familieHendelseRepository, vergeRepository,
            medlemskapRepository, virksomhetTjeneste, personopplysningTjeneste, iayTjeneste, ytelseFordelingTjeneste, foreldrepengerUttakTjeneste);

        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        var oppdragXmlTjenesteImpl = new OppdragXmlTjenesteImpl(hentOppdragMedPositivKvittering);

        dvhVedtakXmlTjenesteFP = new DvhVedtakXmlTjeneste(repositoryProvider,
                vedtakXmlTjeneste,
                new UnitTestLookupInstanceImpl<>(dvhPersonopplysningXmlTjenesteImpl),
                new UnitTestLookupInstanceImpl<>(oppdragXmlTjenesteImpl),
                behandlingsresultatXmlTjeneste,
                skjæringstidspunktTjeneste, null);

    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_opprette_vedtaks_xml_med_oppdrag() {
        var behandling = byggBehandlingMedVedtak();

        Long delytelseId = 65L;
        var delytelseXmlElement = String.format("delytelseId>%s</", delytelseId);
        var fagsystemIdXmlElement = String.format("fagsystemId>%s</", OPPDRAG_FAGSYSTEM_ID);
        var aktørIdElement = String.format("aktoerId>%s", ANNEN_PART_AKTØR_ID.getId());
        buildOppdragskontroll(behandling.getId(), delytelseId);

        var avkortetXmlElement = "avkortet>";

        // Act
        var xml = dvhVedtakXmlTjenesteFP.opprettDvhVedtakXml(behandling.getId());

        // Assert
        assertThat(xml).isNotNull();
        assertThat(xml).contains(avkortetXmlElement);
        assertThat(xml).contains(aktørIdElement);
        assertThat(xml).contains(fagsystemIdXmlElement);
        assertThat(xml).contains(delytelseXmlElement);
    }

    private Behandling byggBehandlingMedVedtak() {

        var ytelseHjelper = new YtelseHjelperTester();
        ytelseHjelper.medArbeidsForhold("55L")
                .medUttakFom(LocalDate.now().minusDays(6).minusYears(2)).medUttakTom(LocalDate.now().plusDays(1).minusMonths(10));

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
                .medSaksnummer(SAKSNUMMER);
        scenario.medDefaultOppgittDekningsgrad();

        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID).medNavn("Anne N. Forelder").medType(SøknadAnnenPartType.FAR);
        scenario.medSøknadHendelse()
                .medFødselsDato(FØDSELSDATO_BARN);

        scenario.medFordeling(opprettOppgittFordeling());
        opprettPeriodeAleneomsorg(scenario);

        var behandling = lagre(scenario);
        var behandlingsresultat = opprettBehandlingsresultatMedVilkårResultatForBehandling(behandling);

        byggYtelse(behandling, RelatertYtelseType.SYKEPENGER, new BigDecimal(90), Arbeidskategori.ARBEIDSTAKER, KUNSTIG_ORG);

        var uttaksperiodegrense = new Uttaksperiodegrense(LocalDate.now());
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandling.getId(), uttaksperiodegrense);

        var periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusDays(11))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        var uttakResultatPerioder1 = new UttakResultatPerioderEntitet();
        uttakResultatPerioder1.leggTilPeriode(periode);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder1);

        opprettStønadskontoer(behandling);
        lagBeregningsgrunnlag(behandling);

        var beregningsresultat = lagBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        var arbeidsgiverVirksomhet = opprettOgLagreVirksomhet(ytelseHjelper);
        var uttakResultatEntitet = opprettUttak(true, behandling, ytelseHjelper.uttakFom, ytelseHjelper.uttakTom,
                arbeidsgiverVirksomhet);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(),
                uttakResultatEntitet.getGjeldendePerioder());

        var behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        var vedtak = BehandlingVedtak.builder()
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
        var uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(behandling.getBehandlingsresultat());

        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
                .medResultatType(innvilget ? PeriodeResultatType.INNVILGET : PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
                .build();
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
                .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .build();

        var periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
                uttakAktivitet)
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
                .medTrekkdager(new Trekkdager(10))
                .medArbeidsprosent(new BigDecimal(100))
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medErSøktGradering(true)
                .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        var uttakResultatPerioder = new UttakResultatPerioderEntitet();
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);

        return uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder)
                .build();
    }

    private void opprettPeriodeAleneomsorg(ScenarioMorSøkerForeldrepenger scenario) {
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett());
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP() {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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

        var behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);
        var vilkårResultat = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .buildFor(behandlingsresultat);
        entityManager.persist(vilkårResultat);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandling.setBehandlingresultat(behandlingsresultat);

        return behandlingsresultat;
    }

    private void opprettStønadskontoer(Behandling behandling) {
        var foreldrepengerFørFødsel = Stønadskonto.builder()
                .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
                .medMaxDager(15)
                .build();
        var mødrekvote = Stønadskonto.builder()
                .medStønadskontoType(StønadskontoType.MØDREKVOTE)
                .medMaxDager(50)
                .build();
        var fellesperiode = Stønadskonto.builder()
                .medStønadskontoType(StønadskontoType.FELLESPERIODE)
                .medMaxDager(50)
                .build();
        var stønadskontoberegning = Stønadskontoberegning.builder()
                .medRegelEvaluering("evaluering")
                .medRegelInput("grunnlag")
                .medStønadskonto(mødrekvote).medStønadskonto(fellesperiode).medStønadskonto(foreldrepengerFørFødsel).build();

        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning);
    }

    private OppgittFordelingEntitet opprettOppgittFordeling() {
        var periode = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
                .medPeriode(FØRSTE_UTTAKSDATO_OPPGITT, FØRSTE_UTTAKSDATO_OPPGITT.plusWeeks(2))
                .medArbeidsgiver(opprettOgLagreArbeidsgiver());

        return new OppgittFordelingEntitet(singletonList(periode.build()), true);
    }

    private Arbeidsgiver opprettOgLagreArbeidsgiver() {
        return Arbeidsgiver.virksomhet(ORGNR);
    }

    private void buildOppdragskontroll(Long behandlingId, Long delytelseId) {
        var oppdrag = Oppdragskontroll.builder()
                .medBehandlingId(behandlingId)
                .medSaksnummer(SAKSNUMMER)
                .medVenterKvittering(false)
                .medProsessTaskId(56L)
                .build();

        var oppdrag110 = buildOppdrag110(oppdrag);
        buildOppdragslinje150(oppdrag110, delytelseId);
        buildOppdragKvittering(oppdrag110);

        økonomioppdragRepository.lagre(oppdrag);
    }

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110, Long delytelseId) {

        var oppdragslinje150 = Oppdragslinje150.builder()
                .medKodeEndringLinje(KodeEndringLinje.ENDR)
                .medKodeStatusLinje(KodeStatusLinje.OPPH)
                .medDatoStatusFom(LocalDate.now())
                .medVedtakId("345")
                .medDelytelseId(delytelseId)
                .medKodeKlassifik(KodeKlassifik.ES_FØDSEL)
                .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
                .medSats(Sats.på(61122L))
                .medTypeSats(TypeSats.DAG)
                .medUtbetalesTilId("123456789")
                .medOppdrag110(oppdrag110)
                .medRefDelytelseId(1L)
                .build();

        Refusjonsinfo156.builder().medOppdragslinje150(oppdragslinje150).medRefunderesId("123").medMaksDato(LocalDate.now())
                .medDatoFom(LocalDate.now())
                .build();

        return oppdragslinje150;
    }

    private OppdragKvittering buildOppdragKvittering(Oppdrag110 oppdrag110) {
        return OppdragKvittering.builder()
                .medOppdrag110(oppdrag110)
                .medAlvorlighetsgrad(Alvorlighetsgrad.OK)
                .build();
    }

    private Oppdrag110 buildOppdrag110(Oppdragskontroll oppdragskontroll) {
        return Oppdrag110.builder()
                .medKodeEndring(KodeEndring.NY)
                .medKodeFagomrade(KodeFagområde.REFUTG)
                .medFagSystemId(OPPDRAG_FAGSYSTEM_ID)
                .medOppdragGjelderId("12345678901")
                .medSaksbehId("J5624215")
                .medAvstemming(Avstemming.ny())
                .medOppdragskontroll(oppdragskontroll)
                .build();
    }

    private void byggYtelse(Behandling behandling, RelatertYtelseType relatertYtelseType, BigDecimal prosent, Arbeidskategori arbeidskategori,
            String virksomhetOrgnr) {
        var inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
                VersjonType.REGISTER);
        var ytelserBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørYtelseBuilder(BRUKER_AKTØR_ID);

        var ytelseBuilder = ytelserBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, relatertYtelseType, SAKSNUMMER)
                .medStatus(RelatertYtelseTilstand.AVSLUTTET)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusDays(6), SKJÆRINGSTIDSPUNKT.plusDays(8)))
                .medKilde(Fagsystem.INFOTRYGD);

        var ytelseStørrelse = YtelseStørrelseBuilder.ny()
                .medBeløp(MÅNEDSBELØP_TILSTØTENDE_YTELSE)
                .medHyppighet(InntektPeriodeType.MÅNEDLIG)
                .medVirksomhet(virksomhetOrgnr)
                .build();
        var ytelseGrunnlagBuilder = ytelseBuilder.getGrunnlagBuilder()
                .medArbeidskategori(arbeidskategori)
                .medYtelseStørrelse(ytelseStørrelse);

        if (RelatertYtelseType.FORELDREPENGER.equals(relatertYtelseType)) {
            ytelseGrunnlagBuilder.medDekningsgradProsent(prosent);
        }
        if (RelatertYtelseType.SYKEPENGER.equals(relatertYtelseType)) {
            ytelseGrunnlagBuilder.medInntektsgrunnlagProsent(prosent);
        }
        var ytelseGrunnlag = ytelseGrunnlagBuilder
                .build();
        ytelseBuilder.medYtelseGrunnlag(ytelseGrunnlag);

        ytelserBuilder.leggTilYtelse(ytelseBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(ytelserBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

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
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.FRILANSER)
            .medAvkortetPrÅr(BigDecimal.TEN)
            .medAvkortetBrukersAndelPrÅr(BigDecimal.TEN)
            .medAvkortetRefusjonPrÅr(BigDecimal.ZERO)
            .medRedusertPrÅr(BigDecimal.TEN)
            .medRedusertBrukersAndelPrÅr(BigDecimal.TEN)
            .medDagsatsBruker(1L)
            .medDagsatsArbeidsgiver(1L)
            .medRedusertRefusjonPrÅr(BigDecimal.ZERO)
            .build();
        var bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(andel)
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilBeregningsgrunnlagPeriode(bgPeriode)
            .build();

        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FASTSATT);
        beregningTjenesteInMemory.lagre(gr, BehandlingReferanse.fra(behandling));
    }
}
