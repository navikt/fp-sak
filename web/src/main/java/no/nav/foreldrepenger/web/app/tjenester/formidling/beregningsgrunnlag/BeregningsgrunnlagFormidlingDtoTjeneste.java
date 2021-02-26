package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BgAndelArbeidsforholdDto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class BeregningsgrunnlagFormidlingDtoTjeneste {

    private final BeregningsgrunnlagGrunnlagEntitet grunnlag;

    public BeregningsgrunnlagFormidlingDtoTjeneste(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        this.grunnlag = Objects.requireNonNull(grunnlag, "beregningsgrunnlaggrunnlag");
    }

    public Optional<BeregningsgrunnlagDto> map() {
        List<BeregningsgrunnlagPeriodeDto> bgPerioder = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .map(this::mapPeriode)
            .collect(Collectors.toList());
        return grunnlag.getBeregningsgrunnlag().map(bg -> new BeregningsgrunnlagDto(mapAktivitetstatuser(bg.getAktivitetStatuser()),
            bg.getHjemmel(),
            bg.getGrunnbeløp().getVerdi(),
            bgPerioder,
            utledBesteberegning()));
    }

    private boolean utledBesteberegning() {
        // Automatisk besteberegnet
        Optional<BesteberegninggrunnlagEntitet> besteBeregningGrunnlag = grunnlag.getBeregningsgrunnlag().flatMap(BeregningsgrunnlagEntitet::getBesteberegninggrunnlag);

        // Manuelt besteberegnet
        boolean finnesBesteberegnetAndel = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(bgp -> finnesBesteberegnetAndel(bgp.getBeregningsgrunnlagPrStatusOgAndelList()));

        return besteBeregningGrunnlag.isPresent() || finnesBesteberegnetAndel;
    }

    private boolean finnesBesteberegnetAndel(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream().anyMatch(bga -> bga.getBesteberegningPrÅr() != null);
    }

    private List<AktivitetStatus> mapAktivitetstatuser(List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        return aktivitetStatuser.stream().map(as -> as.getAktivitetStatus()).collect(Collectors.toList());
    }

    private BeregningsgrunnlagPeriodeDto mapPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        List<BeregningsgrunnlagAndelDto> andeler = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(this::mapAndel)
            .collect(Collectors.toList());
        return new BeregningsgrunnlagPeriodeDto(bgPeriode.getDagsats(),
            bgPeriode.getBruttoPrÅr(),
            bgPeriode.getAvkortetPrÅr(),
            bgPeriode.getPeriodeÅrsaker(),
            bgPeriode.getBeregningsgrunnlagPeriodeFom(),
            bgPeriode.getBeregningsgrunnlagPeriodeTom(),
            andeler);
    }

    private BeregningsgrunnlagAndelDto mapAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        Optional<BgAndelArbeidsforholdDto> arbeidsforholdDto = andel.getBgAndelArbeidsforhold().map(this::mapArbeidsforhold);
        return new BeregningsgrunnlagAndelDto(andel.getDagsats(),
            andel.getAktivitetStatus(),
            andel.getBruttoPrÅr(), andel.getAvkortetPrÅr(),
            andel.getNyIArbeidslivet(),
            andel.getArbeidsforholdType(),
            andel.getBeregningsperiodeFom(),
            andel.getBeregningsperiodeFom(),
            arbeidsforholdDto.orElse(null));
    }

    private BgAndelArbeidsforholdDto mapArbeidsforhold(BGAndelArbeidsforhold bga) {
        // BGAndelArbeidsforhold skal alltid ha en arbeidsgiver satt
        return new BgAndelArbeidsforholdDto(bga.getArbeidsgiver().getIdentifikator(),
            bga.getArbeidsforholdRef().getReferanse(),
            bga.getNaturalytelseBortfaltPrÅr().orElse(null),
            bga.getNaturalytelseTilkommetPrÅr().orElse(null));
    }
}
