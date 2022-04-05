package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderLønnsendringDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)
public class VurderLønnsendringHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (!dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)) {
            return;
        }
        var lønnsendringDto = dto.getVurdertLonnsendring();
        var opprinneligVerdiErLønnsendring = hentOpprinneligVerdiErLønnsendring(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        lagHistorikkinnslag(lønnsendringDto, opprinneligVerdiErLønnsendring, tekstBuilder);
    }


    private Boolean hentOpprinneligVerdiErLønnsendring(Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return forrigeBg.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden)
            .filter(aBoolean -> aBoolean != null)
            .findFirst()
            .orElse(null);
    }

    private void lagHistorikkinnslag(VurderLønnsendringDto dto, Boolean opprinneligVerdiErLønnsendring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!dto.erLønnsendringIBeregningsperioden().equals(opprinneligVerdiErLønnsendring)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.LØNNSENDRING_I_PERIODEN, opprinneligVerdiErLønnsendring, dto.erLønnsendringIBeregningsperioden());
        }
    }
}
