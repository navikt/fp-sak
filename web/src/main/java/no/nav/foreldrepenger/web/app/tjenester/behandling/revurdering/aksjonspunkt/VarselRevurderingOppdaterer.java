package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.VarselRevurderingAksjonspunktDto;
import no.nav.foreldrepenger.dokumentbestiller.VarselRevurderingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VarselRevurderingDto.class, adapter=AksjonspunktOppdaterer.class)
public class VarselRevurderingOppdaterer implements AksjonspunktOppdaterer<VarselRevurderingDto> {

    private VarselRevurderingTjeneste dokumentTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    @Inject
    public VarselRevurderingOppdaterer(VarselRevurderingTjeneste dokumentTjeneste, HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                       DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.dokumentTjeneste = dokumentTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
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
            opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(behandlingRef, dto, HistorikkAktør.SAKSBEHANDLER);
        }
        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(BehandlingReferanse ref, VarselRevurderingDto varselRevurderingDto, HistorikkAktør historikkAktør) {
        var historiebygger = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VRS_REV_IKKE_SNDT)
            .medBegrunnelse(varselRevurderingDto.getBegrunnelse());
        var innslag = new Historikkinnslag();
        innslag.setAktør(historikkAktør);
        innslag.setType(HistorikkinnslagType.VRS_REV_IKKE_SNDT);
        innslag.setBehandlingId(ref.behandlingId());
        historiebygger.build(innslag);

        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private boolean harSendtVarselOmRevurdering(BehandlingReferanse behandlingReferanse) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingReferanse.behandlingId(), DokumentMalType.VARSEL_OM_REVURDERING);
    }

}
