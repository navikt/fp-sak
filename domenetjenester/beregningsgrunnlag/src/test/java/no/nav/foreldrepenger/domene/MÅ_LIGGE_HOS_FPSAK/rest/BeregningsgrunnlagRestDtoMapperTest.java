package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest;

import no.nav.folketrygdloven.kalkulator.kontrakt.v1.ArbeidsgiverOpplysningerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPeriodeRestDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.modell.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BeregningsgrunnlagRestDtoMapperTest {

    @Test
    public void skal_sette_korrekt_dagsats() {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(LocalDate.now(), DatoIntervallEntitet.TIDENES_ENDE).build(bg);
        no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver AG = no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.person(new AktørId("9999999999999"));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medAvkortetPrÅr(BigDecimal.valueOf(336000))
            .medBeregnetPrÅr(BigDecimal.valueOf(336000))
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(336000))
            .medAvkortetBrukersAndelPrÅr(BigDecimal.ZERO)
            .medRedusertPrÅr(BigDecimal.valueOf(336000))
            .medRedusertBrukersAndelPrÅr(BigDecimal.ZERO)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(AG))
            .build(periode);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAvkortetPrÅr(BigDecimal.valueOf(188011))
            .medBeregnetPrÅr(BigDecimal.valueOf(188011))
            .medOverstyrtPrÅr(BigDecimal.valueOf(188011))
            .medAvkortetRefusjonPrÅr(BigDecimal.ZERO)
            .medRedusertRefusjonPrÅr(BigDecimal.ZERO)
            .medRedusertPrÅr(BigDecimal.valueOf(188011))
            .medAvkortetBrukersAndelPrÅr(BigDecimal.valueOf(188011))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(188011))
            .build(periode);
        Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger = new HashMap<>();
        InntektsmeldingDtoBuilder builder = InntektsmeldingDtoBuilder.builder();
        builder.medArbeidsgiver(Arbeidsgiver.person(new no.nav.folketrygdloven.kalkulator.modell.typer.AktørId("9999999999999")));
        builder.medArbeidsforholdId(InternArbeidsforholdRefDto.nullRef());
        builder.medRefusjon(BigDecimal.valueOf(30_000));
        builder.medBeløp(BigDecimal.valueOf(30_000));
        InntektsmeldingDto im = builder.build(true);


        BeregningsgrunnlagPeriodeRestDto.Builder p = BeregningsgrunnlagRestDtoMapper.mapBeregningsgrunnlagPeriode(periode, arbeidsgiverOpplysninger, Collections.singletonList(im));

        BeregningsgrunnlagPeriodeRestDto dto = p.buildForKopi();

        assertThat(dto.getDagsats()).isEqualTo(2015);
    }



}
