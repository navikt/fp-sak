package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class SvangerskapspengerTjenesteTest {
    private static final LocalDate FØRSTE_FRA_DATO_TILR = LocalDate.now();

    private SvangerskapspengerTjeneste svangerskapspengerTjeneste;

    @BeforeEach
    void setUp() {
        svangerskapspengerTjeneste = new SvangerskapspengerTjeneste();
    }

    @Test
    void skal_utlede_riktig_stillingsprosent_med_arbeidsforholdsid() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(80);

        var avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(80)),
            AktivitetsAvtaleBuilder.ny() //ansettelsesperiode uten prosentsats og lønnsendringsdato
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusMonths(5).minusWeeks(1))));

        var ref = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler, arbeidsgiver, ref, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1));

        var tilrettelegging= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medInternArbeidsforholdRef(ref)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(2))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, Collections.emptyList(), tilrettelegging);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    void skal_utlede_riktig_stillingsprosent_med_flere_avtaleperioder() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(50);

        var avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1), FØRSTE_FRA_DATO_TILR.minusMonths(6)))
            .medProsentsats(BigDecimal.valueOf(100)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusMonths(6).plusDays(1)))
                .medProsentsats(BigDecimal.valueOf(50)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusMonths(3))));

        var ref = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler, arbeidsgiver, ref, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1));

        var tilrettelegging= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medInternArbeidsforholdRef(ref)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(2))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, Collections.emptyList(), tilrettelegging);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    void skal_summere_stillingsprosent_hvis_flere_arbeidsforhold_i_samme_virksomhet() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(100);

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(100)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusMonths(5))));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(50)),
            AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusMonths(5))));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilrettelegging= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(6))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, Collections.emptyList(), tilrettelegging);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    void skal_finne_stillingsprosent_for_arbeidsforholdet_om_tilrettelegging_har_arbeidsforholdsid() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosentRef1 = BigDecimal.valueOf(50);

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(50)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1))));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
            .medProsentsats(BigDecimal.valueOf(50)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1))));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilretteleggingRef1= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medInternArbeidsforholdRef(ref1)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(6))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, Collections.emptyList(), tilretteleggingRef1);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosentRef1);
    }

    @Test
    void skal_kun_finne_stillingsprosent_for_arbeidsforholdet_som_har_aktiv_ansettelsesperiode() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var forventetStillingsprosent = BigDecimal.valueOf(100);

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
                .medProsentsats(BigDecimal.valueOf(100)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1))));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1)))
                .medProsentsats(BigDecimal.valueOf(100)),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_FRA_DATO_TILR.minusYears(2), FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(2))));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilretteleggingRef1= new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.ZERO)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(6))
                .build()))
            .build();

        var resultat = svangerskapspengerTjeneste.utledStillingsprosentForTilrPeriode(registerFilter, Collections.emptyList(), tilretteleggingRef1);

        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualByComparingTo(forventetStillingsprosent);
    }

    @Test
    public void skal_vurdere_splitt_av_arbeidsforholdet() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1))));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1))));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilrettelegging = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.valueOf(50))
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(6))
                .build()))
            .build();

        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FØRSTE_FRA_DATO_TILR.minusMonths(6)).build();
        var inntektsmeldinger = List.of(lagIM(arbeidsgiver.getOrgnr(), null, 50000),
            lagIM(arbeidsgiver.getOrgnr(), InternArbeidsforholdRef.nyRef(), 0));

        var skalVurdereSplitt = svangerskapspengerTjeneste.utledOmDetSkalVurderesSplittAvArbeidsforholdet(tilrettelegging, Optional.of(registerFilter), stp, inntektsmeldinger);

        assertThat(skalVurdereSplitt).isTrue();

    }

    @Test
    public void ingen_splitt_av_arbeidsforhold_når_ikke_gradering() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1))));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1))));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilrettelegging = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(6))
                .build()))
            .build();

        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FØRSTE_FRA_DATO_TILR.minusMonths(6)).build();
        var inntektsmeldinger = List.of(lagIM(arbeidsgiver.getOrgnr(), null, 50000));

        var skalVurdereSplitt = svangerskapspengerTjeneste.utledOmDetSkalVurderesSplittAvArbeidsforholdet(tilrettelegging, Optional.of(registerFilter), stp, inntektsmeldinger);

        assertThat(skalVurdereSplitt).isFalse();
    }

    @Test
    public void ingen_splitt_av_arbeidsforhold_når_mottatt_im_med_arbeidsforholdsid() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");

        var avtaler1 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1).minusWeeks(1))));
        var avtaler2 = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(FØRSTE_FRA_DATO_TILR.minusYears(1))));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var yrkesaktivitet1 = lagYrkesaktivitet(avtaler1, arbeidsgiver, ref1, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitet(avtaler2, arbeidsgiver, ref2, List.of());
        var registerFilter = new YrkesaktivitetFilter(new ArbeidsforholdInformasjon(), List.of(yrkesaktivitet1, yrkesaktivitet2));

        var tilrettelegging = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medOverstyrtUtbetalingsgrad(BigDecimal.valueOf(50))
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(FØRSTE_FRA_DATO_TILR.minusMonths(6))
                .build()))
            .build();

        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FØRSTE_FRA_DATO_TILR.minusMonths(6)).build();
        var inntektsmeldinger = List.of(lagIM(arbeidsgiver.getOrgnr(), InternArbeidsforholdRef.nyRef(), 50000), lagIM("987654321", null, 10000),
            lagIM(arbeidsgiver.getOrgnr(), InternArbeidsforholdRef.nyRef(), 0));

        var skalVurdereSplitt = svangerskapspengerTjeneste.utledOmDetSkalVurderesSplittAvArbeidsforholdet(tilrettelegging, Optional.of(registerFilter), stp, inntektsmeldinger);

        assertThat(skalVurdereSplitt).isFalse();
    }

    private Inntektsmelding lagIM(String orgnr, InternArbeidsforholdRef internRef, Integer inntekt) {
        return InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.valueOf(inntekt))
            .medArbeidsforholdId(internRef)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .build();
    }

    private Yrkesaktivitet lagYrkesaktivitet(List<AktivitetsAvtaleBuilder> aktivitetsAvtale,
                                             Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<Permisjon> permisjoner) {
        var builder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsforholdId(ref)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        aktivitetsAvtale.forEach(builder::leggTilAktivitetsAvtale);
        permisjoner.forEach(builder::leggTilPermisjon);
        return builder.build();
    }
}
