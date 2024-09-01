package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.OmsorgRettAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har aleneomsorg
 */
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BrukerHarAleneomsorgAksjonspunktUtleder implements OmsorgRettAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersonopplysningerForUttak personopplysninger;

    BrukerHarAleneomsorgAksjonspunktUtleder() {
        //CDI
    }

    @Inject
    public BrukerHarAleneomsorgAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider, PersonopplysningerForUttak personopplysninger) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.personopplysninger = personopplysninger;
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();

        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());

        return trengerAvklaring(ref, input.getSkjæringstidspunkt().orElseThrow(), ytelseFordelingAggregat) ? List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG) : List.of();
    }

    private boolean trengerAvklaring(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt, YtelseFordelingAggregat ytelseFordelingAggregat) {
        if (!harOppgittÅHaAleneomsorg(ytelseFordelingAggregat)) {
            return false;
        }
        return RelasjonsRolleType.erFarEllerMedmor(ref.relasjonRolle())
            || oppgittNorskAnnenpartMedSammeBosted(ref, skjæringstidspunkt)
            || personopplysninger.ektefelleHarSammeBosted(ref, skjæringstidspunkt);
    }

    private boolean oppgittNorskAnnenpartMedSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        return personopplysninger.harOppgittAnnenpartMedNorskID(ref) && personopplysninger.annenpartHarSammeBosted(ref, skjæringstidspunkt);
    }

    private boolean harOppgittÅHaAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var harAleneomsorgForBarnet = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();
        Objects.requireNonNull(harAleneomsorgForBarnet, "harAleneomsorgForBarnet må være sett");
        return harAleneomsorgForBarnet;
    }
}
