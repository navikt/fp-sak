package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;


@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_BESTEBEREGNING")
public class VurderBesteberegningHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        // TODO: Inkluder valg om besteberegning i endringresultat for å finne riktig fra-verdi
        lagBesteberegningHistorikk(dto, tekstBuilder);
    }

    private void lagBesteberegningHistorikk(FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder) {
        var tilVerdi = finnTilVerdi(dto);
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FORDELING_ETTER_BESTEBEREGNING, null, tilVerdi);
    }

    private boolean finnTilVerdi(FaktaBeregningLagreDto dto) {
        var besteberegningDto = dto.getBesteberegningAndeler();
        if (besteberegningDto != null) {
            var andelListe = besteberegningDto.getBesteberegningAndelListe();
            return !andelListe.isEmpty();
        }
        var kunYtelseDto = dto.getKunYtelseFordeling();
        if (kunYtelseDto != null) {
            return kunYtelseDto.getSkalBrukeBesteberegning();
        }
        return false;
    }


}
