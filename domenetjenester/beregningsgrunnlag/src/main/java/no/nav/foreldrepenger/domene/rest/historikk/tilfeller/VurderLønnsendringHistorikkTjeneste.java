package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderLønnsendringDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)
public class VurderLønnsendringHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();
        if (!dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)) {
            return linjerBuilder;
        }
        var lønnsendringDto = dto.getVurdertLonnsendring();
        var opprinneligVerdiErLønnsendring = hentOpprinneligVerdiErLønnsendring(
            forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        linjerBuilder.add(lagHistorikkinnslag(lønnsendringDto, opprinneligVerdiErLønnsendring));

        return linjerBuilder;
    }


    private Boolean hentOpprinneligVerdiErLønnsendring(Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return forrigeBg.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkinnslag(VurderLønnsendringDto dto, Boolean opprinneligVerdiErLønnsendring) {
        if (!dto.erLønnsendringIBeregningsperioden().equals(opprinneligVerdiErLønnsendring)) {
            return new HistorikkinnslagLinjeBuilder().fraTil("Lønnsendring siste tre måneder", opprinneligVerdiErLønnsendring,
                dto.erLønnsendringIBeregningsperioden());
        }
        return null;
    }
}
