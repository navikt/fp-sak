package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

@ApplicationScoped
public class FpOversiktDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktDtoTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public FpOversiktDtoTjeneste(BehandlingRepository behandlingRepository,
                                 BehandlingVedtakRepository vedtakRepository,
                                 FagsakRelasjonRepository fagsakRelasjonRepository,
                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                 PersonopplysningTjeneste personopplysningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vedtakRepository = vedtakRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    FpOversiktDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Henter saksnummer for behandling {}", behandling.getId());
        var fagsak = behandling.getFagsak();
        LOG.info("Returnerer sak med saksnummer {}", fagsak.getSaksnummer());

        var saksnummer = fagsak.getSaksnummer().getVerdi();
        var aktørId = behandling.getAktørId().getId();
        return switch (fagsak.getYtelseType()) {
            case ENGANGSTØNAD -> new EsSak(saksnummer, aktørId);
            case FORELDREPENGER -> new FpSak(saksnummer, aktørId, finnVedtakForForeldrepenger(fagsak),
                oppgittAnnenPart(fagsak).map(AktørId::getId).orElse(null));
            case SVANGERSKAPSPENGER -> new SvpSak(saksnummer, aktørId);
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + fagsak.getYtelseType());
        };
    }

    private Optional<AktørId> oppgittAnnenPart(Fagsak fagsak) {
        var førstegangsbehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD)
            .orElseGet(() -> behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())
                .stream()
                .max(Comparator.comparing(Behandling::getOpprettetDato))
                .orElseThrow());
        return personopplysningTjeneste.hentOppgittAnnenPartAktørId(førstegangsbehandling.getId());
    }

    private Set<FpSak.Vedtak> finnVedtakForForeldrepenger(Fagsak fagsak) {
        var behandlingerMedVedtak = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsak.getId());
        return behandlingerMedVedtak.stream()
            .map(b -> vedtakRepository.hentForBehandlingHvisEksisterer(b.getId()))
            .filter(Optional::isPresent)
            .map(v -> v.get())
            .map(vedtak -> tilDto(vedtak, fagsak))
            .collect(Collectors.toSet());
    }

    private FpSak.Vedtak tilDto(BehandlingVedtak vedtak, Fagsak fagsak) {
        var dekningsgrad = finnDekningsgrad(fagsak);
        var uttaksperioder = finnUttaksperioder(vedtak.getBehandlingsresultat().getBehandlingId());
        return new FpSak.Vedtak(vedtak.getVedtakstidspunkt(), uttaksperioder, dekningsgrad);
    }

    private List<FpSak.Uttaksperiode> finnUttaksperioder(Long behandlingId) {
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandlingId)
            .map(uttak -> tilDto(uttak.getGjeldendePerioder()))
            .orElse(List.of());
    }

    private List<FpSak.Uttaksperiode> tilDto(List<ForeldrepengerUttakPeriode> gjeldendePerioder) {
        return gjeldendePerioder.stream().map(this::tilDto).toList();
    }

    private FpSak.Uttaksperiode tilDto(ForeldrepengerUttakPeriode periode) {
        var type = switch (periode.getResultatType()) {
            case INNVILGET -> FpSak.Uttaksperiode.Resultat.Type.INNVILGET;
            case AVSLÅTT -> FpSak.Uttaksperiode.Resultat.Type.AVSLÅTT;
            case MANUELL_BEHANDLING -> throw new IllegalStateException("Forventer ikke perioder under manuell behandling");
        };
        var resultat = new FpSak.Uttaksperiode.Resultat(type);
        return new FpSak.Uttaksperiode(periode.getFom(), periode.getTom(), resultat);
    }

    private FpSak.Vedtak.Dekningsgrad finnDekningsgrad(Fagsak fagsak) {
        var dekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(fagsak).getGjeldendeDekningsgrad();
        return dekningsgrad.isÅtti() ? FpSak.Vedtak.Dekningsgrad.ÅTTI : FpSak.Vedtak.Dekningsgrad.HUNDRE;
    }
}
