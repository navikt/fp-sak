package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AdopsjonGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AnnenPartGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.ArbeidGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.BehandlingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.DatoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.MedlemskapGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.OpptjeningGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RettOgOmsorgGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RevurderingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.SøknadGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.YtelserGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.OverstyrUttakResultatValidator;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakStub;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMedMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class FastsettePerioderTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

    private final FagsakRelasjonRepository relasjonRepository = repositoryProvider.getFagsakRelasjonRepository();

    private final FpUttakRepository fpUttakRepository = repositoryProvider.getFpUttakRepository();

    private final UttaksperiodegrenseRepository uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();

    private final FastsettePerioderRegelAdapter regelAdapter;

    {
        var rettOgOmsorgGrunnlagBygger = new RettOgOmsorgGrunnlagBygger(repositoryProvider,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
        regelAdapter = new FastsettePerioderRegelAdapter(
            new FastsettePerioderRegelGrunnlagBygger(new AnnenPartGrunnlagBygger(repositoryProvider.getFpUttakRepository()),
                new ArbeidGrunnlagBygger(repositoryProvider), new BehandlingGrunnlagBygger(),
                new DatoerGrunnlagBygger(new PersonopplysningerForUttakStub()), new MedlemskapGrunnlagBygger(), rettOgOmsorgGrunnlagBygger,
                new RevurderingGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository(), repositoryProvider.getFpUttakRepository()),
                new SøknadGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository()), new InngangsvilkårGrunnlagBygger(repositoryProvider),
                new OpptjeningGrunnlagBygger(), new AdopsjonGrunnlagBygger(),
                new KontoerGrunnlagBygger(repositoryProvider, rettOgOmsorgGrunnlagBygger), new YtelserGrunnlagBygger()),
            new FastsettePerioderRegelResultatKonverterer(fpUttakRepository, ytelsesFordelingRepository));
    }

    private final ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(fpUttakRepository);

    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();

    @Test
    public void skalInnvilgeFedrekvoteForMedmor() {
        // Setup
        var mottattDato = LocalDate.now();
        var fødselsdato = LocalDate.now().minusWeeks(6);

        var virksomhet = virksomhet();
        var fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medArbeidsgiver(virksomhet)
            .build();

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var fordeling = new OppgittFordelingEntitet(List.of(fedrekvote), true);
        var rettighet = beggeRett();

        var behandling = ScenarioMedMorSøkerForeldrepenger.forFødsel()
            .medOppgittDekningsgrad(dekningsgrad)
            .medFordeling(fordeling)
            .medOppgittRettighet(rettighet)
            .lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);

        byggArbeidForBehandling(behandling, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);

        // Act
        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(uttakResultat).isPresent();
        var uttakResultatPerioder = uttakResultat.get().getOpprinneligPerioder().getPerioder();
        assertThat(uttakResultatPerioder).hasSize(1);

        var resultatPeriode = uttakResultatPerioder.iterator().next();
        assertThat(resultatPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FEDREKVOTE);
        assertThat(resultatPeriode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }

    private UttakInput lagInput(Behandling behandling, LocalDate fødselsdato) {
        var ref = BehandlingReferanse.fra(behandling,
            Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fødselsdato).medKreverSammenhengendeUttak(false).build());
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
        return new UttakInput(ref, iayGrunnlag, fpGrunnlag).medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser())
            .medSøknadMottattDato(fødselsdato.minusWeeks(4));
    }

    private OverstyrUttakResultatValidator validator() {
        return mock(OverstyrUttakResultatValidator.class);
    }

    @Test
    public void oppretterOgLagrerUttakResultatPlanOgUttakPerioderPerArbeidsgiver() {
        // Setup
        var mottattDato = LocalDate.now();
        var fødselsdato = LocalDate.now();

        var arbeidsgiver1 = virksomhet("orgnr1");
        var arbeidsgiver2 = virksomhet("orgnr2");

        var fpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();

        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();

        var fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .build();

        var behandling = behandlingMedSøknadsperioder(List.of(fpff, mødrekvote, fellesperiode));
        byggArbeidForBehandling(behandling, fødselsdato, List.of(arbeidsgiver1, arbeidsgiver2));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);


        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver1, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver2, null);

        // Act
        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(uttakResultat).isPresent();
        var uttakResultatPerioder = uttakResultat.get().getOpprinneligPerioder().getPerioder();
        assertThat(uttakResultatPerioder).hasSize(3);
    }

    @Test
    public void arbeidstidsprosentOgUtbetalingsgradSkalHa2Desimaler() {
        // Setup
        var mottattDato = LocalDate.now().minusWeeks(1);
        var fødselsdato = LocalDate.now().minusWeeks(8);

        var arbeidsprosent = new BigDecimal("50.55");

        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();

        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(7))
            .build();

        var virksomhet = virksomhet();
        var periode3 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(7).plusDays(1), fødselsdato.plusWeeks(8))
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        var behandling = behandlingMedSøknadsperioder(List.of(periode1, periode2, periode3));
        byggArbeidForBehandling(behandling, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);

        // Act

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(uttakResultat).isPresent();
        var uttakResultatPerioder = uttakResultat.get().getOpprinneligPerioder().getPerioder();
        assertThat(uttakResultatPerioder.get(3).getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(uttakResultatPerioder.get(3).getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(49.45));
    }

    @Test
    public void skal_filtrere_ut_fri_utsettelse() {
        var fødselsdato = LocalDate.of(2021, 10, 10);
        var oppgittMødrekvote1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();
        var oppgittFriUtsettelse = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(8).minusDays(1))
            .build();
        var oppgittMødrekvote2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(8), fødselsdato.plusWeeks(10))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittMødrekvote1, oppgittFriUtsettelse, oppgittMødrekvote2), true))
            .medOppgittRettighet(beggeRett());
        var behandling = scenario.lagre(repositoryProvider);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        byggArbeidForBehandling(behandling, fødselsdato, List.of(arbeidsgiver));
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);
        opprettStønadskontoerForFarOgMor(behandling);
        opprettGrunnlag(behandling.getId(), fødselsdato);

        var input = lagInput(behandling, fødselsdato);
        tjeneste().fastsettePerioder(input);

        var resultat = fpUttakRepository.hentUttakResultat(input.getBehandlingReferanse().behandlingId());

        assertThat(resultat.getGjeldendePerioder().getPerioder()).hasSize(2);
        assertThat(resultat.getGjeldendePerioder().getPerioder().get(0).isUtsettelse()).isFalse();
        assertThat(resultat.getGjeldendePerioder().getPerioder().get(1).isUtsettelse()).isFalse();
    }

    @Test
    public void skal_ikke_filtrere_ut_fri_utsettelse_når_bare_far_har_rett() {
        var fødselsdato = LocalDate.of(2021, 10, 10);
        var oppgittFriUtsettelse1 = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(8).minusDays(1))
            .build();
        var oppgittForeldrepenger1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(8), fødselsdato.plusWeeks(10).minusDays(1))
            .build();
        var oppgittFriUtsettelse2 = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(12).minusDays(1))
            .build();
        var oppgittForeldrepenger2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(FORELDREPENGER)
            .medPeriode(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(14))
            .build();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(
                new OppgittFordelingEntitet(List.of(oppgittFriUtsettelse1, oppgittForeldrepenger1, oppgittFriUtsettelse2, oppgittForeldrepenger2),
                    true))
            .medOppgittRettighet(bareFarHarRett());
        var behandling = scenario.lagre(repositoryProvider);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        byggArbeidForBehandling(behandling, fødselsdato, List.of(arbeidsgiver));
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);
        opprettStønadskontoerBareFarHarRett(behandling);
        opprettGrunnlag(behandling.getId(), fødselsdato);

        var aktivitetskravAvklaring1 = new AktivitetskravPeriodeEntitet(oppgittFriUtsettelse1.getFom(), oppgittFriUtsettelse1.getTom(),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "begrunnelse");
        var aktivitetskravAvklaring2 = new AktivitetskravPeriodeEntitet(oppgittFriUtsettelse2.getFom(), oppgittFriUtsettelse2.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT, "begrunnelse");
        var aktivitetskravPerioder = new AktivitetskravPerioderEntitet().leggTil(aktivitetskravAvklaring1).leggTil(aktivitetskravAvklaring2);
        var yfMedAktivitetskravAvklaring = YtelseFordelingAggregat.oppdatere(ytelsesFordelingRepository.hentAggregat(behandling.getId()))
            .medSaksbehandledeAktivitetskravPerioder(aktivitetskravPerioder);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfMedAktivitetskravAvklaring.build());

        var input = lagInput(behandling, fødselsdato);
        tjeneste().fastsettePerioder(input);

        var resultat = fpUttakRepository.hentUttakResultat(input.getBehandlingReferanse().behandlingId());

        assertThat(resultat.getGjeldendePerioder().getPerioder()).hasSize(4);
        assertThat(resultat.getGjeldendePerioder().getPerioder().get(0).isUtsettelse()).isTrue();
        //Innvilget pga oppfylt aktkrav
        assertThat(resultat.getGjeldendePerioder().getPerioder().get(0).isInnvilget()).isTrue();
        assertThat(resultat.getGjeldendePerioder().getPerioder().get(2).isUtsettelse()).isTrue();
        //Avslag pga ikke oppfylt aktkrav
        assertThat(resultat.getGjeldendePerioder().getPerioder().get(2).isInnvilget()).isFalse();
    }

    private OppgittRettighetEntitet bareFarHarRett() {
        return new OppgittRettighetEntitet(false, false, false, false);
    }

    private OppgittRettighetEntitet beggeRett() {
        return new OppgittRettighetEntitet(true, false, false, false);
    }

    @Test
    public void sletterGammeltResultatOgOppretterNyttResultatDersomOpprinneligResultatFinnesFraFør() {
        // Steg 1: Opprett uttaksplan med perioder
        var mottattDato = LocalDate.now();
        var fødselsdato = LocalDate.now().minusMonths(3).withDayOfMonth(1);

        var virksomhet = virksomhet();
        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsgiver(virksomhet)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();

        var behandling = behandlingMedSøknadsperioder(List.of(periode1));
        byggArbeidForBehandling(behandling, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);

        // Act
        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(uttakResultat).isPresent();
        var uttakResultatPerioder = uttakResultat.get().getOpprinneligPerioder().getPerioder();
        assertThat(uttakResultatPerioder).hasSize(1);

        var mødrekvote = uttakResultatPerioder.stream()
            .filter(p -> StønadskontoType.MØDREKVOTE.getKode().equals(p.getAktiviteter().get(0).getTrekkonto().getKode()))
            .findFirst();
        assertThat(mødrekvote).isPresent();

        var nyePerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build());
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(new OppgittFordelingEntitet(nyePerioder, true));

        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        // Act
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        var resultat = fpUttakRepository.hentUttakResultat(behandling.getId()).getOpprinneligPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        var foreldrepengerPeriode = resultat.stream()
            .filter(p -> StønadskontoType.FORELDREPENGER.getKode().equals(p.getAktiviteter().get(0).getTrekkonto().getKode()))
            .findFirst();
        assertThat(foreldrepengerPeriode).isPresent();
    }

    @Test
    public void foreldrepengerFødsel_gi_innvilget() {
        // Skal treffe UT1211 i foreldrepenger delregel

        // Setup
        var mottattDato = LocalDate.now().minusWeeks(1);
        var fødselsdato = LocalDate.now().minusWeeks(8);

        var fpffSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var foreldrepengerSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(2))
            .build();

        var virksomhet = virksomhet();

        var behandling = behandlingMedSøknadsperioder(List.of(fpffSøknadsperiode, foreldrepengerSøknadsperiode));
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(new OppgittRettighetEntitet(false, true, false, false));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        byggArbeidForBehandling(behandling, fødselsdato, List.of(virksomhet));

        var fpffKonto = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).medMaxDager(15).build();
        var foreldrepengerKonto = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(50).build();
        var stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(fpffKonto)
            .medStønadskonto(foreldrepengerKonto)
            .build();
        relasjonRepository.lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);

        opprettGrunnlag(behandling.getId(), mottattDato);

        // Act

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        var resultat = uttakResultat.get().getOpprinneligPerioder().getPerioder().get(1);
        assertThat(resultat.getDokRegel().isTilManuellBehandling()).isFalse();
        assertThat(resultat.getResultatÅrsak()).isInstanceOf(PeriodeResultatÅrsak.class);
    }

    @Test
    public void oppretterOgLagrerUttakResultatPlanOgUttakPerioderPerNårArbeidsgivereErKombinasjonAvPrivatpersonOgVirksomhet() {
        // Setup
        var mottattDato = LocalDate.now().minusWeeks(1);
        var fødselsdato = LocalDate.now().minusWeeks(8);

        var fpffSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var foreldrepengerSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(5))
            .build();

        var virksomhet = virksomhet();
        var person = person();

        var behandling = behandlingMedSøknadsperioder(List.of(fpffSøknadsperiode, foreldrepengerSøknadsperiode));

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(new OppgittRettighetEntitet(false, true, false, false));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        byggArbeidForBehandling(behandling, fødselsdato, List.of(virksomhet, person));

        var fpffKonto = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).medMaxDager(15).build();
        var mødrekvoteKonto = Stønadskonto.builder().medStønadskontoType(StønadskontoType.MØDREKVOTE).medMaxDager(50).build();
        var stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(fpffKonto)
            .medStønadskonto(mødrekvoteKonto)
            .build();
        relasjonRepository.lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);

        opprettGrunnlag(behandling.getId(), mottattDato);

        // Act
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(person, null);

        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        var uttakAktivitetVirksomhet = aktivitetMedArbeidsgiverIPeriode(virksomhet, uttakResultat.get().getGjeldendePerioder().getPerioder().get(0));
        var uttakAktivitetPerson = aktivitetMedArbeidsgiverIPeriode(person, uttakResultat.get().getGjeldendePerioder().getPerioder().get(0));
        assertThat(uttakAktivitetVirksomhet).isPresent();
        assertThat(uttakAktivitetPerson).isPresent();
        assertThat(uttakAktivitetVirksomhet.get()).isNotEqualTo(uttakAktivitetPerson.get());
        assertThat(uttakAktivitetVirksomhet.get().getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
        assertThat(uttakAktivitetPerson.get().getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
        assertThat(uttakAktivitetVirksomhet.get().getUttakArbeidType()).isEqualTo(UttakArbeidType.ORDINÆRT_ARBEID);
        assertThat(uttakAktivitetPerson.get().getUttakArbeidType()).isEqualTo(UttakArbeidType.ORDINÆRT_ARBEID);
    }

    @Test
    public void arbeidsforhold_skal_kunne_tilkomme_i_løpet_av_uttaket() {
        var virksomhet1 = Arbeidsgiver.virksomhet("123");
        var virksomhet2 = Arbeidsgiver.virksomhet("456");
        var fødselsdato = LocalDate.of(2019, 12, 4);
        var oppgittFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var oppgittMødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittFpff, oppgittMødrekvote), true))
            .medOppgittRettighet(beggeRett());
        var behandling = scenario.lagre(repositoryProvider);
        relasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        opprettStønadskontoerForFarOgMor(behandling);
        opprettGrunnlag(behandling.getId(), fødselsdato);

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet1, InternArbeidsforholdRef.nullRef());
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet2, InternArbeidsforholdRef.nyRef());

        var aktørArbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty()).medAktørId(behandling.getAktørId());
        leggTilYrkesaktivitet(virksomhet1, aktørArbeid, DatoIntervallEntitet.fraOgMed(LocalDate.of(2016, 1, 1)));
        leggTilYrkesaktivitet(virksomhet2, aktørArbeid, DatoIntervallEntitet.fraOgMed(oppgittMødrekvote.getFom().plusWeeks(3)));
        var iay = iayTjeneste.opprettBuilderForRegister(behandling.getId()).leggTilAktørArbeid(aktørArbeid);

        iayTjeneste.lagreIayAggregat(behandling.getId(), iay);
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse));
        var input = new UttakInput(
            BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fødselsdato).build()),
            iayTjeneste.hentGrunnlag(behandling.getId()), fpGrunnlag).medSøknadMottattDato(oppgittFpff.getFom())
            .medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser());
        tjeneste().fastsettePerioder(input);

        var resultat = fpUttakRepository.hentUttakResultat(input.getBehandlingReferanse().behandlingId());

        assertThat(resultat.getGjeldendePerioder().getPerioder()).hasSize(3);
        var aktiviteterPeriode1 = resultat.getGjeldendePerioder().getPerioder().get(0).getAktiviteter();
        var aktiviteterPeriode2 = resultat.getGjeldendePerioder().getPerioder().get(1).getAktiviteter();
        //Knekker mødrekvote på startdato på arbeidsforholdet
        var aktiviteterPeriode3 = resultat.getGjeldendePerioder().getPerioder().get(2).getAktiviteter();
        assertThat(arbeidsgivereIPeriode(aktiviteterPeriode1)).containsExactly(virksomhet1);
        assertThat(arbeidsgivereIPeriode(aktiviteterPeriode2)).containsExactly(virksomhet1);
        assertThat(arbeidsgivereIPeriode(aktiviteterPeriode3)).containsExactlyInAnyOrder(virksomhet1, virksomhet2);
    }

    private void leggTilYrkesaktivitet(Arbeidsgiver virksomhet,
                                       InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                       DatoIntervallEntitet periode) {
        var aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(periode)
            .medProsentsats(Stillingsprosent.HUNDRED)
            .medSisteLønnsendringsdato(periode.getFomDato());
        var ansettelsesperiode = AktivitetsAvtaleBuilder.ny().medPeriode(periode);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(virksomhet)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet);
    }

    private Set<Arbeidsgiver> arbeidsgivereIPeriode(List<UttakResultatPeriodeAktivitetEntitet> aktiviteterPeriode1) {
        return aktiviteterPeriode1.stream().map(a -> a.getArbeidsgiver()).collect(Collectors.toSet());
    }

    private Optional<UttakAktivitetEntitet> aktivitetMedArbeidsgiverIPeriode(Arbeidsgiver arbeidsgiver, UttakResultatPeriodeEntitet periode) {
        return periode.getAktiviteter()
            .stream()
            .map(UttakResultatPeriodeAktivitetEntitet::getUttakAktivitet)
            .filter(uttakAktivitet -> uttakAktivitet.getArbeidsgiver().orElseThrow().equals(arbeidsgiver))
            .findFirst();
    }

    private Arbeidsgiver person() {
        return Arbeidsgiver.person(AktørId.dummy());
    }

    private Arbeidsgiver virksomhet() {
        return virksomhet(OrgNummer.KUNSTIG_ORG);
    }

    private Arbeidsgiver virksomhet(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    private FastsettePerioderTjeneste tjeneste() {
        return new FastsettePerioderTjeneste(repositoryProvider.getFpUttakRepository(), repositoryProvider.getYtelsesFordelingRepository(),
            validator(), regelAdapter, uttakTjeneste);
    }

    @Test
    public void overstyrtSkalLeggesTilOpprinnelig() {
        // Steg 1: Opprett uttaksplan med perioder
        var mottattDato = LocalDate.now();
        var fødselsdato = LocalDate.now().minusMonths(3).withDayOfMonth(1);

        var opprinneligMødreKvoteSlutt = fødselsdato.plusWeeks(6).minusDays(1);
        var opprinneligFellesPeriodeSlutt = opprinneligMødreKvoteSlutt.plusWeeks(4);
        var virksomhet = virksomhet();
        var opprinneligeMødreKvote = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriode(fødselsdato, opprinneligMødreKvoteSlutt)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var opprinneligFellesPeriode = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriode(opprinneligMødreKvoteSlutt.plusDays(1), opprinneligFellesPeriodeSlutt)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var behandling = behandlingMedSøknadsperioder(List.of(opprinneligeMødreKvote, opprinneligFellesPeriode));
        byggArbeidForBehandling(behandling, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        var fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        var opprinneligResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());

        // Steg 2: Opprett overstyrt uttaksplan med perioder
        var overtstyrtMødrekvote = periodeAktivitet(StønadskontoType.MØDREKVOTE);
        var overstyrtFelleskvote = periodeAktivitet(StønadskontoType.FELLESPERIODE);
        var mødreKvotePeriode = innvilgetPeriode(fødselsdato, opprinneligMødreKvoteSlutt, overtstyrtMødrekvote);
        var fellesKvotePeriode1 = innvilgetPeriode(opprinneligMødreKvoteSlutt.plusDays(1), opprinneligFellesPeriodeSlutt.minusWeeks(2),
            overstyrtFelleskvote);
        var fellesKvotePeriode2 = innvilgetPeriode(opprinneligFellesPeriodeSlutt.minusWeeks(2).plusDays(1), opprinneligFellesPeriodeSlutt,
            overstyrtFelleskvote);
        var perioder = List.of(mødreKvotePeriode, fellesKvotePeriode1, fellesKvotePeriode2);

        // Act
        fastsettePerioderTjeneste.manueltFastsettePerioder(new UttakInput(BehandlingReferanse.fra(behandling), null, null), perioder);

        // Assert
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(uttakResultat).isPresent();
        var opprinneligePerioder = uttakResultat.get().getOpprinneligPerioder().getPerioder();
        assertThat(opprinneligePerioder).hasSize(opprinneligResultat.get().getOpprinneligPerioder().getPerioder().size());
        var overstyrtePerioder = uttakResultat.get()
            .getOverstyrtPerioder()
            .getPerioder()
            .stream()
            .sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getTom))
            .collect(Collectors.toList());
        assertThat(overstyrtePerioder).hasSize(3);
        assertThat(overstyrtePerioder.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(overstyrtePerioder.get(0).getTom()).isEqualTo(opprinneligMødreKvoteSlutt);
        assertThat(overstyrtePerioder.get(1).getFom()).isEqualTo(opprinneligMødreKvoteSlutt.plusDays(1));
        assertThat(overstyrtePerioder.get(1).getTom()).isEqualTo(opprinneligFellesPeriodeSlutt.minusWeeks(2));
        assertThat(overstyrtePerioder.get(2).getFom()).isEqualTo(opprinneligFellesPeriodeSlutt.minusWeeks(2).plusDays(1));
        assertThat(overstyrtePerioder.get(2).getTom()).isEqualTo(opprinneligFellesPeriodeSlutt);
    }

    @Test
    void skal_ikke_lagre_konto_hvis_innvilget_utsettelse() {

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(LocalDate.now()).build())
            .lagre(repositoryProvider);

        var opprinneligePerioder = new UttakResultatPerioderEntitet();
        var opprinneligePeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(2))
            .medResultatType(PeriodeResultatType.MANUELL_BEHANDLING, PeriodeResultatÅrsak.SYKDOM_SKADE_INNLEGGELSE_IKKE_DOKUMENTERT)
            .medUtsettelseType(UttakUtsettelseType.SYKDOM_SKADE)
            .build();
        opprinneligePeriode.leggTilAktivitet(new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligePeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkdager(new Trekkdager(10))
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());
        opprinneligePerioder.leggTilPeriode(opprinneligePeriode);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligePerioder);

        var innvilgetUtsettelse = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(opprinneligePeriode.getFom(), opprinneligePeriode.getTom())
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_SYKDOM)
            .medUtsettelseType(UttakUtsettelseType.SYKDOM_SKADE)
            .medAktiviteter(List.of(new ForeldrepengerUttakPeriodeAktivitet.Builder().medTrekkonto(StønadskontoType.FELLESPERIODE)
                .medTrekkdager(Trekkdager.ZERO)
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .build()))
            .build();
        var perioder = List.of(innvilgetUtsettelse);

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        tjeneste().manueltFastsettePerioder(uttakInput, perioder);

        var lagretUttak = uttakTjeneste.hentUttak(behandling.getId());

        assertThat(lagretUttak.getGjeldendePerioder().get(0).getUtsettelseType()).isEqualTo(UttakUtsettelseType.SYKDOM_SKADE);
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getFom()).isEqualTo(innvilgetUtsettelse.getFom());
        assertThat(lagretUttak.getGjeldendePerioder().get(0).isInnvilgetUtsettelse()).isTrue();
        assertThat(lagretUttak.getGjeldendePerioder().get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.UDEFINERT);

    }

    private ForeldrepengerUttakPeriode innvilgetPeriode(LocalDate fom, LocalDate tom, ForeldrepengerUttakPeriodeAktivitet aktivitet) {
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(new LocalDateInterval(fom, tom))
            .medAktiviteter(List.of(aktivitet))
            .medBegrunnelse("begrunnelse")
            .medResultatType(PeriodeResultatType.INNVILGET)
            .build();
    }

    private ForeldrepengerUttakPeriodeAktivitet periodeAktivitet(StønadskontoType fellesperiode) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medTrekkonto(fellesperiode)
            .medArbeidsprosent(BigDecimal.TEN)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medTrekkdager(new Trekkdager(2))
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, virksomhet(), null))
            .build();
    }

    private void opprettGrunnlag(Long behandlingId, LocalDate mottattDato) {
        var uttaksperiodegrense = new Uttaksperiodegrense(mottattDato);

        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(mottattDato).build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId).medAvklarteDatoer(avklarteUttakDatoer);

        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private void opprettStønadskontoerBareFarHarRett(Behandling behandling) {
        var konto = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(50).build();
        var stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(konto)
            .build();
        relasjonRepository.lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private void opprettStønadskontoerForFarOgMor(Behandling behandling) {
        var foreldrepengerFørFødsel = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).medMaxDager(15).build();
        var mødrekvote = Stønadskonto.builder().medStønadskontoType(StønadskontoType.MØDREKVOTE).medMaxDager(50).build();
        var fedrekvote = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FEDREKVOTE).medMaxDager(50).build();
        var fellesperiode = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FELLESPERIODE).medMaxDager(50).build();
        var stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fedrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(foreldrepengerFørFødsel)
            .build();
        relasjonRepository.lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private Behandling behandlingMedSøknadsperioder(List<OppgittPeriodeEntitet> perioder) {
        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var fordeling = new OppgittFordelingEntitet(perioder, true);
        var rettighet = beggeRett();

        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittDekningsgrad(dekningsgrad)
            .medFordeling(fordeling)
            .medOppgittRettighet(rettighet)
            .lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);
    }

    private void byggArbeidForBehandling(Behandling behandling, LocalDate familieHendelse, List<Arbeidsgiver> arbeidsgivere) {
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        for (var arbeidsgiver : arbeidsgivere) {
            var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(behandling.getAktørId());
            var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(null, arbeidsgiver.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

            var fraOgMed = familieHendelse.minusYears(1);
            var tilOgMed = familieHendelse.plusYears(10);

            var aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
                .medProsentsats(BigDecimal.valueOf(100))
                .medSisteLønnsendringsdato(familieHendelse);

            var ansettelesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));

            yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelesperiode)
                .build();

            var aktørArbeid = aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
    }
}
