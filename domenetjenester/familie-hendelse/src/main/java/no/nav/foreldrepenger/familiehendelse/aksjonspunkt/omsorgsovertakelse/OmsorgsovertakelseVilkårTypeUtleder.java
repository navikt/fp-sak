package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;

/**
 * Håndterer oppdatering av adopsjon/omsorgsvilkåret.
 */
@ApplicationScoped
public class OmsorgsovertakelseVilkårTypeUtleder {

    private SøknadRepository søknadRepository;


    protected OmsorgsovertakelseVilkårTypeUtleder() {
        // for CDI proxy
    }

    @Inject
    public OmsorgsovertakelseVilkårTypeUtleder(SøknadRepository søknadRepository) {
        this.søknadRepository = søknadRepository;
    }


    public OmsorgsovertakelseVilkårType utledDelvilkår(BehandlingReferanse ref, FamilieHendelseEntitet familieHendelse) {
        if (familieHendelse == null || familieHendelse.getAdopsjon().isEmpty()) {
            return null;
        }
        var farSøkerType = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId()).map(SøknadEntitet::getFarSøkerType).orElse(FarSøkerType.UDEFINERT);
        var adopsjon = familieHendelse.getAdopsjon().orElseThrow();
        if (adopsjon.getOmsorgovertakelseVilkår() != null && !OmsorgsovertakelseVilkårType.UDEFINERT.equals(adopsjon.getOmsorgovertakelseVilkår())) {
            return adopsjon.getOmsorgovertakelseVilkår();
        }
        if (FamilieHendelseType.ADOPSJON.equals(familieHendelse.getType())) {
            return switch (ref.fagsakYtelseType()) {
                case ENGANGSTØNAD -> OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET;
                case FORELDREPENGER -> adopsjon.isStebarnsadopsjon() ? OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET : OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET;
                default -> null;
            };
        } else if (FamilieHendelseType.OMSORG.equals(familieHendelse.getType()) || !FarSøkerType.UDEFINERT.equals(farSøkerType)) {
            return switch (ref.fagsakYtelseType()) {
                case ENGANGSTØNAD -> utledEngangsstønadVilkår(farSøkerType);
                case FORELDREPENGER -> OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD;
                default -> null;
            };
        } else {
            return null;
        }
    }

    private static OmsorgsovertakelseVilkårType utledEngangsstønadVilkår(FarSøkerType farSøkerType) {
        return switch (farSøkerType) {
            case ADOPTERER_ALENE -> OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET;
            case ANDRE_FORELDER_DØD -> OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD;
            case OVERTATT_OMSORG -> OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_4_LEDD;
            case OVERTATT_OMSORG_F, ANDRE_FORELD_DØD_F -> OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET;
            case UDEFINERT -> null;
        };
    }

}
