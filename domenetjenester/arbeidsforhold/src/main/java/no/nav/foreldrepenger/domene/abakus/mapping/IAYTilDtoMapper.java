package no.nav.foreldrepenger.domene.abakus.mapping;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdInformasjon;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatOverstyrtDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;

public class IAYTilDtoMapper {

    private IAYTilDtoMapper() {
    }

    public static Optional<InntektArbeidYtelseAggregatOverstyrtDto> mapTilOverstyrtDto(InntektArbeidYtelseGrunnlag grunnlag) {
        return Optional.ofNullable(grunnlag).flatMap(InntektArbeidYtelseGrunnlag::getSaksbehandletVersjon)
            .map(a -> mapSaksbehandlerOverstyrteOpplysninger(getArbeidforholdInfo(grunnlag), a));
    }

    public static Optional<ArbeidsforholdInformasjon> mapTilArbeidsforholdInfoDto(InntektArbeidYtelseGrunnlag grunnlag) {
        return Optional.ofNullable(grunnlag)
            .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon)
            .map(ai -> new MapArbeidsforholdInformasjon.MapTilDto().map(ai, grunnlag.getEksternReferanse(), grunnlag.isAktiv()));
    }

    public static InntektsmeldingerDto mapTilDto(Collection<InntektsmeldingBuilder> inntektsmeldingBuildere) {
        var mapInntektsmeldinger = new MapInntektsmeldinger.MapTilDto();
        return mapInntektsmeldinger.map(inntektsmeldingBuildere);
    }

    public static OppgittOpptjeningDto mapTilDto(OppgittOpptjeningBuilder builder) {
        return new MapOppgittOpptjening().mapTilDto(builder.build());
    }

    private static no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon getArbeidforholdInfo(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getArbeidsforholdInformasjon().orElseThrow(
                () -> new IllegalStateException("Mangler ArbeidsforholdInformasjon i grunnlag (påkrevd her): " + grunnlag.getEksternReferanse()));
    }

    private static InntektArbeidYtelseAggregatOverstyrtDto mapSaksbehandlerOverstyrteOpplysninger(no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon arbeidsforholdInformasjon, InntektArbeidYtelseAggregat aggregat) {
        var tidspunkt = Optional.ofNullable(aggregat.getOpprettetTidspunkt()).orElse(LocalDateTime.now());
        var aktørArbeid = aggregat.getAktørArbeid();
        var arbeid = new MapAktørArbeid.MapTilDto(arbeidsforholdInformasjon).map(aktørArbeid);
        var overstyrt = new InntektArbeidYtelseAggregatOverstyrtDto(tidspunkt, aggregat.getEksternReferanse());
        overstyrt.medArbeid(arbeid);
        return overstyrt;
    }
}
