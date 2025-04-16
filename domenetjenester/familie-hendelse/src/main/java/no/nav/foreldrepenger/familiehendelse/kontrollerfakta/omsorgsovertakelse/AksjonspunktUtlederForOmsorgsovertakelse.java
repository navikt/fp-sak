package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorgsovertakelse;

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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

/**
 * Aksjonspunkter for søknad om engangsstønad og betinget vilkår gjelder omsorgsovertakelse.
 * MERK: Betinget vilkår blir ikke satt før det er manuelt satt av saksbehandler.
 */
@ApplicationScoped
public class AksjonspunktUtlederForOmsorgsovertakelse implements AksjonspunktUtleder {

    private static final List<AksjonspunktUtlederResultat> INGEN_AKSJONSPUNKTER = List.of();
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    AksjonspunktUtlederForOmsorgsovertakelse() {
    }

    @Inject
    public AksjonspunktUtlederForOmsorgsovertakelse(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public List<AksjonspunktUtlederResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (farAdoptererAlene(param.getBehandlingId()) == NEI) {
            return AksjonspunktUtlederResultat.opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE);
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall farAdoptererAlene(Long behandlingId) {
        return familieHendelseTjeneste.hentAggregat(behandlingId).getGjeldendeAdopsjon()
            .map(AdopsjonEntitet::getAdoptererAlene).orElse(false) ? JA : NEI;
    }
}
