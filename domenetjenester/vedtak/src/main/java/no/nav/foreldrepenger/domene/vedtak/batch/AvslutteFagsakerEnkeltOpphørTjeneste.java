package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

@ApplicationScoped
public class AvslutteFagsakerEnkeltOpphørTjeneste {
    BehandlingRepository behandlingRepository;
    FamilieHendelseRepository familieHendelseRepository;
    BehandlingsresultatRepository behandlingsresultatRepository;
    BeregningsresultatRepository beregningsresultatRepository;
    FagsakRepository fagsakRepository;

    private static final Logger LOG = LoggerFactory.getLogger(AvslutteFagsakerEnkeltOpphørTjeneste.class);

    public AvslutteFagsakerEnkeltOpphørTjeneste() {
        //CDI
    }

    @Inject
    public AvslutteFagsakerEnkeltOpphørTjeneste(BehandlingRepository behandlingRepository,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                BehandlingsresultatRepository behandlingsresultatRepository,
                                                BeregningsresultatRepository beregningsresultatRepository,
                                                FagsakRepository fagsakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.fagsakRepository = fagsakRepository;
    }

    public int avslutteSakerMedEnkeltOpphør() {
        List<Fagsak> aktuelleFagsaker = fagsakRepository.hentFagsakerRelevanteForAvslutning();

        int antallSakerSomSkalAvsluttes = 0;

        for (Fagsak fagsak : aktuelleFagsaker) {
            Behandling sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Ugyldig tilstand for faksak " + fagsak.getSaksnummer()));

            if (!alleBarnaErDøde(sisteBehandling) && erBehandlingResultatOpphørt(sisteBehandling)) {
                //avslutter saken om opphørsdato + 3 måneder er passert dagens dato
                var opphørsdato = hentSisteUtbetalingsdato(sisteBehandling).plusDays(1);
                LOG.info("AvslutteFagsakerEnkeltOpphørTjeneste: Sak {} er plukket ut, og oppfyller kriteriene. Opphørsdato + 3 måneder er {}", fagsak.getSaksnummer().toString(), leggPåSøknadsfristMåneder(opphørsdato));

                if (leggPåSøknadsfristMåneder(opphørsdato).isAfter(LocalDate.now())) {
                    //kommenterer ut dette inntil sjekket i produksjon
                    //var callId = MDCOperations.getCallId();
                    //callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

                    //opprettFagsakAvslutningTask(fagsak, callId + fagsak.getSaksnummer());
                    LOG.info("Sak med saksnummer {} skal avsluttes.", fagsak.getSaksnummer().toString());
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

    private ProsessTaskData opprettFagsakAvslutningTask(Fagsak fagsak, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskFagsakAvslutningTask.class);
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setSaksnummer(fagsak.getSaksnummer().toString());
        prosessTaskData.setCallId(callId);
        return prosessTaskData;
    }
}
