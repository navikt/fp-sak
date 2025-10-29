package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.sammebarn;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

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
    public boolean harAktørAnnenSakMedSammeFamilieHendelse(BehandlingReferanse ref) {
        return !andreSakerMedSammeFamilieHendelse(ref).isEmpty();
    }

    public Collection<Saksnummer> andreSakerMedSammeFamilieHendelse(BehandlingReferanse ref) {
        var aktuellFamilieHendelse = gjeldendeFamilieHendelse(ref.behandlingId()).orElse(null);
        if (aktuellFamilieHendelse == null) {
            LOG.warn("Aksjonspunktutleder Samtidig Ytelse: Behandling uten FamilieHendelse");
            return Set.of();
        }

        return fagsakRepository.hentForBruker(ref.aktørId()).stream()
            .filter(sak -> !ref.saksnummer().equals(sak.getSaksnummer()) && SAKSTYPER.contains(sak.getYtelseType()))
            .filter(sak -> gjelderSammeFamilieHendelse(sak, aktuellFamilieHendelse))
            .map(Fagsak::getSaksnummer)
            .collect(Collectors.toSet());
    }

    private boolean gjelderSammeFamilieHendelse(Fagsak fagsak, FamilieHendelseGrunnlagEntitet fhg) {
        return gjeldendeFamilieHendelse(fagsak)
            .filter(sakfh -> familieHendelseTjeneste.matcherGrunnlagene(sakfh, fhg))
            .isPresent();
    }

    private Optional<FamilieHendelseGrunnlagEntitet> gjeldendeFamilieHendelse(Fagsak fagsak) {
        return behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .map(this::gjeldendeFamilieHendelse)
            .flatMap(Optional::stream)
            .max(Comparator.comparing(FamilieHendelseGrunnlagEntitet::getOpprettetTidspunkt))
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
