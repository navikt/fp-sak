package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAktørDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Beløp;
import no.nav.folketrygdloven.kalkulator.modell.typer.FaktaVurdering;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaVurderingKilde;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class BehandlingslagerTilKalkulusMapper {

    private BehandlingslagerTilKalkulusMapper() {
    }

    public static BeregningsgrunnlagDto mapBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagFraFpsak) {
        var builder = BeregningsgrunnlagDto.builder();

        //med
        builder.medGrunnbeløp(Beløp.fra(beregningsgrunnlagFraFpsak.getGrunnbeløp().getVerdi()));
        builder.medOverstyring(beregningsgrunnlagFraFpsak.isOverstyrt());
        builder.medSkjæringstidspunkt(beregningsgrunnlagFraFpsak.getSkjæringstidspunkt());

        if (beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag().isPresent() && beregningsgrunnlagFraFpsak.getSammenligningsgrunnlagPrStatusListe().isEmpty()) {
            // Intill alle grunnlag er migrert til nytt sammenligningsgrunnlag må denne være her
            BGMapperTilKalkulus.mapGammeltTilNyttSammenligningsgrunnlag(beregningsgrunnlagFraFpsak).
                forEach(builder::leggTilSammenligningsgrunnlag);
        }

        //lister
        beregningsgrunnlagFraFpsak.getAktivitetStatuser().forEach(beregningsgrunnlagAktivitetStatus -> builder.leggTilAktivitetStatus(BGMapperTilKalkulus.mapAktivitetStatus(beregningsgrunnlagAktivitetStatus)));
        beregningsgrunnlagFraFpsak.getBeregningsgrunnlagPerioder().forEach(beregningsgrunnlagPeriode -> builder.leggTilBeregningsgrunnlagPeriode(BGMapperTilKalkulus.mapBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)));
        builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagFraFpsak.getFaktaOmBeregningTilfeller().stream().map(KodeverkTilKalkulusMapper::mapFaktaBeregningTilfelle).toList());
        beregningsgrunnlagFraFpsak.getSammenligningsgrunnlagPrStatusListe().forEach(sammenligningsgrunnlagPrStatus -> builder.leggTilSammenligningsgrunnlag(BGMapperTilKalkulus.mapSammenligningsgrunnlagMedStatus(sammenligningsgrunnlagPrStatus)));

        return builder.build();
    }

    public static BeregningRefusjonOverstyringerDto mapRefusjonOverstyring(BeregningRefusjonOverstyringerEntitet refusjonOverstyringerFraFpsak) {
        var dtoBuilder = BeregningRefusjonOverstyringerDto.builder();

        refusjonOverstyringerFraFpsak.getRefusjonOverstyringer().forEach(beregningRefusjonOverstyring -> {
            var refusjonsperioder = beregningRefusjonOverstyring.getRefusjonPerioder().stream()
                .map(BehandlingslagerTilKalkulusMapper::mapRefusjonperiode)
                .toList();
            var dto = new BeregningRefusjonOverstyringDto(
                IAYMapperTilKalkulus.mapArbeidsgiver(beregningRefusjonOverstyring.getArbeidsgiver()),
                beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom().orElse(null),
                refusjonsperioder,
                beregningRefusjonOverstyring.getErFristUtvidet());
            dtoBuilder.leggTilOverstyring(dto);
        });
        return dtoBuilder.build();
    }

    public static BeregningRefusjonPeriodeDto mapRefusjonperiode(BeregningRefusjonPeriodeEntitet refusjonPeriodeEntitet) {
        return new BeregningRefusjonPeriodeDto(refusjonPeriodeEntitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(refusjonPeriodeEntitet.getArbeidsforholdRef()), refusjonPeriodeEntitet.getStartdatoRefusjon());
    }


    public static BeregningAktivitetAggregatDto mapSaksbehandletAktivitet(BeregningAktivitetAggregatEntitet saksbehandletAktiviteterFraFpsak) {
        var dtoBuilder = BeregningAktivitetAggregatDto.builder();
        dtoBuilder.medSkjæringstidspunktOpptjening(saksbehandletAktiviteterFraFpsak.getSkjæringstidspunktOpptjening());
        saksbehandletAktiviteterFraFpsak.getBeregningAktiviteter().forEach(mapAktivitet(dtoBuilder));
        return dtoBuilder.build();
    }

    private static Consumer<BeregningAktivitetEntitet> mapAktivitet(BeregningAktivitetAggregatDto.Builder dtoBuilder) {
        return beregningAktivitet -> {
            var builder = BeregningAktivitetDto.builder();
            builder.medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef()));
            builder.medArbeidsgiver(beregningAktivitet.getArbeidsgiver() == null ? null : IAYMapperTilKalkulus.mapArbeidsgiver(beregningAktivitet.getArbeidsgiver()));
            builder.medOpptjeningAktivitetType(KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(beregningAktivitet.getOpptjeningAktivitetType()));
            builder.medPeriode(mapDatoIntervall(beregningAktivitet.getPeriode()));
            dtoBuilder.leggTilAktivitet(builder.build());
        };
    }

    private static Intervall mapDatoIntervall(ÅpenDatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? Intervall.fraOgMed(periode.getFomDato()) : Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    public static BeregningAktivitetOverstyringerDto mapAktivitetOverstyring(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringerFraFpsak) {
        var dtoBuilder = BeregningAktivitetOverstyringerDto.builder();
        beregningAktivitetOverstyringerFraFpsak.getOverstyringer().forEach(overstyring -> {
            var builder = BeregningAktivitetOverstyringDto.builder();
            builder.medArbeidsforholdRef(overstyring.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(overstyring.getArbeidsforholdRef()));
            overstyring.getArbeidsgiver().ifPresent(arbeidsgiver -> builder.medArbeidsgiver(IAYMapperTilKalkulus.mapArbeidsgiver(arbeidsgiver)));
            builder.medHandling(overstyring.getHandling() == null ? null : KodeverkTilKalkulusMapper.mapBeregningAktivitetHandling(overstyring.getHandling()));
            builder.medOpptjeningAktivitetType(KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(overstyring.getOpptjeningAktivitetType()));
            builder.medPeriode(mapDatoIntervall(overstyring.getPeriode()));
            dtoBuilder.leggTilOverstyring(builder.build());
        });
        return dtoBuilder.build();
    }

    public static BeregningsgrunnlagGrunnlagDto mapGrunnlag(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagFraFpsak) {
        var oppdatere = BeregningsgrunnlagGrunnlagDtoBuilder.oppdatere(Optional.empty());

        beregningsgrunnlagFraFpsak.getBeregningsgrunnlag().ifPresent(beregningsgrunnlagDto -> oppdatere.medBeregningsgrunnlag(mapBeregningsgrunnlag(beregningsgrunnlagDto)));
        beregningsgrunnlagFraFpsak.getOverstyring().ifPresent(beregningAktivitetOverstyringerDto -> oppdatere.medOverstyring(mapAktivitetOverstyring(beregningAktivitetOverstyringerDto)));
        oppdatere.medRegisterAktiviteter(mapRegisterAktiviteter(beregningsgrunnlagFraFpsak.getRegisterAktiviteter()));
        beregningsgrunnlagFraFpsak.getSaksbehandletAktiviteter().ifPresent(beregningAktivitetAggregatDto -> oppdatere.medSaksbehandletAktiviteter(mapSaksbehandletAktivitet(beregningAktivitetAggregatDto)));
        beregningsgrunnlagFraFpsak.getRefusjonOverstyringer().ifPresent(beregningRefusjonOverstyringerDto -> oppdatere.medRefusjonOverstyring(mapRefusjonOverstyring(beregningRefusjonOverstyringerDto)));
        beregningsgrunnlagFraFpsak.getBeregningsgrunnlag().flatMap(BehandlingslagerTilKalkulusMapper::mapFaktaAggregat).ifPresent(oppdatere::medFaktaAggregat);
        return oppdatere.build(KodeverkTilKalkulusMapper.mapBeregningsgrunnlagTilstand(beregningsgrunnlagFraFpsak.getBeregningsgrunnlagTilstand()));
    }

    private static BeregningAktivitetAggregatDto mapRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        var builder = BeregningAktivitetAggregatDto.builder();
        builder.medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter().forEach(mapAktivitet(builder));
        return builder.build();
    }

    public static Optional<FaktaAggregatDto> mapFaktaAggregat(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        // I fakta om beregning settes alle faktaavklaringer på første periode og vi kan derfor bruke denne til å hente ut avklart fakta
        // Enkelte eldre grunnlag har ikke perioder (f.eks i OPPRETTET tilstand)
        if (beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder().isEmpty()) {
            return Optional.empty();
        }
        var førstePeriode = beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder().get(0);
        var andeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        var faktaAggregatBuilder = FaktaAggregatDto.builder();
        mapFaktaArbeidsforhold(andeler).forEach(faktaAggregatBuilder::erstattEksisterendeEllerLeggTil);
        mapFaktaAktør(andeler, beregningsgrunnlagEntitet.getFaktaOmBeregningTilfeller())
            .ifPresent(faktaAggregatBuilder::medFaktaAktør);
        return faktaAggregatBuilder.manglerFakta() ? Optional.empty() : Optional.of(faktaAggregatBuilder.build());
    }

    private static List<FaktaArbeidsforholdDto> mapFaktaArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> andeler) {
        return andeler.stream()
            .filter(a -> a.getAktivitetStatus().erArbeidstaker())
            .filter(a -> a.getArbeidsgiver().isPresent())
            .map(a -> {
                var builder = new FaktaArbeidsforholdDto.Builder(IAYMapperTilKalkulus.mapArbeidsgiver(a.getArbeidsgiver().get()),
                    a.getArbeidsforholdRef().map(IAYMapperTilKalkulus::mapArbeidsforholdRef).orElse(InternArbeidsforholdRefDto.nullRef()));
                a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold).ifPresent(fakta -> builder.medErTidsbegrenset(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
                a.mottarYtelse().ifPresent(fakta -> builder.medHarMottattYtelse(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
                a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden).ifPresent(fakta -> builder.medHarLønnsendringIBeregningsperioden(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
                return builder.manglerFakta() ? null : builder.build();
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static Optional<FaktaAktørDto> mapFaktaAktør(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        var faktaAktørBuilder = FaktaAktørDto.builder();
        mapEtterlønnSluttpakke(faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapMottarFLYtelse(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyIArbeidslivetSN(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapSkalBesteberegnes(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyoppstartetFL(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        return faktaAktørBuilder.erUgyldig() ? Optional.empty() : Optional.of(faktaAktørBuilder.build());
    }

    private static void mapErNyoppstartetFL(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                            List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                            FaktaAktørDto.Builder faktaAktørBuilder) {
        var harVurdertNyoppstartetFL = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL));
        if (harVurdertNyoppstartetFL) {
            andeler.stream().filter(a -> a.getAktivitetStatus().erFrilanser() && a.erNyoppstartet().isPresent())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::erNyoppstartet)
                .map(Optional::get)
                .ifPresent(fakta -> faktaAktørBuilder.medErNyoppstartetFL(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
        }
    }

    private static void mapSkalBesteberegnes(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        var harVurdertBesteberegning = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));
        if (harVurdertBesteberegning) {
            var harFastsattBesteberegning = andeler.stream().anyMatch(a -> a.getBesteberegningPrÅr() != null);
            faktaAktørBuilder.medSkalBesteberegnes(new FaktaVurdering(harFastsattBesteberegning, FaktaVurderingKilde.SAKSBEHANDLER));
        }
    }

    private static void mapErNyIArbeidslivetSN(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        var harVurdertNyIArbeidslivetSN = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET));
        if (harVurdertNyIArbeidslivetSN) {
            andeler.stream().filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
                .ifPresent(fakta -> faktaAktørBuilder.medErNyIArbeidslivetSN(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
        }
    }

    private static void mapMottarFLYtelse(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        var harVurdertMottarYtelse = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE));
        if (harVurdertMottarYtelse) {
            andeler.stream().filter(a -> a.getAktivitetStatus().erFrilanser() && a.mottarYtelse().isPresent())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::mottarYtelse)
                .map(Optional::get)
                .ifPresent(fakta -> faktaAktørBuilder.medHarFLMottattYtelse(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
        }
    }

    private static void mapEtterlønnSluttpakke(List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        var harVurdertEtterlønnSluttpakke = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE));
        var harEtterlønnSlutpakke = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE));
        if (harVurdertEtterlønnSluttpakke) {
            faktaAktørBuilder.medMottarEtterlønnSluttpakke(new FaktaVurdering(harEtterlønnSlutpakke, FaktaVurderingKilde.SAKSBEHANDLER));
        }
    }

}
