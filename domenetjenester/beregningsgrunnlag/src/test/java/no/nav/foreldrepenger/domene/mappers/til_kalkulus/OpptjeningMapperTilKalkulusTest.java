package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class OpptjeningMapperTilKalkulusTest {
    private static final Periode PERIODE = new Periode(LocalDate.now().minusMonths(12), LocalDate.now());

    @Test
    public void skal_ta_med_arbeidsforhold_når_ingen_inntektsmelding() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1), Collections.emptyList());
        assertThat(resultat.getOpptjeningPerioder()).hasSize(1);
        assertFinnes(resultat, orgnr, ref1);
    }

    @Test
    public void skal_ta_med_arbeidsforhold_når_inntektsmelding_med_id() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        var imer = Arrays.asList(lagIM(orgnr, ref1));
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1), imer);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(1);
        assertFinnes(resultat, orgnr, ref1);
    }

    @Test
    public void skal_ta_med_arbeidsforhold_når_inntektsmelding_uten_id() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        var imer = Arrays.asList(lagIM(orgnr, null));
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1), imer);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(1);
        assertFinnes(resultat, orgnr, ref1);
    }

    @Test
    public void skal_ta_med_flere_arbeidsforhold_når_inntektsmelding_uten_id() {
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var ref3 = InternArbeidsforholdRef.nyRef();
        var orgnr = "999999999";
        var imer = Arrays.asList(lagIM(orgnr, null));
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref1);
        var p2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref2);
        var p3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr, null, ref3);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1, p2, p3), imer);
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
        var imer = Arrays.asList(lagIM(orgnr1, ref1), lagIM(orgnr1, ref2), lagIM(orgnr2, null));
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref1);
        var p2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref2);
        var p3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr2, null, ref3);
        var p4 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr2, null, ref4);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1, p2, p3, p4), imer);
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
        var imer = Arrays.asList(lagIM(orgnr1, ref1), lagIM(orgnr1, ref2));
        var p1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref1);
        var p2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref2);
        var p3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, PERIODE, orgnr1, null, ref3);
        var resultat = OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(new OpptjeningAktiviteter(p1, p2, p3), imer);
        assertThat(resultat.getOpptjeningPerioder()).hasSize(2);
        assertFinnes(resultat, orgnr1, ref1);
        assertFinnes(resultat, orgnr1, ref2);
    }

    private Inntektsmelding lagIM(String orgnr, InternArbeidsforholdRef ref1) {
        return InntektsmeldingBuilder.builder().medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr)).medArbeidsforholdId(ref1).build();
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
    private void assertFinnes(OpptjeningAktiviteterDto resultat, OpptjeningAktivitetType type) {
        var match = resultat.getOpptjeningPerioder()
            .stream()
            .filter(op -> op.getOpptjeningAktivitetType().getKode().equals(type.getKode()))
            .findFirst();
        assertThat(match).isPresent();
    }

}
