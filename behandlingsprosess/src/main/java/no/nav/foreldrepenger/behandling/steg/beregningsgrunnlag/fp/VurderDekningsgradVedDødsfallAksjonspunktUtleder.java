package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class VurderDekningsgradVedDødsfallAksjonspunktUtleder {

    private static final int ANTALL_DAGER_I_EN_UKE = 7;
    private static final int ANTALL_LEVEUKER = 6;
    private static final int DEKNINGSGRAD_80 = 80;

    public static List<AksjonspunktResultat> utled(List<AksjonspunktResultat> apResultat, int dekningsgradFraFagsakRelasjon,
            List<UidentifisertBarn> barnList) {
        if (utledAksjonspunktVedDødfødselOgDekningsgradUlik100(dekningsgradFraFagsakRelasjon, barnList)) {
            apResultat.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD));
        }
        return apResultat;
    }

    private static boolean utledAksjonspunktVedDødfødselOgDekningsgradUlik100(int dekningsgradFraFagsakRelasjon,
            List<UidentifisertBarn> registerBarn) {
        if ((dekningsgradFraFagsakRelasjon != DEKNINGSGRAD_80) || registerBarn.isEmpty()) {
            return false;
        }
        return registerBarn.stream().allMatch(VurderDekningsgradVedDødsfallAksjonspunktUtleder::dødeBarnetInnenDeFørsteSeksLeveuker);
    }

    private static boolean dødeBarnetInnenDeFørsteSeksLeveuker(UidentifisertBarn barn) {
        var dødsdato = barn.getDødsdato();
        if (dødsdato.isPresent()) {
            var antallDagerLevd = DatoIntervallEntitet.fraOgMedTilOgMed(barn.getFødselsdato(), dødsdato.get()).antallDager();
            return antallDagerLevd <= (ANTALL_DAGER_I_EN_UKE * ANTALL_LEVEUKER);
        }
        return false;
    }
}
