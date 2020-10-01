package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningAktivitetHandlingType;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagRegelType;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelSporing;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class BehandlingslagerTilKalkulusMapper {


    public static BeregningsgrunnlagDto mapBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagFraFpsak) {
        BeregningsgrunnlagDto.Builder builder = BeregningsgrunnlagDto.builder();

        //med
        builder.medGrunnbeløp(beregningsgrunnlagFraFpsak.getGrunnbeløp().getVerdi());
        builder.medOverstyring(beregningsgrunnlagFraFpsak.isOverstyrt());
        leggTilSporingHvisFinnes(beregningsgrunnlagFraFpsak, builder, BeregningsgrunnlagRegelType.PERIODISERING);
        leggTilSporingHvisFinnes(beregningsgrunnlagFraFpsak, builder, BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE);
        leggTilSporingHvisFinnes(beregningsgrunnlagFraFpsak, builder, BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON);
        leggTilSporingHvisFinnes(beregningsgrunnlagFraFpsak, builder, BeregningsgrunnlagRegelType.BRUKERS_STATUS);
        leggTilSporingHvisFinnes(beregningsgrunnlagFraFpsak, builder, BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT);
        builder.medSkjæringstidspunkt(beregningsgrunnlagFraFpsak.getSkjæringstidspunkt());
        if (beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag() != null) {
            builder.medSammenligningsgrunnlag(BGMapperTilKalkulus.mapSammenligningsgrunnlag(beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag()));
        }

        //lister
        beregningsgrunnlagFraFpsak.getAktivitetStatuser().forEach(beregningsgrunnlagAktivitetStatus -> builder.leggTilAktivitetStatus(BGMapperTilKalkulus.mapAktivitetStatus(beregningsgrunnlagAktivitetStatus)));
        beregningsgrunnlagFraFpsak.getBeregningsgrunnlagPerioder().forEach(beregningsgrunnlagPeriode -> builder.leggTilBeregningsgrunnlagPeriode(BGMapperTilKalkulus.mapBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)));
        builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagFraFpsak.getFaktaOmBeregningTilfeller().stream().map(fakta -> FaktaOmBeregningTilfelle.fraKode(fakta.getKode())).collect(Collectors.toList()));
        beregningsgrunnlagFraFpsak.getSammenligningsgrunnlagPrStatusListe().forEach(sammenligningsgrunnlagPrStatus -> builder.leggTilSammenligningsgrunnlag(BGMapperTilKalkulus.mapSammenligningsgrunnlagMedStatus(sammenligningsgrunnlagPrStatus)));

        return builder.build();
    }

    private static void leggTilSporingHvisFinnes(BeregningsgrunnlagEntitet beregningsgrunnlagFraFpsak, BeregningsgrunnlagDto.Builder builder, BeregningsgrunnlagRegelType regelType) {
        if (beregningsgrunnlagFraFpsak.getRegelsporing(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType.fraKode(regelType.getKode())) != null) {
            BeregningsgrunnlagRegelSporing regelsporing = beregningsgrunnlagFraFpsak.getRegelsporing(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType.fraKode(regelType.getKode()));
            builder.medRegellogg(regelsporing.getRegelInput(), regelsporing.getRegelEvaluering(), regelType);
        }
    }

    public static BeregningRefusjonOverstyringerDto mapRefusjonOverstyring(BeregningRefusjonOverstyringerEntitet refusjonOverstyringerFraFpsak) {
        BeregningRefusjonOverstyringerDto.Builder dtoBuilder = BeregningRefusjonOverstyringerDto.builder();

        refusjonOverstyringerFraFpsak.getRefusjonOverstyringer().forEach(beregningRefusjonOverstyring -> {
            List<BeregningRefusjonPeriodeDto> refusjonsperioder = beregningRefusjonOverstyring.getRefusjonPerioder().stream()
                .map(BehandlingslagerTilKalkulusMapper::mapRefusjonperiode)
                .collect(Collectors.toList());
            BeregningRefusjonOverstyringDto dto = new BeregningRefusjonOverstyringDto(
                IAYMapperTilKalkulus.mapArbeidsgiver(beregningRefusjonOverstyring.getArbeidsgiver()),
                beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom().orElse(null),
                refusjonsperioder);
            dtoBuilder.leggTilOverstyring(dto);
        });
        return dtoBuilder.build();
    }

    public static BeregningRefusjonPeriodeDto mapRefusjonperiode(BeregningRefusjonPeriodeEntitet refusjonPeriodeEntitet) {
        return new BeregningRefusjonPeriodeDto(refusjonPeriodeEntitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(refusjonPeriodeEntitet.getArbeidsforholdRef()), refusjonPeriodeEntitet.getStartdatoRefusjon());
    }


    public static BeregningAktivitetAggregatDto mapSaksbehandletAktivitet(BeregningAktivitetAggregatEntitet saksbehandletAktiviteterFraFpsak) {
        BeregningAktivitetAggregatDto.Builder dtoBuilder = BeregningAktivitetAggregatDto.builder();
        dtoBuilder.medSkjæringstidspunktOpptjening(saksbehandletAktiviteterFraFpsak.getSkjæringstidspunktOpptjening());
        saksbehandletAktiviteterFraFpsak.getBeregningAktiviteter().forEach(mapAktivitet(dtoBuilder));
        return dtoBuilder.build();
    }

    private static Consumer<BeregningAktivitetEntitet> mapAktivitet(BeregningAktivitetAggregatDto.Builder dtoBuilder) {
        return beregningAktivitet -> {
            BeregningAktivitetDto.Builder builder = BeregningAktivitetDto.builder();
            builder.medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef()));
            builder.medArbeidsgiver(beregningAktivitet.getArbeidsgiver() == null ? null : IAYMapperTilKalkulus.mapArbeidsgiver(beregningAktivitet.getArbeidsgiver()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitet.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(beregningAktivitet.getPeriode()));
            dtoBuilder.leggTilAktivitet(builder.build());
        };
    }

    private static Intervall mapDatoIntervall(ÅpenDatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? Intervall.fraOgMed(periode.getFomDato()) : Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    public static BeregningAktivitetOverstyringerDto mapAktivitetOverstyring(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringerFraFpsak) {
        BeregningAktivitetOverstyringerDto.Builder dtoBuilder = BeregningAktivitetOverstyringerDto.builder();
        beregningAktivitetOverstyringerFraFpsak.getOverstyringer().forEach(overstyring -> {
            BeregningAktivitetOverstyringDto.Builder builder = BeregningAktivitetOverstyringDto.builder();
            builder.medArbeidsforholdRef(overstyring.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(overstyring.getArbeidsforholdRef()));
            overstyring.getArbeidsgiver().ifPresent(arbeidsgiver -> builder.medArbeidsgiver(IAYMapperTilKalkulus.mapArbeidsgiver(arbeidsgiver)));
            builder.medHandling(overstyring.getHandling() == null ? null : BeregningAktivitetHandlingType.fraKode(overstyring.getHandling().getKode()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(overstyring.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(overstyring.getPeriode()));
            dtoBuilder.leggTilOverstyring(builder.build());
        });
        return dtoBuilder.build();
    }

    public static BeregningsgrunnlagGrunnlagDto mapGrunnlag(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagFraFpsak) {
        BeregningsgrunnlagGrunnlagDtoBuilder oppdatere = BeregningsgrunnlagGrunnlagDtoBuilder.oppdatere(Optional.empty());

        beregningsgrunnlagFraFpsak.getBeregningsgrunnlag().ifPresent(beregningsgrunnlagDto -> oppdatere.medBeregningsgrunnlag(mapBeregningsgrunnlag(beregningsgrunnlagDto)));
        beregningsgrunnlagFraFpsak.getOverstyring().ifPresent(beregningAktivitetOverstyringerDto -> oppdatere.medOverstyring(mapAktivitetOverstyring(beregningAktivitetOverstyringerDto)));
        oppdatere.medRegisterAktiviteter(mapRegisterAktiviteter(beregningsgrunnlagFraFpsak.getRegisterAktiviteter()));
        beregningsgrunnlagFraFpsak.getSaksbehandletAktiviteter().ifPresent(beregningAktivitetAggregatDto -> oppdatere.medSaksbehandletAktiviteter(mapSaksbehandletAktivitet(beregningAktivitetAggregatDto)));
        beregningsgrunnlagFraFpsak.getRefusjonOverstyringer().ifPresent(beregningRefusjonOverstyringerDto -> oppdatere.medRefusjonOverstyring(mapRefusjonOverstyring(beregningRefusjonOverstyringerDto)));

        return oppdatere.build(no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand.fraKode(beregningsgrunnlagFraFpsak.getBeregningsgrunnlagTilstand().getKode()));
    }

    private static BeregningAktivitetAggregatDto mapRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        BeregningAktivitetAggregatDto.Builder builder = BeregningAktivitetAggregatDto.builder();
        builder.medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter().forEach(mapAktivitet(builder));
        return builder.build();
    }
}
