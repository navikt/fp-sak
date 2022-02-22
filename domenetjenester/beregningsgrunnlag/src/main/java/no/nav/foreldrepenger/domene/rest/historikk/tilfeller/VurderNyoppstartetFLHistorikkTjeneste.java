package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_NYOPPSTARTET_FL")
public class VurderNyoppstartetFLHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        oppdaterResultat.getFaktaOmBeregningVurderinger()
            .flatMap(FaktaOmBeregningVurderinger::getErNyoppstartetFLEndring)
            .ifPresent(e -> lagHistorikkInnslag(e, tekstBuilder));
    }


    private void lagHistorikkInnslag(ToggleEndring verdiEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterVedEndretVerdi(verdiEndring, tekstBuilder);
    }

    private void oppdaterVedEndretVerdi(ToggleEndring verdiEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (verdiEndring.erEndring()) {
            var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(verdiEndring.getFraVerdi());
            var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(verdiEndring.getTilVerdi());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FRILANSVIRKSOMHET, opprinneligVerdi, nyVerdi);
        }
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }

}
