package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

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
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
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
import no.nav.foreldrepenger.domene.typer.Saksnummer;
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
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.OverstyrUttakResultatValidator;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakForTest;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMedMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryProviderForTest;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class FastsettePerioderTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProviderForTest();

    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

    private final FagsakRelasjonRepository relasjonRepository = repositoryProvider.getFagsakRelasjonRepository();

    private final FpUttakRepository fpUttakRepository = repositoryProvider.getFpUttakRepository();

    private final UttaksperiodegrenseRepository uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();

    private final FastsettePerioderRegelAdapter regelAdapter = new FastsettePerioderRegelAdapter(
        new FastsettePerioderRegelGrunnlagBygger(new AnnenPartGrunnlagBygger(repositoryProvider.getFpUttakRepository()),
            new ArbeidGrunnlagBygger(repositoryProvider), new BehandlingGrunnlagBygger(),
            new DatoerGrunnlagBygger(new PersonopplysningerForUttakForTest()), new MedlemskapGrunnlagBygger(),
            new RettOgOmsorgGrunnlagBygger(repositoryProvider,
                new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository())),
            new RevurderingGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository(),
                repositoryProvider.getFpUttakRepository()),
            new SøknadGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository()),
            new InngangsvilkårGrunnlagBygger(repositoryProvider), new OpptjeningGrunnlagBygger(),
            new AdopsjonGrunnlagBygger(), new KontoerGrunnlagBygger(repositoryProvider)),
        new FastsettePerioderRegelResultatKonverterer(fpUttakRepository, ytelsesFordelingRepository));

    private final ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(fpUttakRepository);

    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();

    @Test
    public void skalInnvilgeFedrekvoteForMedmor() {
        // Setup
        LocalDate mottattDato = LocalDate.now();
        LocalDate fødselsdato = LocalDate.now().minusWeeks(6);
        AktørId aktørId = AktørId.dummy();

        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medArbeidsgiver(virksomhet)
            .build();

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var fordeling = new OppgittFordelingEntitet(List.of(fedrekvote), true);
        var rettighet = new OppgittRettighetEntitet(true, true, false);

        var behandling = ScenarioMedMorSøkerForeldrepenger.forFødsel()
            .medOppgittDekningsgrad(dekningsgrad)
            .medFordeling(fordeling)
            .medOppgittRettighet(rettighet)
            .lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);

        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);

        // Act
        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        assertThat(uttakResultat).isPresent();
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = uttakResultat.get()
            .getOpprinneligPerioder()
            .getPerioder();
        assertThat(uttakResultatPerioder).hasSize(1);

        UttakResultatPeriodeEntitet resultatPeriode = uttakResultatPerioder.iterator().next();
        assertThat(resultatPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FEDREKVOTE);
        assertThat(resultatPeriode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }

    private UttakInput lagInput(Behandling behandling, LocalDate fødselsdato) {
        var ref = BehandlingReferanse.fra(behandling, fødselsdato);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(), 0);
        FamilieHendelser familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
        return new UttakInput(ref, iayGrunnlag, fpGrunnlag).medBeregningsgrunnlagStatuser(
            beregningsandelTjeneste.hentStatuser()).medSøknadMottattDato(fødselsdato.minusWeeks(4));
    }

    private OverstyrUttakResultatValidator validator() {
        return mock(OverstyrUttakResultatValidator.class);
    }

    @Test
    public void oppretterOgLagrerUttakResultatPlanOgUttakPerioderPerArbeidsgiver() {
        // Setup
        LocalDate mottattDato = LocalDate.now();
        LocalDate fødselsdato = LocalDate.now();
        AktørId aktørId = AktørId.dummy();

        Arbeidsgiver arbeidsgiver1 = virksomhet("orgnr1");
        Arbeidsgiver arbeidsgiver2 = virksomhet("orgnr2");

        OppgittPeriodeEntitet fpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();

        OppgittPeriodeEntitet mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();

        OppgittPeriodeEntitet fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .build();

        Behandling behandling = behandlingMedSøknadsperioder(List.of(fpff, mødrekvote, fellesperiode));
        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(arbeidsgiver1, arbeidsgiver2));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);


        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver1, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver2, null);

        // Act
        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        assertThat(uttakResultat).isPresent();
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = uttakResultat.get()
            .getOpprinneligPerioder()
            .getPerioder();
        assertThat(uttakResultatPerioder).hasSize(3);
    }

    @Test
    public void arbeidstidsprosentOgUtbetalingsgradSkalHa2Desimaler() {
        // Setup
        LocalDate mottattDato = LocalDate.now().minusWeeks(1);
        LocalDate fødselsdato = LocalDate.now().minusWeeks(8);
        AktørId aktørId = AktørId.dummy();

        BigDecimal arbeidsprosent = new BigDecimal("50.55");

        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(7))
            .build();

        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet periode3 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(7).plusDays(1), fødselsdato.plusWeeks(8))
            .medErArbeidstaker(true)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        Behandling behandling = behandlingMedSøknadsperioder(List.of(periode1, periode2, periode3));
        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);

        // Act

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        assertThat(uttakResultat).isPresent();
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = uttakResultat.get()
            .getOpprinneligPerioder()
            .getPerioder();
        assertThat(uttakResultatPerioder.get(3).getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(uttakResultatPerioder.get(3).getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(
            new Utbetalingsgrad(49.45));
    }

    @Test
    public void sletterGammeltResultatOgOppretterNyttResultatDersomOpprinneligResultatFinnesFraFør() {
        // Steg 1: Opprett uttaksplan med perioder
        LocalDate mottattDato = LocalDate.now();
        LocalDate fødselsdato = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        AktørId aktørId = AktørId.dummy();

        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsgiver(virksomhet)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();

        Behandling behandling = behandlingMedSøknadsperioder(List.of(periode1));
        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);

        // Act
        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        assertThat(uttakResultat).isPresent();
        var uttakResultatPerioder = uttakResultat.get().getOpprinneligPerioder().getPerioder();
        assertThat(uttakResultatPerioder).hasSize(1);

        var mødrekvote = uttakResultatPerioder.stream()
            .filter(
                p -> StønadskontoType.MØDREKVOTE.getKode().equals(p.getAktiviteter().get(0).getTrekkonto().getKode()))
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
            .filter(p -> StønadskontoType.FORELDREPENGER.getKode()
                .equals(p.getAktiviteter().get(0).getTrekkonto().getKode()))
            .findFirst();
        assertThat(foreldrepengerPeriode).isPresent();
    }

    @Test
    public void foreldrepengerFødsel_gi_innvilget() {
        // Skal treffe UT1211 i foreldrepenger delregel

        // Setup
        LocalDate mottattDato = LocalDate.now().minusWeeks(1);
        LocalDate fødselsdato = LocalDate.now().minusWeeks(8);
        AktørId aktørId = AktørId.dummy();

        OppgittPeriodeEntitet fpffSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        OppgittPeriodeEntitet foreldrepengerSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(2))
            .build();

        Arbeidsgiver virksomhet = virksomhet();

        Behandling behandling = behandlingMedSøknadsperioder(List.of(fpffSøknadsperiode, foreldrepengerSøknadsperiode));
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(new OppgittRettighetEntitet(false, true, true));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(virksomhet));

        Stønadskonto fpffKonto = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        Stønadskonto foreldrepengerKonto = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER)
            .medMaxDager(50)
            .build();
        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(fpffKonto)
            .medStønadskonto(foreldrepengerKonto)
            .build();
        relasjonRepository.lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);

        opprettGrunnlag(behandling.getId(), mottattDato);

        // Act

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert
        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        UttakResultatPeriodeEntitet resultat = uttakResultat.get().getOpprinneligPerioder().getPerioder().get(1);
        assertThat(resultat.getDokRegel().isTilManuellBehandling()).isFalse();
        assertThat(resultat.getResultatÅrsak()).isInstanceOf(InnvilgetÅrsak.class);
    }

    @Test
    public void oppretterOgLagrerUttakResultatPlanOgUttakPerioderPerNårArbeidsgivereErKombinasjonAvPrivatpersonOgVirksomhet() {
        // Setup
        LocalDate mottattDato = LocalDate.now().minusWeeks(1);
        LocalDate fødselsdato = LocalDate.now().minusWeeks(8);
        AktørId aktørId = AktørId.dummy();

        OppgittPeriodeEntitet fpffSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        OppgittPeriodeEntitet foreldrepengerSøknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(5))
            .build();

        Arbeidsgiver virksomhet = virksomhet();
        Arbeidsgiver person = person();

        Behandling behandling = behandlingMedSøknadsperioder(List.of(fpffSøknadsperiode, foreldrepengerSøknadsperiode));

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(new OppgittRettighetEntitet(false, true, true));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(virksomhet, person));

        Stønadskonto fpffKonto = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        Stønadskonto mødrekvoteKonto = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
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

        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        // Assert

        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        Optional<UttakAktivitetEntitet> uttakAktivitetVirksomhet = aktivitetMedArbeidsgiverIPeriode(virksomhet,
            uttakResultat.get().getGjeldendePerioder().getPerioder().get(0));
        Optional<UttakAktivitetEntitet> uttakAktivitetPerson = aktivitetMedArbeidsgiverIPeriode(person,
            uttakResultat.get().getGjeldendePerioder().getPerioder().get(0));
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
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        var behandling = scenario.lagre(repositoryProvider);
        relasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        opprettStønadskontoerForFarOgMor(behandling);
        opprettGrunnlag(behandling.getId(), fødselsdato);

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet1, InternArbeidsforholdRef.nullRef());
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet2, InternArbeidsforholdRef.nyRef());

        var aktørArbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId());
        leggTilYrkesaktivitet(virksomhet1, aktørArbeid, DatoIntervallEntitet.fraOgMed(LocalDate.of(2016, 1, 1)));
        leggTilYrkesaktivitet(virksomhet2, aktørArbeid,
            DatoIntervallEntitet.fraOgMed(oppgittMødrekvote.getFom().plusWeeks(3)));
        var iay = iayTjeneste.opprettBuilderForRegister(behandling.getId()).leggTilAktørArbeid(aktørArbeid);

        iayTjeneste.lagreIayAggregat(behandling.getId(), iay);
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medSøknadHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling, fødselsdato),
            iayTjeneste.hentGrunnlag(behandling.getId()), fpGrunnlag).medSøknadMottattDato(oppgittFpff.getFom())
            .medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser());
        tjeneste().fastsettePerioder(input);

        var resultat = fpUttakRepository.hentUttakResultat(input.getBehandlingReferanse().getId());

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

    private Optional<UttakAktivitetEntitet> aktivitetMedArbeidsgiverIPeriode(Arbeidsgiver arbeidsgiver,
                                                                             UttakResultatPeriodeEntitet periode) {
        return periode.getAktiviteter()
            .stream()
            .filter(aktivitet -> aktivitet.getUttakAktivitet().getArbeidsgiver().orElseThrow().equals(arbeidsgiver))
            .map(UttakResultatPeriodeAktivitetEntitet::getUttakAktivitet)
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
        return new FastsettePerioderTjeneste(repositoryProvider.getFpUttakRepository(),
            repositoryProvider.getYtelsesFordelingRepository(), validator(), regelAdapter, uttakTjeneste);
    }

    @Test
    public void overstyrtSkalLeggesTilOpprinnelig() {
        // Steg 1: Opprett uttaksplan med perioder
        LocalDate mottattDato = LocalDate.now();
        LocalDate fødselsdato = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        AktørId aktørId = AktørId.dummy();

        LocalDate opprinneligMødreKvoteSlutt = fødselsdato.plusWeeks(6).minusDays(1);
        LocalDate opprinneligFellesPeriodeSlutt = opprinneligMødreKvoteSlutt.plusWeeks(4);
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet opprinneligeMødreKvote = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriode(fødselsdato, opprinneligMødreKvoteSlutt)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        OppgittPeriodeEntitet opprinneligFellesPeriode = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriode(opprinneligMødreKvoteSlutt.plusDays(1), opprinneligFellesPeriodeSlutt)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        Behandling behandling = behandlingMedSøknadsperioder(List.of(opprinneligeMødreKvote, opprinneligFellesPeriode));
        byggArbeidForBehandling(behandling, aktørId, fødselsdato, List.of(virksomhet));
        opprettStønadskontoerForFarOgMor(behandling);

        opprettGrunnlag(behandling.getId(), mottattDato);

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);
        FastsettePerioderTjeneste fastsettePerioderTjeneste = tjeneste();
        fastsettePerioderTjeneste.fastsettePerioder(lagInput(behandling, fødselsdato));

        Optional<UttakResultatEntitet> opprinneligResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());

        // Steg 2: Opprett overstyrt uttaksplan med perioder
        var overtstyrtMødrekvote = periodeAktivitet(StønadskontoType.MØDREKVOTE);
        var overstyrtFelleskvote = periodeAktivitet(StønadskontoType.FELLESPERIODE);
        var mødreKvotePeriode = innvilgetPeriode(fødselsdato, opprinneligMødreKvoteSlutt, overtstyrtMødrekvote);
        var fellesKvotePeriode1 = innvilgetPeriode(opprinneligMødreKvoteSlutt.plusDays(1),
            opprinneligFellesPeriodeSlutt.minusWeeks(2), overstyrtFelleskvote);
        var fellesKvotePeriode2 = innvilgetPeriode(opprinneligFellesPeriodeSlutt.minusWeeks(2).plusDays(1),
            opprinneligFellesPeriodeSlutt, overstyrtFelleskvote);
        List<ForeldrepengerUttakPeriode> perioder = List.of(mødreKvotePeriode, fellesKvotePeriode1,
            fellesKvotePeriode2);

        // Act
        fastsettePerioderTjeneste.manueltFastsettePerioder(
            new UttakInput(BehandlingReferanse.fra(behandling), null, null), perioder);

        // Assert
        Optional<UttakResultatEntitet> uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(
            behandling.getId());
        assertThat(uttakResultat).isPresent();
        List<UttakResultatPeriodeEntitet> opprinneligePerioder = uttakResultat.get()
            .getOpprinneligPerioder()
            .getPerioder();
        assertThat(opprinneligePerioder).hasSize(
            opprinneligResultat.get().getOpprinneligPerioder().getPerioder().size());
        List<UttakResultatPeriodeEntitet> overstyrtePerioder = uttakResultat.get()
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
        assertThat(overstyrtePerioder.get(2).getFom()).isEqualTo(
            opprinneligFellesPeriodeSlutt.minusWeeks(2).plusDays(1));
        assertThat(overstyrtePerioder.get(2).getTom()).isEqualTo(opprinneligFellesPeriodeSlutt);
    }

    private ForeldrepengerUttakPeriode innvilgetPeriode(LocalDate fom,
                                                        LocalDate tom,
                                                        ForeldrepengerUttakPeriodeAktivitet aktivitet) {
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
        var br = repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(br)
            .medMottattDato(mottattDato)
            .medFørsteLovligeUttaksdag(mottattDato.withDayOfMonth(1).minusMonths(3))
            .build();

        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medJustertEndringsdato(mottattDato).build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medAvklarteDatoer(avklarteUttakDatoer);

        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private Fagsak opprettFagsak(RelasjonsRolleType relasjonsRolleType, AktørId aktørId) {
        return Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(aktørId), relasjonsRolleType,
            new Saksnummer("2" + aktørId.getId()));
    }

    private void opprettStønadskontoerForFarOgMor(Behandling behandling) {
        Stønadskonto foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        Stønadskonto mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskonto fedrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskonto fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(50)
            .build();
        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
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
        var rettighet = new OppgittRettighetEntitet(true, true, false);

        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittDekningsgrad(dekningsgrad)
            .medFordeling(fordeling)
            .medOppgittRettighet(rettighet)
            .lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);
    }

    private void byggArbeidForBehandling(Behandling behandling,
                                         AktørId aktørId,
                                         LocalDate familieHendelse,
                                         List<Arbeidsgiver> arbeidsgivere) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(
            behandling.getId());
        for (Arbeidsgiver arbeidsgiver : arbeidsgivere) {
            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørArbeidBuilder(aktørId);
            YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(null, arbeidsgiver.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

            LocalDate fraOgMed = familieHendelse.minusYears(1);
            LocalDate tilOgMed = familieHendelse.plusYears(10);

            AktivitetsAvtaleBuilder aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
                .medProsentsats(BigDecimal.valueOf(100))
                .medSisteLønnsendringsdato(familieHendelse);

            AktivitetsAvtaleBuilder ansettelesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));

            yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelesperiode)
                .build();

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder.leggTilYrkesaktivitet(
                yrkesaktivitetBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
    }
}
