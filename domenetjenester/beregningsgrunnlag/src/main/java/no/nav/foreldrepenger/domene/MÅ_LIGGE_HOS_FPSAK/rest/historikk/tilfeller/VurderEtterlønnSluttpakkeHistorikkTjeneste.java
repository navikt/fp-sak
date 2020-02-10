package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderEtterlønnSluttpakkeDto;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_ETTERLØNN_SLUTTPAKKE")
public class VurderEtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE)) {
            lagHistorikkForVurdering(dto, tekstBuilder, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        }
    }

    private void lagHistorikkForVurdering(FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderEtterlønnSluttpakkeDto vurderDto = dto.getVurderEtterlønnSluttpakke();
        Boolean opprinneligVerdiEtterlønnSLuttpakke = forrigeBg.map(this::hentOpprinneligVerdiErEtterlønnSluttpakke).orElse(null);
        lagHistorikkinnslagVurderEtterlønnSluttpakke(vurderDto, opprinneligVerdiEtterlønnSLuttpakke, tekstBuilder);
    }

    private Boolean hentOpprinneligVerdiErEtterlønnSluttpakke(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        List<BeregningsgrunnlagPrStatusOgAndel> etterlønnSluttpakkeAndeler = finnEtterlønnSluttpakkeAndel(beregningsgrunnlag);
        if (!etterlønnSluttpakkeAndeler.isEmpty()) {
            return etterlønnSluttpakkeAndeler.stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getFastsattAvSaksbehandler()) && a.getBeregnetPrÅr() != null &&
                    a.getBeregnetPrÅr().compareTo(BigDecimal.ZERO)!=0);
        }
        return null; // NOSONAR
    }

    private void lagHistorikkinnslagVurderEtterlønnSluttpakke(VurderEtterlønnSluttpakkeDto dto, Boolean opprinneligVerdiErEtterlønnSluttpakke, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!dto.erEtterlønnSluttpakke().equals(opprinneligVerdiErEtterlønnSluttpakke)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.VURDER_ETTERLØNN_SLUTTPAKKE, opprinneligVerdiErEtterlønnSluttpakke, dto.erEtterlønnSluttpakke());
        }
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> finnEtterlønnSluttpakkeAndel(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bpsa -> bpsa.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
            .collect(Collectors.toList());
    }

}
