package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_LØPENDE_OMSORG;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har Omsorg
 */
@ApplicationScoped
public class AvklarLøpendeOmsorgAksjonspunktUtleder {
    private PersonopplysningerForUttak personopplysninger;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public AvklarLøpendeOmsorgAksjonspunktUtleder(PersonopplysningerForUttak personopplysninger,
                                                  UttakRepositoryProvider repositoryProvider) {
        this.personopplysninger = personopplysninger;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    AvklarLøpendeOmsorgAksjonspunktUtleder() {

    }

    public Optional<AksjonspunktDefinisjon> utledAksjonspunktFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        var bekreftetFH = familieHendelser.getBekreftetFamilieHendelse();

        if (familieHendelser.getGjeldendeFamilieHendelse().erAlleBarnDøde()) {
            return Optional.empty();
        }
        // Trenger ikke rebekrefte
        if (BehandlingType.REVURDERING.equals(ref.behandlingType())
            && ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId()).filter(a -> Boolean.TRUE.equals(a.getOverstyrtOmsorg())).isPresent()) {
            return Optional.empty();
        }
        if (bekreftetFH.isPresent() && erBarnetFødt(bekreftetFH.get()) == Utfall.JA
            && !personopplysninger.barnHarSammeBosted(ref, input.getSkjæringstidspunkt())) {
            return Optional.of(AVKLAR_LØPENDE_OMSORG);
        }
        return Optional.empty();
    }

    private Utfall erBarnetFødt(FamilieHendelse bekreftet) {
        return !bekreftet.getBarna().isEmpty() ? Utfall.JA : Utfall.NEI;
    }
}
