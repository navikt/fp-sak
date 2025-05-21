package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

final class SakskompleksDekningsgradUtleder {

    private static final int ANTALL_DAGER_I_EN_UKE = 7;
    private static final int ANTALL_LEVEUKER = 6;
    private static final Logger LOG = LoggerFactory.getLogger(SakskompleksDekningsgradUtleder.class);

    private SakskompleksDekningsgradUtleder() {
    }

    static Optional<DekningsgradUtledingResultat> utledFor(Dekningsgrad fagsakRelasjonDekningsgrad,
                                                           Dekningsgrad eksisterendeSakskompleksDekningsgrad,
                                                           Dekningsgrad oppgittDekningsgrad,
                                                           Dekningsgrad annenPartsOppgittDekningsgrad,
                                                           List<UidentifisertBarn> barna) {
        if (!barna.isEmpty() && barna.stream().allMatch(SakskompleksDekningsgradUtleder::dødeBarnetInnenDeFørsteSeksLeveuker)) {
            LOG.info("Dekningsgrad barn dødsfall {}", Dekningsgrad._100);
            return Optional.of(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.DØDSFALL));
        }

        if (fagsakRelasjonDekningsgrad != null) {
            LOG.info("Dekningsgrad fra fagsakrel {}", fagsakRelasjonDekningsgrad);
            return Optional.of(new DekningsgradUtledingResultat(fagsakRelasjonDekningsgrad, DekningsgradUtledingResultat.DekningsgradKilde.FAGSAK_RELASJON));
        }

        if (eksisterendeSakskompleksDekningsgrad != null) {
            LOG.info("Dekningsgrad fra eksisterende sakskompleks {}", eksisterendeSakskompleksDekningsgrad);
            return Optional.of(new DekningsgradUtledingResultat(eksisterendeSakskompleksDekningsgrad, DekningsgradUtledingResultat.DekningsgradKilde.ALLEREDE_FASTSATT));
        }

        if (annenPartsOppgittDekningsgrad == null || Objects.equals(oppgittDekningsgrad, annenPartsOppgittDekningsgrad)) {
            LOG.info("Dekningsgrad fra oppgitt {}", oppgittDekningsgrad);
            return Optional.of(new DekningsgradUtledingResultat(oppgittDekningsgrad, DekningsgradUtledingResultat.DekningsgradKilde.OPPGITT));
        }

        LOG.info("Dekningsgrad oppretter aksjonspunkt");
        return Optional.empty();
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
