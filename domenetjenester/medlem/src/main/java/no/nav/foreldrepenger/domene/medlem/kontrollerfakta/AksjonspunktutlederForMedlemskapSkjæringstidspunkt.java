
package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class AksjonspunktutlederForMedlemskapSkjæringstidspunkt implements AksjonspunktUtleder {

    private VurderMedlemskapTjeneste tjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private static EnumMap<MedlemResultat, AksjonspunktDefinisjon> mapMedlemResulatTilAkDef = new EnumMap<>(MedlemResultat.class);

    static {
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OM_ER_BOSATT, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD, AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OPPHOLDSRETT, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.VENT_PÅ_FØDSEL, AksjonspunktDefinisjon.VENT_PÅ_FØDSEL);
    }

    AksjonspunktutlederForMedlemskapSkjæringstidspunkt() {
        //CDI
    }

    @Inject
    public AksjonspunktutlederForMedlemskapSkjæringstidspunkt(VurderMedlemskapTjeneste tjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.tjeneste = tjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        Long behandlingId = param.getBehandlingId();
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        Set<MedlemResultat> resultat = tjeneste.vurderMedlemskap(param.getRef(), skjæringstidspunkt);
        return resultat.stream()
            .map(mr -> opprettForMedlemResultat(param.getRef(), mr))
           .collect(Collectors.toList());
    }

    private AksjonspunktResultat opprettForMedlemResultat(BehandlingReferanse ref, MedlemResultat mr) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = mapMedlemResulatTilAkDef.get(mr);
        if (aksjonspunktDefinisjon == null) {
            throw new IllegalStateException("Utvikler-feil: Mangler mapping til aksjonspunktDefinisjon for  " + mr.name()); //$NON-NLS-1$
        }
        if (AksjonspunktDefinisjon.VENT_PÅ_FØDSEL.equals(aksjonspunktDefinisjon)) {
            return AksjonspunktResultat.opprettForAksjonspunktMedFrist(aksjonspunktDefinisjon, Venteårsak.UDEFINERT,
                tjeneste.beregnVentPåFødselFristTid(ref).atStartOfDay());
        }
        return AksjonspunktResultat.opprettForAksjonspunkt(aksjonspunktDefinisjon);
    }
}
