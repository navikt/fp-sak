package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import no.nav.foreldrepenger.domene.entiteter.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.AndelKilde;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BgAndelArbeidsforholdDto;

import java.math.BigDecimal;
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
        var bgPerioder = grunnlag.getBeregningsgrunnlag()
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
        var besteBeregningGrunnlag = grunnlag.getBeregningsgrunnlag().flatMap(BeregningsgrunnlagEntitet::getBesteberegninggrunnlag);

        // Manuelt besteberegnet
        var finnesBesteberegnetAndel = grunnlag.getBeregningsgrunnlag()
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
        return aktivitetStatuser.stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toList());
    }

    private BeregningsgrunnlagPeriodeDto mapPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        var andeler = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(this::mapAndel)
            .collect(Collectors.toList());
        var bruttoInkludertBortfaltNaturalytelsePrAar = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(null);
        return new BeregningsgrunnlagPeriodeDto(bgPeriode.getDagsats(),
            bgPeriode.getBruttoPrÅr(),
            bgPeriode.getAvkortetPrÅr() == null
                ? null
                : finnAvkortetUtenGraderingPrÅr(bruttoInkludertBortfaltNaturalytelsePrAar,
                grunnlag.getBeregningsgrunnlag().orElseThrow().getGrunnbeløp()),
            bgPeriode.getPeriodeÅrsaker(),
            bgPeriode.getBeregningsgrunnlagPeriodeFom(),
            bgPeriode.getBeregningsgrunnlagPeriodeTom(),
            andeler);
    }

    private BigDecimal finnAvkortetUtenGraderingPrÅr(BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar, Beløp grunnbeløp) {
        if (bruttoInkludertBortfaltNaturalytelsePrAar == null) {
            return null;
        }
        var seksG = grunnbeløp.multipliser(6).getVerdi();
        return bruttoInkludertBortfaltNaturalytelsePrAar.compareTo(seksG) > 0 ? seksG : bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    private BeregningsgrunnlagAndelDto mapAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        var arbeidsforholdDto = andel.getBgAndelArbeidsforhold().map(this::mapArbeidsforhold);
        return new BeregningsgrunnlagAndelDto(andel.getDagsats(),
            andel.getAktivitetStatus(),
            andel.getBruttoPrÅr(), andel.getAvkortetPrÅr(),
            andel.getNyIArbeidslivet(),
            andel.getArbeidsforholdType(),
            andel.getBeregningsperiodeFom(),
            andel.getBeregningsperiodeTom(),
            arbeidsforholdDto.orElse(null),
            erTilkommetAndel(andel.getKilde()));
    }

    private BgAndelArbeidsforholdDto mapArbeidsforhold(BGAndelArbeidsforhold bga) {
        // BGAndelArbeidsforhold skal alltid ha en arbeidsgiver satt
        return new BgAndelArbeidsforholdDto(bga.getArbeidsgiver().getIdentifikator(),
            bga.getArbeidsforholdRef().getReferanse(),
            bga.getNaturalytelseBortfaltPrÅr().orElse(null),
            bga.getNaturalytelseTilkommetPrÅr().orElse(null));
    }

    private boolean erTilkommetAndel(AndelKilde kilde) {
        return !kilde.equals(AndelKilde.PROSESS_START);
    }

}
