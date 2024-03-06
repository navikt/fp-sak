package no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

class ArbeidsforholdInntektsmeldingDtoTjenesteTest {

    @Test
    public void skal_teste_alle_im_mottatt() {
        LocalDate stp = LocalDate.of(2024,3,1);
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

        var arbeidsforholdInntektsmeldinger = ArbeidsforholdInntektsmeldingDtoTjeneste.mapInntektsmeldingStatus(Map.of(ag, Set.of(ref)), Map.of(),
            Arrays.asList(yaBuilder.build()), stp);

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().erInntektsmeldingMottatt()).isTrue();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().arbeidsgiverIdent()).isEqualTo(ag.getIdentifikator());
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().stillingsprosent()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_teste_im_mangler() {
        LocalDate stp = LocalDate.of(2024,3,1);
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

        var arbeidsforholdInntektsmeldinger = ArbeidsforholdInntektsmeldingDtoTjeneste.mapInntektsmeldingStatus(Map.of(ag, Set.of(ref)), Map.of(ag, Set.of(ref)),
            Arrays.asList(yaBuilder.build()), stp);

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().erInntektsmeldingMottatt()).isFalse();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().arbeidsgiverIdent()).isEqualTo(ag.getIdentifikator());
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding().getFirst().stillingsprosent()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_teste_en_im_mangløer_en_er_mottatt() {
        LocalDate stp = LocalDate.of(2024,3,1);
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var yaBuilder1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(ag)
            .medArbeidsforholdId(ref1)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var yaBuilder2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(ag)
            .medArbeidsforholdId(ref2)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aa1 = yaBuilder1.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(2), stp.plusMonths(2)))
            .medProsentsats(new Stillingsprosent(60));
        var aa2 = yaBuilder2.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(2), stp.plusMonths(2)))
            .medProsentsats(new Stillingsprosent(40));
        yaBuilder1.leggTilAktivitetsAvtale(aa1);
        yaBuilder2.leggTilAktivitetsAvtale(aa2);

        var arbeidsforholdInntektsmeldinger = ArbeidsforholdInntektsmeldingDtoTjeneste.mapInntektsmeldingStatus(Map.of(ag, Set.of(ref1, ref2)), Map.of(ag, Set.of(ref2)),
            Arrays.asList(yaBuilder1.build(), yaBuilder2.build()), stp);

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull();
        assertThat(arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()).hasSize(2);
        var mottattIM = arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()
            .stream()
            .filter(ai -> ai.stillingsprosent().compareTo(BigDecimal.valueOf(60)) == 0)
            .findFirst()
            .orElseThrow();
        assertThat(mottattIM.erInntektsmeldingMottatt()).isTrue();
        var ikkeMottattIM = arbeidsforholdInntektsmeldinger.arbeidsforholdInntektsmelding()
            .stream()
            .filter(ai -> ai.stillingsprosent().compareTo(BigDecimal.valueOf(40)) == 0)
            .findFirst()
            .orElseThrow();
        assertThat(ikkeMottattIM.erInntektsmeldingMottatt()).isFalse();
    }

}
