package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@ApplicationScoped
public class FastsettUttakManueltAksjonspunktUtleder {

    private FpUttakRepository fpUttakRepository;

    @Inject
    FastsettUttakManueltAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider){
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
    }

    FastsettUttakManueltAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();

        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();

        utledAksjonspunktForManuellBehandlingFraRegler(behandlingId).ifPresent(aksjonspunkter::add);
        utledAksjonspunktForDødtBarn(input.getYtelsespesifiktGrunnlag()).ifPresent(aksjonspunkter::add);

        return aksjonspunkter.stream().distinct().collect(Collectors.toList());
    }

    private Optional<AksjonspunktResultat> utledAksjonspunktForManuellBehandlingFraRegler(Long behandlingId) {
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).orElseThrow();
        for (var periode : uttakResultat.getGjeldendePerioder().getPerioder()) {
            if (periode.getResultatType().equals(PeriodeResultatType.MANUELL_BEHANDLING)){
                return Optional.of(fastsettUttakAksjonspunkt());
            }
        }
        return Optional.empty();
    }

    private Optional<AksjonspunktResultat> utledAksjonspunktForDødtBarn(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        if (finnesDødsdatoIRegistertEllerOverstyrtVersjon(foreldrepengerGrunnlag)) {
            return Optional.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD));
        }
        return Optional.empty();
    }

    private boolean finnesDødsdatoIRegistertEllerOverstyrtVersjon(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var familieHendelser = foreldrepengerGrunnlag.getFamilieHendelser();
        var barna = familieHendelser.getGjeldendeFamilieHendelse().getBarna();

        return barna.stream()
            .anyMatch(barn -> barn.getDødsdato().isPresent());
    }

    private AksjonspunktResultat fastsettUttakAksjonspunkt() {
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER);
    }

}
