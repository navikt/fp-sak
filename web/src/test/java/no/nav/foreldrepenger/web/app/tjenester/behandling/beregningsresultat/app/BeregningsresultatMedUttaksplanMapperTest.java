package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeAndelDto;

@ExtendWith(MockitoExtension.class)
class BeregningsresultatMedUttaksplanMapperTest {

    private static final LocalDate P1_FOM = LocalDate.now();
    private static final LocalDate P1_TOM = LocalDate.now().plusDays(10);
    private static final LocalDate P2_FOM = LocalDate.now().plusDays(11);
    private static final LocalDate P2_TOM = LocalDate.now().plusDays(20);
    private static final LocalDate P3_FOM = LocalDate.now().plusDays(21);
    private static final LocalDate P3_TOM = LocalDate.now().plusDays(30);
    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjenesteMock;
    private BeregningsresultatMedUttaksplanMapper beregningsresultatMedUttaksplanMapper;

    @BeforeEach
    void before() {
        beregningsresultatMedUttaksplanMapper = new BeregningsresultatMedUttaksplanMapper(inntektArbeidYtelseTjenesteMock);
    }

    @Test
    void skalLageDto() {
        var behandling = lagBehandling(); // Behandling
        Behandlingsresultat.opprettFor(behandling);
        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat uten perioder

        var dto = beregningsresultatMedUttaksplanMapper.lagBeregningsresultatMedUttaksplan(BehandlingReferanse.fra(behandling),
                beregningsresultat, Optional.empty());

        assertThat(dto.perioder()).isEmpty();
    }

    @Test
    void skalLageEnPeriodePerBeregningsresultatPeriode() {
        var behandling = lagBehandling(); // Behandling
        Behandlingsresultat.opprettFor(behandling);
        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling).getBgBeregningsresultatFP(); // Beregingsresultat
                                                                                                                                // uten perioder
        var virksomhet = arbeidsgiver("123");
        var aktivitetEntitet = ordinærtArbeidsforholdUttakAktivitet(virksomhet, InternArbeidsforholdRef.nyRef());
        var uttakResultat = lagUttakPeriodeMedEnPeriode(Collections.singletonList(aktivitetEntitet)); // Uttaksplan med én periode som inneholder de
                                                                                                      // to beregningsresultatperiodene

        lagP1(beregningsresultat); // Legg til en periode

        var periodeDtoer = beregningsresultatMedUttaksplanMapper.lagPerioder(behandling.getId(), beregningsresultat,
                Optional.of(uttakResultat));

        assertThat(periodeDtoer).hasSize(1);

        lagP2(beregningsresultat); // Legg til en periode til

        periodeDtoer = beregningsresultatMedUttaksplanMapper.lagPerioder(behandling.getId(), beregningsresultat, Optional.of(uttakResultat));

        assertThat(periodeDtoer).hasSize(2);

        var p1 = periodeDtoer.get(0);
        assertThat(p1.getDagsats()).isZero();
        assertThat(p1.getFom()).isEqualTo(P1_FOM);
        assertThat(p1.getTom()).isEqualTo(P1_TOM);
        assertThat(p1.getAndeler()).isEmpty();

        var p2 = periodeDtoer.get(1);
        assertThat(p2.getDagsats()).isZero();
        assertThat(p2.getFom()).isEqualTo(P2_FOM);
        assertThat(p2.getTom()).isEqualTo(P2_TOM);
        assertThat(p2.getAndeler()).isEmpty();
    }

    @Test
    void skalLageEnPeriodePerBeregningsresultatPeriodeUtenUttakResultat() {
        // Arrange
        var behandling = lagBehandling(); // Behandling
        Behandlingsresultat.opprettFor(behandling);
        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat uten perioder

        var bgrPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Legg til en periode
        lagAndelTilSøker(bgrPeriode, 1000, arbeidsgiver("12345"));
        // Act
        var uttaksplan = beregningsresultatMedUttaksplanMapper.lagBeregningsresultatMedUttaksplan(BehandlingReferanse.fra(behandling),
                beregningsresultat, Optional.empty());

        // Assert
        var perioder = uttaksplan.perioder();

        assertThat(perioder).hasSize(1);

        var p1 = perioder.get(0);
        assertThat(p1.getDagsats()).isEqualTo(1000);
        assertThat(p1.getFom()).isEqualTo(P1_FOM);
        assertThat(p1.getTom()).isEqualTo(P1_TOM);

        assertThat(p1.getAndeler()).hasSize(1);
        var andel = p1.getAndeler()[0];
        assertThat(andel.getUttak()).isNotNull();
        assertThat(andel.getUttak().isGradering()).isFalse();

    }

    @Test
    void skalBeregneDagsatsPerPeriode() {
        var behandling = lagBehandling(); // Behandling
        var virksomhet = arbeidsgiver("1234");

        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling).getBgBeregningsresultatFP();
        var beregningsresultatPeriode1 = lagP1(beregningsresultat);
        lagAndelTilSøker(beregningsresultatPeriode1, 100, virksomhet);
        lagAndelTilArbeidsgiver(beregningsresultatPeriode1, virksomhet, 100);
        var beregningsresultatPeriode2 = lagP2(beregningsresultat);
        lagAndelTilArbeidsgiver(beregningsresultatPeriode2, virksomhet, 100);

        var dagsatsP1 = beregningsresultatPeriode1.getDagsats();
        var dagsatsP2 = beregningsresultatPeriode2.getDagsats();

        assertThat(dagsatsP1).isEqualTo(200);
        assertThat(dagsatsP2).isEqualTo(100);
    }

    private Arbeidsgiver arbeidsgiver(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    @Test
    void skalLageAndelerPerPeriodeEttArbeidsforhold() {
        // Arrange 1: Kun andel for søker
        var virksomhet = arbeidsgiver("1234");

        var behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakAktivitet1 = ordinærtArbeidsforholdUttakAktivitet(virksomhet, null);
        var uttakResultat = lagUttakPeriodeMedEnPeriode(Collections.singletonList(uttakAktivitet1));

        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        var beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler
        var arbeidsforholdId = uttakAktivitet1.getArbeidsforholdRef();
        lagAndelTilSøker(beregningsresultatPeriode, 100, uttakAktivitet1.getArbeidsgiver().get(),
                arbeidsforholdId); // Legg til en andel til søker

        var andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode,
                Optional.of(uttakResultat),
                Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(1);

        // Arrange 2: Andel for søker og arbeidsgiver
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet, 100, arbeidsforholdId); // Legg til en andel til arbeidsgiver

        andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode, Optional.of(uttakResultat), Collections.emptyMap(),
                Optional.empty());

        assertThat(andeler).hasSize(1);
        assertAndelArbeidsgiver(andeler, virksomhet.getIdentifikator(), 100);
    }

    @Test
    void skalLageAndelerPerPeriodeToArbeidsforhold() {
        var virksomhet1 = arbeidsgiver("1234");
        var virksomhet2 = arbeidsgiver("3456");

        var behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakAktivitet1 = ordinærtArbeidsforholdUttakAktivitet(virksomhet1, null);
        var uttakAktivitet2 = ordinærtArbeidsforholdUttakAktivitet(virksomhet2, InternArbeidsforholdRef.nyRef());
        var uttakResultat = lagUttakPeriodeMedEnPeriode(List.of(uttakAktivitet1, uttakAktivitet2));

        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        var beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler
        lagAndelTilSøker(beregningsresultatPeriode, 100, uttakAktivitet1.getArbeidsgiver().get(), uttakAktivitet1.getArbeidsforholdRef()); // Legg til
                                                                                                                                           // en andel
                                                                                                                                           // til
                                                                                                                                           // søker
        lagAndelTilSøker(beregningsresultatPeriode, 200, uttakAktivitet2.getArbeidsgiver().get(), uttakAktivitet2.getArbeidsforholdRef()); // Legg til
                                                                                                                                           // en andel
                                                                                                                                           // til
                                                                                                                                           // søker
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet1, 200, uttakAktivitet1.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet2, 100, uttakAktivitet2.getArbeidsforholdRef());

        var andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode,
                Optional.of(uttakResultat),
                Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(2);
        assertAndelArbeidsgiver(andeler, virksomhet1.getIdentifikator(), 200);
        assertAndelArbeidsgiver(andeler, virksomhet2.getIdentifikator(), 100);
    }

    @Test
    void skalLageAndelerForKombibasjonsstatuser() {
        // Arrange 1
        var behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakResultat = lagUttakPeriodeMedEnPeriode(List.of(
                new ForeldrepengerUttakAktivitet(UttakArbeidType.ANNET),
                new ForeldrepengerUttakAktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)));

        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        var beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler

        lagAndelTilSøkerMedAktivitetStatus(beregningsresultatPeriode, 1000, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        lagAndelTilSøkerMedAktivitetStatus(beregningsresultatPeriode, 2000, AktivitetStatus.DAGPENGER);

        var andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode,
                Optional.of(uttakResultat),
                Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(2);
        var andel1 = andeler.stream()
                .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)).findFirst().orElse(null);
        var andel2 = andeler.stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER)).findFirst()
                .orElse(null);
        assertThat(andel1.getTilSoker()).isEqualTo(1000);
        assertThat(andel2.getTilSoker()).isEqualTo(2000);
        assertThat(andel1.getRefusjon()).isZero();
        assertThat(andel2.getRefusjon()).isZero();
    }

    @Test
    void skalSlåSammenAndelerMedSammeArbeidsforholdId() {
        // Arrange
        var virksomhet = arbeidsgiver("1234");

        var behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var uttakAktivitet = ordinærtArbeidsforholdUttakAktivitet(virksomhet, arbeidsforholdRef);
        var uttakResultat = lagUttakPeriodeMedEnPeriode(Collections.singletonList(uttakAktivitet));

        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        var beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler

        lagAndelTilSøker(beregningsresultatPeriode, 500, virksomhet, uttakAktivitet.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode, 1000, virksomhet, uttakAktivitet.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet, 250, uttakAktivitet.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet, 500, uttakAktivitet.getArbeidsforholdRef());

        var andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode,
                Optional.of(uttakResultat),
                Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getTilSoker()).isEqualTo(1500);
        assertAndelArbeidsgiver(andeler, virksomhet.getIdentifikator(), 750);
    }

    @Test
    void skalFinneRiktigSisteUtbetalingsdato() {
        var virksomhet1 = arbeidsgiver("123");
        var virksomhet2 = arbeidsgiver("456");
        var virksomhet3 = arbeidsgiver("789");
        var behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakAktivitet1 = ordinærtArbeidsforholdUttakAktivitet(virksomhet1, InternArbeidsforholdRef.nyRef());
        var uttakAktivitet2 = ordinærtArbeidsforholdUttakAktivitet(virksomhet2, InternArbeidsforholdRef.nyRef());
        var uttakAktivitet3 = ordinærtArbeidsforholdUttakAktivitet(virksomhet3, InternArbeidsforholdRef.nyRef());
        var uttakResultat = lagUttakPeriodeMedEnPeriode(P1_FOM, P3_TOM, List.of(uttakAktivitet1, uttakAktivitet2, uttakAktivitet3));
        var beregningsresultat = lagBeregningsresultatAggregatFP(behandling);
        var beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP());
        var beregningsresultatPeriode2 = lagP2(beregningsresultat.getBgBeregningsresultatFP());
        var beregningsresultatPeriode3 = lagP3(beregningsresultat.getBgBeregningsresultatFP());

        lagAndelTilSøker(beregningsresultatPeriode, 500, uttakAktivitet1.getArbeidsgiver().get(), uttakAktivitet1.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode, 1000, uttakAktivitet2.getArbeidsgiver().get(), uttakAktivitet2.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode2, 0, uttakAktivitet1.getArbeidsgiver().get(), uttakAktivitet1.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode2, 1000, uttakAktivitet2.getArbeidsgiver().get(), uttakAktivitet2.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode, 300, uttakAktivitet3.getArbeidsgiver().get(), uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, uttakAktivitet3.getArbeidsgiver().get(), 250, uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode2, 0, uttakAktivitet3.getArbeidsgiver().get(), uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode2, uttakAktivitet3.getArbeidsgiver().get(), 250, uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode3, 0, uttakAktivitet3.getArbeidsgiver().get(), uttakAktivitet3.getArbeidsforholdRef());

        // Act
        var andeler = beregningsresultatMedUttaksplanMapper.lagPerioder(behandling.getId(),
                beregningsresultat.getBgBeregningsresultatFP(), Optional.of(uttakResultat));

        // Assert
        andeler.stream().flatMap(a -> Arrays.stream(a.getAndeler()))
                .filter(andel -> andel.getArbeidsgiverReferanse().equals(virksomhet1.getIdentifikator()))
                .forEach(andel1 -> assertThat(andel1.getSisteUtbetalingsdato()).isEqualTo(P1_TOM));
        andeler.stream().flatMap(a -> Arrays.stream(a.getAndeler()))
                .filter(andel -> andel.getArbeidsgiverReferanse().equals(virksomhet2.getIdentifikator()))
                .forEach(andel1 -> assertThat(andel1.getSisteUtbetalingsdato()).isEqualTo(P2_TOM));
        andeler.stream().flatMap(a -> Arrays.stream(a.getAndeler()))
                .filter(andel -> andel.getArbeidsgiverReferanse().equals(virksomhet3.getIdentifikator()))
                .forEach(andel1 -> assertThat(andel1.getSisteUtbetalingsdato()).isEqualTo(P2_TOM));
    }

    private static ForeldrepengerUttakAktivitet ordinærtArbeidsforholdUttakAktivitet(Arbeidsgiver virksomhet,
            InternArbeidsforholdRef arbeidsforholdRef) {
        return new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, virksomhet, arbeidsforholdRef);
    }

    private static void assertAndelArbeidsgiver(List<BeregningsresultatPeriodeAndelDto> andeler, String arbeidsgiver, int forventetRefusjon) {
        var andel = hentAndelForArbeidgiver(andeler, arbeidsgiver);
        assertThat(andel).as("arbeidsgiverAndel").hasValueSatisfying(a -> {
            assertThat(a.getArbeidsgiverReferanse()).as("arbeidsgiver").isEqualTo(arbeidsgiver);
            assertThat(a.getRefusjon()).as("refusjon").isEqualTo(forventetRefusjon);
        });
    }

    private static Optional<BeregningsresultatPeriodeAndelDto> hentAndelForArbeidgiver(List<BeregningsresultatPeriodeAndelDto> andeler,
            String arbeidsgiver) {
        return andeler.stream().filter(a -> a.getArbeidsgiverReferanse().equals(arbeidsgiver)).findFirst();
    }

    private static Behandling lagBehandling() {
        var søker = NavBruker.opprettNyNB(AKTØR_ID);
        var fagsak = FagsakBuilder.nyForeldrepengerForMor().medBruker(søker).build();
        var behandling = Behandling.forFørstegangssøknad(fagsak)
                .build();
        behandling.setId(1L);
        return behandling;
    }

    private static ForeldrepengerUttak lagUttakPeriodeMedEnPeriode(List<ForeldrepengerUttakAktivitet> uttakAktiviteter) {
        return lagUttakPeriodeMedEnPeriode(P1_FOM, P1_TOM, uttakAktiviteter);
    }

    private static ForeldrepengerUttak lagUttakPeriodeMedEnPeriode(LocalDate p1Fom, LocalDate p1Tom,
            List<ForeldrepengerUttakAktivitet> uttakAktiviteter) {
        var periodeAktiviteter = uttakAktiviteter.stream()
                .map(ua -> new ForeldrepengerUttakPeriodeAktivitet.Builder()
                        .medAktivitet(ua)
                        .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                        .medTrekkdager(new Trekkdager(20))
                        .medArbeidsprosent(BigDecimal.ZERO)
                        .build())
                .toList();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
                .medTidsperiode(p1Fom, p1Tom)
                .medResultatType(PeriodeResultatType.INNVILGET)
                .medAktiviteter(periodeAktiviteter)
                .build();
        return new ForeldrepengerUttak(List.of(uttakPeriode));
    }

    private static BehandlingBeregningsresultatEntitet lagBeregningsresultatAggregatFP(Behandling behandling) {
        var bgres = BeregningsresultatEntitet.builder()
                .medRegelInput("")
                .medRegelSporing("")
                .build();
        var builder = BehandlingBeregningsresultatBuilder.ny()
                .medBgBeregningsresultatFP(bgres);
        return builder.build(behandling.getId());
    }

    private static BeregningsresultatPeriode lagP1(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(P1_FOM, P1_TOM)
                .build(beregningsresultat);
    }

    private static BeregningsresultatPeriode lagP2(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(P2_FOM, P2_TOM)
                .build(beregningsresultat);
    }

    private static BeregningsresultatPeriode lagP3(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(P3_FOM, P3_TOM)
                .build(beregningsresultat);
    }

    private static BeregningsresultatAndel lagAndelTilArbeidsgiver(BeregningsresultatPeriode periode, Arbeidsgiver arbeidsgiver, int refusjon) {
        return lagAndelTilArbeidsgiver(periode, arbeidsgiver, refusjon, null);
    }

    private static BeregningsresultatAndel lagAndelTilArbeidsgiver(BeregningsresultatPeriode periode,
            Arbeidsgiver arbeidsgiver,
            int refusjon,
            InternArbeidsforholdRef arbeidsforholdId) {
        return BeregningsresultatAndel.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medDagsats(refusjon)
                .medArbeidsforholdRef(arbeidsforholdId)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medDagsatsFraBg(refusjon)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medBrukerErMottaker(false)
                .build(periode);
    }

    private static BeregningsresultatAndel lagAndelTilSøker(BeregningsresultatPeriode periode, int tilSøker, Arbeidsgiver virksomhet) {
        return lagAndelTilSøker(periode, tilSøker, virksomhet, null);
    }

    private static BeregningsresultatAndel lagAndelTilSøker(BeregningsresultatPeriode periode, int tilSøker, Arbeidsgiver arbeidsgiver,
            InternArbeidsforholdRef arbeidsforholdId) {
        return BeregningsresultatAndel.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medDagsats(tilSøker)
                .medArbeidsforholdRef(arbeidsforholdId)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medDagsatsFraBg(tilSøker)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medBrukerErMottaker(true)
                .build(periode);
    }

    private static BeregningsresultatAndel lagAndelTilSøkerMedAktivitetStatus(BeregningsresultatPeriode periode, int tilSøker,
            AktivitetStatus aktivitetStatus) {
        return BeregningsresultatAndel.builder()
                .medArbeidsgiver(null)
                .medDagsats(tilSøker)
                .medAktivitetStatus(aktivitetStatus)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medDagsatsFraBg(tilSøker)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medBrukerErMottaker(true)
                .build(periode);
    }
}
