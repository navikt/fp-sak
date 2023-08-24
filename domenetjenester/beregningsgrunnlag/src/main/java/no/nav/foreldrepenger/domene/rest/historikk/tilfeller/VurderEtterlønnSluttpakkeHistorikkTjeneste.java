package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderEtterlønnSluttpakkeDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE)
public class VurderEtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE)) {
            lagHistorikkForVurdering(dto, tekstBuilder, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        }
    }

    private void lagHistorikkForVurdering(FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var vurderDto = dto.getVurderEtterlønnSluttpakke();
        var opprinneligVerdiEtterlønnSLuttpakke = forrigeBg.flatMap(this::hentOpprinneligVerdiErEtterlønnSluttpakke);
        lagHistorikkinnslagVurderEtterlønnSluttpakke(vurderDto, opprinneligVerdiEtterlønnSLuttpakke.orElse(null), tekstBuilder);
    }

    private Optional<Boolean> hentOpprinneligVerdiErEtterlønnSluttpakke(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var etterlønnSluttpakkeAndeler = finnEtterlønnSluttpakkeAndel(beregningsgrunnlag);
        if (!etterlønnSluttpakkeAndeler.isEmpty()) {
            return Optional.of(etterlønnSluttpakkeAndeler.stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getFastsattAvSaksbehandler()) && a.getBeregnetPrÅr() != null
                    && a.getBeregnetPrÅr().compareTo(BigDecimal.ZERO) != 0));
        }
        return Optional.empty();
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
            .toList();
    }

}
