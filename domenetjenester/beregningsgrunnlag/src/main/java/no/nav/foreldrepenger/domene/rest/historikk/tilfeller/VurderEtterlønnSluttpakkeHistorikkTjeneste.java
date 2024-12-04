package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE)
public class VurderEtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {

        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        if (dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE)) {
            linjeBuilder.add(
                lagHistorikkinnslagVurderEtterlønnSluttpakke(dto, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)));
        }
        return linjeBuilder;
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkinnslagVurderEtterlønnSluttpakke(FaktaBeregningLagreDto dto,
                                                                                      Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var vurderDto = dto.getVurderEtterlønnSluttpakke();
        var opprinneligVerdiEtterlønnSLuttpakke = forrigeBg.flatMap(this::hentOpprinneligVerdiErEtterlønnSluttpakke).orElse(null);
        if (!vurderDto.erEtterlønnSluttpakke().equals(opprinneligVerdiEtterlønnSLuttpakke)) {
            return new HistorikkinnslagLinjeBuilder().fraTil("Inntekt fra etterlønn eller sluttpakke", opprinneligVerdiEtterlønnSLuttpakke,
                vurderDto.erEtterlønnSluttpakke());
        }
        return null;
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

    private List<BeregningsgrunnlagPrStatusOgAndel> finnEtterlønnSluttpakkeAndel(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .getFirst()
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bpsa -> bpsa.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
            .toList();
    }

}
