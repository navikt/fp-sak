package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BGAndelArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagAktivitetStatusDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagPrStatusDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Beløp;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Utfall;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.SammenligningsgrunnlagPrStatus;


public class BGMapperTilKalkulus {

    private BGMapperTilKalkulus() {
    }

    public static List<SammenligningsgrunnlagPrStatusDto> mapGammeltTilNyttSammenligningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        var sammenligningsgrunnlag = beregningsgrunnlagEntitet.getSammenligningsgrunnlag();
        if (sammenligningsgrunnlag.isEmpty()) {
            return Collections.emptyList();
        }
        var gammeltSG = sammenligningsgrunnlag.get();
        var sammenligningsgrunnlagType = finnSGType(beregningsgrunnlagEntitet);
        return Collections.singletonList(SammenligningsgrunnlagPrStatusDto.builder()
            .medSammenligningsgrunnlagType(sammenligningsgrunnlagType)
            .medSammenligningsperiode(gammeltSG.getSammenligningsperiodeFom(), gammeltSG.getSammenligningsperiodeTom())
            .medAvvikPromilleNy(gammeltSG.getAvvikPromille())
            .medRapportertPrÅr(mapTilBeløp(gammeltSG.getRapportertPrÅr()))
            .build());

    }

    private static SammenligningsgrunnlagType finnSGType(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        if (beregningsgrunnlagEntitet.getAktivitetStatuser().stream().anyMatch(st -> st.getAktivitetStatus().erSelvstendigNæringsdrivende())) {
            return SammenligningsgrunnlagType.SAMMENLIGNING_SN;
        } else if (beregningsgrunnlagEntitet.getAktivitetStatuser().stream().anyMatch(st -> st.getAktivitetStatus().erFrilanser()
            || st.getAktivitetStatus().erArbeidstaker())) {
            return SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
        }
        throw new IllegalStateException("Klarte ikke utlede sammenligningstype for gammelt grunnlag. Aktivitetstatuser var " + beregningsgrunnlagEntitet.getAktivitetStatuser());
    }

    public static BeregningsgrunnlagAktivitetStatusDto.Builder mapAktivitetStatus(BeregningsgrunnlagAktivitetStatus fraFpsak) {
        var builder = new BeregningsgrunnlagAktivitetStatusDto.Builder();
        builder.medAktivitetStatus(KodeverkTilKalkulusMapper.mapAktivitetstatus(fraFpsak.getAktivitetStatus()));
        builder.medHjemmel(KodeverkTilKalkulusMapper.mapHjemmel(fraFpsak.getHjemmel()));

        return builder;
    }

    public static BeregningsgrunnlagPeriodeDto.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode fraFpsak) {
        var builder = new BeregningsgrunnlagPeriodeDto.Builder();

        //med
        builder.medAvkortetPrÅr(mapTilBeløp(fraFpsak.getAvkortetPrÅr()));
        builder.medBeregningsgrunnlagPeriode(fraFpsak.getBeregningsgrunnlagPeriodeFom(), fraFpsak.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(mapTilBeløp(fraFpsak.getBruttoPrÅr()));
        builder.medRedusertPrÅr(mapTilBeløp(fraFpsak.getRedusertPrÅr()));

        //legg til
        fraFpsak.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(KodeverkTilKalkulusMapper.mapPeriodeårsak(periodeÅrsak)));
        fraFpsak.getBeregningsgrunnlagPrStatusOgAndelList().forEach( statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel)));

        return builder;
    }

    public static SammenligningsgrunnlagPrStatusDto mapSammenligningsgrunnlagMedStatus(SammenligningsgrunnlagPrStatus fraFpsak) {
        var builder = new SammenligningsgrunnlagPrStatusDto.Builder();
        builder.medAvvikPromilleNy(fraFpsak.getAvvikPromille());
        builder.medRapportertPrÅr(mapTilBeløp(fraFpsak.getRapportertPrÅr()));
        builder.medSammenligningsgrunnlagType(KodeverkTilKalkulusMapper.mapSammenligningsgrunnlagtype(fraFpsak.getSammenligningsgrunnlagType()));
        builder.medSammenligningsperiode(fraFpsak.getSammenligningsperiodeFom(), fraFpsak.getSammenligningsperiodeTom());

        return builder.build();
    }

    private static BeregningsgrunnlagPrStatusOgAndelDto.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel fraFpsak) {
        var builder = BeregningsgrunnlagPrStatusOgAndelDto.ny()
            .medAktivitetStatus(KodeverkTilKalkulusMapper.mapAktivitetstatus(fraFpsak.getAktivitetStatus()))
            .medAndelsnr(fraFpsak.getAndelsnr())
            .medArbforholdType(fraFpsak.getArbeidsforholdType() == null ? null : KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(fraFpsak.getArbeidsforholdType()))
            .medAvkortetBrukersAndelPrÅr(mapTilBeløp(fraFpsak.getAvkortetBrukersAndelPrÅr()))
            .medAvkortetPrÅr(mapTilBeløp(fraFpsak.getAvkortetPrÅr()))
            .medAvkortetRefusjonPrÅr(mapTilBeløp(fraFpsak.getAvkortetRefusjonPrÅr()))
            .medBeregnetPrÅr(mapTilBeløp(fraFpsak.getBeregnetPrÅr()))
            .medBesteberegningPrÅr(mapTilBeløp(fraFpsak.getBesteberegningPrÅr()))
            .medFastsattAvSaksbehandler(fraFpsak.getFastsattAvSaksbehandler())
            .medOverstyrtPrÅr(mapTilBeløp(fraFpsak.getOverstyrtPrÅr()))
            .medFordeltPrÅr(mapTilBeløp(fraFpsak.getFordeltPrÅr()))
            .medManueltFordeltPrÅr(mapTilBeløp(fraFpsak.getManueltFordeltPrÅr()))
            .medRedusertPrÅr(mapTilBeløp(fraFpsak.getRedusertPrÅr()))
            .medRedusertBrukersAndelPrÅr(mapTilBeløp(fraFpsak.getRedusertBrukersAndelPrÅr()))
            .medMaksimalRefusjonPrÅr(mapTilBeløp(fraFpsak.getMaksimalRefusjonPrÅr()))
            .medRedusertRefusjonPrÅr(mapTilBeløp(fraFpsak.getRedusertRefusjonPrÅr()))
            .medÅrsbeløpFraTilstøtendeYtelse(mapTilBeløp(fraFpsak.getÅrsbeløpFraTilstøtendeYtelse()))
            .medInntektskategori(fraFpsak.getInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fraFpsak.getInntektskategori()))
            .medInntektskategoriAutomatiskFordeling(fraFpsak.getInntektskategoriAutomatiskFordeling() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fraFpsak.getInntektskategoriAutomatiskFordeling()))
            .medInntektskategoriManuellFordeling(fraFpsak.getInntektskategoriManuellFordeling() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fraFpsak.getInntektskategoriManuellFordeling()))
            .medKilde(KodeverkTilKalkulusMapper.mapAndelkilde(fraFpsak.getKilde()))
            .medOrginalDagsatsFraTilstøtendeYtelse(fraFpsak.getOrginalDagsatsFraTilstøtendeYtelse());


        if (fraFpsak.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraFpsak.getBeregningsperiodeFom(), fraFpsak.getBeregningsperiodeTom());
        }

        if (fraFpsak.getPgiSnitt() != null) {
            builder.medPgi(mapTilBeløp(fraFpsak.getPgiSnitt()), List.of(mapTilBeløp(fraFpsak.getPgi1()), mapTilBeløp(fraFpsak.getPgi2()), mapTilBeløp(fraFpsak.getPgi3())));
        }

        fraFpsak.getBgAndelArbeidsforhold().ifPresent(bgAndelArbeidsforhold -> builder.medBGAndelArbeidsforhold(BGMapperTilKalkulus.magBGAndelArbeidsforhold(bgAndelArbeidsforhold)));
        return builder;
    }

    private static BGAndelArbeidsforholdDto.Builder magBGAndelArbeidsforhold(BGAndelArbeidsforhold fraFpsak) {
        var builder = BGAndelArbeidsforholdDto.builder();
        builder.medArbeidsforholdRef(IAYMapperTilKalkulus.mapArbeidsforholdRef(fraFpsak.getArbeidsforholdRef()));
        builder.medArbeidsgiver(IAYMapperTilKalkulus.mapArbeidsgiver(fraFpsak.getArbeidsgiver()));
        builder.medArbeidsperiodeFom(fraFpsak.getArbeidsperiodeFom());
        builder.medRefusjonskravPrÅr(mapTilBeløp(fraFpsak.getRefusjonskravPrÅr()), Utfall.UDEFINERT);
        builder.medSaksbehandletRefusjonPrÅr(mapTilBeløp(fraFpsak.getSaksbehandletRefusjonPrÅr()));
        builder.medFordeltRefusjonPrÅr(mapTilBeløp(fraFpsak.getFordeltRefusjonPrÅr()));
        builder.medManueltFordeltRefusjonPrÅr(mapTilBeløp(fraFpsak.getManueltFordeltRefusjonPrÅr()));
        fraFpsak.getArbeidsperiodeTom().ifPresent(builder::medArbeidsperiodeTom);
        fraFpsak.getNaturalytelseBortfaltPrÅr().ifPresent(nat -> builder.medNaturalytelseBortfaltPrÅr(mapTilBeløp(nat)));
        fraFpsak.getNaturalytelseTilkommetPrÅr().ifPresent(nat -> builder.medNaturalytelseTilkommetPrÅr(mapTilBeløp(nat)));
        return builder;
    }

    private static Beløp mapTilBeløp(BigDecimal verdi) {
        return verdi == null ? null : new Beløp(verdi);
    }

    private static Beløp mapTilBeløp(no.nav.foreldrepenger.domene.typer.Beløp belp) {
        return belp == null || belp.getVerdi() == null ? null : new Beløp(belp.getVerdi());
    }

}
