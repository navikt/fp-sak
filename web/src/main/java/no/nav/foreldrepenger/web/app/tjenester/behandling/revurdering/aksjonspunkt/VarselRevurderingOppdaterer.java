package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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

    VarselRevurderingOppdaterer() {
        // CDI
    }

    @Inject
    public VarselRevurderingOppdaterer(VarselRevurderingTjeneste dokumentTjeneste, HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                       DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.dokumentTjeneste = dokumentTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VarselRevurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        if (dto.isSendVarsel() && !harSendtVarselOmRevurdering(behandling)) {
            final var adapter = new VarselRevurderingAksjonspunktDto(dto.getFritekst(), dto.getBegrunnelse(), dto.getFrist(), dto.getVentearsak().getKode());
            dokumentTjeneste.håndterVarselRevurdering(behandling, adapter);
        } else if (!dto.isSendVarsel()) {
            opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(behandling, dto, HistorikkAktør.SAKSBEHANDLER);
        }
        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(Behandling behandling, VarselRevurderingDto varselRevurderingDto, HistorikkAktør historikkAktør) {
        var historiebygger = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VRS_REV_IKKE_SNDT)
            .medBegrunnelse(varselRevurderingDto.getBegrunnelse());
        var innslag = new Historikkinnslag();
        innslag.setAktør(historikkAktør);
        innslag.setType(HistorikkinnslagType.VRS_REV_IKKE_SNDT);
        innslag.setBehandlingId(behandling.getId());
        historiebygger.build(innslag);

        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private Boolean harSendtVarselOmRevurdering(Behandling behandling) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);
    }

}
