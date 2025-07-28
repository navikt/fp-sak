package no.nav.foreldrepenger.domene.uttak.fakta;

import static no.nav.foreldrepenger.domene.uttak.fakta.omsorg.AnnenForelderHarRettAksjonspunktUtleder.oppgittHarAnnenForeldreRett;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class OmsorgRettUttakTjeneste {

    private List<OmsorgRettAksjonspunktUtleder> aksjonspunktUtledere;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private PersonopplysningerForUttak personopplysninger;

    public OmsorgRettUttakTjeneste(List<OmsorgRettAksjonspunktUtleder> uttakUtledere,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   PersonopplysningerForUttak personopplysninger) {
        this.aksjonspunktUtledere = uttakUtledere;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.personopplysninger = personopplysninger;
    }

    @Inject
    public OmsorgRettUttakTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) Instance<OmsorgRettAksjonspunktUtleder> uttakUtledere,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   PersonopplysningerForUttak personopplysninger) {
        this(uttakUtledere.stream().toList(), ytelseFordelingTjeneste, personopplysninger);
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.personopplysninger = personopplysninger;
    }

    OmsorgRettUttakTjeneste() {
        // For CDI
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkter(UttakInput input) {
        return utledAksjonspunkter(input, aksjonspunktUtledere);
    }

    private List<AksjonspunktDefinisjon> utledAksjonspunkter(UttakInput input,
                                                             List<OmsorgRettAksjonspunktUtleder> utledere) {
        return utledere.stream()
            .flatMap(utleder -> utleder.utledAksjonspunkterFor(input).stream())
            .distinct()
            .toList();
    }

    public void avklarOmAnnenForelderHarRett(BehandlingReferanse ref) {
        if (kanAutomatiskAvklareAtAnnenForelderIkkeHarRett(ref)) {
            // Annen forelder uten norsk id har ikke Uføretrygd i Norge - men nok volum til å forsvare denne?
            ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(ref.behandlingId(), false, null, null);
        }
    }

    private boolean kanAutomatiskAvklareAtAnnenForelderIkkeHarRett(BehandlingReferanse ref) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(ref.behandlingId());
        return oppgittHarAnnenForeldreRett(ytelseFordelingAggregat) && personopplysninger.oppgittAnnenpartUtenNorskID(ref);
    }

}
