package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.VarselRevurderingAksjonspunktDto;
import no.nav.foreldrepenger.dokumentbestiller.VarselRevurderingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VarselRevurderingDto.class, adapter=AksjonspunktOppdaterer.class)
public class VarselRevurderingOppdaterer implements AksjonspunktOppdaterer<VarselRevurderingDto> {

    private VarselRevurderingTjeneste dokumentTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public VarselRevurderingOppdaterer(VarselRevurderingTjeneste dokumentTjeneste,
                                       HistorikkinnslagRepository historikkinnslagRepository,
                                       DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.dokumentTjeneste = dokumentTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    VarselRevurderingOppdaterer() {
        // CDI
    }

    @Override
    public OppdateringResultat oppdater(VarselRevurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingRef = param.getRef();
        if (dto.isSendVarsel() && !harSendtVarselOmRevurdering(behandlingRef)) {
            var adapter = new VarselRevurderingAksjonspunktDto(dto.getFritekst(), dto.getBegrunnelse(), dto.getFrist(), dto.getVentearsak().getKode());
            dokumentTjeneste.håndterVarselRevurdering(behandlingRef, adapter);
        } else if (!dto.isSendVarsel()) {
            opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(behandlingRef, dto);
        }
        return OppdateringResultat.utenOverhopp();
    }

    private void opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(BehandlingReferanse ref, VarselRevurderingDto varselRevurderingDto) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId())
            .medTittel("Varsel om revurdering er ikke sendt")
            .addLinje(varselRevurderingDto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private boolean harSendtVarselOmRevurdering(BehandlingReferanse behandlingReferanse) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingReferanse.behandlingId(), DokumentMalType.VARSEL_OM_REVURDERING);
    }

}
