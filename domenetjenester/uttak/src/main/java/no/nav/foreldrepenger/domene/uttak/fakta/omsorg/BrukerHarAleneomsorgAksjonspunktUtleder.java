package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har aleneomsorg
 */
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BrukerHarAleneomsorgAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

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

        if (!Objects.equals(ref.behandlingType(), BehandlingType.FØRSTEGANGSSØKNAD)) {
            return List.of();
        }

        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());

        if (harOppgittÅHaAleneomsorg(ytelseFordelingAggregat)) {
            if (personopplysninger.harOppgittAnnenpartMedNorskID(ref)) {
                if (personopplysninger.annenpartHarSammeBosted(ref)) {
                    return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
                }
            } else if (personopplysninger.ektefelleHarSammeBosted(ref)) {
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
}
