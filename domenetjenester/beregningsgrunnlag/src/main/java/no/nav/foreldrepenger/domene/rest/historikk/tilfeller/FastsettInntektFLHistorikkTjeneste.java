package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.MapTilLønnsendring;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_MAANEDSINNTEKT_FL")
public class FastsettInntektFLHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private InntektHistorikkTjeneste inntektHistorikkTjeneste;

    public FastsettInntektFLHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettInntektFLHistorikkTjeneste(InntektHistorikkTjeneste inntektHistorikkTjeneste) {
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var frilansEndring = oppdaterResultat.getBeregningsgrunnlagEndring()
            .stream()
            .flatMap(bgEndring -> bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0).getBeregningsgrunnlagPrStatusOgAndelEndringer().stream())
            .filter(a -> a.getAktivitetStatus().erFrilanser())
            .findFirst();
        var lønnsendringer = frilansEndring.map(MapTilLønnsendring::mapAndelEndringTilLønnsendring)
            .map(List::of).orElse(Collections.emptyList());
        inntektHistorikkTjeneste.lagHistorikk(tekstBuilder, lønnsendringer, iayGrunnlag);
    }
}
