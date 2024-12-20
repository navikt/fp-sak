package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE)
public class EtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private static final BigDecimal MÅNEDER_I_ET_ÅR = BigDecimal.valueOf(12);

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return lagHistorikkForFastsetting(dto, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkForFastsetting(FaktaBeregningLagreDto dto,
                                                                          Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var fastsattPrMndForrige = forrigeBg.map(this::hentOpprinneligVerdiFastsattEtterlønnSluttpakke).orElse(null);
        var nyVerdiEtterlønnSLuttpakke = BigDecimal.valueOf(dto.getFastsettEtterlønnSluttpakke().getFastsattPrMnd());
        var opprinneligInntektInt = fastsattPrMndForrige == null ? null : fastsattPrMndForrige.intValue();
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        if (!Objects.equals(nyVerdiEtterlønnSLuttpakke.intValue(), opprinneligInntektInt)) {
            linjeBuilder.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Inntekten", opprinneligInntektInt, nyVerdiEtterlønnSLuttpakke.intValue()));
            linjeBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        }
        return linjeBuilder;
    }

    private BigDecimal hentOpprinneligVerdiFastsattEtterlønnSluttpakke(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var etterlønnSluttpakkeAndeler = finnEtterlønnSluttpakkeAndel(beregningsgrunnlag);
        if (etterlønnSluttpakkeAndeler.get(0).getBeregnetPrÅr() == null) {
            return null;
        }
        return etterlønnSluttpakkeAndeler.get(0).getBeregnetPrÅr().divide(MÅNEDER_I_ET_ÅR, 0, RoundingMode.HALF_UP);
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
