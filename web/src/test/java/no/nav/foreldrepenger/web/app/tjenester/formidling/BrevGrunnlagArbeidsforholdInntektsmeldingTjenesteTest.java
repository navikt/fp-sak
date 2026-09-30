package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

class BrevGrunnlagArbeidsforholdInntektsmeldingTjenesteTest {

    @Test
    void skal_teste_alle_im_mottatt() {
        LocalDate stp = LocalDate.of(2024, 3, 1);
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref = InternArbeidsforholdRef.nyRef();
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(ag)
            .medArbeidsforholdId(ref)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aa = yaBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(2), stp.plusMonths(2)))
            .medProsentsats(Stillingsprosent.HUNDRED);
        yaBuilder.leggTilAktivitetsAvtale(aa);
        var arbeidImStatus = new ArbeidsforholdInntektsmeldingStatus(ag, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);

        var arbeidsforholdInntektsmeldinger = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(Arrays.asList(arbeidImStatus),
            Arrays.asList(yaBuilder.build()), stp);

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().erInntektsmeldingMottatt()).isTrue();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().arbeidsgiverIdent()).isEqualTo(ag.getIdentifikator());
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().stillingsprosent()).isEqualByComparingTo(
            BigDecimal.valueOf(100));
    }

    @Test
    void skal_teste_im_mangler() {
        LocalDate stp = LocalDate.of(2024, 3, 1);
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref = InternArbeidsforholdRef.nyRef();
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(ag)
            .medArbeidsforholdId(ref)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aa = yaBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(2), stp.plusMonths(2)))
            .medProsentsats(Stillingsprosent.HUNDRED);
        yaBuilder.leggTilAktivitetsAvtale(aa);


        var arbeidImStatus = new ArbeidsforholdInntektsmeldingStatus(ag, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
        var arbeidsforholdInntektsmeldinger = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(Arrays.asList(arbeidImStatus),
            Arrays.asList(yaBuilder.build()), stp);

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().erInntektsmeldingMottatt()).isFalse();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().arbeidsgiverIdent()).isEqualTo(ag.getIdentifikator());
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().stillingsprosent()).isEqualByComparingTo(
            BigDecimal.valueOf(100));
    }

    @Test
    void skal_gi_en_linje_per_arbeidsgiver_med_summert_stillingsprosent() {
        LocalDate stp = LocalDate.of(2024, 3, 1);
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ya1 = lagYrkesaktivitet(ag, stp.minusMonths(2), stp.plusMonths(2), new Stillingsprosent(60));
        var ya2 = lagYrkesaktivitet(ag, stp.minusMonths(2), stp.plusMonths(2), new Stillingsprosent(40));
        var avsluttetFørStp = lagYrkesaktivitet(ag, stp.minusYears(2), stp.minusYears(1), new Stillingsprosent(50));
        var arbeidImStatus = new ArbeidsforholdInntektsmeldingStatus(ag, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);

        var arbeidsforholdInntektsmeldinger = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(List.of(arbeidImStatus),
            List.of(ya1, ya2, avsluttetFørStp), stp);

        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).singleElement().satisfies(im -> {
            assertThat(im.arbeidsgiverIdent()).isEqualTo(ag.getIdentifikator());
            assertThat(im.erInntektsmeldingMottatt()).isFalse();
            assertThat(im.stillingsprosent()).isEqualByComparingTo(BigDecimal.valueOf(100));
        });
    }

    @Test
    void skal_teste_en_im_mangler_en_er_mottatt_og_en_er_avklart() {
        LocalDate stp = LocalDate.of(2024, 3, 1);
        var agMottatt = Arbeidsgiver.virksomhet("999999999");
        var agMangler = Arbeidsgiver.virksomhet("888888888");
        var agAvklart = Arbeidsgiver.virksomhet("777777777");
        var yrkesaktiviteter = List.of(
            lagYrkesaktivitet(agMottatt, stp.minusMonths(2), stp.plusMonths(2), new Stillingsprosent(60)),
            lagYrkesaktivitet(agMangler, stp.minusMonths(2), stp.plusMonths(2), new Stillingsprosent(40)),
            lagYrkesaktivitet(agAvklart, stp.minusMonths(2), stp.plusMonths(2), new Stillingsprosent(20)));
        var statuser = List.of(
            new ArbeidsforholdInntektsmeldingStatus(agMottatt, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT),
            new ArbeidsforholdInntektsmeldingStatus(agMangler, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT),
            new ArbeidsforholdInntektsmeldingStatus(agAvklart, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD));

        var arbeidsforholdInntektsmeldinger = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(statuser, yrkesaktiviteter, stp);

        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).hasSize(2);
        var mottattIM = arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()
            .stream()
            .filter(ai -> ai.arbeidsgiverIdent().equals(agMottatt.getIdentifikator()))
            .findFirst()
            .orElseThrow();
        assertThat(mottattIM.erInntektsmeldingMottatt()).isTrue();
        assertThat(mottattIM.stillingsprosent()).isEqualByComparingTo(BigDecimal.valueOf(60));
        var ikkeMottattIM = arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()
            .stream()
            .filter(ai -> ai.arbeidsgiverIdent().equals(agMangler.getIdentifikator()))
            .findFirst()
            .orElseThrow();
        assertThat(ikkeMottattIM.erInntektsmeldingMottatt()).isFalse();
        assertThat(ikkeMottattIM.stillingsprosent()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    private static Yrkesaktivitet lagYrkesaktivitet(Arbeidsgiver ag, LocalDate fom, LocalDate tom, Stillingsprosent stillingsprosent) {
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(ag)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aa = yaBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent);
        yaBuilder.leggTilAktivitetsAvtale(aa);
        return yaBuilder.build();
    }
}
