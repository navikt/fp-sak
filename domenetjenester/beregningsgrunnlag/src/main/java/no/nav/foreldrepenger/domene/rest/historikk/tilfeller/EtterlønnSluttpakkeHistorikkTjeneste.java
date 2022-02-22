package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_ETTERLØNN_SLUTTPAKKE")
public class EtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        lagHistorikkForFastsetting(oppdaterResultat, tekstBuilder);
    }

    private void lagHistorikkForFastsetting(OppdaterBeregningsgrunnlagResultat oppdaterResultat, HistorikkInnslagTekstBuilder tekstBuilder) {
        var etterlønnSluttpakkeEndring = finnEndringForEtterlønnSluttpakke(oppdaterResultat);
        etterlønnSluttpakkeEndring.ifPresent(
            beløpEndring -> lagHistorikkInnslagFastsattEtterlønnSluttpakke(beløpEndring.getTilMånedsbeløp(), beløpEndring.getFraMånedsbeløp(),
                tekstBuilder));
    }

    private Optional<BeløpEndring> finnEndringForEtterlønnSluttpakke(OppdaterBeregningsgrunnlagResultat oppdaterResultat) {
        return oppdaterResultat.getBeregningsgrunnlagEndring()
            .flatMap(bgEndring -> bgEndring.getBeregningsgrunnlagPeriodeEndringer()
                .get(0)
                .getBeregningsgrunnlagPrStatusOgAndelEndringer()
                .stream()
                .filter(a -> a.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
                .findFirst()
                .flatMap(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring));
    }

    private void lagHistorikkInnslagFastsattEtterlønnSluttpakke(BigDecimal nyVerdiEtterlønnSLuttpakke,
                                                                BigDecimal opprinneligEtterlønnSluttpakkeInntekt,
                                                                HistorikkInnslagTekstBuilder tekstBuilder) {
        var opprinneligInntektInt = opprinneligEtterlønnSluttpakkeInntekt == null ? null : opprinneligEtterlønnSluttpakkeInntekt.intValue();
        oppdaterVedEndretVerdi(nyVerdiEtterlønnSLuttpakke, opprinneligInntektInt, tekstBuilder);
    }

    private void oppdaterVedEndretVerdi(BigDecimal nyVerdiEtterlønnSLuttpakke,
                                        Integer opprinneligEtterlønnSluttpakkeInntekt,
                                        HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!Objects.equals(nyVerdiEtterlønnSLuttpakke.intValue(), opprinneligEtterlønnSluttpakkeInntekt)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE, opprinneligEtterlønnSluttpakkeInntekt,
                nyVerdiEtterlønnSLuttpakke.intValue());
        }
    }
}
