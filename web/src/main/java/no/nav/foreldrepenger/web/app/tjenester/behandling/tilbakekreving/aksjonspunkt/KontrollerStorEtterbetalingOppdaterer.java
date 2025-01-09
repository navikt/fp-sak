package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerStorEtterbetalingSøkerDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollerStorEtterbetalingOppdaterer implements AksjonspunktOppdaterer<KontrollerStorEtterbetalingSøkerDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;

    public KontrollerStorEtterbetalingOppdaterer() {
        //CDI
    }

    @Inject
    public KontrollerStorEtterbetalingOppdaterer(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerStorEtterbetalingSøkerDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .addLinje(fraTilEquals("Vurdert etterbetaling til søker", null, "Godkjent"))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
        return OppdateringResultat.utenOverhopp();
    }

}


