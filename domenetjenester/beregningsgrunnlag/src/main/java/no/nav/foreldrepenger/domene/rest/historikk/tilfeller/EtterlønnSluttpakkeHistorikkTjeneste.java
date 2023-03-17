package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE)
public class EtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private static final BigDecimal MÅNEDER_I_ET_ÅR = BigDecimal.valueOf(12);

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        lagHistorikkForFastsetting(dto, tekstBuilder, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
    }

    private void lagHistorikkForFastsetting(FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var fastettDto = dto.getFastsettEtterlønnSluttpakke();
        var fastsattPrMnd = fastettDto.getFastsattPrMnd();
        var fastsattPrMndForrige = forrigeBg.map(this::hentOpprinneligVerdiFastsattEtterlønnSluttpakke).orElse(null);
        lagHistorikkInnslagFastsattEtterlønnSluttpakke(BigDecimal.valueOf(fastsattPrMnd), fastsattPrMndForrige, tekstBuilder);
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

    private void lagHistorikkInnslagFastsattEtterlønnSluttpakke(BigDecimal nyVerdiEtterlønnSLuttpakke, BigDecimal opprinneligEtterlønnSluttpakkeInntekt,
                                                                HistorikkInnslagTekstBuilder tekstBuilder) {
        var opprinneligInntektInt = opprinneligEtterlønnSluttpakkeInntekt == null ? null : opprinneligEtterlønnSluttpakkeInntekt.intValue();
        oppdaterVedEndretVerdi(nyVerdiEtterlønnSLuttpakke, opprinneligInntektInt, tekstBuilder);
    }

    private void oppdaterVedEndretVerdi(BigDecimal nyVerdiEtterlønnSLuttpakke, Integer opprinneligEtterlønnSluttpakkeInntekt, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!Objects.equals(nyVerdiEtterlønnSLuttpakke.intValue(), opprinneligEtterlønnSluttpakkeInntekt)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE, opprinneligEtterlønnSluttpakkeInntekt, nyVerdiEtterlønnSLuttpakke.intValue());
        }
    }
}
