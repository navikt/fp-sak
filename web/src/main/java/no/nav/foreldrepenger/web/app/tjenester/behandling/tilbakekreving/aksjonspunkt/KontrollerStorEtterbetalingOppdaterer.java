package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerStorEtterbetalingSøkerDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollerStorEtterbetalingOppdaterer implements AksjonspunktOppdaterer<KontrollerStorEtterbetalingSøkerDto> {

    private Historikkinnslag2Repository historikkinnslagRepository;

    public KontrollerStorEtterbetalingOppdaterer() {
        //CDI
    }

    @Inject
    public KontrollerStorEtterbetalingOppdaterer(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerStorEtterbetalingSøkerDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .addTekstlinje(fraTilEquals("Vurdert etterbetaling til søker", null, "Godkjent"))
            .addTekstlinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
        return OppdateringResultat.utenOverhopp();
    }

}


