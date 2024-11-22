package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

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
@DtoTilServiceAdapter(dto = KontrollerRevurderingsBehandlingDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollerRevurderingsBehandlingOppdaterer implements AksjonspunktOppdaterer<KontrollerRevurderingsBehandlingDto> {

    private Historikkinnslag2Repository historikkinnslagRepository;

    KontrollerRevurderingsBehandlingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KontrollerRevurderingsBehandlingOppdaterer(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerRevurderingsBehandlingDto dto, AksjonspunktOppdaterParameter param) {
        lagHistorikkinnslag(param.getRef());
        return OppdateringResultat.utenOverhopp();
    }

    private void lagHistorikkinnslag(BehandlingReferanse ref) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel("Oppgave før vedtak")
            .addTekstlinje("Vurder varsel om ugunst")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
