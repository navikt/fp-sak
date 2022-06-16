package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

class OpptjeningMapperTilKalkulusTest {
    private static final Periode PERIODE = new Periode(LocalDate.now().minusMonths(12), LocalDate.now());
    private static final BehandlingReferanse REF = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());
    private InntektArbeidYtelseAggregatBuilder data = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = data.getAktørArbeidBuilder(REF.aktørId());
    private List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();

    @Test
    public void skal_ta_med_arbeidsforhold_når_ingen_inntektsmelding() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        lagArbeid(orgnr, ref1);
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1), byggIAY(), REF);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(1);
        assertFinnes(resultat, orgnr, ref1);
    }

    @Test
    public void skal_ta_med_arbeidsforhold_når_inntektsmelding_med_id() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        lagIM(orgnr, ref1);
        lagArbeid(orgnr, ref1);
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1), byggIAY(), REF);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(1);
        assertFinnes(resultat, orgnr, ref1);
    }

    @Test
    public void skal_ta_med_arbeidsforhold_når_inntektsmelding_uten_id() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        lagIM(orgnr, null);
        lagArbeid(orgnr, ref1);
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1), byggIAY(), REF);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(1);
        assertFinnes(resultat, orgnr, ref1);
    }

    @Test
    public void skal_ta_med_flere_arbeidsforhold_når_inntektsmelding_uten_id() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var ref3 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        lagIM(orgnr, null);
        lagArbeid(orgnr, ref1);
        lagArbeid(orgnr, ref2);
        lagArbeid(orgnr, ref3);
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var p2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref2);
        var p3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref3);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1, p2, p3), byggIAY(), REF);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(3);
        assertFinnes(resultat, orgnr, ref1);
        assertFinnes(resultat, orgnr, ref2);
        assertFinnes(resultat, orgnr, ref3);
    }

    @Test
    public void skal_håndtere_aktiviteter_med_og_uten_id_i_hos_arbeidsgivere() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var ref3 = InternArbeidsforholdRef.nyRef();
        var ref4 = InternArbeidsforholdRef.nyRef();
        var orgnr1 = "999999999";
        var orgnr2 = "999999998";
        lagIM(orgnr1, ref1);
        lagIM(orgnr1, ref2);
        lagIM(orgnr2, null);
        lagArbeid(orgnr1, ref1);
        lagArbeid(orgnr1, ref2);
        lagArbeid(orgnr2, ref3);
        lagArbeid(orgnr2, ref4);
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref1);
        var p2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref2);
        var p3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr2, null, ref3);
        var p4 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr2, null, ref4);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1, p2, p3, p4), byggIAY(), REF);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(4);
        assertFinnes(resultat, orgnr1, ref1);
        assertFinnes(resultat, orgnr1, ref2);
        assertFinnes(resultat, orgnr2, ref3);
        assertFinnes(resultat, orgnr2, ref4);
    }

    @Test
    public void flere_arbfor_samme_arbeidsgiver_noen_med_im_andre_uten() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var ref3 = InternArbeidsforholdRef.nyRef();
        var orgnr1 = "999999999";
        lagIM(orgnr1, ref1);
        lagIM(orgnr1, ref2);
        lagArbeid(orgnr1, ref1);
        lagArbeid(orgnr1, ref2);
        lagArbeid(orgnr1, ref3);
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref1);
        var p2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref2);
        var p3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref3);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1, p2, p3), byggIAY(), REF);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(2);
        assertFinnes(resultat, orgnr1, ref1);
        assertFinnes(resultat, orgnr1, ref2);
    }

    private void lagIM(String orgnr, InternArbeidsforholdRef internRef) {
        inntektsmeldinger.add(InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.ONE)
            .medRefusjon(BigDecimal.ONE)
            .medArbeidsforholdId(internRef)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medStartDatoPermisjon(LocalDate.now())
            .medInnsendingstidspunkt(LocalDateTime.now())
            .build());
    }

    private void lagArbeid(String orgnr, InternArbeidsforholdRef internRef) {
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aaBuilder = yaBuilder.getAktivitetsAvtaleBuilder();
        var aa = aaBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE));
        yaBuilder.leggTilAktivitetsAvtale(aa)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        if(internRef!= null) {
            yaBuilder.medArbeidsforholdId(internRef);
        }
        arbeidBuilder.leggTilYrkesaktivitet(yaBuilder);
    }

    private InntektArbeidYtelseGrunnlag byggIAY() {
        data.leggTilAktørArbeid(arbeidBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt().medData(data).medInntektsmeldinger(inntektsmeldinger).build();
    }


    private void assertFinnes(OpptjeningAktiviteterDto resultat, String orgnr, InternArbeidsforholdRef ref1) {
        var match = resultat.getOpptjeningPerioder()
            .stream()
            .filter(op -> op.getArbeidsgiver().get().getIdentifikator().equals(orgnr) && op.getArbeidsforholdId()
                .getReferanse()
                .equals(ref1.getReferanse()))
            .findFirst();
        assertThat(match).isPresent();
    }
}
