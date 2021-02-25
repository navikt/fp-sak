package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

/**
 * Lager historikk innslag for endringer på felt, og setter Aksjonspunkt til totrinnskontroll hvis endret.
 */
class HistorikkAksjonspunktAdapter {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private Behandling behandling;
    private AksjonspunktOppdaterParameter param;

    public HistorikkAksjonspunktAdapter(Behandling behandling, HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                        AksjonspunktOppdaterParameter param) {
        this.behandling = behandling;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.param = param;
    }

    public boolean håndterAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon, Vilkår vilkår, Boolean erVilkarOk, String begrunnelse,
                                    HistorikkEndretFeltType historikkEndretFeltType) {
        boolean erEndret = oppdaterVedEndretVerdi(historikkEndretFeltType, vilkår.getGjeldendeVilkårUtfall(), erVilkarOk
            ? VilkårUtfallType.OPPFYLT
            : VilkårUtfallType.IKKE_OPPFYLT);

        if (!erEndret) {
            historikkTjenesteAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, null,
                erVilkarOk ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT);
        }

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        VilkårType vilkårType = behandling.getVilkårTypeForRelasjonTilBarnet().orElse(null);
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(getSkjermlenkeType(vilkårType, aksjonspunktDefinisjon.getKode()));

        return erEndret;
    }

    static SkjermlenkeType getSkjermlenkeType(VilkårType vilkårType, String aksjonspunktKode) {
        return switch (aksjonspunktKode) {
            case "5007" -> SkjermlenkeType.SOEKNADSFRIST;
            case "5011" -> SkjermlenkeType.PUNKT_FOR_OMSORG;
            case "5013" -> SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
            case "5014" -> SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
            case "5017" -> SkjermlenkeType.OPPLYSNINGSPLIKT;
            case "5031" -> SkjermlenkeType.getSkjermlenkeTypeForMottattStotte(vilkårType);  // avklar om søker har mottatt støte
            case "5032" -> SkjermlenkeType.getSkjermlenkeTypeForMottattStotte(vilkårType);  // avklar om annen forelder har mottatt støtte
            default -> throw new UnsupportedOperationException("Støtter ikke aksjonspunktKode=" + aksjonspunktKode);
        };
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType feltkode, VilkårUtfallType original, VilkårUtfallType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkTjenesteAdapter.tekstBuilder().medEndretFelt(feltkode, original, bekreftet);
            return true;
        }
        return false;
    }
}
