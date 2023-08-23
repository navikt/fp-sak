package no.nav.foreldrepenger.domene.rest.historikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    public void lagHistorikk(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto,
                             AksjonspunktOppdaterParameter param) {
        oppdaterVedEndretVerdi(dto.getBruttoBeregningsgrunnlag());
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterVedEndretVerdi(Integer bruttoNæringsInntekt) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT, null, bruttoNæringsInntekt);
    }

}
