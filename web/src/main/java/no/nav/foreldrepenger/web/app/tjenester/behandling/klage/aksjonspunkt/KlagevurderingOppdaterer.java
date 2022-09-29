package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktUtil;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageVurderingResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlagevurderingOppdaterer implements AksjonspunktOppdaterer<KlageVurderingResultatAksjonspunktDto> {
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;

    KlagevurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlagevurderingOppdaterer(HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                    BehandlingsutredningTjeneste behandlingsutredningTjeneste,
                                    KlageVurderingTjeneste klageVurderingTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsutredningTjeneste = behandlingsutredningTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(KlageVurderingResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        var totrinn = håndterToTrinnsBehandling(behandling, aksjonspunktDefinisjon, dto.getKlageVurdering());

        håndterKlageVurdering(dto, behandling, aksjonspunktDefinisjon);

        opprettHistorikkinnslag(behandling, aksjonspunktDefinisjon, dto);
        oppdatereDatavarehus(dto, behandling, aksjonspunktDefinisjon);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private void håndterKlageVurdering(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var vurdertAv = erNfpAksjonspunkt(aksjonspunktDefinisjon) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, vurdertAv)
            .medKlageVurdering(dto.getKlageVurdering())
            .medKlageVurderingOmgjør(dto.getKlageVurderingOmgjoer())
            .medKlageMedholdÅrsak(dto.getKlageMedholdArsak())
            .medKlageHjemmel(dto.getKlageHjemmel())
            .medBegrunnelse(dto.getBegrunnelse())
            .medFritekstTilBrev(dto.getFritekstTilBrev());

        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, builder, vurdertAv);
    }

    private boolean håndterToTrinnsBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, KlageVurdering klageVurdering) {
        if (erNfpAksjonspunkt(aksjonspunktDefinisjon) && KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurdering)) {
            // Må fjerne totrinnsbehandling i tilfeller hvor totrinn er satt for NFP (klagen ikke er innom NK),
            // beslutter sender behandlingen tilbake til NFP, og NFP deretter gjør et valgt som sender
            // behandlingen til NK. Da skal ikke aksjonspunkt NFP totrinnsbehandles.
            fjernToTrinnsBehandling(behandling, aksjonspunktDefinisjon);
        }
        return erNfpAksjonspunkt(aksjonspunktDefinisjon) && !KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurdering);
    }

    private void fjernToTrinnsBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
        if (aksjonspunkt.isToTrinnsBehandling()) {
            AksjonspunktUtil.fjernToTrinnsBehandlingKreves(aksjonspunkt);
        }
    }

    private void opprettHistorikkinnslag(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, KlageVurderingResultatAksjonspunktDto dto) {
        var klageVurdering = dto.getKlageVurdering();
        var klageVurderingOmgjør = dto.getKlageVurderingOmgjoer() != null ? dto.getKlageVurderingOmgjoer() : null;
        var erNfpAksjonspunkt = erNfpAksjonspunkt(aksjonspunktDefinisjon);
        var historikkinnslagType = erNfpAksjonspunkt ? HistorikkinnslagType.KLAGE_BEH_NFP : HistorikkinnslagType.KLAGE_BEH_NK;
        Kodeverdi årsak = null;
        if (dto.getKlageMedholdArsak() != null) {
            årsak = dto.getKlageMedholdArsak();
        } else if (dto.getKlageAvvistArsak() != null) {
            årsak = dto.getKlageAvvistArsak();
        }

        var resultat = KlageVurderingTjeneste.historikkResultatForKlageVurdering(klageVurdering, erNfpAksjonspunkt ? KlageVurdertAv.NFP : KlageVurdertAv.NK, klageVurderingOmgjør);
        var historiebygger = new HistorikkInnslagTekstBuilder();
        if (erNfpAksjonspunkt) {
            historiebygger.medEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_NFP, null, resultat.getNavn());
        }
        if (årsak != null) {
            historiebygger.medEndretFelt(HistorikkEndretFeltType.KLAGE_OMGJØR_ÅRSAK, null, årsak.getNavn());
        }
        var skjermlenkeType = getSkjermlenkeType(dto.getAksjonspunktDefinisjon());
        historiebygger.medBegrunnelse(dto.getBegrunnelse());
        historiebygger.medSkjermlenke(skjermlenkeType);

        var innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setType(historikkinnslagType);
        innslag.setBehandlingId(behandling.getId());
        historiebygger.build(innslag);

        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private SkjermlenkeType getSkjermlenkeType(AksjonspunktDefinisjon apDef) {
        return AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(apDef) ? SkjermlenkeType.KLAGE_BEH_NFP : SkjermlenkeType.KLAGE_BEH_NK;
    }

    private HistorikkResultatType konverterKlageVurderingTilResultatType(KlageVurdering vurdering, boolean erNfpAksjonspunkt, KlageVurderingOmgjør klageVurderingOmgjør) {
        if (KlageVurdering.AVVIS_KLAGE.equals(vurdering)) {
            return HistorikkResultatType.AVVIS_KLAGE;
        }
        if (KlageVurdering.MEDHOLD_I_KLAGE.equals(vurdering)) {
            if (KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return HistorikkResultatType.DELVIS_MEDHOLD_I_KLAGE;
            }
            if (KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return HistorikkResultatType.UGUNST_MEDHOLD_I_KLAGE;
            }
            return HistorikkResultatType.MEDHOLD_I_KLAGE;
        }
        if (KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(vurdering)) {
            return HistorikkResultatType.OPPHEVE_VEDTAK;
        }
        if (KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(vurdering)) {
            return HistorikkResultatType.KLAGE_HJEMSENDE_UTEN_OPPHEVE;
        }
        if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            if (erNfpAksjonspunkt) {
                return HistorikkResultatType.OPPRETTHOLDT_VEDTAK;
            }
            return HistorikkResultatType.STADFESTET_VEDTAK;
        }
        return null;
    }

    private void oppdatereDatavarehus(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var klageVurdering = dto.getKlageVurdering();
        if (erNfpAksjonspunkt(aksjonspunktDefinisjon) && KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurdering)) {
            behandlingsutredningTjeneste.byttBehandlendeEnhet(behandling.getId(),BehandlendeEnhetTjeneste.getKlageInstans(),
                "", //Det er ikke behov for en begrunnelse i dette tilfellet.
                HistorikkAktør.VEDTAKSLØSNINGEN);
        }
    }

    private boolean erNfpAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(aksjonspunktDefinisjon);
    }
}
