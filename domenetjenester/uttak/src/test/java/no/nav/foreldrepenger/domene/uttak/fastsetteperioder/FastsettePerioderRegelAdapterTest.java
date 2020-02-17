package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

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
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
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
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.Manuellbehandlingårsak;
import no.nav.foreldrepenger.regler.uttak.konfig.Konfigurasjon;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.IntervalUtils;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FastsettePerioderRegelAdapterTest {

    private final LocalDate fødselsdato = LocalDate.of(2018, 6, 22);
    private final LocalDate mottattDato = LocalDate.of(2018, 6, 22);

    private final Konfigurasjon konfigurasjon = StandardKonfigurasjon.KONFIGURASJON;
    private final int virkedagar_i_ei_veke = 5;
    private final int uker_før_fødsel_fellesperiode_grense = konfigurasjon.getParameter(Parametertype.LOVLIG_UTTAK_FØR_FØDSEL_UKER, fødselsdato);
    private final int uker_før_fødsel_foreldrepenger_grense = konfigurasjon.getParameter(Parametertype.UTTAK_FELLESPERIODE_FØR_FØDSEL_UKER, fødselsdato);
    private final int veker_etter_fødsel_mødrekvote_grense = konfigurasjon.getParameter(Parametertype.UTTAK_MØDREKVOTE_ETTER_FØDSEL_UKER, fødselsdato);
    private final int maxDagerMødrekvote = 75;
    private final int maxDagerForeldrepengerFørFødsel = 15;
    private final int maxDagerFedrekvote = 75;
    private final int maxDagerFellesperiode = 80;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private FastsettePerioderRegelResultatKonverterer fastsettePerioderRegelResultatKonverterer;

    @Inject
    private FastsettePerioderRegelGrunnlagBygger grunnlagBygger;

    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());
    private LocalDate førsteLovligeUttaksdato = mottattDato.withDayOfMonth(1).minusMonths(3);

    private AktivitetIdentifikator arbeidsforhold = AktivitetIdentifikator.forArbeid("1234", "123");
    private AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private FastsettePerioderRegelAdapter fastsettePerioderRegelAdapter;

    private static UttakResultatPeriodeEntitet finnPeriode(List<UttakResultatPeriodeEntitet> perioder, LocalDate fom, LocalDate tom) {
        for (UttakResultatPeriodeEntitet uttakResultatPeriode : perioder) {
            if (uttakResultatPeriode.getFom().equals(fom) && uttakResultatPeriode.getTom().equals(tom)) {
                return uttakResultatPeriode;
            }
        }
        throw new AssertionError("Fant ikke uttakresultatperiode med fom " + fom + " tom " + tom + " blant " + perioder);
    }

    @Before
    public void setUp() {
        this.fastsettePerioderRegelAdapter = new FastsettePerioderRegelAdapter(grunnlagBygger, fastsettePerioderRegelResultatKonverterer);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    @Test
    public void skalReturnerePlanMedMødrekvotePeriode() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, 6, 22);
        OppgittPeriodeEntitet fpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        BigDecimal arbeidsprosent = BigDecimal.valueOf(50.0);
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(5))
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .medArbeidsprosent(arbeidsprosent)
            .build();
        Behandling behandling = setupMor(List.of(fpff, mødrekvote), virksomhet, fpff.getFom(), fpff.getTom().plusYears(5));
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(3);

        UttakResultatPeriodeEntitet fpffPeriode = finnPeriode(uttakResultatPerioder, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        UttakResultatPeriodeEntitet mkPeriode = finnPeriode(uttakResultatPerioder, fødselsdato, fødselsdato.plusWeeks(5));
        UttakResultatPeriodeEntitet manglendeSøktPeriode = finnPeriode(uttakResultatPerioder, mkPeriode.getTom().plusDays(3),
            fødselsdato.plusWeeks(6).minusDays(1));

        // manglene søkt periode
        assertThat(fpffPeriode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(fpffPeriode.getAktiviteter()).hasSize(1);
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(15));
        // mødrekvote innvilget
        assertThat(mkPeriode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mkPeriode.getAktiviteter()).hasSize(1);
        assertThat(mkPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
        assertThat(mkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(new IntervalUtils(fødselsdato, fødselsdato.plusWeeks(5)).antallArbeidsdager()));
        // manglene søkt periode.. automatisk avslag og trekk dager
        assertThat(manglendeSøktPeriode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(manglendeSøktPeriode.getAktiviteter()).hasSize(1);
        assertThat(manglendeSøktPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
        assertThat(manglendeSøktPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(new IntervalUtils(fødselsdato.plusWeeks(5).plusDays(1),
            fødselsdato.plusWeeks(6).minusDays(1)).antallArbeidsdager()));
    }

    private UttakResultatPerioderEntitet fastsettePerioder(UttakInput input, FastsettePerioderRegelAdapter adapter) {
        return adapter.fastsettePerioder(input);
    }

    @Test
    public void skalReturnerePlanMedHeleForeldrepengerFørFødselPeriode() {
        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense), fødselsdato.minusDays(1))
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        Behandling behandling = setupMor(oppgittPeriode, virksomhet, fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense),
            fødselsdato.minusDays(1));
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(2);

        List<UttakResultatPeriodeEntitet> foreldrepengerUttakPerioder = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter().get(0).getTrekkonto().getKode()
                .equals(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode()))
            .collect(Collectors.toList());
        assertThat(foreldrepengerUttakPerioder).hasSize(1);

        UttakResultatPeriodeEntitet foreldrePengerUttakPeriode = foreldrepengerUttakPerioder.get(0);
        assertThat(foreldrePengerUttakPeriode.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(foreldrePengerUttakPeriode.getTom()).isEqualTo(fødselsdato.minusDays(1));

        int gjenståendeDager_foreldrepenger = uker_før_fødsel_foreldrepenger_grense * virkedagar_i_ei_veke - maxDagerForeldrepengerFørFødsel;
        assertThat(maxDagerForeldrepengerFørFødsel - foreldrePengerUttakPeriode.getAktiviteter().get(0).getTrekkdager().decimalValue().intValue())
            .isEqualTo(gjenståendeDager_foreldrepenger);

        UttakResultatPeriodeEntitet fpffPeriode = finnPeriode(uttakResultatPerioder, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        UttakResultatPeriodeEntitet mangledeSøktmkPeriode = finnPeriode(uttakResultatPerioder, fødselsdato,
            fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1));

        assertThat(mangledeSøktmkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(veker_etter_fødsel_mødrekvote_grense * 5));
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(15));
    }

    private UttakInput lagInput(Behandling behandling, UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste, LocalDate fødselsdato) {
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        return lagInput(behandling, beregningsandelTjeneste, false,
            new FamilieHendelser().medBekreftetHendelse(familieHendelse));
    }

    private UttakInput lagInput(Behandling behandling,
                                UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste,
                                boolean tapendeBehandling,
                                FamilieHendelser familieHendelser) {
        var ref = BehandlingReferanse.fra(behandling, førsteLovligeUttaksdato);
        var originalBehandling = behandling.getOriginalBehandling().isPresent() ? new OriginalBehandling(behandling.getOriginalBehandling().get().getId(), null) : null;
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medErTapendeBehandling(tapendeBehandling)
            .medFamilieHendelser(familieHendelser)
            .medOriginalBehandling(originalBehandling);
        return new UttakInput(ref, iayTjeneste.hentGrunnlag(behandling.getId()), ytelsespesifiktGrunnlag)
            .medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser())
            .medSøknadMottattDato(familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato().minusWeeks(4));
    }

    @Test
    public void skalReturnereManuellBehandlingForPlanMedForTidligOppstartAvFedrekvote() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDato = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusWeeks(2);
        LocalDate forventetKnekk = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense);
        LocalDate sluttDato = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).plusWeeks(2);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(startDato, sluttDato)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        Behandling behandling = setupFar(oppgittPeriode, virksomhet, startDato, sluttDato);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(2);

        UttakResultatPeriodeEntitet periode1 = uttakResultatPerioder.get(0);
        assertThat(periode1.getFom()).isEqualTo(startDato);
        assertThat(periode1.getTom()).isEqualTo(forventetKnekk.minusDays(1));
        assertThat(periode1.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode1.getManuellBehandlingÅrsak().getKode()).isEqualTo(String.valueOf(Manuellbehandlingårsak.UGYLDIG_STØNADSKONTO.getId()));

        UttakResultatPeriodeEntitet periode2 = uttakResultatPerioder.get(1);
        assertThat(periode2.getFom()).isEqualTo(forventetKnekk);
        assertThat(periode2.getTom()).isEqualTo(sluttDato);
        assertThat(periode2.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }

    @Test
    public void skalReturnerePlanMedManuelleFellesperiodeFørFødselNårSøkerErFar() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(5);
        LocalDate sluttDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(2);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(startDatoFellesperiode, sluttDatoFellesperiode)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        Behandling behandling = setupFar(fellesperiode, virksomhet, startDatoFellesperiode, sluttDatoFellesperiode);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        Stream<UttakResultatPeriodeEntitet> manuelleFellesPerioderStream = uttakResultatPerioder.stream().filter(
            uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter().get(0).getTrekkonto().getKode().equals(UttakPeriodeType.FELLESPERIODE.getKode()) &&
                uttakResultatPeriode.getPeriodeResultatType().equals(PeriodeResultatType.MANUELL_BEHANDLING));
        List<UttakResultatPeriodeEntitet> manuelleFellesPerioder = manuelleFellesPerioderStream.collect(Collectors.toList());
        assertThat(manuelleFellesPerioder).hasSize(1);

        UttakResultatPeriodeEntitet manuellFellesPeriode = manuelleFellesPerioder.get(0);
        assertThat(manuellFellesPeriode.getFom()).isEqualTo(startDatoFellesperiode);
        assertThat(manuellFellesPeriode.getTom()).isEqualTo(sluttDatoFellesperiode);
    }

    @Test
    public void morSøkerFellesperiodeFørFødselMedOppholdFørForeldrepenger() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(5);
        LocalDate sluttDatoFellesperiode = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(2);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(startDatoFellesperiode, sluttDatoFellesperiode)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        Behandling behandling = setupMor(fellesperiode, virksomhet, startDatoFellesperiode, sluttDatoFellesperiode);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet tidligUttakFP = finnPeriode(uttakResultatPerioder, startDatoFellesperiode, sluttDatoFellesperiode);
        UttakResultatPeriodeEntitet manglendeSøktPeriodeFP = finnPeriode(uttakResultatPerioder, sluttDatoFellesperiode.plusDays(3),
            fødselsdato.minusWeeks(3).minusDays(1));
        UttakResultatPeriodeEntitet manglendeSøktPeriodeFPFF = finnPeriode(uttakResultatPerioder, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        UttakResultatPeriodeEntitet manglendeSøktPeriodeMK = finnPeriode(uttakResultatPerioder, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(tidligUttakFP.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(tidligUttakFP.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(manglendeSøktPeriodeFP.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(manglendeSøktPeriodeFPFF.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(manglendeSøktPeriodeMK.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
        assertThat(manglendeSøktPeriodeMK.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
    }

    @Test
    public void morSøkerFellesperiodeFørFødselMedOppholdInniPerioden() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDatoFellesperiode1 = fødselsdato.minusWeeks(uker_før_fødsel_fellesperiode_grense);
        LocalDate sluttDatoFellesperiode1 = fødselsdato.minusWeeks(uker_før_fødsel_fellesperiode_grense).plusWeeks(2);

        LocalDate startDatoFellesperiode2 = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusWeeks(3);
        LocalDate sluttDatoFellesperiode2 = fødselsdato.minusWeeks(uker_før_fødsel_foreldrepenger_grense).minusDays(1);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet fellesperiode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(virksomhet)
            .medPeriode(startDatoFellesperiode1, sluttDatoFellesperiode1)
            .medSamtidigUttak(false)
            .build();

        OppgittPeriodeEntitet fellesperiode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(virksomhet)
            .medPeriode(startDatoFellesperiode2, sluttDatoFellesperiode2)
            .medSamtidigUttak(false)
            .build();

        Behandling behandling = setupMor(List.of(fellesperiode1, fellesperiode2), virksomhet,
            startDatoFellesperiode1, sluttDatoFellesperiode2);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(5);

        UttakResultatPeriodeEntitet tidligUttakFP1 = finnPeriode(uttakResultatPerioder, startDatoFellesperiode1, sluttDatoFellesperiode1);
        UttakResultatPeriodeEntitet manglendeSøktPeriodeFP = finnPeriode(uttakResultatPerioder, sluttDatoFellesperiode1.plusDays(3),
            startDatoFellesperiode2.minusDays(1));
        UttakResultatPeriodeEntitet tidligUttakFP2 = finnPeriode(uttakResultatPerioder, startDatoFellesperiode2, sluttDatoFellesperiode2);
        UttakResultatPeriodeEntitet manglendeSøktPeriodeFPFF = finnPeriode(uttakResultatPerioder, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        UttakResultatPeriodeEntitet manglendeSøktPeriodeMK = finnPeriode(uttakResultatPerioder, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(tidligUttakFP1.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(tidligUttakFP1.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        assertThat(manglendeSøktPeriodeFP.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(manglendeSøktPeriodeFP.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);

        assertThat(tidligUttakFP2.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(tidligUttakFP2.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(manglendeSøktPeriodeFPFF.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(manglendeSøktPeriodeFPFF.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(manglendeSøktPeriodeMK.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
        assertThat(manglendeSøktPeriodeMK.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
    }

    @Test
    public void søknadMedOppholdForAnnenForelderFellesperiodeOgIkkeNokDagerPåKontoTest() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDatoMødrekvote = fødselsdato;
        LocalDate sluttDatoMødrekvote = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1);

        LocalDate startDatoOpphold = sluttDatoMødrekvote.plusDays(1);
        LocalDate sluttDatoOpphold = sluttDatoMødrekvote.plusWeeks(5);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();

        OppgittPeriodeEntitet førFødsel = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(startDatoMødrekvote.minusWeeks(3), startDatoMødrekvote.minusDays(1))
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        OppgittPeriodeEntitet mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(startDatoMødrekvote, sluttDatoMødrekvote)
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        // Angitt periode for annen forelder
        OppgittPeriodeEntitet opphold = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.ANNET)
            .medÅrsak(KVOTE_FELLESPERIODE_ANNEN_FORELDER)
            .medPeriode(startDatoOpphold, sluttDatoOpphold)
            .build();

        OppgittPeriodeEntitet fellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(sluttDatoOpphold.plusDays(1), sluttDatoOpphold.plusWeeks(16))
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        Behandling behandling = setupMor(List.of(førFødsel, mødrekvote, opphold, fellesperiode), virksomhet, førFødsel.getFom(), fellesperiode.getTom());
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(5);

        UttakResultatPeriodeEntitet periode = uttakResultatPerioder.get(0);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        periode = uttakResultatPerioder.get(1);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        // Oppholdsperioder for annen forelder skal returneres som en uttakResultatPeriode uten aktivitet
        periode = uttakResultatPerioder.get(2);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(periode.getOppholdÅrsak()).isEqualTo(KVOTE_FELLESPERIODE_ANNEN_FORELDER);
        assertThat(periode.getFom()).isEqualTo(startDatoOpphold);
        assertThat(periode.getTom()).isEqualTo(sluttDatoOpphold);

        // Periode knukket pga ikke nok dager igjen på konto.
        periode = uttakResultatPerioder.get(3);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(periode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(periode.getTom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(16));

        periode = uttakResultatPerioder.get(4);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode.getFom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(16).plusDays(1));
        assertThat(periode.getTom()).isEqualTo(fellesperiode.getTom());

    }

    @Test
    public void søknadMedMerEnnTilgjengeligDagerForOpphold() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDatoMødrekvote = fødselsdato;
        LocalDate sluttDatoMødrekvote = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1);

        LocalDate startDatoOpphold = sluttDatoMødrekvote.plusDays(1);
        LocalDate sluttDatoOpphold = sluttDatoMødrekvote.plusWeeks(16);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();

        OppgittPeriodeEntitet førFødsel = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(startDatoMødrekvote.minusWeeks(3), startDatoMødrekvote.minusDays(1))
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        OppgittPeriodeEntitet mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(startDatoMødrekvote, sluttDatoMødrekvote)
            .medSamtidigUttak(false)
            .medArbeidsgiver(virksomhet)
            .build();

        // Angitt periode for annen forelder
        OppgittPeriodeEntitet opphold = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.ANNET)
            .medÅrsak(FEDREKVOTE_ANNEN_FORELDER)
            .medPeriode(startDatoOpphold, sluttDatoOpphold)
            .build();

        Behandling behandling = setupMor(List.of(førFødsel, mødrekvote, opphold), virksomhet, førFødsel.getFom(), sluttDatoOpphold);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(4);

        UttakResultatPeriodeEntitet periode = uttakResultatPerioder.get(0);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        periode = uttakResultatPerioder.get(1);
        assertThat(periode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);

        // Oppholdsperioder for annen forelder skal returneres som en uttakResultatPeriode uten aktivitet
        periode = uttakResultatPerioder.get(2);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(periode.getPeriodeResultatÅrsak()).isEqualTo(PeriodeResultatÅrsak.UKJENT);
        assertThat(periode.getOppholdÅrsak()).isEqualTo(FEDREKVOTE_ANNEN_FORELDER);
        assertThat(periode.getFom()).isEqualTo(startDatoOpphold);
        assertThat(periode.getTom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(15));

        // Periode knukket pga ikke nok dager igjen på konto.
        periode = uttakResultatPerioder.get(3);
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(periode.getFom()).isEqualTo(sluttDatoMødrekvote.plusWeeks(15).plusDays(1));
        assertThat(periode.getTom()).isEqualTo(sluttDatoOpphold);
    }

    @Test
    public void morSøkerMødrekvoteOgFedrekvote_FårInnvilgetMødrekvoteOgFedrekvoteGårTilManuellBehandling() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate startDatoMødrekvote = fødselsdato;
        LocalDate sluttDatoMødrekvote = fødselsdato.plusWeeks(veker_etter_fødsel_mødrekvote_grense).minusDays(1);

        LocalDate startDatoFedrekvote = sluttDatoMødrekvote.plusDays(1);
        LocalDate sluttDatoFedrekvote = startDatoFedrekvote.plusWeeks(5);

        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(startDatoMødrekvote, sluttDatoMødrekvote)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        OppgittPeriodeEntitet fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(startDatoFedrekvote, sluttDatoFedrekvote)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        Behandling behandling = setupMor(List.of(mødrekvote, fedrekvote), virksomhet, startDatoMødrekvote.minusYears(1), sluttDatoFedrekvote);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        // Act

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        // Assert
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        List<UttakResultatPeriodeEntitet> fedrekvotePerioder = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter().get(0).getTrekkonto().getKode().equals(StønadskontoType.FEDREKVOTE.getKode()))
            .collect(Collectors.toList());
        assertThat(fedrekvotePerioder).hasSize(1);
        assertThat(fedrekvotePerioder.get(0).getFom()).isEqualTo(startDatoFedrekvote);
        assertThat(fedrekvotePerioder.get(0).getTom()).isEqualTo(sluttDatoFedrekvote);

        assertThat(fedrekvotePerioder.get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.MANUELL_BEHANDLING);
        assertThat(fedrekvotePerioder.get(0).getPeriodeResultatÅrsak().getKode()).isEqualTo("4007");
        assertThat(fedrekvotePerioder.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FEDREKVOTE);

        List<UttakResultatPeriodeEntitet> mødrekvotePerioder = uttakResultatPerioder.stream()
            .filter(uttakResultatPeriode -> uttakResultatPeriode.getAktiviteter().get(0).getTrekkonto().getKode().equals(StønadskontoType.MØDREKVOTE.getKode()))
            .collect(Collectors.toList());
        assertThat(mødrekvotePerioder).hasSize(1);
        assertThat(mødrekvotePerioder.get(0).getFom()).isEqualTo(startDatoMødrekvote);
        assertThat(mødrekvotePerioder.get(0).getTom()).isEqualTo(sluttDatoMødrekvote);
        assertThat(mødrekvotePerioder.get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mødrekvotePerioder.get(0).getPeriodeResultatÅrsak().getKode()).isEqualTo("2003");
        assertThat(mødrekvotePerioder.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.MØDREKVOTE);
    }

    @Test
    public void morSkalFåAvslåttUtenTrekkdagerPåUtsettelsePgaSykdomIPeriodeFarTarUttak() {
        LocalDate fødselsdato = LocalDate.of(2018, 10, 10);
        OppgittPeriodeEntitet morFpffFørstegangs = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        OppgittPeriodeEntitet morMødrekvoteFørstegangs = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .build();
        OppgittPeriodeEntitet morUtsettelseFørstegangs = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fødselsdato.plusWeeks(10).plusDays(1), fødselsdato.plusWeeks(12))
            .build();
        ScenarioMorSøkerForeldrepenger morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morScenario.medFordeling(new OppgittFordelingEntitet(List.of(morFpffFørstegangs, morMødrekvoteFørstegangs, morUtsettelseFørstegangs), true));
        morScenario.medBehandlingType(BehandlingType.REVURDERING);
        Behandling morBehandling = morScenario.lagre(repositoryProvider);

        lagreStønadskontoer(morBehandling);

        ScenarioFarSøkerForeldrepenger farScenario = ScenarioFarSøkerForeldrepenger.forFødsel();

        Behandling farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        UttakResultatPerioderEntitet farUttakresultat = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet farPeriode = new UttakResultatPeriodeEntitet.Builder(morUtsettelseFørstegangs.getFom(), morUtsettelseFørstegangs.getTom())
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(farPeriode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(BigDecimal.TEN))
            .medUtbetalingsprosent(BigDecimal.TEN)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        farUttakresultat.leggTilPeriode(farPeriode);
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(farBehandling.getId(), farUttakresultat);

        OppgittPeriodeEntitet morUtsettelseRevurdering = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(morUtsettelseFørstegangs.getFom(), morUtsettelseFørstegangs.getTom())
            .build();
        ScenarioMorSøkerForeldrepenger morScenarioRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morScenarioRevurdering.medFordeling(new OppgittFordelingEntitet(List.of(morUtsettelseRevurdering), true));
        morScenarioRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morScenarioRevurdering.medOriginalBehandling(morBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        morScenarioRevurdering.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));

        Behandling morBehandlingRevurdering = morScenarioRevurdering.lagre(repositoryProvider);

        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(morFpffFørstegangs.getFom())
            .medOpprinneligEndringsdato(morUtsettelseRevurdering.getFom())
            .build();
        Long behandlingId = morBehandlingRevurdering.getId();
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandlingId, avklarteUttakDatoer);
        PerioderUttakDokumentasjonEntitet dokumentasjon = new PerioderUttakDokumentasjonEntitet();
        dokumentasjon.leggTil(
            new PeriodeUttakDokumentasjonEntitet(morUtsettelseRevurdering.getFom(), morUtsettelseRevurdering.getTom(), UttakDokumentasjonType.SYK_SØKER));
        OppgittPeriodeEntitet morUtsettelseRevurderingOverstyrt = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(morUtsettelseFørstegangs.getFom(), morUtsettelseFørstegangs.getTom())
            .build();
        repositoryProvider.getYtelsesFordelingRepository().lagreOverstyrtFordeling(behandlingId,
            new OppgittFordelingEntitet(List.of(morUtsettelseRevurderingOverstyrt), true),
            dokumentasjon);

        UttakBeregningsandelTjenesteTestUtil andelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        andelTjeneste.leggTilFrilans();

        lagreUttaksperiodegrense(morBehandlingRevurdering);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        var ref = BehandlingReferanse.fra(morBehandlingRevurdering, førsteLovligeUttaksdato);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medErTapendeBehandling(true)
            .medFamilieHendelser(familieHendelser)
            .medAnnenpart(new Annenpart(false, farBehandling.getId()))
            .medOriginalBehandling(new OriginalBehandling(morBehandling.getId(), null));
        var input = new UttakInput(ref, null, ytelsespesifiktGrunnlag)
            .medBeregningsgrunnlagStatuser(andelTjeneste.hentStatuser())
            .medSøknadMottattDato(familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato().minusWeeks(4));
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        assertThat(resultat.getPerioder()).hasSize(1);
        assertThat(resultat.getPerioder().get(0).getUtsettelseType()).isEqualTo(UttakUtsettelseType.SYKDOM_SKADE);
        assertThat(resultat.getPerioder().get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(resultat.getPerioder().get(0).getAktiviteter().get(0).getTrekkdager().decimalValue()).isZero();

    }

    @Test
    public void skal_avslå_periode_når_far_søker_fedrekvote_uten_å_ha_omsorg() {
        // Arrange
        Arbeidsgiver virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate fom = fødselsdato.plusWeeks(10);
        LocalDate tom = fødselsdato.plusWeeks(15).minusDays(1);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fom, tom)
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        PerioderUtenOmsorgEntitet perioderUtenOmsorg = new PerioderUtenOmsorgEntitet();
        perioderUtenOmsorg.leggTil(new PeriodeUtenOmsorgEntitet(fom, tom));

        Behandling behandling = setupFar(oppgittPeriode, virksomhet, fom, tom, perioderUtenOmsorg);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).hasSize(1);

        UttakResultatPeriodeEntitet periode = uttakResultatPerioder.iterator().next();
        assertThat(periode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(periode.getAktiviteter()).hasSize(1);
        var aktivitet = periode.getAktiviteter().get(0);
        assertThat(aktivitet.getTrekkdager()).isEqualTo(new Trekkdager(5 * 5));
        assertThat(aktivitet.getTrekkonto()).isEqualTo(StønadskontoType.FEDREKVOTE);
        assertThat(aktivitet.getUtbetalingsprosent()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void skal_ta_med_arbeidsforholdprosent_når_gradering_er_opplyst() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate start = fødselsdato.plusWeeks(20);
        LocalDate slutt = fødselsdato.plusWeeks(25).minusDays(1);
        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhet)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medErArbeidstaker(true)
            .medArbeidsprosent(arbeidsprosent).build();

        Behandling behandling = setupFar(oppgittPeriode, virksomhet, start, slutt);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        UttakResultatPeriodeEntitet fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FEDREKVOTE);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager((int) (5 * 5 * 0.4)));
        assertThat(fkPeriode.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
    }

    @Test
    public void skal_fastsette_perioder_ved_adopsjon_uten_annenpart() {
        var startdato = LocalDate.of(2019, 10, 3);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(startdato, startdato.plusWeeks(6).minusDays(1))
            .build();

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        Behandling behandling = setup(List.of(oppgittPeriode), virksomhet(), startdato.minusYears(2), startdato.plusYears(2), scenario, null, BigDecimal.valueOf(100));
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(startdato, List.of(new Barn()), 1, null, false);
        FamilieHendelser familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        var input = lagInput(behandling, beregningsandelTjeneste, false, familieHendelser);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();
        assertThat(uttakResultatPerioder).isNotEmpty();
    }

    @Test
    public void graderingSkalSettesRiktigEtterKjøringAvRegler() {
        BigDecimal arbeidsprosent = BigDecimal.valueOf(50);
        Arbeidsgiver virksomhetSomGradereresHos = virksomhet("orgnr1");
        Arbeidsgiver annenVirksomhet = virksomhet("orgnr2");
        LocalDate fødselsdato = LocalDate.of(2018, 10, 1);
        OppgittPeriodeEntitet oppgittFPFF = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        OppgittPeriodeEntitet oppgittMødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(oppgittFPFF.getTom().plusDays(1), oppgittFPFF.getTom().plusWeeks(6)).build();
        OppgittPeriodeEntitet oppgittGradertMødrekvote = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(virksomhetSomGradereresHos)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            //11 dager
            .medPeriode(oppgittMødrekvote.getTom().plusDays(1), oppgittMødrekvote.getTom().plusDays(1).plusWeeks(2))
            .medErArbeidstaker(true)
            .medArbeidsprosent(arbeidsprosent).build();
        AktørId aktørId = AktørId.dummy();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittFPFF, oppgittMødrekvote, oppgittGradertMødrekvote), true);
        scenario.medFordeling(oppgittFordeling);
        AktivitetsAvtaleBuilder aktivitetsAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(50))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        YrkesaktivitetBuilder yrkesaktivitetBuilder1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder ansettelsesperiode1 = yrkesaktivitetBuilder1.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        YrkesaktivitetBuilder yrkesaktivitet1 = yrkesaktivitetBuilder1
            .medArbeidsgiver(virksomhetSomGradereresHos)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1);
        AktivitetsAvtaleBuilder aktivitetsAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(50))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        YrkesaktivitetBuilder yrkesaktivitetBuilder2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder ansettelsesperiode2 = yrkesaktivitetBuilder2.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        YrkesaktivitetBuilder yrkesaktivitet2 = yrkesaktivitetBuilder2
            .medArbeidsgiver(annenVirksomhet)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2);
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet1))
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet2));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        Behandling behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhetSomGradereresHos, null);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(annenVirksomhet, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet mødrekvote = finnPeriode(uttakResultatPerioder, oppgittMødrekvote.getFom(), oppgittMødrekvote.getTom());
        UttakResultatPeriodeAktivitetEntitet aktivitetMørdrekvoteVirksomhet1 = aktivitetForArbeidsgiver(mødrekvote.getAktiviteter(),
            virksomhetSomGradereresHos);
        UttakResultatPeriodeAktivitetEntitet aktivitetMørdrekvoteVirksomhet2 = aktivitetForArbeidsgiver(mødrekvote.getAktiviteter(), annenVirksomhet);
        assertThat(mødrekvote.getPeriodeResultatÅrsak()).isInstanceOf(InnvilgetÅrsak.class);
        assertThat(mødrekvote.isGraderingInnvilget()).isFalse();
        assertThat(mødrekvote.getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.UKJENT);
        assertThat(aktivitetMørdrekvoteVirksomhet1.getTrekkdager()).isEqualTo(new Trekkdager(5 * 6));
        assertThat(aktivitetMørdrekvoteVirksomhet1.getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(aktivitetMørdrekvoteVirksomhet1.isGraderingInnvilget()).isFalse();
        assertThat(aktivitetMørdrekvoteVirksomhet2.getTrekkdager()).isEqualTo(new Trekkdager(5 * 6));
        assertThat(aktivitetMørdrekvoteVirksomhet2.getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(aktivitetMørdrekvoteVirksomhet2.isGraderingInnvilget()).isFalse();

        UttakResultatPeriodeEntitet gradertMødrekvote = finnPeriode(uttakResultatPerioder, oppgittGradertMødrekvote.getFom(),
            oppgittGradertMødrekvote.getTom());
        UttakResultatPeriodeAktivitetEntitet aktivitetGradertMørdrekvoteVirksomhet1 = aktivitetForArbeidsgiver(gradertMødrekvote.getAktiviteter(),
            virksomhetSomGradereresHos);
        UttakResultatPeriodeAktivitetEntitet aktivitetGradertMørdrekvoteVirksomhet2 = aktivitetForArbeidsgiver(gradertMødrekvote.getAktiviteter(),
            annenVirksomhet);
        assertThat(gradertMødrekvote.getPeriodeResultatÅrsak()).isInstanceOf(InnvilgetÅrsak.class);
        assertThat(gradertMødrekvote.isGraderingInnvilget()).isTrue();
        assertThat(gradertMødrekvote.getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.UKJENT);
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.getTrekkdager()).isEqualTo(new Trekkdager(new BigDecimal(5.5)));
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.isGraderingInnvilget()).isTrue();
        assertThat(aktivitetGradertMørdrekvoteVirksomhet1.isSøktGradering()).isTrue();
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.getTrekkdager()).isEqualTo(new Trekkdager(11));
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.isGraderingInnvilget()).isFalse();
        assertThat(aktivitetGradertMørdrekvoteVirksomhet2.isSøktGradering()).isFalse();
    }

    @Test
    public void skalGi100UtbetalingIPeriodenHvisUtenforAnsettelsesperiode() {
        Arbeidsgiver virksomhet = virksomhet();
        LocalDate fødselsdato = LocalDate.of(2018, 10, 1);
        OppgittPeriodeEntitet oppgittFPFF = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .build();
        OppgittPeriodeEntitet oppgittMødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(oppgittFPFF.getTom().plusDays(1), oppgittFPFF.getTom().plusWeeks(6)).build();
        AktørId aktørId = AktørId.dummy();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittFPFF, oppgittMødrekvote), true);
        scenario.medFordeling(oppgittFordeling);
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(100))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), LocalDate.of(2020, 1, 1)));
        YrkesaktivitetBuilder yrkesaktivitetBuilder1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());

        // Ansettelsesperiode slutter før første uttaksdag
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder1.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), oppgittFPFF.getFom().minusDays(1)));
        YrkesaktivitetBuilder yrkesaktivitet1 = yrkesaktivitetBuilder1
            .medArbeidsgiver(virksomhet)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet1));

        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        Behandling behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet mødrekvote = finnPeriode(uttakResultatPerioder, oppgittMødrekvote.getFom(), oppgittMødrekvote.getTom());
        UttakResultatPeriodeAktivitetEntitet aktivitetMørdrekvoteVirksomhet = aktivitetForArbeidsgiver(mødrekvote.getAktiviteter(), virksomhet);
        assertThat(aktivitetMørdrekvoteVirksomhet.getUtbetalingsprosent()).isEqualTo(new BigDecimal("100.00"));
    }

    private UttakResultatPeriodeAktivitetEntitet aktivitetForArbeidsgiver(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter, Arbeidsgiver arbeidsgiver) {
        return aktiviteter.stream().filter(aktivitet -> {
            Optional<Arbeidsgiver> arbeidsgiverUttak = aktivitet.getUttakAktivitet().getArbeidsgiver();
            if (arbeidsgiverUttak.isPresent()) {
                return Objects.equals(arbeidsgiver, arbeidsgiverUttak.get());
            }
            return arbeidsgiver == null;
        }).findFirst().get();
    }

    @Test
    public void graderingSkalSettesRiktigVedAvslagAvGraderingEtterKjøringAvRegler() {
        // Søker gradert fpff slik at gradering avslås
        BigDecimal arbeidsprosent = BigDecimal.valueOf(50);
        Arbeidsgiver virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        OppgittPeriodeEntitet oppgittGradertFPFF = OppgittPeriodeBuilder.ny()
            .medArbeidsprosent(arbeidsprosent)
            .medErArbeidstaker(true)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medArbeidsgiver(virksomhet)
            .build();

        Behandling behandling = setupMor(oppgittGradertFPFF, virksomhet, oppgittGradertFPFF.getFom(), oppgittGradertFPFF.getTom());
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet mødrekvote = finnPeriode(uttakResultatPerioder, oppgittGradertFPFF.getFom(), oppgittGradertFPFF.getTom());
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

    private Arbeidsgiver virksomhet(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    @Test
    public void skal_ta_med_arbeidsforholdprosent_når_gradering_er_opplyst_også_når_periode_avviker_med_lørdag_søndag() {

        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate start = mandag(fødselsdato.plusWeeks(20));
        LocalDate slutt = start.plusWeeks(5).minusDays(1);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slutt.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        Behandling behandling = setupFar(oppgittPeriode, virksomhet, start, slutt);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        UttakResultatPeriodeEntitet fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FEDREKVOTE);
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager((int) (5 * 5 * 0.4)));
        assertThat(fkPeriode.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
    }

    @Test
    public void utbetalingsprosentSkalHa2Desimaler() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        LocalDate start = mandag(fødselsdato.plusWeeks(20));
        LocalDate slutt = start.plusWeeks(5).minusDays(1);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slutt.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        BigDecimal arbeidsprosent = BigDecimal.valueOf(53);
        Arbeidsgiver virksomhet = virksomhet();
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .medArbeidsprosent(arbeidsprosent)
            .build();

        Behandling behandling = setupFar(oppgittPeriode, virksomhet, start, slutt);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        UttakResultatPeriodeEntitet fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        // Utbetalingsgrad (i %) = (stillingsprosent – arbeidsprosent) x 100 / stillingsprosent
        assertThat(fkPeriode.getAktiviteter().get(0).getUtbetalingsprosent()).isEqualTo(new BigDecimal("47.00"));
    }

    @Test
    public void samtidigUttaksprosentSkalHa2Desimaler() {
        var fødselsdato = LocalDate.of(2019, 6, 22);
        LocalDate start = mandag(fødselsdato.plusWeeks(20));
        LocalDate slutt = start.plusWeeks(5).minusDays(1);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slutt.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        BigDecimal samtidigUttaksprosent = BigDecimal.valueOf(53.33);
        Arbeidsgiver arbeidsgiver = virksomhet();
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(start, slutt)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(samtidigUttaksprosent)
            .build();

        Behandling behandling = setupFar(oppgittPeriode, arbeidsgiver, start, slutt, null);
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(1);

        UttakResultatPeriodeEntitet fkPeriode = finnPeriode(uttakResultatPerioder, start, slutt);
        assertThat(fkPeriode.getSamtidigUttaksprosent()).isEqualTo(new BigDecimal("53.33"));
    }

    @Test
    public void skal_håndtere_manuell_behandling_av_for_tidlig_gradering() {
        Arbeidsgiver virksomhet = virksomhet();
        var fødselsdato = LocalDate.of(2018, 6, 22);
        OppgittPeriodeEntitet fpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medArbeidsgiver(virksomhet)
            .medSamtidigUttak(false)
            .build();

        OppgittPeriodeEntitet mk = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .medArbeidsprosent(BigDecimal.valueOf(50)).build();

        Behandling behandling = setupMor(List.of(fpff, mk), virksomhet, fpff.getFom(), fødselsdato.plusYears(5));
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);


        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        assertThat(uttakResultatPerioder).hasSize(2);

        UttakResultatPeriodeEntitet fpffPeriode = uttakResultatPerioder.stream()
            .filter(p -> p.getAktiviteter().get(0).getTrekkonto().equals(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)).findFirst().get();
        UttakResultatPeriodeEntitet mkPeriode = uttakResultatPerioder.stream()
            .filter(p -> p.getAktiviteter().get(0).getTrekkonto().equals(StønadskontoType.MØDREKVOTE)).findFirst().get();

        // Innvilget FPFF
        assertThat(fpffPeriode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(fpffPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(15));
        // Innvilget mødrekvote, men avslag på gradering.
        assertThat(mkPeriode.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
        assertThat(mkPeriode.getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.GRADERING_FØR_UKE_7);
        assertThat(mkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(new IntervalUtils(fødselsdato,
            fødselsdato.plusWeeks(6).minusDays(1)).antallArbeidsdager()));
    }

    @Test
    public void skalPrependeUttaksresultatPerioderFørEndringsdatoVedRevurdering() {
        // Lager opprinnelig uttak

        UttakRevurderingTestUtil uttakRevurderingTestUtil = new UttakRevurderingTestUtil(repositoryProvider, iayTjeneste);
        UttakResultatPeriodeEntitet opprinneligFpff = new UttakResultatPeriodeEntitet.Builder(fødselsdato.minusWeeks(3).minusDays(1), fødselsdato.minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build();
        UttakResultatPeriodeAktivitetEntitet aktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligFpff, uttakAktivitet)
            .medTrekkonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medErSøktGradering(false)
            .medArbeidsprosent(BigDecimal.TEN)
            .medTrekkdager(new Trekkdager(new IntervalUtils(opprinneligFpff.getFom(), opprinneligFpff.getTom()).antallArbeidsdager()))
            .medUtbetalingsprosent(BigDecimal.TEN)
            .build();
        UttakResultatPeriodeEntitet opprinneligMødrekvote = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusWeeks(8))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet aktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligMødrekvote, uttakAktivitet)
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medErSøktGradering(false)
            .medArbeidsprosent(BigDecimal.TEN)
            .medTrekkdager(new Trekkdager(new IntervalUtils(opprinneligMødrekvote.getFom(), opprinneligMødrekvote.getTom()).antallArbeidsdager()))
            .medUtbetalingsprosent(BigDecimal.TEN)
            .build();
        opprinneligFpff.leggTilAktivitet(aktivitet1);
        opprinneligMødrekvote.leggTilAktivitet(aktivitet2);

        OppgittPeriodeEntitet revurderingSøknadsperiodeFellesperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(7), fødselsdato.plusWeeks(12))
            .build();

        Behandling revurdering = uttakRevurderingTestUtil.opprettRevurdering(UttakRevurderingTestUtil.AKTØR_ID_MOR, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER,
            List.of(opprinneligFpff, opprinneligMødrekvote),
            new OppgittFordelingEntitet(List.of(revurderingSøknadsperiodeFellesperiode),
                true), OppgittDekningsgradEntitet.bruk100());

        FagsakRelasjonRepository fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        fagsakRelasjonRepository.opprettEllerOppdaterRelasjon(revurdering.getFagsak(),
            Optional.ofNullable(fagsakRelasjonRepository.finnRelasjonFor(revurdering.getFagsak())),
            Dekningsgrad._100);
        fagsakRelasjonRepository.lagre(revurdering.getFagsak(), revurdering.getId(), new Stønadskontoberegning.Builder()
            .medRegelEvaluering("sdawd")
            .medRegelInput("sdawd")
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(100).medStønadskontoType(StønadskontoType.MØDREKVOTE).build())
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(100).medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).build())
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(100).medStønadskontoType(StønadskontoType.FELLESPERIODE).build())
            .build());

        LocalDate endringsdato = revurderingSøknadsperiodeFellesperiode.getFom();
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(LocalDate.of(2018, 1, 1))
            .medOpprinneligEndringsdato(endringsdato)
            .build();
        repositoryProvider.getYtelsesFordelingRepository().lagre(revurdering.getId(), avklarteUttakDatoer);

        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilFrilans();
        var input = lagInput(revurdering, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultatRevurdering = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        assertThat(resultatRevurdering.getPerioder()).hasSize(3);
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter()).hasSize(1);
        assertThat(resultatRevurdering.getPerioder().get(1).getAktiviteter()).hasSize(1);
        assertThat(resultatRevurdering.getPerioder().get(2).getAktiviteter()).hasSize(1);
        assertThat(resultatRevurdering.getPerioder().get(0).getFom()).isEqualTo(opprinneligFpff.getFom());
        assertThat(resultatRevurdering.getPerioder().get(0).getTom()).isEqualTo(opprinneligFpff.getTom());
        assertThat(resultatRevurdering.getPerioder().get(1).getFom()).isEqualTo(opprinneligMødrekvote.getFom());
        assertThat(resultatRevurdering.getPerioder().get(1).getTom()).isEqualTo(revurderingSøknadsperiodeFellesperiode.getFom().minusDays(1));
        assertThat(resultatRevurdering.getPerioder().get(2).getFom()).isEqualTo(revurderingSøknadsperiodeFellesperiode.getFom());
        assertThat(resultatRevurdering.getPerioder().get(2).getTom()).isEqualTo(revurderingSøknadsperiodeFellesperiode.getTom());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getTrekkdager())
            .isEqualTo(opprinneligFpff.getAktiviteter().get(0).getTrekkdager());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getUttakAktivitet())
            .isEqualTo(opprinneligFpff.getAktiviteter().get(0).getUttakAktivitet());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getArbeidsprosent())
            .isEqualTo(opprinneligFpff.getAktiviteter().get(0).getArbeidsprosent());
        assertThat(resultatRevurdering.getPerioder().get(0).getAktiviteter().get(0).getTrekkonto())
            .isEqualTo(opprinneligFpff.getAktiviteter().get(0).getTrekkonto());
        assertThat(resultatRevurdering.getPerioder().get(1).getAktiviteter().get(0).getTrekkdager())
            .isEqualTo(new Trekkdager(new IntervalUtils(opprinneligMødrekvote.getFom(), revurderingSøknadsperiodeFellesperiode.getFom().minusDays(1)).antallArbeidsdager()));
        assertThat(resultatRevurdering.getPerioder().get(1).getAktiviteter().get(0).getTrekkonto())
            .isEqualTo(opprinneligMødrekvote.getAktiviteter().get(0).getTrekkonto());
        assertThat(resultatRevurdering.getPerioder().get(2).getAktiviteter().get(0).getTrekkdager())
            .isEqualTo(new Trekkdager(new IntervalUtils(revurderingSøknadsperiodeFellesperiode.getFom(), revurderingSøknadsperiodeFellesperiode.getTom()).antallArbeidsdager()));
        assertThat(resultatRevurdering.getPerioder().get(2).getAktiviteter().get(0).getTrekkonto()).isEqualTo(StønadskontoType.FELLESPERIODE);

    }

    @Test
    public void skalLageUttaksresultatMedOverføringperiode() {
        var fødselsdato = LocalDate.of(2019, 10, 10);
        OppgittPeriodeEntitet fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(1))
            .medÅrsak(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER)
            .build();
        Behandling behandling = setupMor(fedrekvote, virksomhet(), fødselsdato.minusYears(1), fødselsdato.plusYears(2));
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet fkPeriode = finnPeriode(uttakResultatPerioder, fedrekvote.getFom(), fedrekvote.getTom());

        assertThat(fkPeriode.getOverføringÅrsak()).isEqualTo(fedrekvote.getÅrsak());
    }

    /**
     * Ligger søknader fra tidligere i prod med samtidig uttak og 0%
     */
    @Test
    public void samtidigUttakPå0ProsentSkalTolkesSom100Prosent() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        OppgittPeriodeEntitet mk = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(7))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(BigDecimal.ZERO)
            .build();
        OppgittPeriodeEntitet periodeMedSamtidigUttak = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(7).plusDays(1), fødselsdato.plusWeeks(8))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(BigDecimal.ZERO)
            .build();
        Behandling behandling = setupMor(List.of(mk, periodeMedSamtidigUttak), virksomhet(), fødselsdato.minusYears(1), fødselsdato.plusWeeks(10));
        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet(), null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet fkPeriode = finnPeriode(uttakResultatPerioder, periodeMedSamtidigUttak.getFom(), periodeMedSamtidigUttak.getTom());

        assertThat(fkPeriode.getSamtidigUttaksprosent()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(fkPeriode.getAktiviteter().get(0).getUtbetalingsprosent()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(fkPeriode.getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(5));
    }

    @Test
    public void utsettelse_pga_arbeid_under_100_stilling_skal_gå_til_manuelt() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        OppgittPeriodeEntitet mk = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(7))
            .build();
        OppgittPeriodeEntitet utsettelse = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fødselsdato.plusWeeks(7).plusDays(1), fødselsdato.plusWeeks(8))
            .build();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(mk, utsettelse), true));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));

        AktivitetsAvtaleBuilder aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.minusYears(1), utsettelse.getTom()))
            .medProsentsats(BigDecimal.valueOf(50))
            .medSisteLønnsendringsdato(fødselsdato);
        AktivitetsAvtaleBuilder ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.minusYears(1), utsettelse.getTom()));
        var arbeidsgiver = virksomhet();
        YrkesaktivitetBuilder yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(scenario.getDefaultBrukerAktørId())
                .leggTilYrkesaktivitet(yrkesaktivitet));

        Behandling behandling = lagre(scenario);

        lagreUttaksgrunnlag(behandling, fødselsdato);

        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);

        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet utsettelseResultat = finnPeriode(uttakResultatPerioder, utsettelse.getFom(), utsettelse.getTom());

        assertThat(utsettelseResultat.getUtsettelseType()).isEqualTo(UttakUtsettelseType.ARBEID);
        assertThat(utsettelseResultat.getManuellBehandlingÅrsak()).isEqualTo(ManuellBehandlingÅrsak.IKKE_HELTIDSARBEID);
        assertThat(utsettelseResultat.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    public void tilkommet_i_løpet_av_aktivitet_skal_arve_saldo() {
        var fødselsdato = LocalDate.of(2018, 6, 22);
        var arbeidsgiver1 = virksomhet("123");
        var arbeidsgiver2 = virksomhet("456");
        var arbeidsgiver3 = virksomhet("789");

        var mk1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .build();
        var gradering1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medErArbeidstaker(true)
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsprosent(BigDecimal.valueOf(50))
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .build();
        var gradering2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medErArbeidstaker(true)
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
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(scenario.getDefaultBrukerAktørId())
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

        var resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter).getPerioder();

        var førsteMødrekvote = finnPeriode(resultat, mk1.getFom(), mk1.getTom());
        var førsteGradering = finnPeriode(resultat, gradering1.getFom(), gradering1.getTom());
        var andreGradering = finnPeriode(resultat, gradering2.getFom(), gradering2.getTom());
        var mødrekvoteAlleHarDager = finnPeriode(resultat, mk2.getFom(), mk2.getFom().plusWeeks(3).minusDays(1));
        var mødrekvoteArbeidsgiver1TomForDager = finnPeriode(resultat, mk2.getFom().plusWeeks(3), mk2.getFom().plusWeeks(5).minusDays(1));
        var mødrekvoteAlleTomForDager = finnPeriode(resultat, mk2.getFom().plusWeeks(5), mk2.getTom());

        assertThat(resultat).hasSize(6);
        assertThat(førsteMødrekvote.getAktiviteter()).hasSize(1);
        assertThat(førsteGradering.getAktiviteter()).hasSize(1);
        assertThat(andreGradering.getAktiviteter()).hasSize(2);
        assertThat(mødrekvoteAlleHarDager.getAktiviteter()).hasSize(3);
        assertThat(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter()).hasSize(3);
        assertThat(mødrekvoteAlleTomForDager.getAktiviteter()).hasSize(3);

        assertThat(aktivitetForArbeidsgiver(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter(), arbeidsgiver1).getTrekkdager()).isEqualTo(Trekkdager.ZERO);
        assertThat(aktivitetForArbeidsgiver(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter(), arbeidsgiver2).getTrekkdager()).isEqualTo(new Trekkdager(10));
        assertThat(aktivitetForArbeidsgiver(mødrekvoteArbeidsgiver1TomForDager.getAktiviteter(), arbeidsgiver3).getTrekkdager()).isEqualTo(new Trekkdager(10));

        assertThat(mødrekvoteAlleTomForDager.isInnvilget()).isFalse();
        assertThat(mødrekvoteAlleTomForDager.getPeriodeResultatÅrsak()).isEqualTo(IkkeOppfyltÅrsak.IKKE_STØNADSDAGER_IGJEN);
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
    public void stillingsprosentPå0SkalGi100prosentUtbetalingOg0Arbeidstidsprosent() {

        LocalDate fødselsdato = LocalDate.of(2018, 10, 1);

        Arbeidsgiver arbeidsgiver = virksomhet();

        OppgittPeriodeEntitet oppgittPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(8).minusDays(10))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        OppgittPeriodeEntitet oppgittPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(oppgittPeriode1.getTom().plusDays(1), fødselsdato.plusWeeks(8))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        AktørId aktørId = AktørId.dummy();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittPeriode1, oppgittPeriode2), true);
        scenario.medFordeling(oppgittFordeling);
        AktivitetsAvtaleBuilder aktivitetsAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(100))
            .medAntallTimer(BigDecimal.TEN)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), oppgittPeriode1.getTom()));
        AktivitetsAvtaleBuilder aktivitetsAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medAntallTimer(BigDecimal.TEN)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode2.getFom(), LocalDate.of(2020, 1, 1)));
        AktivitetsAvtaleBuilder ansettelesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode1.getFom().minusYears(1), oppgittPeriode2.getTom().plusWeeks(1)));
        YrkesaktivitetBuilder yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelesperiode);

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet));

        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        Behandling behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet mødrekvote = finnPeriode(uttakResultatPerioder, oppgittPeriode2.getFom(), oppgittPeriode2.getTom());
        assertThat(mødrekvote.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(mødrekvote.getAktiviteter().get(0).getUtbetalingsprosent()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    public void stillingsprosentPåNullSkalGi100prosentUtbetalingOg0Arbeidstidsprosent() {

        LocalDate fødselsdato = LocalDate.of(2018, 10, 1);

        Arbeidsgiver arbeidsgiver = virksomhet();

        OppgittPeriodeEntitet oppgittPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(8).minusDays(10))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        OppgittPeriodeEntitet oppgittPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(oppgittPeriode1.getTom().plusDays(1), fødselsdato.plusWeeks(8))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        AktørId aktørId = AktørId.dummy();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(aktørId);
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(List.of(oppgittPeriode1, oppgittPeriode2), true);
        scenario.medFordeling(oppgittFordeling);
        AktivitetsAvtaleBuilder aktivitetsAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.valueOf(100))
            .medAntallTimer(BigDecimal.TEN)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2016, 1, 1), oppgittPeriode1.getTom()));
        AktivitetsAvtaleBuilder aktivitetsAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medAntallTimer(BigDecimal.ONE)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode2.getFom(), LocalDate.of(2020, 1, 1)));
        AktivitetsAvtaleBuilder ansettelesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittPeriode1.getFom().minusYears(1), oppgittPeriode2.getTom().plusWeeks(1)));
        YrkesaktivitetBuilder yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelesperiode);

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(aktørId)
                .leggTilYrkesaktivitet(yrkesaktivitet));

        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        Behandling behandling = lagre(scenario);
        lagreUttaksgrunnlag(behandling, fødselsdato);

        UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, null);

        var input = lagInput(behandling, beregningsandelTjeneste, fødselsdato);
        UttakResultatPerioderEntitet resultat = fastsettePerioder(input, fastsettePerioderRegelAdapter);
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = resultat.getPerioder();

        UttakResultatPeriodeEntitet mødrekvote = finnPeriode(uttakResultatPerioder, oppgittPeriode2.getFom(), oppgittPeriode2.getTom());
        assertThat(mødrekvote.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(BigDecimal.ZERO);
        assertThat(mødrekvote.getAktiviteter().get(0).getUtbetalingsprosent()).isEqualTo(new BigDecimal("100.00"));
    }

    private LocalDate mandag(LocalDate dato) {
        return dato.minusDays(dato.getDayOfWeek().getValue() - 1);
    }

    private void lagreStønadskontoer(Behandling behandling) {
        Stønadskonto mødrekvote = Stønadskonto.builder().medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(maxDagerMødrekvote).build();
        Stønadskonto fellesperiode = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(maxDagerFellesperiode).build();
        Stønadskonto fedrekvote = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(maxDagerFedrekvote).build();
        Stønadskonto foreldrepengerFørFødsel = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(maxDagerForeldrepengerFørFødsel).build();

        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fedrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(foreldrepengerFørFødsel)
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .build();
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private void lagreUttaksperiodegrense(Behandling behandling) {
        Uttaksperiodegrense grense = new Uttaksperiodegrense.Builder(behandling.getBehandlingsresultat())
            .medFørsteLovligeUttaksdag(førsteLovligeUttaksdato).medMottattDato(mottattDato).build();
        repositoryProvider.getUttakRepository().lagreUttaksperiodegrense(behandling.getId(), grense);
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
                                LocalDate arbeidTom,
                                PerioderUtenOmsorgEntitet perioderUtenOmsorg) {
        return setupFødsel(List.of(oppgittPeriode), arbeidsgiver, arbeidFom, arbeidTom,
            ScenarioFarSøkerForeldrepenger.forFødsel(), perioderUtenOmsorg);
    }

    private Behandling setupFødsel(List<OppgittPeriodeEntitet> oppgittPerioder,
                                   Arbeidsgiver arbeidsgiver,
                                   LocalDate arbeidFom,
                                   LocalDate arbeidTom,
                                   AbstractTestScenario<?> scenario,
                                   PerioderUtenOmsorgEntitet perioderUtenOmsorg) {
        return setup(oppgittPerioder, arbeidsgiver, arbeidFom, arbeidTom, scenario, perioderUtenOmsorg, BigDecimal.valueOf(100));
    }

    private Behandling setup(List<OppgittPeriodeEntitet> oppgittPerioder,
                             Arbeidsgiver arbeidsgiver,
                             LocalDate arbeidFom,
                             LocalDate arbeidTom,
                             AbstractTestScenario<?> scenario,
                             PerioderUtenOmsorgEntitet perioderUtenOmsorg,
                             BigDecimal stillingsprosent) {

        scenario.medFordeling(new OppgittFordelingEntitet(oppgittPerioder, true));
        scenario.medPerioderUtenOmsorg(perioderUtenOmsorg);
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));

        AktivitetsAvtaleBuilder aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidFom, arbeidTom))
            .medProsentsats(stillingsprosent)
            .medAntallTimer(BigDecimal.valueOf(20.4d))
            .medAntallTimerFulltid(BigDecimal.valueOf(10.2d));
        AktivitetsAvtaleBuilder ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidFom, arbeidTom));
        YrkesaktivitetBuilder yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd()
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(scenario.getDefaultBrukerAktørId())
                .leggTilYrkesaktivitet(yrkesaktivitet));

        Behandling behandling = lagre(scenario);

        var førsteUttaksdato = oppgittPerioder.stream().min(Comparator.comparing(OppgittPeriodeEntitet::getFom)).get().getFom();
        lagreUttaksgrunnlag(behandling, førsteUttaksdato);

        return behandling;
    }

    private void lagreUttaksgrunnlag(Behandling behandling, LocalDate endringsdato) {
        lagreUttaksperiodegrense(behandling);
        lagreStønadskontoer(behandling);
        lagreEndringsdato(behandling, endringsdato);
    }

    private void lagreEndringsdato(Behandling behandling, LocalDate endringsdato) {
        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medJustertEndringsdato(endringsdato)
            .build();
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), avklarteDatoer);
    }
}
