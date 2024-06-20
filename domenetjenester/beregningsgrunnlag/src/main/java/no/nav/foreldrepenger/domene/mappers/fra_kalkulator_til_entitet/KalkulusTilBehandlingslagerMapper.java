package no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAggregatDto;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingAggregat;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingGrunnlag;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingPeriode;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

/**
 * Skal etterhvert benytte seg av kontrakten som skal lages i ft-Kalkulus, benytter foreløping en, en-til-en mapping på klassenivå...
 */
public final class KalkulusTilBehandlingslagerMapper {

    private KalkulusTilBehandlingslagerMapper() {
    }

    public static BeregningsgrunnlagTilstand mapTilstand(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return BeregningsgrunnlagTilstand.fraKode(beregningsgrunnlagTilstand.getKode());
    }

    public static BeregningsgrunnlagEntitet mapBeregningsgrunnlag(BeregningsgrunnlagDto beregningsgrunnlagFraKalkulus,
                                                                  Optional<FaktaAggregatDto> faktaAggregat,
                                                                  Optional<RegelSporingAggregat> regelSporingAggregat) {
        var builder = BeregningsgrunnlagEntitet.ny();

        //med
        builder.medGrunnbeløp(new Beløp(beregningsgrunnlagFraKalkulus.getGrunnbeløp().verdi()));
        builder.medOverstyring(beregningsgrunnlagFraKalkulus.isOverstyrt());
        var regelSporingerGrunnlag = regelSporingAggregat.map(RegelSporingAggregat::regelsporingerGrunnlag)
            .orElse(Collections.emptyList());
        leggTilRegelsporing(regelSporingerGrunnlag, builder, BeregningsgrunnlagRegelType.PERIODISERING);
        leggTilRegelsporing(regelSporingerGrunnlag, builder, BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE);
        leggTilRegelsporing(regelSporingerGrunnlag, builder, BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON);
        leggTilRegelsporing(regelSporingerGrunnlag, builder, BeregningsgrunnlagRegelType.PERIODISERING_GRADERING);
        leggTilRegelsporing(regelSporingerGrunnlag, builder, BeregningsgrunnlagRegelType.BRUKERS_STATUS);
        leggTilRegelsporing(regelSporingerGrunnlag, builder, BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT);
        builder.medSkjæringstidspunkt(beregningsgrunnlagFraKalkulus.getSkjæringstidspunkt());

        //lister
        beregningsgrunnlagFraKalkulus.getAktivitetStatuser()
            .forEach(beregningsgrunnlagAktivitetStatus -> builder.leggTilAktivitetStatus(
                KalkulusTilBGMapper.mapAktivitetStatus(beregningsgrunnlagAktivitetStatus)));
        beregningsgrunnlagFraKalkulus.getBeregningsgrunnlagPerioder()
            .forEach(beregningsgrunnlagPeriode -> builder.leggTilBeregningsgrunnlagPeriode(
                KalkulusTilBGMapper.mapBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode, faktaAggregat,
                    finnRegelsporingerForPeriode(regelSporingAggregat, beregningsgrunnlagPeriode.getPeriode()))));
        builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagFraKalkulus.getFaktaOmBeregningTilfeller()
            .stream()
            .map(fakta -> FaktaOmBeregningTilfelle.fraKode(fakta.getKode()))
            .toList());
        beregningsgrunnlagFraKalkulus.getSammenligningsgrunnlagPrStatusListe()
            .forEach(sammenligningsgrunnlagPrStatus -> builder.leggTilSammenligningsgrunnlag(
                KalkulusTilBGMapper.mapSammenligningsgrunnlagMedStatus(sammenligningsgrunnlagPrStatus)));

        return builder.build();
    }

    private static List<RegelSporingPeriode> finnRegelsporingerForPeriode(Optional<RegelSporingAggregat> regelSporingAggregat,
                                                                          Intervall periode) {
        return regelSporingAggregat.stream()
            .flatMap(rs -> rs.regelsporingPerioder().stream())
            .filter(rs -> rs.periode().overlapper(periode))
            .toList();
    }

    private static void leggTilRegelsporing(List<RegelSporingGrunnlag> regelSporingerGrunnlag,
                                            BeregningsgrunnlagEntitet.Builder builder,
                                            BeregningsgrunnlagRegelType regelType) {
        var regelLogg = regelSporingerGrunnlag.stream()
            .filter(rs -> rs.regelType().getKode().equals(regelType.getKode()))
            .findFirst();
        regelLogg.ifPresent(regelSporingGrunnlag -> builder.medRegelSporing(regelSporingGrunnlag.regelInput(), regelSporingGrunnlag.regelEvaluering(),
            no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.fraKode(regelType.getKode()),
            Optional.ofNullable(regelSporingGrunnlag.regelVersjon()).map(v -> v.startsWith("f") ? v : "ft-beregning:" + v).orElse(null)));
    }

    public static BeregningRefusjonOverstyringerEntitet mapRefusjonOverstyring(BeregningRefusjonOverstyringerDto refusjonOverstyringerFraKalkulus) {
        var entitetBuilder = BeregningRefusjonOverstyringerEntitet.builder();

        refusjonOverstyringerFraKalkulus.getRefusjonOverstyringer().forEach(beregningRefusjonOverstyring -> {
            var builder = BeregningRefusjonOverstyringEntitet.builder()
                .medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(beregningRefusjonOverstyring.getArbeidsgiver()))
                .medErFristUtvidet(beregningRefusjonOverstyring.getErFristUtvidet().orElse(null))
                .medFørsteMuligeRefusjonFom(beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom().orElse(null));
            beregningRefusjonOverstyring.getRefusjonPerioder()
                .forEach(periode -> builder.leggTilRefusjonPeriode(mapRefusjonPeriode(periode)));
            entitetBuilder.leggTilOverstyring(builder.build());
        });
        return entitetBuilder.build();
    }

    private static BeregningRefusjonPeriodeEntitet mapRefusjonPeriode(BeregningRefusjonPeriodeDto refusjonsperiode) {
        return new BeregningRefusjonPeriodeEntitet(
            refusjonsperiode.getArbeidsforholdRef() == null ? null : KalkulusTilIAYMapper.mapArbeidsforholdRef(
                refusjonsperiode.getArbeidsforholdRef()), refusjonsperiode.getStartdatoRefusjon());
    }

    public static BeregningAktivitetAggregatEntitet mapSaksbehandletAktivitet(BeregningAktivitetAggregatDto saksbehandletAktiviteterFraKalkulus) {
        var entitetBuilder = BeregningAktivitetAggregatEntitet.builder();
        entitetBuilder.medSkjæringstidspunktOpptjening(
            saksbehandletAktiviteterFraKalkulus.getSkjæringstidspunktOpptjening());
        saksbehandletAktiviteterFraKalkulus.getBeregningAktiviteter().forEach(mapBeregningAktivitet(entitetBuilder));
        return entitetBuilder.build();
    }

    private static Consumer<BeregningAktivitetDto> mapBeregningAktivitet(BeregningAktivitetAggregatEntitet.Builder entitetBuilder) {
        return beregningAktivitet -> {
            var builder = BeregningAktivitetEntitet.builder();
            builder.medArbeidsforholdRef(
                beregningAktivitet.getArbeidsforholdRef() == null ? null : KalkulusTilIAYMapper.mapArbeidsforholdRef(
                    beregningAktivitet.getArbeidsforholdRef()));
            builder.medArbeidsgiver(
                beregningAktivitet.getArbeidsgiver() == null ? null : KalkulusTilIAYMapper.mapArbeidsgiver(
                    beregningAktivitet.getArbeidsgiver()));
            builder.medOpptjeningAktivitetType(
                OpptjeningAktivitetType.fraKode(beregningAktivitet.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(beregningAktivitet.getPeriode()));
            entitetBuilder.leggTilAktivitet(builder.build());
        };
    }

    public static BeregningAktivitetOverstyringerEntitet mapAktivitetOverstyring(BeregningAktivitetOverstyringerDto beregningAktivitetOverstyringerFraKalkulus) {
        var entitetBuilder = BeregningAktivitetOverstyringerEntitet.builder();
        beregningAktivitetOverstyringerFraKalkulus.getOverstyringer().forEach(overstyring -> {
            var builder = BeregningAktivitetOverstyringEntitet.builder();
            builder.medArbeidsforholdRef(
                overstyring.getArbeidsforholdRef() == null ? null : KalkulusTilIAYMapper.mapArbeidsforholdRef(
                    overstyring.getArbeidsforholdRef()));
            overstyring.getArbeidsgiver()
                .ifPresent(arbeidsgiver -> builder.medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(arbeidsgiver)));
            builder.medHandling(overstyring.getHandling() == null ? null : BeregningAktivitetHandlingType.fraKode(
                overstyring.getHandling().getKode()));
            builder.medOpptjeningAktivitetType(
                OpptjeningAktivitetType.fraKode(overstyring.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(overstyring.getPeriode()));
            entitetBuilder.leggTilOverstyring(builder.build());
        });
        return entitetBuilder.build();
    }

    private static ÅpenDatoIntervallEntitet mapDatoIntervall(Intervall periode) {
        return ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    public static BeregningsgrunnlagGrunnlagEntitet mapGrunnlag(BeregningsgrunnlagGrunnlagDto beregningsgrunnlagFraKalkulus,
                                                                Optional<RegelSporingAggregat> regelSporingAggregat) {
        var oppdatere = BeregningsgrunnlagGrunnlagBuilder.nytt();

        beregningsgrunnlagFraKalkulus.getBeregningsgrunnlagHvisFinnes()
            .ifPresent(beregningsgrunnlagDto -> oppdatere.medBeregningsgrunnlag(
                mapBeregningsgrunnlag(beregningsgrunnlagDto, beregningsgrunnlagFraKalkulus.getFaktaAggregat(),
                    regelSporingAggregat)));
        beregningsgrunnlagFraKalkulus.getOverstyring()
            .ifPresent(beregningAktivitetOverstyringerDto -> oppdatere.medOverstyring(
                mapAktivitetOverstyring(beregningAktivitetOverstyringerDto)));
        oppdatere.medRegisterAktiviteter(
            mapRegisterAktiviteter(beregningsgrunnlagFraKalkulus.getRegisterAktiviteter()));
        beregningsgrunnlagFraKalkulus.getSaksbehandletAktiviteter()
            .ifPresent(beregningAktivitetAggregatDto -> oppdatere.medSaksbehandletAktiviteter(
                mapSaksbehandletAktivitet(beregningAktivitetAggregatDto)));
        beregningsgrunnlagFraKalkulus.getRefusjonOverstyringer()
            .ifPresent(beregningRefusjonOverstyringerDto -> oppdatere.medRefusjonOverstyring(
                mapRefusjonOverstyring(beregningRefusjonOverstyringerDto)));

        return oppdatere.buildUtenIdOgTilstand();
    }

    public static BeregningAktivitetAggregatEntitet mapRegisterAktiviteter(BeregningAktivitetAggregatDto registerAktiviteter) {
        var builder = BeregningAktivitetAggregatEntitet.builder();
        registerAktiviteter.getBeregningAktiviteter().forEach(mapBeregningAktivitet(builder));
        builder.medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        return builder.build();
    }
}
