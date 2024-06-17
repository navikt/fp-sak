package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

/**
 * Lager historikk innslag for endringer på felt, og setter Aksjonspunkt til totrinnskontroll hvis endret.
 */
class HistorikkAksjonspunktAdapter {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private final Behandlingsresultat behandlingsresultat;
    private AksjonspunktOppdaterParameter param;

    public HistorikkAksjonspunktAdapter(Behandlingsresultat behandlingsresultat,
                                        HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                        AksjonspunktOppdaterParameter param) {
        this.behandlingsresultat = behandlingsresultat;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.param = param;
    }

    public boolean håndterAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                       Vilkår vilkår,
                                       Boolean erVilkarOk,
                                       String begrunnelse,
                                       HistorikkEndretFeltType historikkEndretFeltType) {
        var erEndret = oppdaterVedEndretVerdi(historikkEndretFeltType, vilkår.getGjeldendeVilkårUtfall(),
            erVilkarOk ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT);

        if (!erEndret) {
            historikkTjenesteAdapter.tekstBuilder()
                .medEndretFelt(historikkEndretFeltType, null,
                    erVilkarOk ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT);
        }

        var vilkårType = Optional.ofNullable(behandlingsresultat)
            .map(Behandlingsresultat::getVilkårResultat)
            .flatMap(VilkårResultat::getVilkårForRelasjonTilBarn)
            .orElse(null);
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, param.erBegrunnelseEndret())
            .medSkjermlenke(getSkjermlenkeType(vilkårType, aksjonspunktDefinisjon));

        return erEndret;
    }

    static SkjermlenkeType getSkjermlenkeType(VilkårType vilkårType, AksjonspunktDefinisjon aksjonspunktKode) {
        return switch (aksjonspunktKode) {
            case MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET -> SkjermlenkeType.SOEKNADSFRIST;
            case MANUELL_VURDERING_AV_OMSORGSVILKÅRET -> SkjermlenkeType.PUNKT_FOR_OMSORG;
            case MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD -> SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
            case MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD -> SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
            case SØKERS_OPPLYSNINGSPLIKT_MANU -> SkjermlenkeType.OPPLYSNINGSPLIKT;
            case AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE ->
                SkjermlenkeType.getSkjermlenkeTypeForMottattStotte(vilkårType);  // avklar om søker har mottatt støte
            case AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE ->
                SkjermlenkeType.getSkjermlenkeTypeForMottattStotte(vilkårType);  // avklar om annen forelder har mottatt støtte
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
