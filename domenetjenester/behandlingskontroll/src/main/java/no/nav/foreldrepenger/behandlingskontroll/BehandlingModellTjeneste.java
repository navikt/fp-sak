package no.nav.foreldrepenger.behandlingskontroll;

import static java.util.Collections.singletonList;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Grensesnitt for oppslag av stegrekkefølger for en behandling, egentlig ytelseType og behandlingType.
 */
@ApplicationScoped // I praksis for å kunne mocke injects.
public class BehandlingModellTjeneste {

    public BehandlingModellTjeneste() {
        // Empty supplier
    }

    public boolean inneholderSteg(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType behandlingStegType) {
        var modell = getModell(behandlingType, ytelseType);
        return modell.inneholderSteg(behandlingStegType);
    }

    public boolean erStegAFørStegB(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType stegA, BehandlingStegType stegB) {
        var modell = getModell(behandlingType, ytelseType);
        return modell.erStegAFørStegB(stegA, stegB);
    }

    public boolean erStegAEtterStegB(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType stegA, BehandlingStegType stegB) {
        var modell = getModell(behandlingType, ytelseType);
        return modell.erStegAEtterStegB(stegA, stegB);
    }

    public boolean skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType ytelseType, BehandlingType behandlingType,
                                                        BehandlingStegType behandlingSteg, AksjonspunktDefinisjon apDef) {

        var modell = getModell(behandlingType, ytelseType);
        var apLøsesteg = Optional.ofNullable(modell.finnTidligsteStegForAksjonspunktDefinisjon(singletonList(apDef)))
            .map(BehandlingStegModell::getBehandlingStegType)
            .orElse(null);

        return apLøsesteg != null && !modell.erStegAFørStegB(apLøsesteg, behandlingSteg);
    }

    private static BehandlingModell getModell(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return BehandlingModellRepository.getModell(behandlingType, ytelseType);
    }



}
