package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.FaktaUttakAksjonspunktUtleder;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har aleneomsorg
 */
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class BrukerHarAleneomsorgAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;

    BrukerHarAleneomsorgAksjonspunktUtleder() {
        //CDI
    }

    @Inject
    BrukerHarAleneomsorgAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider, PersonopplysningTjeneste personopplysningTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());

        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        var annenPartAktørId = personopplysningerAggregat.getOppgittAnnenPart().map(OppgittAnnenPartEntitet::getAktørId);
        var søker = personopplysningerAggregat.getSøker();

        if (harOppgittÅHaAleneomsorg(ytelseFordelingAggregat)) {
            if (annenPartAktørId.isPresent()) {
                if (harAnnenforeldreSammeBosted(personopplysningerAggregat)) {
                    return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
                }
            } else if (harSivilstatusGift(søker) && harEktefelleSammeBosted(personopplysningerAggregat)) {
                return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
            }
        }
        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private boolean harOppgittÅHaAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var harAleneomsorgForBarnet = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();
        Objects.requireNonNull(harAleneomsorgForBarnet, "harAleneomsorgForBarnet må være sett"); //$NON-NLS-1$
        return harAleneomsorgForBarnet;
    }

    private boolean harAnnenforeldreSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        final Optional<PersonopplysningEntitet> annenPart = personopplysningerAggregat.getAnnenPart();
        if (annenPart.isPresent()) {
            // ANNEN PART HAR IKKE RELASJON
            return personopplysningerAggregat.søkerHarSammeAdresseSom(annenPart.get().getAktørId(), RelasjonsRolleType.UDEFINERT);
        }
        return false;
    }

    private boolean harSivilstatusGift(PersonopplysningEntitet søker) {
        return søker.getSivilstand().equals(SivilstandType.GIFT);
    }

    private boolean harEktefelleSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        final Optional<PersonopplysningEntitet> ektefelle = personopplysningerAggregat.getEktefelle();
        if (ektefelle.isPresent()) {
            return personopplysningerAggregat.søkerHarSammeAdresseSom(ektefelle.get().getAktørId(), RelasjonsRolleType.EKTE);
        }
        return false;
    }

}
