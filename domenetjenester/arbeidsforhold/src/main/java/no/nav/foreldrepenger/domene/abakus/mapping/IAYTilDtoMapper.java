package no.nav.foreldrepenger.domene.abakus.mapping;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatOverstyrtDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class IAYTilDtoMapper {

    private AktørId aktørId;
    private YtelseType ytelseType;
    private UUID grunnlagReferanse;
    private UUID behandlingReferanse;

    public IAYTilDtoMapper(AktørId aktørId, YtelseType ytelseType, UUID grunnlagReferanse, UUID behandlingReferanse) {
        this.aktørId = aktørId;
        this.ytelseType = ytelseType;
        this.grunnlagReferanse = grunnlagReferanse;
        this.behandlingReferanse = behandlingReferanse;
    }

    public OverstyrtInntektArbeidYtelseDto mapTilOverstyringDto(InntektArbeidYtelseGrunnlag grunnlag) {
        if (grunnlag == null) {
            return null;
        }

        var overstyrt = grunnlag.getSaksbehandletVersjon().map(a -> mapSaksbehandlerOverstyrteOpplysninger(getArbeidforholdInfo(grunnlag), a));
        var arbeidsforholdInfo = grunnlag.getArbeidsforholdInformasjon()
            .map(ai -> new MapArbeidsforholdInformasjon.MapTilDto().map(ai, grunnlag.getEksternReferanse(), grunnlag.isAktiv()));
        return new OverstyrtInntektArbeidYtelseDto(new AktørIdPersonident(aktørId.getId()), grunnlagReferanse, behandlingReferanse, ytelseType,
            arbeidsforholdInfo.orElse(null), overstyrt.orElse(null));
    }

    public InntektsmeldingerDto mapTilDto(Collection<InntektsmeldingBuilder> inntektsmeldingBuildere) {
        var mapInntektsmeldinger = new MapInntektsmeldinger.MapTilDto();
        return mapInntektsmeldinger.map(inntektsmeldingBuildere);
    }

    public OppgittOpptjeningDto mapTilDto(OppgittOpptjeningBuilder builder) {
        return new MapOppgittOpptjening().mapTilDto(builder.build());
    }

    private ArbeidsforholdInformasjon getArbeidforholdInfo(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getArbeidsforholdInformasjon()
            .orElseThrow(
                () -> new IllegalStateException("Mangler ArbeidsforholdInformasjon i grunnlag (påkrevd her): " + grunnlag.getEksternReferanse()));
    }

    private InntektArbeidYtelseAggregatOverstyrtDto mapSaksbehandlerOverstyrteOpplysninger(ArbeidsforholdInformasjon arbeidsforholdInformasjon,
                                                                                           InntektArbeidYtelseAggregat aggregat) {
        var tidspunkt = Optional.ofNullable(aggregat.getOpprettetTidspunkt()).orElse(LocalDateTime.now());
        var aktørArbeid = aggregat.getAktørArbeid();
        var arbeid = new MapAktørArbeid.MapTilDto(arbeidsforholdInformasjon).map(aktørArbeid);
        var overstyrt = new InntektArbeidYtelseAggregatOverstyrtDto(tidspunkt, aggregat.getEksternReferanse());
        overstyrt.medArbeid(arbeid);
        return overstyrt;
    }
}
