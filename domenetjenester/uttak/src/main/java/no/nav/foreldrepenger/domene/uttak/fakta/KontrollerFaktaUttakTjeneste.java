package no.nav.foreldrepenger.domene.uttak.fakta;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.domene.uttak.fakta.omsorg.AnnenForelderHarRettAksjonspunktUtleder.oppgittHarAnnenForeldreRett;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class KontrollerFaktaUttakTjeneste {

    private List<FaktaUttakAksjonspunktUtleder> aksjonspunktUtledere;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private PersonopplysningerForUttak personopplysninger;

    public KontrollerFaktaUttakTjeneste(List<FaktaUttakAksjonspunktUtleder> uttakUtledere,
                                        YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        PersonopplysningerForUttak personopplysninger) {
        this.aksjonspunktUtledere = uttakUtledere;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.personopplysninger = personopplysninger;
    }

    @Inject
    public KontrollerFaktaUttakTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) Instance<FaktaUttakAksjonspunktUtleder> uttakUtledere,
                                        YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        PersonopplysningerForUttak personopplysninger) {
        this(uttakUtledere.stream().collect(Collectors.toList()), ytelseFordelingTjeneste, personopplysninger);
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.personopplysninger = personopplysninger;
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

    public void avklarOmAnnenForelderHarRett(BehandlingReferanse ref) {
        if (kanAutomatiskAvklareAtAnnenForelderIkkeHarRett(ref)) {
            ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(ref.getBehandlingId(), false);
        }
    }

    private boolean kanAutomatiskAvklareAtAnnenForelderIkkeHarRett(BehandlingReferanse ref) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(ref.getBehandlingId());
        return oppgittHarAnnenForeldreRett(ytelseFordelingAggregat) && personopplysninger.oppgittAnnenpartUtenNorskID(ref);
    }

}
