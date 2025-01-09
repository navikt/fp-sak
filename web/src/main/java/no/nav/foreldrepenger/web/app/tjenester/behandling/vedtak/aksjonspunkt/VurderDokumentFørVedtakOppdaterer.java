package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereDokumentFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderDokumentFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereDokumentFørVedtakDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;

    VurderDokumentFørVedtakOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderDokumentFørVedtakOppdaterer(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurdereDokumentFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        lagHistorikkinnslag(param.getRef());
        return OppdateringResultat.utenOverhopp();
    }

    private void lagHistorikkinnslag(BehandlingReferanse behandlingReferanse) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel("Oppgave før vedtak")
            .addLinje("Vurder dokument")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
