package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.familiehendelse.YtelserSammeBarnTjeneste;

@ApplicationScoped
class AksjonspunktUtlederForTidligereMottattYtelse implements AksjonspunktUtleder {

    private static final List<AksjonspunktUtlederResultat> INGEN_AKSJONSPUNKTER = List.of();

    private YtelserSammeBarnTjeneste ytelseTjeneste;

    // For CDI.
    AksjonspunktUtlederForTidligereMottattYtelse() {
    }

    @Inject
    AksjonspunktUtlederForTidligereMottattYtelse(YtelserSammeBarnTjeneste ytelseTjeneste) {
        this.ytelseTjeneste = ytelseTjeneste;
    }

    @Override
    public List<AksjonspunktUtlederResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        if (harBrukerAnnenSakForSammeBarn(param) == JA) {
            return AksjonspunktUtlederResultat.opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall harBrukerAnnenSakForSammeBarn(AksjonspunktUtlederInput param) {
        var annenSakSammeBarn = ytelseTjeneste.harAktørAnnenSakMedSammeFamilieHendelse(param.getSaksnummer(), param.getBehandlingId(), param.getAktørId());
        return annenSakSammeBarn ? JA : NEI;
    }

}
