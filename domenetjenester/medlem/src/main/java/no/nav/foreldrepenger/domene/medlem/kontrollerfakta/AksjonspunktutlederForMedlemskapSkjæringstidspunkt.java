
package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import java.util.EnumMap;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;

@ApplicationScoped
public class AksjonspunktutlederForMedlemskapSkjæringstidspunkt implements AksjonspunktUtleder {

    private VurderMedlemskapTjeneste tjeneste;
    private static EnumMap<MedlemResultat, AksjonspunktDefinisjon> mapMedlemResulatTilAkDef = new EnumMap<>(MedlemResultat.class);

    static {
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OM_ER_BOSATT, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD, AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OPPHOLDSRETT, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT);
    }

    AksjonspunktutlederForMedlemskapSkjæringstidspunkt() {
        //CDI
    }

    @Inject
    public AksjonspunktutlederForMedlemskapSkjæringstidspunkt(VurderMedlemskapTjeneste tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var resultat = tjeneste.vurderMedlemskap(param.getRef(), skjæringstidspunkt);
        return resultat.stream()
            .map(mr -> opprettForMedlemResultat(param.getRef(), mr))
           .toList();
    }

    private AksjonspunktResultat opprettForMedlemResultat(BehandlingReferanse ref, MedlemResultat mr) {
        var aksjonspunktDefinisjon = mapMedlemResulatTilAkDef.get(mr);
        if (aksjonspunktDefinisjon == null) {
            throw new IllegalStateException("Utvikler-feil: Mangler mapping til aksjonspunktDefinisjon for  " + mr.name());
        }
        return AksjonspunktResultat.opprettForAksjonspunkt(aksjonspunktDefinisjon);
    }
}
