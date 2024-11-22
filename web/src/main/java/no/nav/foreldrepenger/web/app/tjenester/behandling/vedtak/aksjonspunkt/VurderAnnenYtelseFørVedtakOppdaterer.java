package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereAnnenYteleseFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderAnnenYtelseFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereAnnenYteleseFørVedtakDto> {

    private Historikkinnslag2Repository historikkinnslagRepository;

    VurderAnnenYtelseFørVedtakOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderAnnenYtelseFørVedtakOppdaterer(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurdereAnnenYteleseFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        lagHistorikkinnslag(param.getRef());
        return OppdateringResultat.utenOverhopp();
    }

    private void lagHistorikkinnslag(BehandlingReferanse behandlingReferanse) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel("Oppgave før vedtak")
            .addTekstlinje("Vurder konsekvens for ytelse")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
