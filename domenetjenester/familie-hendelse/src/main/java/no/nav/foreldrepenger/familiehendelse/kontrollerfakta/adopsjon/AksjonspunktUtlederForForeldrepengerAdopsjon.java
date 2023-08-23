package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.adopsjon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

import java.util.ArrayList;
import java.util.List;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;

@ApplicationScoped
public class AksjonspunktUtlederForForeldrepengerAdopsjon implements AksjonspunktUtleder {

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    AksjonspunktUtlederForForeldrepengerAdopsjon (){
    }

    @Inject
    AksjonspunktUtlederForForeldrepengerAdopsjon(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON));

        var søknadVersjon = familieHendelseTjeneste.hentAggregat(param.getBehandlingId()).getSøknadVersjon();
        var adopsjon = søknadVersjon.getAdopsjon();
        if (adopsjon.isEmpty()) {
            return aksjonspunktResultater;
        }

        var erEktefellesBarn = adopsjon.get().getErEktefellesBarn();
        if (erEktefellesBarn == null || erEktefellesBarn) {
            aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN));
        }
        return aksjonspunktResultater;
    }

}
