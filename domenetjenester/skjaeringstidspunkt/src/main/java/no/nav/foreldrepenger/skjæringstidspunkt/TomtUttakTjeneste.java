package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class TomtUttakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TomtUttakTjeneste.class);
    private static final boolean ER_PROD = Environment.current().isProd();

    private FamilieHendelseRepository familieGrunnlagRepository;
    private FpUttakRepository fpUttakRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private UtsettelseCore2021 utsettelse2021;

    TomtUttakTjeneste() {
        // CDI
    }

    @Inject
    public TomtUttakTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             UtsettelseCore2021 utsettelse2021) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.utsettelse2021 = utsettelse2021;
    }

    public Optional<LocalDate> startdatoUttakResultatFrittUttak(Fagsak fagsak) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .filter(b -> !kreverSammenhengendeUttak(b))
            .map(b -> fpUttakRepository.hentUttakResultat(b.getId()).getGjeldendePerioder())
            .flatMap(ur -> UtsettelseCore2021.finnFørsteDatoFraUttakResultat(ur.getPerioder(), false));
    }

    private boolean kreverSammenhengendeUttak(Behandling behandling) {
        var sammenhengendeUttak = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> finnFHSistVedtatteBehandlingKobletFagsak(behandling))
            .map(utsettelse2021::kreverSammenhengendeUttak).orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
        if (!sammenhengendeUttak && ER_PROD) {
            LOG.info("Prod uten krav om sammenhengende periode - sjekk om korrekt, saksnummer {}", behandling.getFagsak().getSaksnummer());
        } else if (!sammenhengendeUttak) {
            LOG.info("Non-prod uten krav om sammenhengende periode, saksnummer {} behandling {}", behandling.getFagsak().getSaksnummer(), behandling.getId());
        }
        return sammenhengendeUttak;
    }

    private Optional<FamilieHendelseGrunnlagEntitet> finnFHSistVedtatteBehandlingKobletFagsak(Behandling behandling) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
            .flatMap(fr -> fr.getRelatertFagsak(behandling.getFagsak()))
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(b -> familieGrunnlagRepository.hentAggregatHvisEksisterer(b.getId()));
    }
}
