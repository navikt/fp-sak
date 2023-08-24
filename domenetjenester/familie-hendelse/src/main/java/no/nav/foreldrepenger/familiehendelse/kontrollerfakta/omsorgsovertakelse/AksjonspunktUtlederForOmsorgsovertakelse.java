package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorgsovertakelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

/**
 * Aksjonspunkter for søknad om engangsstønad og betinget vilkår gjelder omsorgsovertakelse.
 * MERK: Betinget vilkår blir ikke satt før det er manuelt satt av saksbehandler.
 */
@ApplicationScoped
public class AksjonspunktUtlederForOmsorgsovertakelse implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    AksjonspunktUtlederForOmsorgsovertakelse() {
    }

    @Inject
    public AksjonspunktUtlederForOmsorgsovertakelse(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (farAdoptererAlene(param.getBehandlingId()) == NEI) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE);
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall farAdoptererAlene(Long behandlingId) {
        return familieHendelseTjeneste.hentAggregat(behandlingId).getGjeldendeAdopsjon()
            .map(AdopsjonEntitet::getAdoptererAlene).orElse(false) ? JA : NEI;
    }
}
