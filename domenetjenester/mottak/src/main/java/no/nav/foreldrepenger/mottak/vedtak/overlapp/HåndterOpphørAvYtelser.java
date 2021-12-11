package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 *  Dersom det er identifisert overlapp av VurderOpphørAvYtelser, vil denne tjenesten opprette en
 *  "vurder konsekvens for ytelse"-oppgave i Gosys, og en revurdering med egen årsak slik at saksbehandler kan vurdere
 *  om opphør skal gjennomføres eller ikke. Saksbehandling må skje manuelt, og fritekstbrev må benyttes for opphør av løpende sak.
 */
@ApplicationScoped
public class HåndterOpphørAvYtelser {
    private static final Logger LOG = LoggerFactory.getLogger(HåndterOpphørAvYtelser.class);

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private RevurderingTjeneste revurderingTjenesteFP;
    private RevurderingTjeneste revurderingTjenesteSVP;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private KøKontroller køKontroller;

    @Inject
    public HåndterOpphørAvYtelser(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                  @FagsakYtelseTypeRef("FP") RevurderingTjeneste revurderingTjenesteFP,
                                  @FagsakYtelseTypeRef("SVP") RevurderingTjeneste revurderingTjenesteSVP,
                                  ProsessTaskTjeneste taskTjeneste,
                                  BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  KøKontroller køKontroller) {
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.taskTjeneste = taskTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjenesteFP = revurderingTjenesteFP;
        this.revurderingTjenesteSVP = revurderingTjenesteSVP;
        this.køKontroller = køKontroller;
    }

    HåndterOpphørAvYtelser() {
        // CDI
    }

    void oppdaterEllerOpprettRevurdering(Fagsak fagsak, String beskrivelse, BehandlingÅrsakType årsakType) {
        var eksisterendeBehandling = finnÅpenOrdinærYtelsesbehandlingUtenÅrsakType(fagsak, årsakType);

        if (eksisterendeBehandling != null) {
            opprettVurderKonsekvens(eksisterendeBehandling, beskrivelse);
            oppdatereBehMedÅrsak(eksisterendeBehandling.getId(), årsakType);
        } else {
            behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId())
                .filter(b -> !b.harBehandlingÅrsak(årsakType))
                .ifPresent(b -> {
                    var enhet = opprettVurderKonsekvens(b, beskrivelse);

                    fagsakLåsRepository.taLås(fagsak.getId());
                    var skalKøes = køKontroller.skalEvtNyBehandlingKøes(fagsak);
                    var revurdering = opprettRevurdering(fagsak, årsakType, enhet, skalKøes);
                    if (revurdering != null) {
                        LOG.info("Overlapp FPSAK: Opprettet revurdering med behandlingId {} saksnummer {} pga {}", revurdering.getId(), fagsak.getSaksnummer(), beskrivelse);
                    } else {
                        LOG.info("Overlapp FPSAK: Kunne ikke opprette revurdering saksnummer {}", fagsak.getSaksnummer());
                    }
                });
        }
    }

    private Behandling finnÅpenOrdinærYtelsesbehandlingUtenÅrsakType(Fagsak fagsak, BehandlingÅrsakType årsakType) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .filter(b -> !BehandlingStatus.getFerdigbehandletStatuser().contains(b.getStatus()))
            .filter(b -> !b.harBehandlingÅrsak(årsakType))
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .orElse(null);
    }

    private OrganisasjonsEnhet opprettVurderKonsekvens(Behandling behandling, String beskrivelse) {
        var enhet = utledEnhetFraBehandling(behandling);
        if (beskrivelse != null) {
            opprettTaskForÅVurdereKonsekvens(behandling.getFagsakId(), enhet.enhetId(), beskrivelse, Optional.empty());
        }
        return enhet;
    }

    private Behandling opprettRevurdering(Fagsak sakRevurdering, BehandlingÅrsakType behandlingÅrsakType, OrganisasjonsEnhet enhet, boolean skalKøes) {
        var revurdering = getRevurderingTjeneste(sakRevurdering).opprettAutomatiskRevurdering(sakRevurdering, behandlingÅrsakType, enhet);

        if (skalKøes) {
            køKontroller.enkøBehandling(revurdering);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }

        return revurdering;
    }

    private RevurderingTjeneste getRevurderingTjeneste(Fagsak fagsak) {
        return FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType()) ? revurderingTjenesteSVP : revurderingTjenesteFP;
    }

    private void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId, String oppgaveBeskrivelse, Optional<String> gjeldendeAktørId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, oppgaveBeskrivelse);
        gjeldendeAktørId.ifPresent(a-> prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_GJELDENDE_AKTØR_ID, a));
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private void oppdatereBehMedÅrsak(Long behandlingId, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) return;
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!behandling.harBehandlingÅrsak(behandlingÅrsakType)) {
            var årsakBuilder = BehandlingÅrsak.builder(behandlingÅrsakType);
            behandling.getOriginalBehandlingId().ifPresent(årsakBuilder::medOriginalBehandlingId);
            årsakBuilder.buildFor(behandling);
            behandlingRepository.lagre(behandling, lås);
        }
    }

    private OrganisasjonsEnhet utledEnhetFraBehandling(Behandling behandling) {
        return behandlendeEnhetTjeneste.gyldigEnhetNfpNk(behandling.getBehandlendeEnhet()) ?
            behandling.getBehandlendeOrganisasjonsEnhet() : behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
    }
}
