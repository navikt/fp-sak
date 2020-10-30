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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.vedtak.konfig.Tid;

public class MapUttakResultatFraVLTilRegelTest {

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

    private ForeldrepengerUttak vlPlan;
    private Behandling behandling;
    private MapUttakResultatFraVLTilRegel overriddenMapper;
    private MapUttakResultatFraVLTilRegel mapper;

    @BeforeEach
    public void setup() {
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor().build();
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
    public void skalMappeUttakResultatPlan() {
        // Act
        UttakResultat regelPlan = overriddenMapper.mapFra(vlPlan, lagRef(behandling));

        // Assert
        assertThat(regelPlan).isNotNull();
        var uttakResultatPerioder = regelPlan.getUttakResultatPerioder();
        assertThat(uttakResultatPerioder).isNotNull();
        assertThat(uttakResultatPerioder).hasSize(3);

        no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode førFødselPeriode = getPeriodeByFom(uttakResultatPerioder, FOM_FØR_FØDSEL);
        no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode mødrePeriode = getPeriodeByFom(uttakResultatPerioder, FOM_MØDREKVOTE);
        no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode fellesPeriode = getPeriodeByFom(uttakResultatPerioder, FOM_FELLESPERIODE);

        assertPeriode(førFødselPeriode, FOM_FØR_FØDSEL, TOM_FØR_FØDSEL);
        assertPeriode(mødrePeriode, FOM_MØDREKVOTE, TOM_MØDREKVOTE);
        assertPeriode(fellesPeriode, FOM_FELLESPERIODE, TOM_FELLESPERIODE);
    }

    private UttakInput lagRef(Behandling behandling) {
        LocalDate skjæringstidspunkt = LocalDate.now();
        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt), null, null);
    }

    @Test
    public void skalMappeUttakAktivitet() {
        //Arrange
        BigDecimal prosentArbeid = BigDecimal.valueOf(10);
        var utbetalingsgrad = new Utbetalingsgrad(66); //overstyrt
        ForeldrepengerUttak uttakPlan = lagUttaksPeriode(prosentArbeid, utbetalingsgrad);
        //Act
        UttakResultat resultat = overriddenMapper.mapFra(uttakPlan, lagRef(behandling));
        //Assert
        UttakResultatPeriode resultPeriode = onlyOne(resultat);
        UttakAktivitet uttakAktivitet = resultPeriode.getUttakAktiviteter().get(0);
        assertThat(uttakAktivitet.getUtbetalingsgrad()).isEqualByComparingTo(utbetalingsgrad.decimalValue());
        assertThat(uttakAktivitet.getArbeidstidsprosent()).isEqualByComparingTo(prosentArbeid);
        assertThat(uttakAktivitet.getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(uttakAktivitet.getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(uttakAktivitet.isErGradering()).isTrue();
        assertThat(uttakAktivitet.getArbeidsforhold().getIdentifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID.getReferanse());
        assertThat(uttakAktivitet.getArbeidsforhold().erFrilanser()).isFalse();
    }

    private ArbeidsforholdInformasjon lagArbeidsforholdInformasjon(Arbeidsgiver arbeidsgiver) {
        ArbeidsforholdInformasjonBuilder arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
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

        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = Tid.TIDENES_ENDE;

        AktivitetsAvtaleBuilder aa1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        AktivitetsAvtaleBuilder aa1_2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(new BigDecimal(20));

        AktivitetsAvtaleBuilder aa2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        AktivitetsAvtaleBuilder aa2_2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(new BigDecimal(60));

        YrkesaktivitetBuilder ya1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD).medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aa1).leggTilAktivitetsAvtale(aa1_2).medArbeidsforholdId(ARBEIDSFORHOLD_ID);
        YrkesaktivitetBuilder ya2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD).medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aa2).leggTilAktivitetsAvtale(aa2_2).medArbeidsforholdId(ARBEIDSFORHOLD_ID_2);

        yrkesAktiviteter.add(ya1);
        yrkesAktiviteter.add(ya2);
        return yrkesAktiviteter;

    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlag(List<YrkesaktivitetBuilder> yrkesaktivitetList, AktørId aktørId, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        InntektArbeidYtelseAggregatBuilder aggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = aggregat.getAktørArbeidBuilder(aktørId);
        for (YrkesaktivitetBuilder yrkesaktivitet : yrkesaktivitetList) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet);
        }
        aggregat.leggTilAktørArbeid(aktørArbeidBuilder);

        InntektArbeidYtelseGrunnlagBuilder inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty());
        inntektArbeidYtelseGrunnlagBuilder.medInformasjon(arbeidsforholdInformasjon);
        inntektArbeidYtelseGrunnlagBuilder.medData(aggregat);
        return inntektArbeidYtelseGrunnlagBuilder;
    }

    private UttakInput lagRefMedIay(Behandling behandling, List<YrkesaktivitetBuilder> yrkesaktiviteter, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        LocalDate skjæringstidspunkt = LocalDate.now();

        var bgStatuser = yrkesaktiviteter.stream().map((YrkesaktivitetBuilder yb) -> {
            Yrkesaktivitet y = yb.build();
            var arbeidsforholdRef = y.getArbeidsforholdRef();
            var arbeidsgiver = y.getArbeidsgiver();
            return new BeregningsgrunnlagStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef);
        }).collect(Collectors.toSet());

        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt), iayGrunnlag, null).medBeregningsgrunnlagStatuser(bgStatuser);
    }

    @Test
    public void skalMappeUttakAktivitetMedFlereArbeidforholdHosSammeArbeidtaker() {
        //Arrange
        var arbeidsgiver = Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR);

        ArbeidsforholdInformasjon arbeidsforholdInformasjon = lagArbeidsforholdInformasjon(arbeidsgiver);

        List<YrkesaktivitetBuilder> yrkesaktiviteter = lagYrkesAkiviteter(arbeidsgiver);
        InntektArbeidYtelseGrunnlag iayGrunnlag = opprettGrunnlag(yrkesaktiviteter, behandling.getAktørId(), arbeidsforholdInformasjon).build();

        BigDecimal prosentArbeid = BigDecimal.valueOf(60);

        BigDecimal prosentArbeidAndel1 = BigDecimal.valueOf(15);
        BigDecimal prosentArbeidAndel2 = BigDecimal.valueOf(45);

        var utbetalingsgrad1 = new Utbetalingsgrad(15); //overstyrt
        var utbetalingsgrad2 = new Utbetalingsgrad(45); //overstyrt
        ForeldrepengerUttak uttakPlan = lagUttaksPeriodeMedMultipleAktiviteter(prosentArbeid, utbetalingsgrad1, utbetalingsgrad2);
        //Act
        UttakResultat resultat = mapper.mapFra(uttakPlan, lagRefMedIay(behandling, yrkesaktiviteter, iayGrunnlag));
        //Assert
        UttakResultatPeriode resultPeriode = onlyOne(resultat);
        UttakAktivitet uttakAktivitet = resultPeriode.getUttakAktiviteter().get(0);
        assertThat(uttakAktivitet.getUtbetalingsgrad()).isEqualByComparingTo(utbetalingsgrad1.decimalValue());
        assertThat(uttakAktivitet.getArbeidstidsprosent()).isEqualByComparingTo(prosentArbeidAndel1);
        assertThat(uttakAktivitet.getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(uttakAktivitet.getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(uttakAktivitet.isErGradering()).isTrue();
        assertThat(uttakAktivitet.getArbeidsforhold().getIdentifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID.getReferanse());
        assertThat(uttakAktivitet.getArbeidsforhold().erFrilanser()).isFalse();

        UttakAktivitet uttakAktivitet2 = resultPeriode.getUttakAktiviteter().get(1);
        assertThat(uttakAktivitet2.getUtbetalingsgrad()).isEqualByComparingTo(utbetalingsgrad2.decimalValue());
        assertThat(uttakAktivitet2.getArbeidstidsprosent()).isEqualByComparingTo(prosentArbeidAndel2);
        assertThat(uttakAktivitet2.getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(uttakAktivitet2.isErGradering()).isTrue();
        assertThat(uttakAktivitet2.getArbeidsforhold().getIdentifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet2.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID_2.getReferanse());

    }



    private void assertPeriode(UttakResultatPeriode periode, LocalDate expectedFom, LocalDate expectedTom) {
        assertThat(periode).isNotNull();
        assertThat(periode.getFom()).as("fom").isEqualTo(expectedFom);
        assertThat(periode.getTom()).as("tom").isEqualTo(expectedTom);
        assertThat(periode.getErOppholdsPeriode()).isFalse();
    }

    private no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode getPeriodeByFom(List<UttakResultatPeriode> uttakResultatPerioder, LocalDate fom) {
        return uttakResultatPerioder.stream().filter(a -> fom.equals(a.getFom())).findFirst().orElse(null);
    }

    private ForeldrepengerUttak lagUttakResultatPlan() {
        ForeldrepengerUttakPeriode førFødselPeriode = lagUttakResultatPeriode(FOM_FØR_FØDSEL, TOM_FØR_FØDSEL, INNVILGET);
        ForeldrepengerUttakPeriode mødrekvote = lagUttakResultatPeriode(FOM_MØDREKVOTE, TOM_MØDREKVOTE, INNVILGET);
        ForeldrepengerUttakPeriode fellesperiode = lagUttakResultatPeriode(FOM_FELLESPERIODE, TOM_FELLESPERIODE, AVSLÅTT);

        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<ForeldrepengerUttakPeriode>();

        perioder.add(førFødselPeriode);
        perioder.add(mødrekvote);
        perioder.add(fellesperiode);

        ForeldrepengerUttak resultat = new ForeldrepengerUttak(perioder);
        return resultat;
    }

    private UttakResultatPeriode onlyOne(UttakResultat resultat) {
        assertThat(resultat.getUttakResultatPerioder()).hasSize(1);
        return resultat.getUttakResultatPerioder().iterator().next();
    }

    private ForeldrepengerUttak lagUttaksPeriode(BigDecimal prosentArbeid, Utbetalingsgrad prosentUtbetaling) {
        LocalDate idag = LocalDate.now();
        ForeldrepengerUttakAktivitet uttakAktivtet = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID,Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR), ARBEIDSFORHOLD_ID);
        ForeldrepengerUttakPeriodeAktivitet periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivtet)
            .medUtbetalingsgrad(prosentUtbetaling)
            .medArbeidsprosent(prosentArbeid)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();

        aktiviteter.add(periodeAktivitet);

        ForeldrepengerUttakPeriode periode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(idag, idag.plusDays(6))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medAktiviteter(aktiviteter)
            .build();

        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<>();
        perioder.add(periode);
        return new ForeldrepengerUttak(perioder);
    }

    private ForeldrepengerUttak lagUttaksPeriodeMedMultipleAktiviteter(BigDecimal prosentArbeid, Utbetalingsgrad utbetalingsgrad1, Utbetalingsgrad utbetalingsgrad2) {
        LocalDate idag = LocalDate.now();
        ForeldrepengerUttakAktivitet uttakAktivtet = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID,Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR), ARBEIDSFORHOLD_ID);
        ForeldrepengerUttakPeriodeAktivitet periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivtet)
            .medUtbetalingsgrad(utbetalingsgrad1)
            .medArbeidsprosent(prosentArbeid)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();

        ForeldrepengerUttakAktivitet uttakAktivtet2 = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID,Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR), ARBEIDSFORHOLD_ID_2);
        ForeldrepengerUttakPeriodeAktivitet periodeAktivitet2 = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivtet2)
            .medUtbetalingsgrad(utbetalingsgrad2)
            .medArbeidsprosent(prosentArbeid)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();

        aktiviteter.add(periodeAktivitet);
        aktiviteter.add(periodeAktivitet2);

        ForeldrepengerUttakPeriode periode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(idag, idag.plusDays(6))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medAktiviteter(aktiviteter)
            .build();

        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<>();
        perioder.add(periode);
        return new ForeldrepengerUttak(perioder);
    }

    private ForeldrepengerUttakPeriode lagUttakResultatPeriode(LocalDate fom, LocalDate tom, PeriodeResultatType periodeResultatType) {
        ForeldrepengerUttakAktivitet uttakAktivitet = new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID,
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR), ARBEIDSFORHOLD_ID);
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();
        ForeldrepengerUttakPeriodeAktivitet periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        aktiviteter.add(periodeAktivitet);
        ForeldrepengerUttakPeriode periode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(fom, tom)
            .medResultatType(periodeResultatType)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medAktiviteter(aktiviteter)
            .build();
        return periode;
    }
}
