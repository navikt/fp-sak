package no.nav.foreldrepenger.domene.uttak.fakta;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@ApplicationScoped
public class KontrollerFaktaUttakTjeneste {

    private List<FaktaUttakAksjonspunktUtleder> aksjonspunktUtledere;

    public KontrollerFaktaUttakTjeneste(List<FaktaUttakAksjonspunktUtleder> uttakUtledere) {
        this.aksjonspunktUtledere = uttakUtledere;
    }

    @Inject
    public KontrollerFaktaUttakTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) Instance<FaktaUttakAksjonspunktUtleder> uttakUtledere) {
        this(uttakUtledere.stream().collect(Collectors.toList()));
    }

    KontrollerFaktaUttakTjeneste() {
        // For CDI
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkter(UttakInput input) {
        return utledAksjonspunkter(input, aksjonspunktUtledere);
    }

    public List<AksjonspunktDefinisjon> reutledAksjonspunkterVedOppdateringAvYtelseFordeling(UttakInput input) {
        var utledere = aksjonspunktUtledere.stream()
            .filter(utleder -> utleder.skalBrukesVedOppdateringAvYtelseFordeling())
            .collect(Collectors.toList());
        return utledAksjonspunkter(input, utledere);
    }

    private List<AksjonspunktDefinisjon> utledAksjonspunkter(UttakInput input,
                                                             List<FaktaUttakAksjonspunktUtleder> utledere) {
        return utledere.stream()
            .flatMap(utleder -> utleder.utledAksjonspunkterFor(input).stream())
            .distinct()
            .collect(toList());
    }


}
