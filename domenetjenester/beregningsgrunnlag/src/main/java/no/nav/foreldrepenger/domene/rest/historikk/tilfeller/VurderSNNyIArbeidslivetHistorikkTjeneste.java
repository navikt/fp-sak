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
@FaktaOmBeregningTilfelleRef("VURDER_SN_NY_I_ARBEIDSLIVET")
public class VurderSNNyIArbeidslivetHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        oppdaterResultat.getFaktaOmBeregningVurderinger()
            .flatMap(FaktaOmBeregningVurderinger::getErSelvstendingNyIArbeidslivetEndring)
            .ifPresent(e -> lagHistorikkInnslag(e, tekstBuilder));
    }

    private void lagHistorikkInnslag(ToggleEndring verdiEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterVedEndretVerdi(verdiEndring, tekstBuilder);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? HistorikkEndretFeltVerdiType.NY_I_ARBEIDSLIVET : HistorikkEndretFeltVerdiType.IKKE_NY_I_ARBEIDSLIVET;
    }

    private void oppdaterVedEndretVerdi(ToggleEndring verdiEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (verdiEndring.erEndring()) {
            var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(verdiEndring.getFraVerdi());
            var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(verdiEndring.getTilVerdi());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, opprinneligVerdi, nyVerdi);
        }
    }

}
