package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.oppdateringresultat.VarigEndretNæringVurdering;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;

public class UtledEndring {

    private UtledEndring() {
        // skjul
    }

    public static OppdaterBeregningsgrunnlagResultat utled(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagDto,
                                                           BeregningsgrunnlagGrunnlagEntitet grunnlagFraSteg,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           BekreftetAksjonspunktDto dto,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var beregningsgrunnlagDto = beregningsgrunnlagGrunnlagDto.getBeregningsgrunnlag()
            .orElseThrow(() -> new IllegalArgumentException("Skal ha beregningsgrunnlag her"));
        var bgFraSteg = grunnlagFraSteg.getBeregningsgrunnlag().orElseThrow(() -> new IllegalArgumentException("Skal ha beregningsgrunnlag her"));
        var forrigeBeregningsgrunnlagOpt = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        var beregningsgrunnlagEndring = utledBeregningsgrunnlagEndring(beregningsgrunnlagDto, bgFraSteg, forrigeBeregningsgrunnlagOpt);
        var aktivitetEndringer = UtledEndringIAktiviteter.utedEndring(dto, beregningsgrunnlagGrunnlagDto.getRegisterAktiviteter(),
            beregningsgrunnlagGrunnlagDto.getGjeldendeAktiviteter(), forrigeGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getRegisterAktiviteter),
            forrigeGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getGjeldendeAktiviteter));
        var faktaOmBeregningVurderinger = mapFaktaOmBeregningEndring(beregningsgrunnlagGrunnlagDto, forrigeGrunnlag, dto);
        var varigEndretEllerNyoppstartetNæringEndring = mapVarigEndretNæringEndring(forrigeGrunnlag, dto, beregningsgrunnlagDto,
            iayGrunnlag);
        var refusjonoverstyringEndring = UtledEndringForRefusjonOverstyring.utled(beregningsgrunnlagGrunnlagDto, forrigeGrunnlag, dto);
        return new OppdaterBeregningsgrunnlagResultat(beregningsgrunnlagEndring,
            aktivitetEndringer.orElse(null),
            faktaOmBeregningVurderinger.orElse(null),
            varigEndretEllerNyoppstartetNæringEndring.orElse(null),
            refusjonoverstyringEndring.orElse(null),
            null
            );
    }

    private static Optional<VarigEndretNæringVurdering> mapVarigEndretNæringEndring(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                                    BekreftetAksjonspunktDto dto,
                                                                                    BeregningsgrunnlagEntitet beregningsgrunnlagDto,
                                                                                    InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (dto instanceof VurderVarigEndringEllerNyoppstartetSNDto) {
            return Optional.of(UtledVarigEndringEllerNyoppstartetSNVurderinger.utled(beregningsgrunnlagDto,
                forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag), iayGrunnlag));
        }
        return Optional.empty();
    }

    private static Optional<FaktaOmBeregningVurderinger> mapFaktaOmBeregningEndring(BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                                                    Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                                    BekreftetAksjonspunktDto dto) {
        if (dto instanceof VurderFaktaOmBeregningDto) {
            return Optional.ofNullable(
                UtledFaktaOmBeregningVurderinger.utled((VurderFaktaOmBeregningDto) dto, grunnlag,
                    forrigeGrunnlag));
        }
        return Optional.empty();
    }

    private static BeregningsgrunnlagEndring utledBeregningsgrunnlagEndring(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet,
                                                                            BeregningsgrunnlagEntitet bgFraSteg,
                                                                            Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlagOpt) {
        var perioder = beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder();
        var perioderFraSteg = bgFraSteg.getBeregningsgrunnlagPerioder();
        var forrigePerioder = forrigeBeregningsgrunnlagOpt.map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        var beregningsgrunnlagPeriodeEndringer = utledPeriodeEndringer(perioder, perioderFraSteg, forrigePerioder);
        return beregningsgrunnlagPeriodeEndringer.isEmpty() ? null : new BeregningsgrunnlagEndring(beregningsgrunnlagPeriodeEndringer);
    }

    private static List<BeregningsgrunnlagPeriodeEndring> utledPeriodeEndringer(List<BeregningsgrunnlagPeriode> perioder,
                                                                                List<BeregningsgrunnlagPeriode> perioderFraSteg,
                                                                                List<BeregningsgrunnlagPeriode> forrigePerioder) {
        return perioder.stream().map(p -> {
            var periodeFraSteg = finnPeriode(perioderFraSteg, p.getBeregningsgrunnlagPeriodeFom()).orElseThrow(
                () -> new IllegalStateException("Skal ikke ha endring i periode fra steg"));
            var forrigePeriode = finnPeriode(forrigePerioder, p.getBeregningsgrunnlagPeriodeFom());
            return UtledEndringIPeriodeFraEntitet.utled(p, periodeFraSteg, forrigePeriode);
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    private static Optional<BeregningsgrunnlagPeriode> finnPeriode(List<BeregningsgrunnlagPeriode> forrigePerioder,
                                                                   LocalDate beregningsgrunnlagPeriodeFom) {
        return forrigePerioder.stream().filter(p -> p.getBeregningsgrunnlagPeriodeFom().equals(beregningsgrunnlagPeriodeFom)).findFirst();
    }
}
