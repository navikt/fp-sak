package no.nav.foreldrepenger.datavarehus.xml.fp;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
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
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlTjeneste;
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
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@CdiDbAwareTest
class VedtakXmlTest {

    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER = new Saksnummer("12345");
    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();
    private static LocalDateTime VEDTAK_TIDSPUNKT = LocalDateTime.parse("2017-10-11T08:00");
    private static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    private static final String ANSVARLIG_SAKSBEHANDLER = "fornavn etternavn";
    private static final String ORGNR = KUNSTIG_ORG;

    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);
    private static final LocalDate FØRSTE_UTTAKSDATO_OPPGITT = LocalDate.now().minusDays(20);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final BigDecimal MÅNEDSBELØP_TILSTØTENDE_YTELSE = BigDecimal.valueOf(10000L);

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    private BeregningTjenesteInMemory beregningTjenesteInMemory;

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjeneste;

    @Inject
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    @Inject
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    private FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste;

    @BeforeEach
    void oppsett() {
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        Mockito.lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(stp);
        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);
        var personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider, personopplysningTjeneste,
                iayTjeneste, ytelseFordelingTjeneste, mock(VergeRepository.class), mock(VirksomhetTjeneste.class), foreldrepengerUttakTjeneste
            );
        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
                new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
                behandlingsresultatXmlTjeneste,
                skjæringstidspunktTjeneste);
    }

    @Test
    void skal_opprette_vedtaks_xml(EntityManager em) {
        var behandling = byggBehandlingMedVedtak(em);
        var avkortetXmlElement = "avkortet>";

        // Act
        var xml = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());

        // Assert
        assertThat(xml)
            .isNotNull()
            .contains(avkortetXmlElement);
    }

    private Behandling byggBehandlingMedVedtak(EntityManager em) {
        var selvstendigNæringsdrivendeOrgnr = KUNSTIG_ORG;

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
                .medSaksnummer(SAKSNUMMER);
        scenario.medDefaultOppgittDekningsgrad();
        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID);
        scenario.medSøknadHendelse()
                .medFødselsDato(FØDSELSDATO_BARN);

        scenario.medFordeling(opprettOppgittFordeling());
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett());

        var behandling = lagre(scenario);
        var behandlingsresultat = opprettBehandlingsresultatMedVilkårResultatForBehandling(em, behandling);
        em.persist(behandlingsresultat);
        em.flush();
        em.clear();

        mockTidligereYtelse(behandling, RelatertYtelseType.SYKEPENGER, null, Arbeidskategori.ARBEIDSTAKER, selvstendigNæringsdrivendeOrgnr);

        var uttaksperiodegrense = new Uttaksperiodegrense(LocalDate.now());
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandling.getId(), uttaksperiodegrense);

        var periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusDays(11))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        var uttakResultatPerioder1 = new UttakResultatPerioderEntitet();

        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(),
                LocalDate.now().plusMonths(3))
                        .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                        .build();

        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
                .medArbeidsforhold(Arbeidsgiver.virksomhet(selvstendigNæringsdrivendeOrgnr), InternArbeidsforholdRef.nyRef())
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .build();

        var periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
                uttakAktivitet)
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
                .medTrekkdager(new Trekkdager(10))
                .medArbeidsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(null) // PFP-4396 tester at utbetalingsgrad kan være null
                .build();

        periode.leggTilAktivitet(periodeAktivitet);

        uttakResultatPerioder1.leggTilPeriode(periode);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder1);

        opprettStønadskontoer(behandling);
        lagBeregningsgrunnlag(behandling);

        var beregningsresultat = lagBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        var behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        var vedtak = BehandlingVedtak.builder()
                .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .medIverksettingStatus(IVERKSETTING_STATUS)
                .medVedtakstidspunkt(VEDTAK_TIDSPUNKT)
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));

        var fagsakMora = scenario.getFagsak();
        var fagsakForFar = FagsakBuilder.nyForeldrepengesak(RelasjonsRolleType.FARA).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsakForFar);

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(fagsakMora, fagsakForFar);
        em.flush();
        em.clear();

        return behandling;
    }

    private void lagBeregningsgrunnlag(Behandling behandling) {
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.FRILANSER)
            .medAvkortetPrÅr(BigDecimal.TEN)
            .medAvkortetBrukersAndelPrÅr(BigDecimal.TEN)
            .medAvkortetRefusjonPrÅr(BigDecimal.ZERO)
            .medRedusertPrÅr(BigDecimal.TEN)
            .medDagsatsBruker(1L)
            .medDagsatsArbeidsgiver(1L)
            .medRedusertBrukersAndelPrÅr(BigDecimal.TEN)
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

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    private void mockTidligereYtelse(Behandling behandling, RelatertYtelseType relatertYtelseType, BigDecimal prosent,
            Arbeidskategori arbeidskategori,
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

    private Behandlingsresultat opprettBehandlingsresultatMedVilkårResultatForBehandling(EntityManager em, Behandling behandling) {

        var behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);
        var vilkårResultat = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .buildFor(behandlingsresultat);
        em.persist(vilkårResultat);
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
                .medArbeidsgiver(opprettOgLagreArbeidsgiver(ORGNR));

        return new OppgittFordelingEntitet(singletonList(periode.build()), true);
    }

    private Arbeidsgiver opprettOgLagreArbeidsgiver(String orgNr) {
        return Arbeidsgiver.virksomhet(orgNr);
    }
}
