package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringUtenlandssakMarkeringDto.class, adapter = Overstyringshåndterer.class)
public class MarkereUtenlandssakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringUtenlandssakMarkeringDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    MarkereUtenlandssakOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MarkereUtenlandssakOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                    FagsakEgenskapRepository fagsakEgenskapRepository) {
        super(historikkAdapter, AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE);
        this.historikkAdapter = historikkAdapter;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringUtenlandssakMarkeringDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var builder = OppdateringResultat.utenTransisjon();
        behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        var nymerking = UtlandMarkering.valueOf(dto.getBegrunnelse());
        fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(behandling.getFagsakId(), nymerking);
        return builder.build();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringUtenlandssakMarkeringDto dto) {
        var fraVerdi = getHistorikkEndretFeltVerdiTypeFra(dto.getGammelVerdi());
        var tilVerdi = getHistorikkEndretFeltVerdiTypeFra(dto.getBegrunnelse());

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medSkjermlenke(SkjermlenkeType.UTLAND)
            .medEndretFelt(HistorikkEndretFeltType.UTLAND, fraVerdi, tilVerdi);

        builder.build(historikkinnslag);
        historikkAdapter.lagInnslag(historikkinnslag);
    }

    private HistorikkEndretFeltVerdiType getHistorikkEndretFeltVerdiTypeFra(String feltVerdi) {
        var nasjonal = HistorikkEndretFeltVerdiType.NASJONAL;
        var eøs = HistorikkEndretFeltVerdiType.EØS_BOSATT_NORGE;
        var bosattUtland = HistorikkEndretFeltVerdiType.BOSATT_UTLAND;

        return eøs.getKode().equals(feltVerdi) ? eøs
            : bosattUtland.getKode().equals(feltVerdi) ? bosattUtland
                : nasjonal;
    }
}
