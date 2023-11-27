package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerStorEtterbetalingSøkerDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollerStorEtterbetalingOppdaterer implements AksjonspunktOppdaterer<KontrollerStorEtterbetalingSøkerDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    public KontrollerStorEtterbetalingOppdaterer() {
        //CDI
    }

    @Inject
    public KontrollerStorEtterbetalingOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerStorEtterbetalingSøkerDto dto, AksjonspunktOppdaterParameter param) {
        historikkTjenesteAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.VURDERT_ETTERBETALING_TIL_SØKER, null, "Godkjent")
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .medBegrunnelse(dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        return OppdateringResultat.utenOverhopp();
    }

}


