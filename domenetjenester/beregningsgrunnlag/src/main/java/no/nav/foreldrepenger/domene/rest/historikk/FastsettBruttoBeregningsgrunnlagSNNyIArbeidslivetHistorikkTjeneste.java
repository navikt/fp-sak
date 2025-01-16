package no.nav.foreldrepenger.domene.rest.historikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;

@ApplicationScoped
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste(HistorikkinnslagRepository historikkRepository) {
        this.historikkinnslagRepository = historikkRepository;
    }

    public void lagHistorikk(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getRef().fagsakId())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Brutto næringsinntekt", null, HistorikkBeløp.of(dto.getBruttoBeregningsgrunnlag())))
            .addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT)
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
