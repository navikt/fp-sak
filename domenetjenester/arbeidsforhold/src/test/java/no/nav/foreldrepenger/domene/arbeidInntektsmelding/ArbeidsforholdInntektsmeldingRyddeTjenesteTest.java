package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class ArbeidsforholdInntektsmeldingRyddeTjenesteTest {

    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("999999999");

    @Test
    void eldre_valg_lagret_på_arbeidsforhold_er_gyldig_når_mangel_gjelder_hele_arbeidsgiver() {
        var valgAf1 = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef());
        var valgAf2 = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef());
        var mangel = new ArbeidsforholdMangel(ARBEIDSGIVER, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING);

        var ugyldigeValg = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeValgSomErGjort(List.of(valgAf1, valgAf2), List.of(mangel));

        assertThat(ugyldigeValg).isEmpty();
    }

    @Test
    void valg_for_arbeidsgiver_uten_mangel_er_ugyldig() {
        var valg = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nullRef());
        var mangel = new ArbeidsforholdMangel(Arbeidsgiver.virksomhet("888888888"), InternArbeidsforholdRef.nullRef(),
            AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING);

        var ugyldigeValg = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeValgSomErGjort(List.of(valg), List.of(mangel));

        assertThat(ugyldigeValg).containsExactly(valg);
    }

    @Test
    void nytt_valg_på_arbeidsgiver_erstatter_eldre_valg_pr_arbeidsforhold() {
        var gammeltValgAf1 = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef());
        var gammeltValgAf2 = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef());
        var valgAnnenArbeidsgiver = lagValg(Arbeidsgiver.virksomhet("888888888"), InternArbeidsforholdRef.nyRef());
        var valgImUtenArbeidsforhold = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef(), ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING);
        var nyttValg = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nullRef(), ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);

        var erstattes = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnValgSomErstattesAvValgPåArbeidsgiver(
            List.of(gammeltValgAf1, gammeltValgAf2, valgAnnenArbeidsgiver, valgImUtenArbeidsforhold), List.of(nyttValg));

        assertThat(erstattes).containsExactlyInAnyOrder(gammeltValgAf1, gammeltValgAf2);
    }

    @Test
    void nytt_valg_pr_arbeidsforhold_erstatter_ikke_andre_valg() {
        var gammeltValg = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef());
        var nyttValg = lagValg(ARBEIDSGIVER, InternArbeidsforholdRef.nyRef());

        var erstattes = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnValgSomErstattesAvValgPåArbeidsgiver(List.of(gammeltValg), List.of(nyttValg));

        assertThat(erstattes).isEmpty();
    }

    private static ArbeidsforholdValg lagValg(Arbeidsgiver ag, InternArbeidsforholdRef ref) {
        return lagValg(ag, ref, ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
    }

    private static ArbeidsforholdValg lagValg(Arbeidsgiver ag, InternArbeidsforholdRef ref, ArbeidsforholdKomplettVurderingType vurdering) {
        return ArbeidsforholdValg.builder()
            .medArbeidsgiver(ag.getIdentifikator())
            .medArbeidsforholdRef(ref)
            .medVurdering(vurdering)
            .medBegrunnelse("begrunnelse")
            .build();
    }
}
