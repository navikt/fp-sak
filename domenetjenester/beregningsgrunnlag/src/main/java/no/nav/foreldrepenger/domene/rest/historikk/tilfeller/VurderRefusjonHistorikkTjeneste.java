package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT")
public class VurderRefusjonHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    VurderRefusjonHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        oppdaterResultat.getFaktaOmBeregningVurderinger()
            .stream()
            .flatMap(fv -> fv.getVurderRefusjonskravGyldighetEndringer().stream())
            .forEach(e -> lagHistorikkInnslag(e.getErGyldighetUtvidet(),
                arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(e.getArbeidsgiver(), iayGrunnlag.getArbeidsforholdOverstyringer()),
                tekstBuilder));
    }

    private void lagHistorikkInnslag(ToggleEndring verdiEndring, String arbeidsgivernavn, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (verdiEndring.erEndring()) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NY_REFUSJONSFRIST, arbeidsgivernavn, verdiEndring.getFraVerdi(),
                verdiEndring.getTilVerdi());
        }
    }

}
