package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

final class VurderDekningsgradVedDødsfall {

    private static final int ANTALL_DAGER_I_EN_UKE = 7;
    private static final int ANTALL_LEVEUKER = 6;

    private VurderDekningsgradVedDødsfall() {
    }

    static boolean skalEndreDekningsgrad(Dekningsgrad gjeldendeDekningsgrad, List<UidentifisertBarn> barnList) {
        if (Dekningsgrad._100.equals(gjeldendeDekningsgrad) || barnList.isEmpty()) {
            return false;
        }
        return barnList.stream().allMatch(VurderDekningsgradVedDødsfall::dødeBarnetInnenDeFørsteSeksLeveuker);
    }

    private static boolean dødeBarnetInnenDeFørsteSeksLeveuker(UidentifisertBarn barn) {
        var dødsdato = barn.getDødsdato();
        if (dødsdato.isPresent()) {
            var antallDagerLevd = DatoIntervallEntitet.fraOgMedTilOgMed(barn.getFødselsdato(), dødsdato.get()).antallDager();
            return antallDagerLevd <= ANTALL_DAGER_I_EN_UKE * ANTALL_LEVEUKER;
        }
        return false;
    }
}
