package no.nav.foreldrepenger.domene.uttak.kontroller.fakta;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.AnnenForelderHarRettAksjonspunktUtleder.oppgittHarAnnenForeldreRett;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class KontrollerFaktaUttakTjeneste {

    private List<FaktaUttakAksjonspunktUtleder> aksjonspunktUtledere;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private PersonopplysningRepository personopplysningTjeneste;

    @Inject
    public KontrollerFaktaUttakTjeneste(@FagsakYtelseTypeRef("FP") Instance<FaktaUttakAksjonspunktUtleder> uttakUtledere,
                                        YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        PersonopplysningRepository personopplysningTjeneste) {
        this.aksjonspunktUtledere = uttakUtledere.stream().collect(Collectors.toList());
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
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

    private List<AksjonspunktDefinisjon> utledAksjonspunkter(UttakInput input, List<FaktaUttakAksjonspunktUtleder> utledere) {
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
        var oppgittAnnenpart = hentOppgittAnnenpart(ref);
        if (oppgittAnnenpart.isPresent() && !erUkjent(oppgittAnnenpart.get()) && !finnesITps(oppgittAnnenpart.get())) {
            var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(ref.getBehandlingId());
            return oppgittHarAnnenForeldreRett(ytelseFordelingAggregat);
        }
        return false;
    }

    private boolean erUkjent(OppgittAnnenPartEntitet oppgittAnnenPartEntitet) {
        return oppgittAnnenPartEntitet.getAktørId() == null && oppgittAnnenPartEntitet.getUtenlandskPersonident() == null;
    }

    private boolean finnesITps(OppgittAnnenPartEntitet annenpart) {
        return annenpart.getAktørId() != null;
    }

    private Optional<OppgittAnnenPartEntitet> hentOppgittAnnenpart(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref.getBehandlingId());
        return personopplysningerAggregat.getOppgittAnnenPart();
    }
}
