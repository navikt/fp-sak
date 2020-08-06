package no.nav.foreldrepenger.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

@ApplicationScoped
public class TapendeBehandlingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TapendeBehandlingTjeneste.class);

    private SøknadRepository søknadRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    public TapendeBehandlingTjeneste(SøknadRepository søknadRepository,
                                     RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                     ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.søknadRepository = søknadRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    TapendeBehandlingTjeneste() {
        //CDI
    }

    public boolean erTapendeBehandling(Behandling behandling) {
        if (erBerørtBehandling(behandling)) {
            return true;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)) {
            return false;
        }
        var annenpartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(behandling.getFagsak().getSaksnummer());
        if (annenpartBehandling.isEmpty() || !harUttak(annenpartBehandling.get())) {
            return false;
        }
        return annenpartSøknadMottattEtterSøkersSøknad(behandling, annenpartBehandling.get());
    }

    private boolean harUttak(Behandling annenpartBehandling) {
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(annenpartBehandling.getId()).isPresent();
    }

    private boolean annenpartSøknadMottattEtterSøkersSøknad(Behandling søkersBehandling, Behandling annenpartBehandling) {
        var nyesteSøkersSøknad = søknadRepository.hentSøknad(søkersBehandling.getId());
        var nyesteAnnenpartSøknad = søknadRepository.hentSøknad(annenpartBehandling.getId());
        if (nyesteSøkersSøknad == null || nyesteAnnenpartSøknad == null) {
            LOG.info("Behandling {} mangler søknad", nyesteSøkersSøknad == null ? søkersBehandling.getId() : annenpartBehandling.getId());
            return false;
        }
        if (mottattSammeDag(nyesteSøkersSøknad, nyesteAnnenpartSøknad)) {
            return nyesteAnnenpartSøknad.getOpprettetTidspunkt().isAfter(nyesteSøkersSøknad.getOpprettetTidspunkt());
        }
        return nyesteAnnenpartSøknad.getMottattDato().isAfter(nyesteSøkersSøknad.getMottattDato());
    }

    private boolean mottattSammeDag(SøknadEntitet nyesteSøkersSøknad, SøknadEntitet nyesteAnnenpartSøknad) {
        return nyesteAnnenpartSøknad.getMottattDato().isEqual(nyesteSøkersSøknad.getMottattDato());
    }

    private boolean erBerørtBehandling(Behandling behandling) {
        return behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING);
    }
}
