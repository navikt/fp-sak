package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.sammebarn;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class YtelserSammeBarnTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(YtelserSammeBarnTjeneste.class);

    private static final Set<FagsakYtelseType> SAKSTYPER = Set.of(FagsakYtelseType.ENGANGSTØNAD, FagsakYtelseType.FORELDREPENGER);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    YtelserSammeBarnTjeneste() {
        // CDI
    }

    @Inject
    public YtelserSammeBarnTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                    FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    /**
     * Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til
     * behandling i VL (som ennå ikke har vedtak).
     */
    public boolean harAktørAnnenSakMedSammeFamilieHendelse(Saksnummer saksnummer, Long behandlingId, AktørId aktørId) {
        var aktuellFamilieHendelse = gjeldendeFamilieHendelse(behandlingId).orElse(null);
        if (aktuellFamilieHendelse == null) {
            LOG.warn("Aksjonspunktutleder Samtidig Ytelse: Behandling uten FamilieHendelse");
            return false;
        }

        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> !saksnummer.equals(sak.getSaksnummer()) && SAKSTYPER.contains(sak.getYtelseType()))
            .map(this::gjeldendeFamilieHendelse)
            .flatMap(Optional::stream)
            .anyMatch(fh -> familieHendelseTjeneste.matcherGrunnlagene(aktuellFamilieHendelse, fh));
    }

    private Optional<FamilieHendelseGrunnlagEntitet> gjeldendeFamilieHendelse(Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId())
            .filter(this::ikkeAvslått)
            .flatMap(this::gjeldendeFamilieHendelse)
            .or(() -> behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
                .filter(this::ikkeAvslått)
                .flatMap(this::gjeldendeFamilieHendelse));
    }

    private Optional<FamilieHendelseGrunnlagEntitet> gjeldendeFamilieHendelse(Behandling behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling.getId());
    }

    private Optional<FamilieHendelseGrunnlagEntitet> gjeldendeFamilieHendelse(Long behandlingId) {
        return familieHendelseTjeneste.finnAggregat(behandlingId);
    }

    private boolean ikkeAvslått(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .map(Behandlingsresultat::getBehandlingResultatType)
            .filter(BehandlingResultatType.AVSLÅTT::equals)
            .isEmpty();
    }

}
