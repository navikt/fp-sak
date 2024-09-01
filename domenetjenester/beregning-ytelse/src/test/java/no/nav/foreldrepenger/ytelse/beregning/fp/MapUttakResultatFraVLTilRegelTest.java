package no.nav.foreldrepenger.ytelse.beregning.fp;

import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.AVSLÅTT;
import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.INNVILGET;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultatPeriode;
import no.nav.vedtak.konfig.Tid;

class MapUttakResultatFraVLTilRegelTest {

    private static final String ARBEIDSFORHOLD_ORGNR = "000000000";
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID_2 = InternArbeidsforholdRef.namedRef("TEST-REF-2");
    private static final LocalDate TERMIN_DATO = LocalDate.now();
    private static final LocalDate TOM_FELLESPERIODE = TERMIN_DATO.plusWeeks(20).minusDays(1);
    private static final LocalDate TOM_MØDREKVOTE = TERMIN_DATO.plusWeeks(10).minusDays(1);
    private static final LocalDate TOM_FØR_FØDSEL = TERMIN_DATO.minusDays(1);
    private static final LocalDate FOM_FELLESPERIODE = TERMIN_DATO.plusWeeks(10);
    private static final LocalDate FOM_MØDREKVOTE = TERMIN_DATO;
    private static final LocalDate FOM_FØR_FØDSEL = TERMIN_DATO.minusWeeks(3);

    private List<ForeldrepengerUttakPeriode> vlPlan;
    private Behandling behandling;
    private MapUttakResultatFraVLTilRegel overriddenMapper;
    private MapUttakResultatFraVLTilRegel mapper;

    @BeforeEach
    public void setup() {
        var fagsak = FagsakBuilder.nyForeldrepengerForMor().build();
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Behandlingsresultat.opprettFor(behandling);
        vlPlan = lagUttakResultatPlan();
        mapper = new MapUttakResultatFraVLTilRegel();
        overriddenMapper = new MapUttakResultatFraVLTilRegel() {
            @Override
            protected BigDecimal finnStillingsprosent(UttakInput input, ForeldrepengerUttakPeriodeAktivitet uttakAktivitet, LocalDate periodeFom) {
                return BigDecimal.valueOf(50);
            }
        };
    }

    @Test
    void skalMappeUttakResultatPlan() {
        // Act
        var regelPlan = overriddenMapper.mapFra(vlPlan, lagRef(behandling));

        // Assert
        assertThat(regelPlan).isNotNull();
        var uttakResultatPerioder = regelPlan.uttakResultatPerioder();
        assertThat(uttakResultatPerioder).isNotNull();
        assertThat(uttakResultatPerioder).hasSize(3);

        var førFødselPeriode = getPeriodeByFom(uttakResultatPerioder, FOM_FØR_FØDSEL);
        var mødrePeriode = getPeriodeByFom(uttakResultatPerioder, FOM_MØDREKVOTE);
        var fellesPeriode = getPeriodeByFom(uttakResultatPerioder, FOM_FELLESPERIODE);

        assertPeriode(førFødselPeriode, FOM_FØR_FØDSEL, TOM_FØR_FØDSEL);
        assertPeriode(mødrePeriode, FOM_MØDREKVOTE, TOM_MØDREKVOTE);
        assertPeriode(fellesPeriode, FOM_FELLESPERIODE, TOM_FELLESPERIODE);
    }

    private UttakInput lagRef(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build(),
                null, null);
    }

    @Test
    void skalMappeUttakAktivitet() {
        // Arrange
        var prosentArbeid = BigDecimal.valueOf(10);
        var utbetalingsgrad = new Utbetalingsgrad(66); // overstyrt
        var uttakPlan = lagUttaksPeriode(prosentArbeid, utbetalingsgrad);
        // Act
        var resultat = overriddenMapper.mapFra(uttakPlan, lagRef(behandling));
        // Assert
        var resultPeriode = onlyOne(resultat);
        var uttakAktivitet = resultPeriode.uttakAktiviteter().get(0);
        assertThat(uttakAktivitet.utbetalingsgrad()).isEqualByComparingTo(utbetalingsgrad.decimalValue());
        assertThat(uttakAktivitet.arbeidstidsprosent()).isEqualByComparingTo(prosentArbeid);
        assertThat(uttakAktivitet.stillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(uttakAktivitet.aktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(uttakAktivitet.erGradering()).isTrue();
        assertThat(uttakAktivitet.arbeidsforhold().identifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet.arbeidsforhold().arbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID.getReferanse());
        assertThat(uttakAktivitet.arbeidsforhold().frilanser()).isFalse();
    }

    private ArbeidsforholdInformasjon lagArbeidsforholdInformasjon(Arbeidsgiver arbeidsgiver) {
        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var internArbeidsforholdId_1 = InternArbeidsforholdRef.ref(ARBEIDSFORHOLD_ID.getReferanse());
        var eksternArbeidsforholdId_1 = EksternArbeidsforholdRef.ref("ID1");
        var internArbeidsforholdId_2 = InternArbeidsforholdRef.ref(ARBEIDSFORHOLD_ID_2.getReferanse());
        var eksternArbeidsforholdId_2 = EksternArbeidsforholdRef.ref("ID2");
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsgiver, internArbeidsforholdId_1, eksternArbeidsforholdId_1);
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsgiver, internArbeidsforholdId_2, eksternArbeidsforholdId_2);

        return arbeidsforholdInformasjonBuilder.build();
    }

    private List<YrkesaktivitetBuilder> lagYrkesAkiviteter(Arbeidsgiver arbeidsgiver) {
        List<YrkesaktivitetBuilder> yrkesAktiviteter = new ArrayList<>();

        var fom = LocalDate.of(2015, 8, 1);
        var tom = Tid.TIDENES_ENDE;

        var aa1 = AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var aa1_2 = AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).medProsentsats(new BigDecimal(20));

        var aa2 = AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        var aa2_2 = AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).medProsentsats(new BigDecimal(60));

        var ya1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aa1)
            .leggTilAktivitetsAvtale(aa1_2)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID);
        var ya2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aa2)
            .leggTilAktivitetsAvtale(aa2_2)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID_2);

        yrkesAktiviteter.add(ya1);
        yrkesAktiviteter.add(ya2);
        return yrkesAktiviteter;

    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlag(List<YrkesaktivitetBuilder> yrkesaktivitetList,
                                                               AktørId aktørId,
                                                               ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        var aggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = aggregat.getAktørArbeidBuilder(aktørId);
        for (var yrkesaktivitet : yrkesaktivitetList) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet);
        }
        aggregat.leggTilAktørArbeid(aktørArbeidBuilder);

        var inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty());
        inntektArbeidYtelseGrunnlagBuilder.medInformasjon(arbeidsforholdInformasjon);
        inntektArbeidYtelseGrunnlagBuilder.medData(aggregat);
        return inntektArbeidYtelseGrunnlagBuilder;
    }

    private UttakInput lagRefMedIay(Behandling behandling, List<YrkesaktivitetBuilder> yrkesaktiviteter, InntektArbeidYtelseGrunnlag iayGrunnlag) {

        var bgStatuser = yrkesaktiviteter.stream().map((YrkesaktivitetBuilder yb) -> {
            var y = yb.build();
            var arbeidsforholdRef = y.getArbeidsforholdRef();
            var arbeidsgiver = y.getArbeidsgiver();
            return new BeregningsgrunnlagStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER,
                arbeidsgiver, arbeidsforholdRef);
        }).collect(Collectors.toSet());

        return new UttakInput(BehandlingReferanse.fra(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build(), iayGrunnlag, null).medBeregningsgrunnlagStatuser(bgStatuser);
    }

    @Test
    void skalMappeUttakAktivitetMedFlereArbeidforholdHosSammeArbeidtaker() {
        // Arrange
        var arbeidsgiver = Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR);

        var arbeidsforholdInformasjon = lagArbeidsforholdInformasjon(arbeidsgiver);

        var yrkesaktiviteter = lagYrkesAkiviteter(arbeidsgiver);
        var iayGrunnlag = opprettGrunnlag(yrkesaktiviteter, behandling.getAktørId(), arbeidsforholdInformasjon).build();

        var prosentArbeid = BigDecimal.valueOf(60);

        var prosentArbeidAndel1 = BigDecimal.valueOf(15);
        var prosentArbeidAndel2 = BigDecimal.valueOf(45);

        var utbetalingsgrad1 = new Utbetalingsgrad(15); // overstyrt
        var utbetalingsgrad2 = new Utbetalingsgrad(45); // overstyrt
        var uttakPlan = lagUttaksPeriodeMedMultipleAktiviteter(prosentArbeid, utbetalingsgrad1, utbetalingsgrad2);
        // Act
        var resultat = mapper.mapFra(uttakPlan, lagRefMedIay(behandling, yrkesaktiviteter, iayGrunnlag));
        // Assert
        var resultPeriode = onlyOne(resultat);
        var uttakAktivitet = resultPeriode.uttakAktiviteter().get(0);
        assertThat(uttakAktivitet.utbetalingsgrad()).isEqualByComparingTo(utbetalingsgrad1.decimalValue());
        assertThat(uttakAktivitet.arbeidstidsprosent()).isEqualByComparingTo(prosentArbeidAndel1);
        assertThat(uttakAktivitet.stillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(uttakAktivitet.aktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(uttakAktivitet.erGradering()).isTrue();
        assertThat(uttakAktivitet.arbeidsforhold().identifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet.arbeidsforhold().arbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID.getReferanse());
        assertThat(uttakAktivitet.arbeidsforhold().frilanser()).isFalse();

        var uttakAktivitet2 = resultPeriode.uttakAktiviteter().get(1);
        assertThat(uttakAktivitet2.utbetalingsgrad()).isEqualByComparingTo(utbetalingsgrad2.decimalValue());
        assertThat(uttakAktivitet2.arbeidstidsprosent()).isEqualByComparingTo(prosentArbeidAndel2);
        assertThat(uttakAktivitet2.stillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(uttakAktivitet2.erGradering()).isTrue();
        assertThat(uttakAktivitet2.arbeidsforhold().identifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet2.arbeidsforhold().arbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID_2.getReferanse());

    }

    private void assertPeriode(UttakResultatPeriode periode, LocalDate expectedFom, LocalDate expectedTom) {
        assertThat(periode).isNotNull();
        assertThat(periode.fom()).as("fom").isEqualTo(expectedFom);
        assertThat(periode.tom()).as("tom").isEqualTo(expectedTom);
        assertThat(periode.erOppholdsPeriode()).isFalse();
    }

    private UttakResultatPeriode getPeriodeByFom(List<UttakResultatPeriode> uttakResultatPerioder, LocalDate fom) {
        return uttakResultatPerioder.stream().filter(a -> fom.equals(a.fom())).findFirst().orElse(null);
    }

    private List<ForeldrepengerUttakPeriode> lagUttakResultatPlan() {
        var førFødselPeriode = lagUttakResultatPeriode(FOM_FØR_FØDSEL, TOM_FØR_FØDSEL, INNVILGET);
        var mødrekvote = lagUttakResultatPeriode(FOM_MØDREKVOTE, TOM_MØDREKVOTE, INNVILGET);
        var fellesperiode = lagUttakResultatPeriode(FOM_FELLESPERIODE, TOM_FELLESPERIODE, AVSLÅTT);

        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<>();

        perioder.add(førFødselPeriode);
        perioder.add(mødrekvote);
        perioder.add(fellesperiode);

        return perioder;
    }

    private UttakResultatPeriode onlyOne(UttakResultat resultat) {
        assertThat(resultat.uttakResultatPerioder()).hasSize(1);
        return resultat.uttakResultatPerioder().iterator().next();
    }

    private List<ForeldrepengerUttakPeriode> lagUttaksPeriode(BigDecimal prosentArbeid, Utbetalingsgrad prosentUtbetaling) {
        var idag = LocalDate.now();
        var uttakAktivtet = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR),
            ARBEIDSFORHOLD_ID);
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivtet)
            .medUtbetalingsgrad(prosentUtbetaling)
            .medArbeidsprosent(prosentArbeid)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();

        aktiviteter.add(periodeAktivitet);

        var periode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(idag, idag.plusDays(6))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medAktiviteter(aktiviteter)
            .build();

        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<>();
        perioder.add(periode);
        return perioder;
    }

    private List<ForeldrepengerUttakPeriode> lagUttaksPeriodeMedMultipleAktiviteter(BigDecimal prosentArbeid,
                                                                                    Utbetalingsgrad utbetalingsgrad1,
                                                                                    Utbetalingsgrad utbetalingsgrad2) {
        var idag = LocalDate.now();
        var uttakAktivtet = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR),
            ARBEIDSFORHOLD_ID);
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivtet)
            .medUtbetalingsgrad(utbetalingsgrad1)
            .medArbeidsprosent(prosentArbeid)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();

        var uttakAktivtet2 = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR),
            ARBEIDSFORHOLD_ID_2);
        var periodeAktivitet2 = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivtet2)
            .medUtbetalingsgrad(utbetalingsgrad2)
            .medArbeidsprosent(prosentArbeid)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();

        aktiviteter.add(periodeAktivitet);
        aktiviteter.add(periodeAktivitet2);

        var periode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(idag, idag.plusDays(6))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medAktiviteter(aktiviteter)
            .build();

        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<>();
        perioder.add(periode);
        return perioder;
    }

    private ForeldrepengerUttakPeriode lagUttakResultatPeriode(LocalDate fom, LocalDate tom, PeriodeResultatType periodeResultatType) {
        var uttakAktivitet = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR),
            ARBEIDSFORHOLD_ID);
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        aktiviteter.add(periodeAktivitet);
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(fom, tom)
            .medResultatType(periodeResultatType)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medAktiviteter(aktiviteter)
            .build();
    }
}
