package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;

@ApplicationScoped
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste {

    private Historikkinnslag2Repository historikkinnslagRepository;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste(Historikkinnslag2Repository historikkRepository) {
        this.historikkinnslagRepository = historikkRepository;
    }

    public void lagHistorikk(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjeBuilderList = new ArrayList<>();
        HistorikkinnslagTekstlinjeBuilder tekstlinjeBuilder = new HistorikkinnslagTekstlinjeBuilder();
        tekstlinjeBuilderList.add(tekstlinjeBuilder.fraTil("Brutto næringsinntekt", null, dto.getBruttoBeregningsgrunnlag()));
        tekstlinjeBuilderList.add(tekstlinjeBuilder.linjeskift());
        tekstlinjeBuilderList.add(tekstlinjeBuilder.tekst(dto.getBegrunnelse()));

        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getRef().fagsakId())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .medTekstlinjer(tekstlinjeBuilderList)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
