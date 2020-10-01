package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTaskProperties;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class EtterkontrollTjeneste {

    private static final Logger log = LoggerFactory.getLogger(EtterkontrollTjeneste.class);
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private RevurderingHistorikk revurderingHistorikk;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private LegacyESBeregningRepository legacyESBeregningRepository;

    EtterkontrollTjeneste() {
        // for CDI proxy
    }

    @Inject
    public EtterkontrollTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 ProsessTaskRepository prosessTaskRepository,
                                 BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                 LegacyESBeregningRepository legacyESBeregningRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.revurderingHistorikk = new RevurderingHistorikk(repositoryProvider.getHistorikkRepository());
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.legacyESBeregningRepository = legacyESBeregningRepository;
    }

    public void utfør(Behandling behandlingForRevurdering, Behandling opprettetRevurdering) {

        Optional<Behandling> behandlingMedforelder = behandlingRevurderingRepository.finnSisteInnvilgetBehandlingForMedforelder(behandlingForRevurdering.getFagsak());

        if (behandlingMedforelder.isPresent()) {
            // hvem av behandlingForRevurdering  og berørtBehandling  starter uttak  sist ? Den skal køes
            log.info("Etterkontroll har funnet fagsak (id={}) på medforelder for fagsak med fagsakId={}", behandlingMedforelder.get().getFagsakId(), opprettetRevurdering.getFagsakId());
            boolean berørtBehandlingStarterUttakSist = false;
            Optional<LocalDate> førsteUttaksdato = finnFørsteUttaksdato(behandlingForRevurdering.getId());
            Optional<LocalDate> førsteUttaksdatoMedforelder = finnFørsteUttaksdato(behandlingMedforelder.get().getId());
            if ((førsteUttaksdatoMedforelder.isPresent() && førsteUttaksdato.isPresent() && førsteUttaksdatoMedforelder.get().isAfter(førsteUttaksdato.get()))
                || (førsteUttaksdatoMedforelder.isPresent() && førsteUttaksdato.isEmpty())) {
                berørtBehandlingStarterUttakSist = true;
            }

            if (berørtBehandlingStarterUttakSist) {
                opprettTaskForProsesserBehandling(opprettetRevurdering);
            } else {
                enkøBehandling(opprettetRevurdering);
                revurderingHistorikk.opprettHistorikkinnslagForVenteFristRelaterteInnslag(opprettetRevurdering.getId(), opprettetRevurdering.getFagsakId(), HistorikkinnslagType.BEH_KØET, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
            }
        } else {
            opprettTaskForProsesserBehandling(opprettetRevurdering);
        }
    }

    private Optional<LocalDate> finnFørsteUttaksdato(Long behandling) {
        var uttakResultat = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling);
        if (uttakResultat.isPresent()) {
            return Optional.of(uttakResultat.get().finnFørsteUttaksdato());
        }
        return Optional.empty();
    }

    private void opprettTaskForProsesserBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void enkøBehandling(Behandling behandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

    public boolean skalReberegneES(Behandling behandling, LocalDate fødselsdato) {
        return legacyESBeregningRepository.skalReberegne(behandling.getId(), fødselsdato);
    }
}
