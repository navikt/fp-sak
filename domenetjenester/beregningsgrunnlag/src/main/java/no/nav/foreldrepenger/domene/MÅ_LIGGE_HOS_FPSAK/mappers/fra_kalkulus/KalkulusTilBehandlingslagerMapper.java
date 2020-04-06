package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

/**
 * Skal etterhvert benytte seg av kontrakten som skal lages i ft-Kalkulus, benytter foreløping en, en-til-en mapping på klassenivå...
 *
 */
public class KalkulusTilBehandlingslagerMapper {

    public static BeregningsgrunnlagTilstand mapTilstand(no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return BeregningsgrunnlagTilstand.fraKode(beregningsgrunnlagTilstand.getKode());
    }

    public static BeregningsgrunnlagEntitet mapBeregningsgrunnlag(BeregningsgrunnlagDto beregningsgrunnlagFraKalkulus) {
        BeregningsgrunnlagEntitet.Builder builder = BeregningsgrunnlagEntitet.builder();

        //med
        builder.medGrunnbeløp(new Beløp(beregningsgrunnlagFraKalkulus.getGrunnbeløp().getVerdi()));
        builder.medOverstyring(beregningsgrunnlagFraKalkulus.isOverstyrt());
        if (beregningsgrunnlagFraKalkulus.getRegelinputPeriodisering() != null) {
            builder.medRegelinputPeriodisering(beregningsgrunnlagFraKalkulus.getRegelinputPeriodisering());
        }
        if (beregningsgrunnlagFraKalkulus.getRegelInputBrukersStatus() != null && beregningsgrunnlagFraKalkulus.getRegelloggBrukersStatus() != null) {
            builder.medRegelloggBrukersStatus(beregningsgrunnlagFraKalkulus.getRegelInputBrukersStatus(), beregningsgrunnlagFraKalkulus.getRegelloggBrukersStatus());
        }
        if (beregningsgrunnlagFraKalkulus.getRegelInputSkjæringstidspunkt() != null && beregningsgrunnlagFraKalkulus.getRegelloggSkjæringstidspunkt() != null) {
            builder.medRegelloggSkjæringstidspunkt(beregningsgrunnlagFraKalkulus.getRegelInputSkjæringstidspunkt(), beregningsgrunnlagFraKalkulus.getRegelloggSkjæringstidspunkt());
        }
        builder.medSkjæringstidspunkt(beregningsgrunnlagFraKalkulus.getSkjæringstidspunkt());
        if (beregningsgrunnlagFraKalkulus.getSammenligningsgrunnlag() != null) {
            builder.medSammenligningsgrunnlagOld(KalkulusTilBGMapper.mapSammenligningsgrunnlag(beregningsgrunnlagFraKalkulus.getSammenligningsgrunnlag()));
        }

        //lister
        beregningsgrunnlagFraKalkulus.getAktivitetStatuser().forEach(beregningsgrunnlagAktivitetStatus -> builder.leggTilAktivitetStatus(KalkulusTilBGMapper.mapAktivitetStatus(beregningsgrunnlagAktivitetStatus)));
        beregningsgrunnlagFraKalkulus.getBeregningsgrunnlagPerioder().forEach(beregningsgrunnlagPeriode -> builder.leggTilBeregningsgrunnlagPeriode(KalkulusTilBGMapper.mapBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)));
        builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagFraKalkulus.getFaktaOmBeregningTilfeller().stream().map(fakta -> FaktaOmBeregningTilfelle.fraKode(fakta.getKode())).collect(Collectors.toList()));
        beregningsgrunnlagFraKalkulus.getSammenligningsgrunnlagPrStatusListe().forEach(sammenligningsgrunnlagPrStatus -> builder.leggTilSammenligningsgrunnlag(KalkulusTilBGMapper.mapSammenligningsgrunnlagMedStatus(sammenligningsgrunnlagPrStatus)));

        return builder.build();
    }

    public static BeregningRefusjonOverstyringerEntitet mapRefusjonOverstyring(BeregningRefusjonOverstyringerDto refusjonOverstyringerFraKalkulus) {
        BeregningRefusjonOverstyringerEntitet.Builder entitetBuilder = BeregningRefusjonOverstyringerEntitet.builder();

        refusjonOverstyringerFraKalkulus.getRefusjonOverstyringer().forEach(beregningRefusjonOverstyring -> {
            BeregningRefusjonOverstyringEntitet entitet = BeregningRefusjonOverstyringEntitet.builder()
                .medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(beregningRefusjonOverstyring.getArbeidsgiver()))
                .medFørsteMuligeRefusjonFom(beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom())
                .build();
            entitetBuilder.leggTilOverstyring(entitet);
        });
        return entitetBuilder.build();
    }

    public static BeregningAktivitetAggregatEntitet mapSaksbehandletAktivitet(BeregningAktivitetAggregatDto saksbehandletAktiviteterFraKalkulus) {
        BeregningAktivitetAggregatEntitet.Builder entitetBuilder = BeregningAktivitetAggregatEntitet.builder();
        entitetBuilder.medSkjæringstidspunktOpptjening(saksbehandletAktiviteterFraKalkulus.getSkjæringstidspunktOpptjening());
        saksbehandletAktiviteterFraKalkulus.getBeregningAktiviteter().forEach(mapBeregningAktivitet(entitetBuilder));
        return entitetBuilder.build();
    }

    private static Consumer<BeregningAktivitetDto> mapBeregningAktivitet(BeregningAktivitetAggregatEntitet.Builder entitetBuilder) {
        return beregningAktivitet -> {
            BeregningAktivitetEntitet.Builder builder = BeregningAktivitetEntitet.builder();
            builder.medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef() == null ? null : KalkulusTilIAYMapper.mapArbeidsforholdRed(beregningAktivitet.getArbeidsforholdRef()));
            builder.medArbeidsgiver(beregningAktivitet.getArbeidsgiver() == null ? null : KalkulusTilIAYMapper.mapArbeidsgiver(beregningAktivitet.getArbeidsgiver()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitet.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(beregningAktivitet.getPeriode()));
            entitetBuilder.leggTilAktivitet(builder.build());
        };
    }

    public static BeregningAktivitetOverstyringerEntitet mapAktivitetOverstyring(BeregningAktivitetOverstyringerDto beregningAktivitetOverstyringerFraKalkulus) {
        BeregningAktivitetOverstyringerEntitet.Builder entitetBuilder = BeregningAktivitetOverstyringerEntitet.builder();
        beregningAktivitetOverstyringerFraKalkulus.getOverstyringer().forEach(overstyring -> {
            BeregningAktivitetOverstyringEntitet.Builder builder = BeregningAktivitetOverstyringEntitet.builder();
            builder.medArbeidsforholdRef(overstyring.getArbeidsforholdRef() == null ? null : KalkulusTilIAYMapper.mapArbeidsforholdRed(overstyring.getArbeidsforholdRef()));
            overstyring.getArbeidsgiver().ifPresent(arbeidsgiver -> builder.medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(arbeidsgiver)));
            builder.medHandling(overstyring.getHandling() == null ? null : BeregningAktivitetHandlingType.fraKode(overstyring.getHandling().getKode()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(overstyring.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(overstyring.getPeriode()));
            entitetBuilder.leggTilOverstyring(builder.build());
        });
        return entitetBuilder.build();
    }

    private static ÅpenDatoIntervallEntitet mapDatoIntervall(Intervall periode) {
        return ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    public static BeregningsgrunnlagGrunnlagEntitet mapGrunnlag(BeregningsgrunnlagGrunnlagDto beregningsgrunnlagFraKalkulus) {
        BeregningsgrunnlagGrunnlagBuilder oppdatere = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty());

        beregningsgrunnlagFraKalkulus.getBeregningsgrunnlag().ifPresent(beregningsgrunnlagDto -> oppdatere.medBeregningsgrunnlag(mapBeregningsgrunnlag(beregningsgrunnlagDto)));
        beregningsgrunnlagFraKalkulus.getOverstyring().ifPresent(beregningAktivitetOverstyringerDto -> oppdatere.medOverstyring(mapAktivitetOverstyring(beregningAktivitetOverstyringerDto)));
        oppdatere.medRegisterAktiviteter(mapRegisterAktiviteter(beregningsgrunnlagFraKalkulus.getRegisterAktiviteter()));
        beregningsgrunnlagFraKalkulus.getSaksbehandletAktiviteter().ifPresent(beregningAktivitetAggregatDto -> oppdatere.medSaksbehandletAktiviteter(mapSaksbehandletAktivitet(beregningAktivitetAggregatDto)));
        beregningsgrunnlagFraKalkulus.getRefusjonOverstyringer().ifPresent(beregningRefusjonOverstyringerDto -> oppdatere.medRefusjonOverstyring(mapRefusjonOverstyring(beregningRefusjonOverstyringerDto)));

        return oppdatere.buildUtenIdOgTilstand();
    }

    public static BeregningAktivitetAggregatEntitet mapRegisterAktiviteter(BeregningAktivitetAggregatDto registerAktiviteter) {
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder();
        registerAktiviteter.getBeregningAktiviteter().forEach(mapBeregningAktivitet(builder));
        builder.medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        return builder.build();
    }
}
