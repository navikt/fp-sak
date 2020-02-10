package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.omsorg;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
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
import no.nav.foreldrepenger.domene.typer.AktørId;
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

        if (harOppgittÅHaAleneomsorg(ytelseFordelingAggregat) == JA) {
            if (harOppgittAndreforelderen(annenPartAktørId) == JA) {
                if (harAnnenforeldreSammeBosted(personopplysningerAggregat) == JA) {
                    return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
                }
            } else if (harSivilstatusGift(søker) == JA && harEktefelleSammeBosted(personopplysningerAggregat) == JA) {
                return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
            }
        }
        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private Utfall harOppgittÅHaAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        Boolean harAleneomsorgForBarnet = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();
        Objects.requireNonNull(harAleneomsorgForBarnet, "harAleneomsorgForBarnet må være sett"); //$NON-NLS-1$
        return harAleneomsorgForBarnet ? JA : NEI;
    }

    private Utfall harOppgittAndreforelderen(Optional<AktørId> annenPartAktørId) {
        return annenPartAktørId.isPresent() ? JA : NEI;
    }

    private Utfall harAnnenforeldreSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        final Optional<PersonopplysningEntitet> annenPart = personopplysningerAggregat.getAnnenPart();
        if (annenPart.isPresent()) {
            // ANNEN PART HAR IKKE RELASJON
            return personopplysningerAggregat.søkerHarSammeAdresseSom(annenPart.get().getAktørId(), RelasjonsRolleType.UDEFINERT) ? JA : NEI;
        }
        return NEI;
    }

    private Utfall harSivilstatusGift(PersonopplysningEntitet søker) {
        return søker.getSivilstand().equals(SivilstandType.GIFT) ? JA : NEI;
    }

    private Utfall harEktefelleSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        final Optional<PersonopplysningEntitet> ektefelle = personopplysningerAggregat.getEktefelle();
        if (ektefelle.isPresent()) {
            return personopplysningerAggregat.søkerHarSammeAdresseSom(ektefelle.get().getAktørId(), RelasjonsRolleType.EKTE) ? JA : NEI;
        }
        return NEI;
    }

}
