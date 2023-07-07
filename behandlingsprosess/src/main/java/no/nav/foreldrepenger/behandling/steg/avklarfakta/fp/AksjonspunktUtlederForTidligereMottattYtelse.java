package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.familiehendelse.YtelserSammeBarnTjeneste;

@ApplicationScoped
class AksjonspunktUtlederForTidligereMottattYtelse implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private YtelserSammeBarnTjeneste ytelseTjeneste;

    // For CDI.
    AksjonspunktUtlederForTidligereMottattYtelse() {
    }

    @Inject
    AksjonspunktUtlederForTidligereMottattYtelse(YtelserSammeBarnTjeneste ytelseTjeneste) {
        this.ytelseTjeneste = ytelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (harBrukerAnnenSakForSammeBarn(param) == JA) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall harBrukerAnnenSakForSammeBarn(AksjonspunktUtlederInput param) {
        var annenSakSammeBarn = ytelseTjeneste.harAktørAnnenSakMedSammeFamilieHendelse(param.getSaksnummer(), param.getBehandlingId(), param.getAktørId());
        return annenSakSammeBarn ? JA : NEI;
    }
}
