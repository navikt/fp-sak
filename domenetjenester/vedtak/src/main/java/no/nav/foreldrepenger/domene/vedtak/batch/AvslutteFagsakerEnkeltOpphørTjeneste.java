package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.vedtak.intern.AutomatiskFagsakAvslutningTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;


@ApplicationScoped
public class AvslutteFagsakerEnkeltOpphørTjeneste {
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;


    private static final Logger LOG = LoggerFactory.getLogger(AvslutteFagsakerEnkeltOpphørTjeneste.class);

    public AvslutteFagsakerEnkeltOpphørTjeneste() {
        //CDI
    }

    @Inject
    public AvslutteFagsakerEnkeltOpphørTjeneste(BehandlingRepository behandlingRepository,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                BehandlingsresultatRepository behandlingsresultatRepository,
                                                BeregningsresultatRepository beregningsresultatRepository,
                                                FagsakRepository fagsakRepository,
                                                ProsessTaskTjeneste taskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.fagsakRepository = fagsakRepository;
        this.taskTjeneste = taskTjeneste;
    }

    public int avslutteSakerMedEnkeltOpphør() {
        var aktuelleFagsaker = fagsakRepository.hentFagsakerRelevanteForAvslutning();

        var antallSakerSomSkalAvsluttes = 0;

        var dato = LocalDate.now();
        var baseline = LocalTime.now();
        var callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);

        for (var fagsak : aktuelleFagsaker) {
            var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Ugyldig tilstand for faksak " + fagsak.getSaksnummer()));

            if (!alleBarnaErDøde(sisteBehandling) && erBehandlingResultatOpphørt(sisteBehandling)) {
                var opphørsdato = hentSisteUtbetalingsdato(sisteBehandling).plusDays(1);
                LOG.info("AvslutteFagsakerEnkeltOpphørTjeneste: Sak med {} oppfyller kriteriene. Opphørsdato + 3 måneder: {}", fagsak.getSaksnummer().toString(), leggPåSøknadsfristMåneder(opphørsdato));

                if (LocalDate.now().isAfter(leggPåSøknadsfristMåneder(opphørsdato))) {

                    var avslutningstaskData = opprettFagsakAvslutningTask(fagsak, callId, dato, baseline, 7199);
                    taskTjeneste.lagre(avslutningstaskData);

                    LOG.info("AvslutteFagsakerEnkeltOpphørTjeneste: Sak med {} vil avsluttes.", fagsak.getSaksnummer().toString());
                    antallSakerSomSkalAvsluttes++;
                }
            }
        }
        return antallSakerSomSkalAvsluttes;
    }

    private LocalDate leggPåSøknadsfristMåneder(LocalDate fraDato) {
        return fraDato.plusMonths(3).with(TemporalAdjusters.lastDayOfMonth());
    }

    private LocalDate hentSisteUtbetalingsdato(Behandling sisteBehandling) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(sisteBehandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .orElse(LocalDate.now().minusMonths(4));
    }

    private boolean alleBarnaErDøde(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList())
            .stream().allMatch(b-> b.getDødsdato().isPresent());
    }

    private boolean erBehandlingResultatOpphørt(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatOpphørt)
            .isPresent();
    }

    private ProsessTaskData opprettFagsakAvslutningTask(Fagsak fagsak, String callId, LocalDate dato, LocalTime tid, int spread) {
        var nesteKjøring = LocalDateTime.of(dato, tid.plusSeconds(Math.abs(System.nanoTime()) % spread));
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskFagsakAvslutningTask.class);
        prosessTaskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        prosessTaskData.setNesteKjøringEtter(nesteKjøring);
        prosessTaskData.setCallId(callId + "_" + fagsak.getId());
        return prosessTaskData;
    }
}
