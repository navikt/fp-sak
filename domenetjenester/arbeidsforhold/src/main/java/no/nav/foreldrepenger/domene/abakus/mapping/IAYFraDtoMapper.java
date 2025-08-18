package no.nav.foreldrepenger.domene.abakus.mapping;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.typer.AktørId;

/**
 * Merk denne mapper alltid hele aggregat tilbake til nye instanser av IAY
 * Aggregat. (i motsetning til tilsvarende implementasjon i ABakus som mapper
 * til eksisterende instans).
 */
public class IAYFraDtoMapper {

    public IAYFraDtoMapper() {
        // sonar
    }

    /**
     * Til bruk for migrering (sender inn registerdata, istdf. å hente fra
     * registerne.). Merk tar ikke hensyn til eksisterende grunnlag lagret (mapper
     * kun input).
     */
    public InntektArbeidYtelseGrunnlag mapTilGrunnlagInklusivRegisterdata(InntektArbeidYtelseGrunnlagDto dto, boolean erAktivtGrunnlag) {
        var builder = InntektArbeidYtelseGrunnlagBuilder.ny(UUID.fromString(dto.getGrunnlagReferanse()),
                dto.getGrunnlagTidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        builder.medErAktivtGrunnlag(erAktivtGrunnlag);
        return mapTilGrunnlagInklusivRegisterdata(dto, builder);
    }

    /**
     * @see #mapTilGrunnlagInklusivRegisterdata(InntektArbeidYtelseGrunnlagDto,
     *      boolean)
     */
    public InntektArbeidYtelseGrunnlag mapTilGrunnlagInklusivRegisterdata(InntektArbeidYtelseGrunnlagDto dto,
            InntektArbeidYtelseGrunnlagBuilder builder) {
        mapSaksbehandlerDataTilBuilder(dto, builder);
        mapTilGrunnlagBuilder(dto, builder);

        // ta med registerdata til grunnlaget
        mapRegisterDataTilMigrering(dto, builder);

        return builder.build();
    }

    // brukes kun til migrering av data (dytter inn IAYG)
    private void mapRegisterDataTilMigrering(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        var register = dto.getRegister();
        if (register == null) {
            return;
        }

        var tidspunkt = register.getOpprettetTidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        var registerBuilder = InntektArbeidYtelseAggregatBuilder.builderFor(Optional.empty(), register.getEksternReferanse(), tidspunkt,
                VersjonType.REGISTER);

        var aktørArbeid = new MapAktørArbeid.MapFraDto(registerBuilder).map(register.getArbeid());
        var aktørInntekt = new MapAktørInntekt.MapFraDto(registerBuilder).map(register.getInntekt());
        var aktørYtelse = new MapAktørYtelse.MapFraDto(registerBuilder).map(register.getYtelse());

        aktørArbeid.forEach(registerBuilder::leggTilAktørArbeid);
        aktørInntekt.forEach(registerBuilder::leggTilAktørInntekt);
        aktørYtelse.forEach(registerBuilder::leggTilAktørYtelse);

        builder.medData(registerBuilder);
    }

    private void mapTilGrunnlagBuilder(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        var arbeidsforholdInformasjonBuilder = new MapArbeidsforholdInformasjon.MapFraDto(builder).map(dto.getArbeidsforholdInformasjon());
        var mapInntektsmeldinger = new MapInntektsmeldinger.MapFraDto();
        var inntektsmeldinger = mapInntektsmeldinger.map(arbeidsforholdInformasjonBuilder, dto.getInntektsmeldinger());

        var oppgittOpptjening = new MapOppgittOpptjening().mapFraDto(dto.getOppgittOpptjening());
        var overstyrtOppgittOpptjening = new MapOppgittOpptjening().mapFraDto(dto.getOverstyrtOppgittOpptjening());
        var arbeidsforholdInformasjon = arbeidsforholdInformasjonBuilder.build();

        builder.medOppgittOpptjening(oppgittOpptjening);
        builder.medOverstyrtOppgittOpptjening(overstyrtOppgittOpptjening);
        builder.setInntektsmeldinger(inntektsmeldinger);
        builder.medInformasjon(arbeidsforholdInformasjon);
    }

    private void mapSaksbehandlerDataTilBuilder(InntektArbeidYtelseGrunnlagDto dto, InntektArbeidYtelseGrunnlagBuilder builder) {
        var overstyrt = dto.getOverstyrt();
        if (overstyrt != null) {
            var tidspunkt = overstyrt.getOpprettetTidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            var saksbehandlerOverstyringer = InntektArbeidYtelseAggregatBuilder.builderFor(Optional.empty(), overstyrt.getEksternReferanse(),
                    tidspunkt, VersjonType.SAKSBEHANDLET);
            var overstyrtAktørArbeid = new MapAktørArbeid.MapFraDto(saksbehandlerOverstyringer).map(overstyrt.getArbeid());
            overstyrtAktørArbeid.forEach(saksbehandlerOverstyringer::leggTilAktørArbeid);
            builder.medData(saksbehandlerOverstyringer);
        }
    }
}
