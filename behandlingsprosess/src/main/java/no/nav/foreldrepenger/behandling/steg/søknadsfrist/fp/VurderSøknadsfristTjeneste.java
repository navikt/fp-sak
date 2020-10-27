package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristResultat;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class VurderSøknadsfristTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @Inject
    public VurderSøknadsfristTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    VurderSøknadsfristTjeneste() {
        //For CDI
    }

    public Optional<AksjonspunktDefinisjon> vurder(Long behandlingId) {
        var fordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var oppgittePerioder = fordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        //Ingen perioder betyr behandling ut ny søknad. Trenger ikke å sjekke søknadsfrist på nytt ettersom uttaksperiodegrense
        // er kopiert fra forrige behandling
        if (oppgittePerioder.isEmpty()) {
            return Optional.empty();
        }

        var søknad = søknadRepository.hentSøknad(behandlingId);
        var regelAdapter = new SøknadsfristRegelAdapter();
        var resultat = regelAdapter.vurderSøknadsfristFor(søknad, oppgittePerioder);

        lagreResultat(behandlingId, resultat, søknad.getMottattDato());
        return utledAksjonspunkt(resultat);
    }

    private void lagreResultat(Long behandlingId, SøknadsfristResultat resultat, LocalDate mottattDato) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
            .medFørsteLovligeUttaksdag(resultat.getTidligsteLovligeUttak())
            .medMottattDato(mottattDato)
            .medSporingInput(resultat.getInnsendtGrunnlag())
            .medSporingRegel(resultat.getEvalueringResultat())
            .build();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);
    }

    private Optional<AksjonspunktDefinisjon> utledAksjonspunkt(SøknadsfristResultat resultat) {
        Optional<String> årsakKode = resultat.getÅrsakKodeIkkeVurdert();
        if (!resultat.isRegelOppfylt() && årsakKode.isPresent()) {
            AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(årsakKode.get());

            return Optional.of(aksjonspunktDefinisjon);
        }
        return Optional.empty();
    }
}
