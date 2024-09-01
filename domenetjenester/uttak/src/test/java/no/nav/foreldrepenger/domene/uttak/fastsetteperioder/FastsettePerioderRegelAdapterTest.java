package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
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
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
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
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakStub;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ArbeidsgiverIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.Manuellbehandlingårsak;

class FastsettePerioderRegelAdapterTest {

    private final LocalDate fødselsdato = LocalDate.of(2018, 6, 22);
    private final LocalDate mottattDato = LocalDate.of(2018, 6, 22);

    private final int uker_før_fødsel_fellesperiode_grense = UttakParametre.ukerFørTerminTidligstUttak(fødselsdato);
    private final int uker_før_fødsel_foreldrepenger_grense = UttakParametre.ukerFørTerminSenestUttak(fødselsdato);
    private final int veker_etter_fødsel_mødrekvote_grense = UttakParametre.ukerReservertMorEtterFødsel(fødselsdato);
    private final int maxDagerForeldrepengerFørFødsel = 15;

    private final AktivitetIdentifikator arbeidsforhold = AktivitetIdentifikator.forArbeid(new Orgnummer("1234"), "123");

    private UttakRepositoryProvider repositoryProvider;
    private AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste;
    private FastsettePerioderRegelAdapter fastsettePerioderRegelAdapter;

    @BeforeEach
    void setUp() {
        repositoryProvider = new UttakRepositoryStubProvider();
        var fastsettePerioderRegelResultatKonverterer = new FastsettePerioderRegelResultatKonverterer(
            repositoryProvider.getFpUttakRepository(), repositoryProvider.getYtelsesFordelingRepository());
        var rettOgOmsorgGrunnlagBygger = new RettOgOmsorgGrunnlagBygger(repositoryProvider,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
        var grunnlagBygger = new FastsettePerioderRegelGrunnlagBygger(
            new AnnenPartGrunnlagBygger(repositoryProvider.getFpUttakRepository()),
            new ArbeidGrunnlagBygger(repositoryProvider), new BehandlingGrunnlagBygger(),
            new DatoerGrunnlagBygger(new PersonopplysningerForUttakStub()), new MedlemskapGrunnlagBygger(), rettOgOmsorgGrunnlagBygger,
            new RevurderingGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository(),
                repositoryProvider.getFpUttakRepository()),
            new SøknadGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository()),
            new InngangsvilkårGrunnlagBygger(repositoryProvider), new OpptjeningGrunnlagBygger(),
            new AdopsjonGrunnlagBygger(),
            new KontoerGrunnlagBygger(),
            new YtelserGrunnlagBygger());
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        fastsettePerioderRegelAdapter = new FastsettePerioderRegelAdapter(grunnlagBygger,
            fastsettePerioderRegelResultatKonverterer);
    }

    @Test
    void skalReturnerePlanMedMødrekvotePeriode() {
        // Arrange
        var fødselsdato = LocalDate.of(2021, 11, 4);
        var fpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var arbeidsprosent = BigDecimal.valueOf(50.0);
        var virksomhet = virksomhet();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(arbeidsprosent)
            .build();
        var behandling = setupMor(List.of(fpff, mødrekvote), virksomhet, fpff.getFom(),
            fpff.getTom().plusYears(5));
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(3);

        var fpffPeriode = finnPeriode(uttakResultatPerioder, fødselsdato.minusWeeks(3),
            fødselsdato.minusDays(1));
        var mkPeriode1 = finnPeriode(uttakResultatPerioder, fødselsdato,
            fødselsdato.plusWeeks(6).minusDays(1));
        var mkPeriode2 = finnPeriode(uttakResultatPerioder, mkPeriode1.getTom().plusDays(1),
            fødselsdato.plusWeeks(10));

        assertThat(fpffPeriode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(fpffPeriode.getAktiviteter()).hasSize(1);
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(15));
        // mødrekvote innvilget
        assertThat(mkPeriode1.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mkPeriode1.isGraderingInnvilget()).isFalse();
        assertThat(mkPeriode1.getAktiviteter()).hasSize(1);
        assertThat(mkPeriode1.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(mkPeriode1.getAktiviteter().get(0).getTrekkdager()).isEqualTo(
            new Trekkdager(new SimpleLocalDateInterval(mkPeriode1.getFom(), mkPeriode1.getTom()).antallArbeidsdager()));
        assertThat(mkPeriode2.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mkPeriode2.getAktiviteter()).hasSize(1);
        assertThat(mkPeriode2.isGraderingInnvilget()).isTrue();
        assertThat(mkPeriode2.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(mkPeriode2.getAktiviteter().get(0).getTrekkdager().merEnn0()).isTrue();
    }

    @Test
    void skalReturnerePlanMedHeleForeldrepengerFørFødselPeriode() {
        // Arrange
        var virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense), fødselsdato.minusDays(1))
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupMor(oppgittPeriode, virksomhet,
            fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense), fødselsdato.minusDays(1));
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(2);

        var foreldrepengerUttakPerioder = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter()
                .get(0)
                .getTrekkonto()
                .getKode()
                .equals(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode()))
            .toList();
        assertThat(foreldrepengerUttakPerioder).hasSize(1);

        var foreldrePengerUttakPeriode = foreldrepengerUttakPerioder.get(0);
        assertThat(foreldrePengerUttakPeriode.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(foreldrePengerUttakPeriode.getTom()).isEqualTo(fødselsdato.minusDays(1));

        var gjenståendeDager_foreldrepenger =
            uker_før_fødsel_foreldrepenger_grense * 5 - maxDagerForeldrepengerFørFødsel;
        assertThat(maxDagerForeldrepengerFørFødsel - foreldrePengerUttakPeriode.getAktiviteter()
            .get(0)
            .getTrekkdager()
            .decimalValue()
            .intValue()).isEqualTo(gjenståendeDager_foreldrepenger);

        var fpffPeriode = finnPeriode(uttakResultatPerioder, fødselsdato.minusWeeks(3),
            fødselsdato.minusDays(1));
        var mangledeSøktmkPeriode = finnPeriode(uttakResultatPerioder, fødselsdato,
            fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1));

        assertThat(mangledeSøktmkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(
            new Trekkdager(veker_etter_fødsel_mødrekvote_grense * 5));
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(15));
    }

    private UttakInput lagInput(Behandling behandling,
                                UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste,
                                LocalDate fødselsdato) {
        return lagInput(behandling, beregningsandelTjeneste, fødselsdato, false);
    }

    private UttakInput lagInput(Behandling behandling,
                                UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste,
                                LocalDate fødselsdato,
                                boolean kreverSammenhengendeUttak) {
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        return lagInput(behandling, beregningsandelTjeneste, false,
            new FamilieHendelser().medBekreftetHendelse(familieHendelse), kreverSammenhengendeUttak);
    }

    private UttakInput lagInput(Behandling behandling,
                                UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste,
                                boolean berørtBehandling,
                                FamilieHendelser familieHendelser,
                                boolean kreverSammenhengendeUttak) {
        var utledetStp = familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato().minusMonths(3);
        var stp = Skjæringstidspunkt.builder()
            .medKreverSammenhengendeUttak(kreverSammenhengendeUttak)
            .medUtenMinsterett(utledetStp.isBefore(LocalDate.of(2022,8,2)))
            //Stp ikke relevant for testene, må bare være fom første søkte periode
            .medUtledetSkjæringstidspunkt(utledetStp)
            .build();
        var ref = BehandlingReferanse.fra(behandling);
        var originalBehandling = behandling.getOriginalBehandlingId().isPresent() ? new OriginalBehandling(
            behandling.getOriginalBehandlingId().get(), null) : null;
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medErBerørtBehandling(berørtBehandling)
            .medFamilieHendelser(familieHendelser)
            .medOriginalBehandling(originalBehandling);
        return new UttakInput(ref, stp, iayTjeneste.hentGrunnlag(behandling.getId()), ytelsespesifiktGrunnlag)
            .medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser());
    }

    @Test
    void skalReturnereManuellBehandlingForPlanMedForTidligOppstartAvFedrekvote() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var startDato = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusWeeks(3);
        var sluttDato = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).plusWeeks(1);

        // Arrange
        var virksomhet = virksomhet();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(startDato, sluttDato)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupFar(oppgittPeriode, virksomhet, startDato, sluttDato);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(2);

        var periode1 = uttakResultatPerioder.get(0);
        assertThat(periode1.getFom()).isEqualTo(startDato);
        assertThat(periode1.getTom()).isEqualTo(fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1));
        assertThat(periode1.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode1.getManuellBehandlingÅrsak().getKode()).isEqualTo(
            String.valueOf(Manuellbehandlingårsak.UGYLDIG_STØNADSKONTO.getId()));

        var periode2 = uttakResultatPerioder.get(1);
        assertThat(periode2.getFom()).isEqualTo(fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense));
        assertThat(periode2.getTom()).isEqualTo(sluttDato);
        assertThat(periode2.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }

    @Test
    void skalReturnereManuellBehandlingForPlanMedForTidligFedrekvoteRundtFødsel() {
        var fødselsdato = LocalDate.of(2023, 6, 22);
        var startDato = fødselsdato.minusWeeks(3);
        var sluttDato = fødselsdato.plusWeeks(1);

        // Arrange
        var virksomhet = virksomhet();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(startDato, sluttDato)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupFar(oppgittPeriode, virksomhet, startDato, sluttDato);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act
        var kontoer = lagStønadskontoer();
        var farRundtFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FAR_RUNDT_FØDSEL)
            .medMaxDager(10)
            .build();
        kontoer.leggTilStønadskonto(farRundtFødsel);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, kontoer);
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(2);

        var periode1 = uttakResultatPerioder.get(0);
        assertThat(periode1.getFom()).isEqualTo(startDato);
        assertThat(periode1.getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(periode1.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode1.getManuellBehandlingÅrsak().getKode()).isEqualTo(
            String.valueOf(Manuellbehandlingårsak.FAR_SØKER_FØR_FØDSEL.getId()));

        var periode2 = uttakResultatPerioder.get(1);
        assertThat(periode2.getFom()).isEqualTo(fødselsdato);
        assertThat(periode2.getTom()).isEqualTo(sluttDato);
        assertThat(periode2.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }

    @Test
    void skalReturnerePlanMedManuelleFellesperiodeFørFødselNårSøkerErFar() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var startDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(5);
        var sluttDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(2);

        // Arrange
        var virksomhet = virksomhet();
        var fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(startDatoFellesperiode, sluttDatoFellesperiode)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupFar(fellesperiode, virksomhet, startDatoFellesperiode, sluttDatoFellesperiode);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();

        var manuelleFellesPerioderStream = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter()
                .get(0)
                .getTrekkonto()
                .getKode()
                .equals(UttakPeriodeType.FELLESPERIODE.getKode()) && uttakResultatPeriode.getResultatType()
                .equals(PeriodeResultatType.MANUELL_BEHANDLING));
        var manuelleFellesPerioder = manuelleFellesPerioderStream.collect(
            Collectors.toList());
        assertThat(manuelleFellesPerioder).hasSize(1);

        var manuellFellesPeriode = manuelleFellesPerioder.get(0);
        assertThat(manuellFellesPeriode.getFom()).isEqualTo(startDatoFellesperiode);
        assertThat(manuellFellesPeriode.getTom()).isEqualTo(sluttDatoFellesperiode);
    }

    @Test
    void morSøkerFellesperiodeFørFødselMedOppholdFørForeldrepenger() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var startDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(5);
        var sluttDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(2);

        // Arrange
        var virksomhet = virksomhet();
        var fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(startDatoFellesperiode, sluttDatoFellesperiode)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupMor(fellesperiode, virksomhet, startDatoFellesperiode, sluttDatoFellesperiode);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato, true);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();

        var tidligUttakFP = finnPeriode(uttakResultatPerioder, startDatoFellesperiode,
            sluttDatoFellesperiode);
        var manglendeSøktPeriodeFP = finnPeriode(uttakResultatPerioder,
            sluttDatoFellesperiode.plusDays(3), fødselsdato.minusWeeks(3).minusDays(1));
        var manglendeSøktPeriodeFPFF = finnPeriode(uttakResultatPerioder,
            fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var manglendeSøktPeriodeMK = finnPeriode(uttakResultatPerioder, fødselsdato,
            fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(tidligUttakFP.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(tidligUttakFP.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(manglendeSøktPeriodeFP.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(manglendeSøktPeriodeFPFF.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(manglendeSøktPeriodeMK.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(manglendeSøktPeriodeMK.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
    }

    @Test
    void morSøkerFellesperiodeFørFødselMedOppholdInniPerioden() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var startDatoFellesperiode1 = fødselsdato.minusWeeks(uker_før_fødsel_fellesperiode_grense);
        var sluttDatoFellesperiode1 = fødselsdato.minusWeeks(uker_før_fødsel_fellesperiode_grense).plusWeeks(2);

        var startDatoFellesperiode2 = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(3);
        var sluttDatoFellesperiode2 = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusDays(1);

        // Arrange
        var virksomhet = virksomhet();
        var fellesperiode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(virksomhet)
            .medPeriode(startDatoFellesperiode1, sluttDatoFellesperiode1)
            .medSamtidigUttak(false)
            .build();

        var fellesperiode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(virksomhet)
            .medPeriode(startDatoFellesperiode2, sluttDatoFellesperiode2)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupMor(List.of(fellesperiode1, fellesperiode2), virksomhet, startDatoFellesperiode1,
            sluttDatoFellesperiode2);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato, true);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(5);

        var tidligUttakFP1 = finnPeriode(uttakResultatPerioder, startDatoFellesperiode1,
            sluttDatoFellesperiode1);
        var manglendeSøktPeriodeFP = finnPeriode(uttakResultatPerioder,
            sluttDatoFellesperiode1.plusDays(3), startDatoFellesperiode2.minusDays(1));
        var tidligUttakFP2 = finnPeriode(uttakResultatPerioder, startDatoFellesperiode2,
            sluttDatoFellesperiode2);
        var manglendeSøktPeriodeFPFF = finnPeriode(uttakResultatPerioder,
            fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var manglendeSøktPeriodeMK = finnPeriode(uttakResultatPerioder, fødselsdato,
            fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(tidligUttakFP1.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(tidligUttakFP1.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        assertThat(manglendeSøktPeriodeFP.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(manglendeSøktPeriodeFP.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);

        assertThat(tidligUttakFP2.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(tidligUttakFP2.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(manglendeSøktPeriodeFPFF.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(manglendeSøktPeriodeFPFF.getResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(manglendeSøktPeriodeMK.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(manglendeSøktPeriodeMK.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
    }

    @Test
    void søknadMedOppholdForAnnenForelderFellesperiodeOgIkkeNokDagerPåKontoTest() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var sluttDatoMødrekvote = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1);

        var startDatoOpphold = sluttDatoMødrekvote.plusDays(1);
        var sluttDatoOpphold = sluttDatoMødrekvote.plusWeeks(5);

        // Arrange
        var virksomhet = virksomhet();

        var førFødsel = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, sluttDatoMødrekvote)
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        // Angitt periode for annen forelder
        var opphold = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(KVOTE_FELLESPERIODE_ANNEN_FORELDER)
            .medPeriode(startDatoOpphold, sluttDatoOpphold)
            .build();

        var fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(sluttDatoOpphold.plusDays(1), sluttDatoOpphold.plusWeeks(16))
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        var behandling = setupMor(List.of(førFødsel, mødrekvote, opphold, fellesperiode), virksomhet,
            førFødsel.getFom(), fellesperiode.getTom());
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato, true);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(5);

        var periode = uttakResultatPerioder.get(0);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        periode = uttakResultatPerioder.get(1);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        // Oppholdsperioder for annen forelder skal returneres som en uttakResultatPeriode uten aktivitet
        periode = uttakResultatPerioder.get(2);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(periode.getOppholdÅrsak()).isEqualTo(KVOTE_FELLESPERIODE_ANNEN_FORELDER);
        assertThat(periode.getFom()).isEqualTo(startDatoOpphold);
        assertThat(periode.getTom()).isEqualTo(sluttDatoOpphold);

        // Periode knukket pga ikke nok dager igjen på konto.
        periode = uttakResultatPerioder.get(3);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(periode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(periode.getTom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(16));

        periode = uttakResultatPerioder.get(4);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode.getFom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(16).plusDays(1));
        assertThat(periode.getTom()).isEqualTo(fellesperiode.getTom());

    }

    @Test
    void søknadMedMerEnnTilgjengeligDagerForOpphold() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var sluttDatoMødrekvote = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1);

        var startDatoOpphold = sluttDatoMødrekvote.plusDays(1);
        var sluttDatoOpphold = sluttDatoMødrekvote.plusWeeks(16);

        // Arrange
        var virksomhet = virksomhet();

        var førFødsel = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, sluttDatoMødrekvote)
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        // Angitt periode for annen forelder
        var opphold = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(FEDREKVOTE_ANNEN_FORELDER)
            .medPeriode(startDatoOpphold, sluttDatoOpphold)
            .build();

        var behandling = setupMor(List.of(førFødsel, mødrekvote, opphold), virksomhet, førFødsel.getFom(),
            sluttDatoOpphold);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato, true);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(4);

        var periode = uttakResultatPerioder.get(0);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        periode = uttakResultatPerioder.get(1);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        // Oppholdsperioder for annen forelder skal returneres som en uttakResultatPeriode uten aktivitet
        periode = uttakResultatPerioder.get(2);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(periode.getResultatÅrsak()).isEqualTo(PeriodeResultatÅrsak.UKJENT);
        assertThat(periode.getOppholdÅrsak()).isEqualTo(FEDREKVOTE_ANNEN_FORELDER);
        assertThat(periode.getFom()).isEqualTo(startDatoOpphold);
        assertThat(periode.getTom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(15));

        // Periode knukket pga ikke nok dager igjen på konto.
        periode = uttakResultatPerioder.get(3);
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode.getFom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(15).plusDays(1));
        assertThat(periode.getTom()).isEqualTo(sluttDatoOpphold);
    }

    @Test
    void morSøkerMødrekvoteOgFedrekvote_FårInnvilgetMødrekvoteOgFedrekvoteGårTilManuellBehandling() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var sluttDatoMødrekvote = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1);

        var startDatoFedrekvote = sluttDatoMødrekvote.plusDays(1);
        var sluttDatoFedrekvote = startDatoFedrekvote.plusWeeks(5);

        // Arrange
        var virksomhet = virksomhet();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, sluttDatoMødrekvote)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(startDatoFedrekvote, sluttDatoFedrekvote)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupMor(List.of(mødrekvote, fedrekvote), virksomhet, fødselsdato.minusYears(1),
            sluttDatoFedrekvote);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        // Assert
        var uttakResultatPerioder = resultat.getPerioder();

        var fedrekvotePerioder = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter()
                .get(0)
                .getTrekkonto()
                .getKode()
                .equals(StønadskontoType.FEDREKVOTE.getKode()))
            .toList();
        assertThat(fedrekvotePerioder).hasSize(1);
        assertThat(fedrekvotePerioder.get(0).getFom()).isEqualTo(startDatoFedrekvote);
        assertThat(fedrekvotePerioder.get(0).getTom()).isEqualTo(sluttDatoFedrekvote);

        assertThat(fedrekvotePerioder.get(0).getResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(fedrekvotePerioder.get(0).getResultatÅrsak().getKode()).isEqualTo("4007");
        assertThat(fedrekvotePerioder.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FEDREKVOTE);

        var mødrekvotePerioder = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter()
                .get(0)
                .getTrekkonto()
                .getKode()
                .equals(StønadskontoType.MØDREKVOTE.getKode()))
            .toList();
        assertThat(mødrekvotePerioder).hasSize(1);
        assertThat(mødrekvotePerioder.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(mødrekvotePerioder.get(0).getTom()).isEqualTo(sluttDatoMødrekvote);
        assertThat(mødrekvotePerioder.get(0).getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mødrekvotePerioder.get(0).getResultatÅrsak().getKode()).isEqualTo("2003");
        assertThat(mødrekvotePerioder.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
    }

    @Test
    void morSkalFåAvslåttUtenTrekkdagerPåUtsettelsePgaSykdomIPeriodeFarTarUttak() {
        var fødselsdato = LocalDate.of(2018, 10, 10);
        var morFpffFørstegangs = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var morMødrekvoteFørstegangs = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .build();
        var morUtsettelseFørstegangs = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fødselsdato.plusWeeks(10).plusDays(1), fødselsdato.plusWeeks(12))
            .build();
        var oppgittFordelingMor = new OppgittFordelingEntitet(List.of(morFpffFørstegangs, morMødrekvoteFørstegangs,
            morUtsettelseFørstegangs), true);
        var originalBehandlingMor = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandlingMor, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(oppgittFordelingMor);
        var morBehandling = morScenario.lagre(repositoryProvider);

        var kontoer = lagStønadskontoer();

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel();

        var farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morBehandling.getFagsak());
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak());

        var farUttakresultat = new UttakResultatPerioderEntitet();
        var farPeriode = new UttakResultatPeriodeEntitet.Builder(
            morUtsettelseFørstegangs.getFom(), morUtsettelseFørstegangs.getTom()).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(farPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(BigDecimal.TEN))
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        farUttakresultat.leggTilPeriode(farPeriode);
        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(farBehandling.getId(), kontoer, farUttakresultat);

        var morUtsettelseRevurdering = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(morUtsettelseFørstegangs.getFom(), morUtsettelseFørstegangs.getTom())
            .build();
        var morScenarioRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morScenarioRevurdering.medFordeling(new OppgittFordelingEntitet(List.of(morUtsettelseRevurdering), true));
        morScenarioRevurdering.medOriginalBehandling(morBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        morScenarioRevurdering.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());

        var morBehandlingRevurdering = morScenarioRevurdering.lagre(repositoryProvider);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            morFpffFørstegangs.getFom()).medOpprinneligEndringsdato(morUtsettelseRevurdering.getFom()).build();
        var behandlingId = morBehandlingRevurdering.getId();
        var morUtsettelseRevurderingOverstyrt = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(morUtsettelseFørstegangs.getFom(), morUtsettelseFørstegangs.getTom())
            .medDokumentasjonVurdering(new DokumentasjonVurdering(SYKDOM_SØKER_GODKJENT))
            .build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medAvklarteDatoer(avklarteUttakDatoer)
            .medOverstyrtFordeling(new OppgittFordelingEntitet(List.of(morUtsettelseRevurderingOverstyrt), true));
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());

        var andelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        andelTjeneste.leggTilFrilans();

        lagreUttaksperiodegrense(morBehandlingRevurdering.getId());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        var ref = BehandlingReferanse.fra(morBehandlingRevurdering);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medErBerørtBehandling(true)
            .medFamilieHendelser(familieHendelser)
            .medAnnenpart(new Annenpart(farBehandling.getId(), fødselsdato.atStartOfDay()))
            .medOriginalBehandling(new OriginalBehandling(morBehandling.getId(), null));
        var input = new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fødselsdato).medUtenMinsterett(true).build(), tomIay(), ytelsespesifiktGrunnlag)
            .medBeregningsgrunnlagStatuser(andelTjeneste.hentStatuser());
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        assertThat(resultat.getPerioder()).hasSize(1);
        assertThat(resultat.getPerioder().get(0).getUtsettelseType()).isEqualTo(UttakUtsettelseType.SYKDOM_SKADE);
        assertThat(resultat.getPerioder().get(0).getResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(resultat.getPerioder().get(0).getAktiviteter().get(0).getTrekkdager().decimalValue()).isZero();
    }

    private InntektArbeidYtelseGrunnlag tomIay() {
        return InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty()).build();
    }

    @Test
    void skal_avslå_periode_når_far_søker_fedrekvote_uten_å_ha_omsorg() {
        // Arrange
        var virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var fom = fødselsdato.plusWeeks(10);
        var tom = fødselsdato.plusWeeks(15).minusDays(1);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fom, tom)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var behandling = setupFar(oppgittPeriode, virksomhet, fom, tom, Boolean.FALSE);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(1);

        var periode = uttakResultatPerioder.iterator().next();
        assertThat(periode.getResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(periode.getAktiviteter()).hasSize(1);
        var aktivitet = periode.getAktiviteter().get(0);
        assertThat(aktivitet.getTrekkdager()).isEqualTo(new Trekkdager(5 * 5));
        assertThat(aktivitet.getTrekkonto()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(aktivitet.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.ZERO);
    }

    @Test
    void skal_ta_med_arbeidsforholdprosent_når_gradering_er_opplyst() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var start = fødselsdato.plusWeeks(20);
        var slutt = fødselsdato.plusWeeks(25).minusDays(1);
        var arbeidsprosent = BigDecimal.valueOf(60);
        var virksomhet = virksomhet();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        var behandling = setupFar(oppgittPeriode, virksomhet, start, slutt);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        var fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager((int) (5 * 5 * 0.4)));
        assertThat(fkPeriode.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
    }

    @Test
    void skal_fastsette_perioder_ved_adopsjon_uten_annenpart() {
        var startdato = LocalDate.of(2019, 10, 3);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(startdato, startdato.plusWeeks(6).minusDays(1))
            .build();

        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        var behandling = setup(List.of(oppgittPeriode), virksomhet(), startdato.minusYears(2),
            startdato.plusYears(2), scenario, BigDecimal.valueOf(100), null);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(startdato, List.of(new Barn()), 1, null,
            false);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        var input = lagInput(behandling, beregningsandelTjeneste, false, familieHendelser, false);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).isNotEmpty();
    }

    @Test
    void graderingSkalSettesRiktigEtterKjøringAvRegler() {
        var arbeidsprosent = BigDecimal.valueOf(50);
        var virksomhetSomGradereresHos = virksomhet(new Orgnummer("orgnr1"));
        var annenVirksomhet = virksomhet(new Orgnummer("orgnr2"));
        var fødselsdato = LocalDate.of(2018, 10, 1);
        var oppgittFPFF = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var oppgittMødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(oppgittFPFF.getTom().plusDays(1), oppgittFPFF.getTom().plusWeeks(6))
            .build();
        var oppgittGradertMødrekvote = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhetSomGradereresHos)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            //11 dager
            .medPeriode(oppgittMødrekvote.getTom().plusDays(1), oppgittMødrekvote.getTom().plusDays(1).plusWeeks(2))
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(arbeidsprosent)
            .build();
        var aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        var oppgittFordeling = new OppgittFordelingEntitet(
            List.of(oppgittFPFF, oppgittMødrekvote, oppgittGradertMødrekvote), true);
        scenario.medFordeling(oppgittFordeling);
        var aktivitetsAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(50))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        var yrkesaktivitetBuilder1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var ansettelsesperiode1 = yrkesaktivitetBuilder1.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        var yrkesaktivitet1 = yrkesaktivitetBuilder1.medArbeidsgiver(virksomhetSomGradereresHos)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1);
        var aktivitetsAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(50))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        var yrkesaktivitetBuilder2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var ansettelsesperiode2 = yrkesaktivitetBuilder2.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        var yrkesaktivitet2 = yrkesaktivitetBuilder2.medArbeidsgiver(annenVirksomhet)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2);
        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet1))
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet2));
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhetSomGradereresHos, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(annenVirksomhet, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        var mødrekvote = finnPeriode(uttakResultatPerioder, oppgittMødrekvote.getFom(),
            oppgittMødrekvote.getTom());
        var aktivitetMørdrekvoteVirksomhet1 = aktivitetForArbeidsgiver(
            mødrekvote.getAktiviteter(), virksomhetSomGradereresHos);
        var aktivitetMørdrekvoteVirksomhet2 = aktivitetForArbeidsgiver(
            mødrekvote.getAktiviteter(), annenVirksomhet);
        assertThat(mødrekvote.getResultatÅrsak()).isInstanceOf(PeriodeResultatÅrsak.class);
        assertThat(mødrekvote.isGraderingInnvilget()).isFalse();
        assertThat(mødrekvote.getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.UKJENT);
        assertThat(aktivitetMørdrekvoteVirksomhet1.getTrekkdager()).isEqualTo(new Trekkdager(5 * 6));
        assertThat(aktivitetMørdrekvoteVirksomhet1.getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(aktivitetMørdrekvoteVirksomhet1.isGraderingInnvilget()).isFalse();
        assertThat(aktivitetMørdrekvoteVirksomhet2.getTrekkdager()).isEqualTo(new Trekkdager(5 * 6));
        assertThat(aktivitetMørdrekvoteVirksomhet2.getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(aktivitetMørdrekvoteVirksomhet2.isGraderingInnvilget()).isFalse();

        var gradertMødrekvote = finnPeriode(uttakResultatPerioder,
            oppgittGradertMødrekvote.getFom(), oppgittGradertMødrekvote.getTom());
        var aktivitetGradertMørdrekvoteVirksomhet1 = aktivitetForArbeidsgiver(
            gradertMødrekvote.getAktiviteter(), virksomhetSomGradereresHos);
        var aktivitetGradertMørdrekvoteVirksomhet2 = aktivitetForArbeidsgiver(
            gradertMødrekvote.getAktiviteter(), annenVirksomhet);
        assertThat(gradertMødrekvote.getResultatÅrsak()).isInstanceOf(PeriodeResultatÅrsak.class);
        assertThat(gradertMødrekvote.isGraderingInnvilget()).isTrue();
        assertThat(gradertMødrekvote.getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.UKJENT);
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.getTrekkdager()).isEqualTo(
            new Trekkdager(new BigDecimal("5.5")));
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.isGraderingInnvilget()).isTrue();
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.isSøktGradering()).isTrue();
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.getTrekkdager()).isEqualTo(new Trekkdager(11));
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.isGraderingInnvilget()).isFalse();
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.isSøktGradering()).isFalse();
    }

    @Test
    void skalGi100UtbetalingIPeriodenHvisUtenforAnsettelsesperiode() {
        var virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 10, 1);
        var oppgittFPFF = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        var oppgittMødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(oppgittFPFF.getTom().plusDays(1), oppgittFPFF.getTom().plusWeeks(6))
            .build();
        var aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        var oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittFPFF, oppgittMødrekvote),
            true);
        scenario.medFordeling(oppgittFordeling);
        var aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(100))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        var yrkesaktivitetBuilder1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());

        // Ansettelsesperiode slutter før første uttaksdag
        var ansettelsesperiode = yrkesaktivitetBuilder1.getAktivitetsAvtaleBuilder()
            .medPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), oppgittFPFF.getFom().minusDays(1)));
        var yrkesaktivitet1 = yrkesaktivitetBuilder1.medArbeidsgiver(virksomhet)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet1));

        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        var mødrekvote = finnPeriode(uttakResultatPerioder, oppgittMødrekvote.getFom(),
            oppgittMødrekvote.getTom());
        var aktivitetMørdrekvoteVirksomhet = aktivitetForArbeidsgiver(
            mødrekvote.getAktiviteter(), virksomhet);
        assertThat(aktivitetMørdrekvoteVirksomhet.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100.00));
    }

    private UttakResultatPeriodeAktivitetEntitet aktivitetForArbeidsgiver(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter,
                                                                          Arbeidsgiver arbeidsgiver) {
        return aktiviteter.stream().filter(aktivitet -> {
            var arbeidsgiverUttak = aktivitet.getUttakAktivitet().getArbeidsgiver();
            if (arbeidsgiverUttak.isPresent()) {
                return Objects.equals(arbeidsgiver, arbeidsgiverUttak.get());
            }
            return arbeidsgiver == null;
        }).findFirst().get();
    }

    @Test
    void graderingSkalSettesRiktigVedAvslagAvGraderingEtterKjøringAvRegler() {
        // Søker gradert fpff slik at gradering avslås
        var arbeidsprosent = BigDecimal.valueOf(50);
        var virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var oppgittGradertFPFF = OppgittPeriodeBuilder.ny()
            .medArbeidsprosent(arbeidsprosent)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medArbeidsgiver(virksomhet)
            .build();

        var behandling = setupMor(oppgittGradertFPFF, virksomhet, oppgittGradertFPFF.getFom(),
            oppgittGradertFPFF.getTom());
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        var mødrekvote = finnPeriode(uttakResultatPerioder, oppgittGradertFPFF.getFom(),
            oppgittGradertFPFF.getTom());
        assertThat(mødrekvote.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(3 * 5));
        assertThat(mødrekvote.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(mødrekvote.getAktiviteter().get(0).isGraderingInnvilget()).isFalse();
        assertThat(mødrekvote.getAktiviteter().get(0).isSøktGradering()).isTrue();
        assertThat(mødrekvote.isGraderingInnvilget()).isFalse();
        assertThat(mødrekvote.getGraderingAvslagÅrsak()).isNotEqualTo(GraderingAvslagÅrsak.UKJENT);
    }

    private Arbeidsgiver virksomhet() {
        return virksomhet(arbeidsforhold.getArbeidsgiverIdentifikator());
    }

    private Arbeidsgiver virksomhet(ArbeidsgiverIdentifikator arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator.value());
    }

    @Test
    void skal_ta_med_arbeidsforholdprosent_når_gradering_er_opplyst_også_når_periode_avviker_med_lørdag_søndag() {

        var fødselsdato = LocalDate.of(2018, 6, 22);
        var start = mandag(fødselsdato.plusWeeks(20));
        var slutt = start.plusWeeks(5).minusDays(1);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slutt.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        var arbeidsprosent = BigDecimal.valueOf(60);
        var virksomhet = virksomhet();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        var behandling = setupFar(oppgittPeriode, virksomhet, start, slutt);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        var fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager((int) (5 * 5 * 0.4)));
        assertThat(fkPeriode.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
    }

    @Test
    void utbetalingsgradSkalHa2Desimaler() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var start = mandag(fødselsdato.plusWeeks(20));
        var slutt = start.plusWeeks(5).minusDays(1);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slutt.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        var arbeidsprosent = BigDecimal.valueOf(53);
        var virksomhet = virksomhet();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        var behandling = setupFar(oppgittPeriode, virksomhet, start, slutt);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        var fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        // Utbetalingsgrad (i %) = (stillingsprosent – arbeidsprosent) x 100 / stillingsprosent
        assertThat(fkPeriode.getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(47.00));
    }

    @Test
    void samtidigUttaksprosentSkalHa2Desimaler() {
        var fødselsdato = LocalDate.of(2019, 6, 22);
        var start = mandag(fødselsdato.plusWeeks(20));
        var slutt = start.plusWeeks(5).minusDays(1);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slutt.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        var samtidigUttaksprosent = new SamtidigUttaksprosent(53.33);
        var arbeidsgiver = virksomhet();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(samtidigUttaksprosent)
            .build();

        var behandling = setupFar(oppgittPeriode, arbeidsgiver, start, slutt, null);
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        var fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        assertThat(fkPeriode.getSamtidigUttaksprosent()).isEqualTo(new SamtidigUttaksprosent(53.33));
    }

    @Test
    void skal_håndtere_manuell_behandling_av_for_tidlig_gradering() {
        var virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var fpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        var mk = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsprosent(BigDecimal.valueOf(50))
            .build();

        var behandling = setupMor(List.of(fpff, mk), virksomhet, fpff.getFom(), fødselsdato.plusYears(5));
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(2);

        var fpffPeriode = uttakResultatPerioder.stream()
            .filter(p -> p.getAktiviteter().get(0).getTrekkonto().equals(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL))
            .findFirst()
            .get();
        var mkPeriode = uttakResultatPerioder.stream()
            .filter(p -> p.getAktiviteter().get(0).getTrekkonto().equals(UttakPeriodeType.MØDREKVOTE))
            .findFirst()
            .get();

        // Innvilget FPFF
        assertThat(fpffPeriode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(15));
        // Innvilget mødrekvote, men avslag på gradering.
        assertThat(mkPeriode.getResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mkPeriode.getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.GRADERING_FØR_UKE_7);
        assertThat(mkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(
            new Trekkdager(new SimpleLocalDateInterval(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1)).antallArbeidsdager()));
    }

    @Test
    void skalPrependeUttaksresultatPerioderFørEndringsdatoVedRevurdering() {
        // Lager opprinnelig uttak

        var uttakRevurderingTestUtil = new UttakRevurderingTestUtil(repositoryProvider,
            iayTjeneste);
        var opprinneligFpff = new UttakResultatPeriodeEntitet.Builder(
            fødselsdato.minusWeeks(3).minusDays(1), fødselsdato.minusDays(1)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.FRILANS).build();
        var aktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(
            opprinneligFpff, uttakAktivitet).medTrekkonto(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medErSøktGradering(false)
            .medArbeidsprosent(BigDecimal.TEN)
            .medTrekkdager(new Trekkdager(
                new SimpleLocalDateInterval(opprinneligFpff.getFom(), opprinneligFpff.getTom()).antallArbeidsdager()))
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .build();
        var opprinneligMødrekvote = new UttakResultatPeriodeEntitet.Builder(fødselsdato,
            fødselsdato.plusWeeks(8)).medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT).build();
        var aktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(
            opprinneligMødrekvote, uttakAktivitet).medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medErSøktGradering(false)
            .medArbeidsprosent(BigDecimal.TEN)
            .medTrekkdager(new Trekkdager(
                new SimpleLocalDateInterval(opprinneligMødrekvote.getFom(), opprinneligMødrekvote.getTom()).antallArbeidsdager()))
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .build();
        opprinneligFpff.leggTilAktivitet(aktivitet1);
        opprinneligMødrekvote.leggTilAktivitet(aktivitet2);

        var revurderingSøknadsperiodeFellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(7), fødselsdato.plusWeeks(12))
            .build();

        var revurdering = uttakRevurderingTestUtil.opprettRevurdering(UttakRevurderingTestUtil.AKTØR_ID_MOR,
            BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, List.of(opprinneligFpff, opprinneligMødrekvote),
            new OppgittFordelingEntitet(List.of(revurderingSøknadsperiodeFellesperiode), true));

        var fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        var stønadskontoberegning = new Stønadskontoberegning.Builder().medRegelEvaluering("sdawd")
            .medRegelInput("sdawd")
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(100).medStønadskontoType(StønadskontoType.MØDREKVOTE).build())
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(100).medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).build())
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(100).medStønadskontoType(StønadskontoType.FELLESPERIODE).build())
            .build();

        fagsakRelasjonRepository.opprettRelasjon(revurdering.getFagsak());
        fagsakRelasjonRepository.oppdaterDekningsgrad(revurdering.getFagsak(), Dekningsgrad._100);
        fagsakRelasjonRepository.lagre(revurdering.getFagsak(), stønadskontoberegning);

        var endringsdato = revurderingSøknadsperiodeFellesperiode.getFom();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(
            LocalDate.of(2018, 1, 1)).medOpprinneligEndringsdato(endringsdato).build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
            .medAvklarteDatoer(avklarteUttakDatoer);

        ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilFrilans();
        var input = lagInput(revurdering, beregningsandelTjeneste, fødselsdato);
        var resultatRevurdering = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        assertThat(resultatRevurdering.getPerioder()).hasSize(3);
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter()).hasSize(1);
        assertThat(resultatRevurdering.getPerioder().get(1).getAktiviteter()).hasSize(1);
        assertThat(resultatRevurdering.getPerioder().get(2).getAktiviteter()).hasSize(1);
        assertThat(resultatRevurdering.getPerioder().get(0).getFom()).isEqualTo(opprinneligFpff.getFom());
        assertThat(resultatRevurdering.getPerioder().get(0).getTom()).isEqualTo(opprinneligFpff.getTom());
        assertThat(resultatRevurdering.getPerioder().get(1).getFom()).isEqualTo(opprinneligMødrekvote.getFom());
        assertThat(resultatRevurdering.getPerioder().get(1).getTom()).isEqualTo(
            revurderingSøknadsperiodeFellesperiode.getFom().minusDays(1));
        assertThat(resultatRevurdering.getPerioder().get(2).getFom()).isEqualTo(
            revurderingSøknadsperiodeFellesperiode.getFom());
        assertThat(resultatRevurdering.getPerioder().get(2).getTom()).isEqualTo(
            revurderingSøknadsperiodeFellesperiode.getTom());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(
            opprinneligFpff.getAktiviteter().get(0).getTrekkdager());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getUttakAktivitet()).isEqualTo(
            opprinneligFpff.getAktiviteter().get(0).getUttakAktivitet());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(
            opprinneligFpff.getAktiviteter().get(0).getArbeidsprosent());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(
            opprinneligFpff.getAktiviteter().get(0).getTrekkonto());
        assertThat(resultatRevurdering.getPerioder().get(1).getAktiviteter().get(0).getTrekkdager()).isEqualTo(
            new Trekkdager(new SimpleLocalDateInterval(opprinneligMødrekvote.getFom(),
                revurderingSøknadsperiodeFellesperiode.getFom().minusDays(1)).antallArbeidsdager()));
        assertThat(resultatRevurdering.getPerioder().get(1).getAktiviteter().get(0).getTrekkonto()).isEqualTo(
            opprinneligMødrekvote.getAktiviteter().get(0).getTrekkonto());
        assertThat(resultatRevurdering.getPerioder().get(2).getAktiviteter().get(0).getTrekkdager()).isEqualTo(
            new Trekkdager(new SimpleLocalDateInterval(revurderingSøknadsperiodeFellesperiode.getFom(),
                revurderingSøknadsperiodeFellesperiode.getTom()).antallArbeidsdager()));
        assertThat(resultatRevurdering.getPerioder().get(2).getAktiviteter().get(0).getTrekkonto()).isEqualTo(UttakPeriodeType.FELLESPERIODE);

    }

    @Test
    void skalLageUttaksresultatMedOverføringperiode() {
        var fødselsdato = LocalDate.of(2019, 10, 10);
        var fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(1))
            .medÅrsak(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER)
            .build();
        var behandling = setupMor(fedrekvote, virksomhet(), fødselsdato.minusYears(1), fødselsdato.plusYears(2));
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        var fkPeriode = finnPeriode(uttakResultatPerioder, fedrekvote.getFom(),
            fedrekvote.getTom());

        assertThat(fkPeriode.getOverføringÅrsak()).isEqualTo(fedrekvote.getÅrsak());
    }

    @Test
    void arbeidstaker_uten_arbeidsgiver_skal_fastsettes() {
        var fødselsdato = LocalDate.of(2019, 10, 10);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();
        var behandling = setupMor(mødrekvote, null, fødselsdato.minusYears(1), fødselsdato.plusYears(2));
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(null, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        var resultatPeriode = finnPeriode(uttakResultatPerioder, mødrekvote.getFom(), mødrekvote.getTom());

        assertThat(resultatPeriode.isInnvilget()).isTrue();
    }

    /**
     * Ligger søknader fra tidligere i prod med samtidig uttak og 0%
     */
    @Test
    void samtidigUttakPå0ProsentSkalTolkesSom100Prosent() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var mk = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(7))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.ZERO)
            .build();
        var periodeMedSamtidigUttak = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(7).plusDays(1), fødselsdato.plusWeeks(8))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.ZERO)
            .build();
        var behandling = setupMor(List.of(mk, periodeMedSamtidigUttak), virksomhet(), fødselsdato.minusYears(1),
            fødselsdato.plusWeeks(10));
        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        var fkPeriode = finnPeriode(uttakResultatPerioder, periodeMedSamtidigUttak.getFom(),
            periodeMedSamtidigUttak.getTom());

        assertThat(fkPeriode.getSamtidigUttaksprosent()).isEqualTo(new SamtidigUttaksprosent(100));
        assertThat(fkPeriode.getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(5));
    }

    @Test
    void utsettelse_pga_arbeid_under_100_stilling_skal_gå_til_manuelt() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var mk = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(7))
            .build();
        var utsettelse = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fødselsdato.plusWeeks(7).plusDays(1), fødselsdato.plusWeeks(8))
            .build();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(mk, utsettelse), true));
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());

        var aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.minusYears(1), utsettelse.getTom()))
            .medProsentsats(BigDecimal.valueOf(50))
            .medSisteLønnsendringsdato(fødselsdato);
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.minusYears(1), utsettelse.getTom()));
        var arbeidsgiver = virksomhet();
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);

        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(scenario.getAktørId())
                .leggTilYrkesaktivitet(yrkesaktivitet));

        var behandling = lagre(scenario);

        lagreUttaksgrunnlag(behandling, fødselsdato);

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato, true);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());

        var uttakResultatPerioder = resultat.getPerioder();

        var utsettelseResultat = finnPeriode(uttakResultatPerioder, utsettelse.getFom(),
            utsettelse.getTom());

        assertThat(utsettelseResultat.getUtsettelseType()).isEqualTo(UttakUtsettelseType.ARBEID);
        assertThat(utsettelseResultat.getManuellBehandlingÅrsak()).isEqualTo(ManuellBehandlingÅrsak.IKKE_HELTIDSARBEID);
        assertThat(utsettelseResultat.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void tilkommet_i_løpet_av_aktivitet_skal_arve_saldo() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var arbeidsgiver1 = virksomhet(new Orgnummer("123"));
        var arbeidsgiver2 = virksomhet(new Orgnummer("456"));
        var arbeidsgiver3 = virksomhet(new Orgnummer("789"));

        var mk1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();
        var gradering1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsprosent(BigDecimal.valueOf(50))
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .build();
        var gradering2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver2)
            .medArbeidsprosent(BigDecimal.valueOf(50))
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(14).minusDays(1))
            .build();
        var mk2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(14), fødselsdato.plusWeeks(100).minusDays(1))
            .build();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(mk1, gradering1, gradering2, mk2), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(scenario.getAktørId())
                .leggTilYrkesaktivitet(lagYrkesaktivitet(arbeidsgiver1, fødselsdato.minusYears(1)))
                .leggTilYrkesaktivitet(lagYrkesaktivitet(arbeidsgiver2, gradering2.getFom()))
                .leggTilYrkesaktivitet(lagYrkesaktivitet(arbeidsgiver3, mk2.getFom())));

        var behandling = lagre(scenario);

        lagreUttaksgrunnlag(behandling, fødselsdato);

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver1, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver2, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver3, null);
        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);

        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer()).getPerioder();

        var førsteMødrekvote = finnPeriode(resultat, mk1.getFom(), mk1.getTom());
        var førsteGradering = finnPeriode(resultat, gradering1.getFom(), gradering1.getTom());
        var andreGradering = finnPeriode(resultat, gradering2.getFom(), gradering2.getTom());
        var mødrekvoteAlleHarDager = finnPeriode(resultat, mk2.getFom(), mk2.getFom().plusWeeks(3).minusDays(1));
        var mødrekvoteArbeidsgiver1TomForDager = finnPeriode(resultat, mk2.getFom().plusWeeks(3),
            mk2.getFom().plusWeeks(5).minusDays(1));
        var mødrekvoteAlleTomForDager = finnPeriode(resultat, mk2.getFom().plusWeeks(5), mk2.getTom());

        assertThat(resultat).hasSize(6);
        assertThat(førsteMødrekvote.getAktiviteter()).hasSize(1);
        assertThat(førsteGradering.getAktiviteter()).hasSize(1);
        assertThat(andreGradering.getAktiviteter()).hasSize(2);
        assertThat(mødrekvoteAlleHarDager.getAktiviteter()).hasSize(3);
        assertThat(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter()).hasSize(3);
        assertThat(mødrekvoteAlleTomForDager.getAktiviteter()).hasSize(3);

        assertThat(aktivitetForArbeidsgiver(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter(),
            arbeidsgiver1).getTrekkdager()).isEqualTo(Trekkdager.ZERO);
        assertThat(aktivitetForArbeidsgiver(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter(),
            arbeidsgiver2).getTrekkdager()).isEqualTo(new Trekkdager(10));
        assertThat(aktivitetForArbeidsgiver(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter(),
            arbeidsgiver3).getTrekkdager()).isEqualTo(new Trekkdager(10));

        assertThat(mødrekvoteAlleTomForDager.isInnvilget()).isFalse();
        assertThat(mødrekvoteAlleTomForDager.getResultatÅrsak()).isEqualTo(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN);
    }

    private YrkesaktivitetBuilder lagYrkesaktivitet(Arbeidsgiver arbeidsgiver, LocalDate fom) {
        var aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, LocalDate.MAX))
            .medProsentsats(BigDecimal.valueOf(50))
            .medSisteLønnsendringsdato(fom);
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, LocalDate.MAX));
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);
    }

    @Test
    void stillingsprosentPå0SkalGi100prosentUtbetalingOg0Arbeidstidsprosent() {

        var fødselsdato = LocalDate.of(2018, 10, 1);

        var arbeidsgiver = virksomhet();

        var oppgittPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(8).minusDays(10))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var oppgittPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(oppgittPeriode1.getTom().plusDays(1), fødselsdato.plusWeeks(8))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        var oppgittFordeling = new OppgittFordelingEntitet(
            List.of(oppgittPeriode1, oppgittPeriode2), true);
        scenario.medFordeling(oppgittFordeling);
        var aktivitetsAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(100))
            .medSisteLønnsendringsdato(fødselsdato)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), oppgittPeriode1.getTom()));
        var aktivitetsAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medSisteLønnsendringsdato(fødselsdato)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode2.getFom(), LocalDate.of(2020, 1, 1)));
        var ansettelesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode1.getFom().minusYears(1),
                oppgittPeriode2.getTom().plusWeeks(1)));
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelesperiode);

        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet));

        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        var mødrekvote = finnPeriode(uttakResultatPerioder, oppgittPeriode2.getFom(),
            oppgittPeriode2.getTom());
        assertThat(mødrekvote.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(mødrekvote.getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100.00));
    }

    @Test
    void stillingsprosentPåNullSkalGi100prosentUtbetalingOg0Arbeidstidsprosent() {

        var fødselsdato = LocalDate.of(2018, 10, 1);

        var arbeidsgiver = virksomhet();

        var oppgittPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(8).minusDays(10))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var oppgittPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(oppgittPeriode1.getTom().plusDays(1), fødselsdato.plusWeeks(8))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        var oppgittFordeling = new OppgittFordelingEntitet(
            List.of(oppgittPeriode1, oppgittPeriode2), true);
        scenario.medFordeling(oppgittFordeling);
        var aktivitetsAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(100))
            .medSisteLønnsendringsdato(fødselsdato)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), oppgittPeriode1.getTom()));
        var aktivitetsAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medSisteLønnsendringsdato(fødselsdato)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode2.getFom(), LocalDate.of(2020, 1, 1)));
        var ansettelesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode1.getFom().minusYears(1),
                oppgittPeriode2.getTom().plusWeeks(1)));
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelesperiode);

        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet));

        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        var beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        var resultat = fastsettePerioderRegelAdapter.fastsettePerioder(input, lagStønadskontoer());
        var uttakResultatPerioder = resultat.getPerioder();

        var mødrekvote = finnPeriode(uttakResultatPerioder, oppgittPeriode2.getFom(),
            oppgittPeriode2.getTom());
        assertThat(mødrekvote.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(mødrekvote.getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100.00));
    }

    private static UttakResultatPeriodeEntitet finnPeriode(List<UttakResultatPeriodeEntitet> perioder,
                                                           LocalDate fom,
                                                           LocalDate tom) {
        for (var uttakResultatPeriode : perioder) {
            if (uttakResultatPeriode.getFom().equals(fom) && uttakResultatPeriode.getTom().equals(tom)) {
                return uttakResultatPeriode;
            }
        }
        throw new AssertionError(
            "Fant ikke uttakresultatperiode med fom " + fom + " tom " + tom + " blant " + perioder);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);
    }

    private LocalDate mandag(LocalDate dato) {
        return dato.minusDays(dato.getDayOfWeek().getValue() - 1);
    }

    private Stønadskontoberegning lagStønadskontoer() {
        var mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(75)
            .build();
        var fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(80)
            .build();
        var fedrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(75)
            .build();
        var foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(maxDagerForeldrepengerFørFødsel)
            .build();

        return Stønadskontoberegning.builder()
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fedrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(foreldrepengerFørFødsel)
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .build();
    }

    private void lagreUttaksperiodegrense(Long behandlingId) {
        var grense = new Uttaksperiodegrense(mottattDato);
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandlingId, grense);
    }

    private Behandling setupMor(OppgittPeriodeEntitet oppgittPeriode,
                                Arbeidsgiver arbeidsgiver,
                                LocalDate arbeidFom,
                                LocalDate arbeidTom) {
        return setupMor(List.of(oppgittPeriode), arbeidsgiver, arbeidFom, arbeidTom);
    }

    private Behandling setupMor(List<OppgittPeriodeEntitet> oppgittPerioder,
                                Arbeidsgiver arbeidsgiver,
                                LocalDate arbeidFom,
                                LocalDate arbeidTom) {
        return setupFødsel(oppgittPerioder, arbeidsgiver, arbeidFom, arbeidTom,
            ScenarioMorSøkerForeldrepenger.forFødsel(), null);
    }

    private Behandling setupFar(OppgittPeriodeEntitet oppgittPeriode,
                                Arbeidsgiver arbeidsgiver,
                                LocalDate arbeidFom,
                                LocalDate arbeidTom) {
        return setupFødsel(List.of(oppgittPeriode), arbeidsgiver, arbeidFom, arbeidTom,
            ScenarioFarSøkerForeldrepenger.forFødsel(), null);
    }

    private Behandling setupFar(OppgittPeriodeEntitet oppgittPeriode,
                                Arbeidsgiver arbeidsgiver,
                                LocalDate arbeidFom,
                                LocalDate arbeidTom, Boolean overstyrtOmsorg) {
        return setupFødsel(List.of(oppgittPeriode), arbeidsgiver, arbeidFom, arbeidTom,
            ScenarioFarSøkerForeldrepenger.forFødsel(), overstyrtOmsorg);
    }

    private Behandling setupFødsel(List<OppgittPeriodeEntitet> oppgittPerioder,
                                   Arbeidsgiver arbeidsgiver,
                                   LocalDate arbeidFom,
                                   LocalDate arbeidTom,
                                   AbstractTestScenario<?> scenario, Boolean overstyrtOmsorg) {
        return setup(oppgittPerioder, arbeidsgiver, arbeidFom, arbeidTom, scenario, BigDecimal.valueOf(100), overstyrtOmsorg);
    }

    private Behandling setup(List<OppgittPeriodeEntitet> oppgittPerioder,
                             Arbeidsgiver arbeidsgiver,
                             LocalDate arbeidFom,
                             LocalDate arbeidTom,
                             AbstractTestScenario<?> scenario, BigDecimal stillingsprosent, Boolean overstyrtOmsorg) {

        scenario.medFordeling(new OppgittFordelingEntitet(oppgittPerioder, true));
        scenario.medOverstyrtOmsorg(overstyrtOmsorg);
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());

        var aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidFom, arbeidTom))
            .medProsentsats(stillingsprosent)
            .medSisteLønnsendringsdato(arbeidFom);
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidFom, arbeidTom));
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);

        scenario.getInntektArbeidYtelseScenarioTestBuilder()
            .getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(scenario.getAktørId())
                .leggTilYrkesaktivitet(yrkesaktivitet));

        var behandling = lagre(scenario);

        var førsteUttaksdato = oppgittPerioder.stream()
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .orElseThrow()
            .getFom();
        lagreUttaksgrunnlag(behandling, førsteUttaksdato);

        return behandling;
    }

    private void lagreUttaksgrunnlag(Behandling behandling, LocalDate endringsdato) {
        lagreUttaksperiodegrense(behandling.getId());
        lagreEndringsdato(behandling, endringsdato);
    }

    private void lagreEndringsdato(Behandling behandling, LocalDate endringsdato) {
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medJustertEndringsdato(endringsdato)
            .build();

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medAvklarteDatoer(avklarteDatoer);

        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }
}
