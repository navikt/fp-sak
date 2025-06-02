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
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.FASTSETT_INNTEKT_FOR_ARBEID_UNDER_AAP)
public class ArbeidUnderAAPHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private static final BigDecimal MÅNEDER_I_ET_ÅR = BigDecimal.valueOf(12);

    public ArbeidUnderAAPHistorikkTjeneste() {
        // For CDI
    }

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return lagHistorikkForFastsetting(dto, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkForFastsetting(FaktaBeregningLagreDto dto, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var fraInntekt = forrigeBg.map(this::hentForrigeFastsattInntekt).orElse(null);
        var tilInntekt = dto.getFastsettArbeidUnderAap().getFastsattPrMnd();
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        if (!Objects.equals(tilInntekt, fraInntekt)) {
            linjeBuilder.add(new HistorikkinnslagLinjeBuilder().fraTil("Inntekten", fraInntekt, tilInntekt));
            linjeBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        }
        return linjeBuilder;
    }

    private Integer hentForrigeFastsattInntekt(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var arbeidUnderAAPAndel = finnArbeidUnderAAPAndel(beregningsgrunnlag);
        if (arbeidUnderAAPAndel.getBeregnetPrÅr() == null) {
            return null;
        }
        return arbeidUnderAAPAndel.getBeregnetPrÅr().divide(MÅNEDER_I_ET_ÅR, 0, RoundingMode.HALF_UP).intValue();
    }

    private static BeregningsgrunnlagPrStatusOgAndel finnArbeidUnderAAPAndel(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        var arbeidUnderAAPAndeler = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .getFirst()
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bpsa -> bpsa.getArbeidsforholdType().equals(OpptjeningAktivitetType.ARBEID_UNDER_AAP))
            .toList();

        if (arbeidUnderAAPAndeler.size() != 1) {
            throw new IllegalStateException("Det skal være én andel med arbeid under AAP, antall funnet: " + arbeidUnderAAPAndeler.size());
        }
        return arbeidUnderAAPAndeler.getFirst();
    }
}
