package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.vedtak.intern.AutomatiskFagsakAvslutningTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
public class AvslutteFagsakerEnkeltOpphørTjeneste {
    EntityManager entityManager;
    BehandlingRepository behandlingRepository;
    FamilieHendelseRepository familieHendelseRepository;
    BehandlingsresultatRepository behandlingsresultatRepository;
    BeregningsresultatRepository beregningsresultatRepository;

    private static final Logger LOG = LoggerFactory.getLogger(AvslutteFagsakerEnkeltOpphørTjeneste.class);

    public AvslutteFagsakerEnkeltOpphørTjeneste() {
        //CDI
    }

    @Inject
    public AvslutteFagsakerEnkeltOpphørTjeneste(EntityManager entityManager,
                                                BehandlingRepository behandlingRepository,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                BehandlingsresultatRepository behandlingsresultatRepository,
                                                BeregningsresultatRepository beregningsresultatRepository) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public int avslutteSakerMedEnkeltOpphør() {
        List<Fagsak> aktuelleFagsaker = hentaktuelleFagsaker();
        int antallSakerSomSkalAvsluttes = 0;

        if (!aktuelleFagsaker.isEmpty()) {
            for (Fagsak fagsak : aktuelleFagsaker) {
                Behandling sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Ugyldig tilstand for faksak " + fagsak.getSaksnummer()));

                if (!alleBarnaErDøde(sisteBehandling) && erBehandlingResultatOpphørt(sisteBehandling)) {
                    //avslutter saken om opphørsdato + 3 måneder er passert dagens dato
                    var opphørsdato = hentSisteUtbetalingsdato(fagsak, sisteBehandling).plusDays(1);
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
        }
        return antallSakerSomSkalAvsluttes;
    }

    private LocalDate leggPåSøknadsfristMåneder(LocalDate fraDato) {
        return fraDato.plusMonths(3).with(TemporalAdjusters.lastDayOfMonth());
    }

    private LocalDate hentSisteUtbetalingsdato(Fagsak fagsak, Behandling sisteBehandling) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(sisteBehandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .max(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).orElseThrow(() -> new IllegalStateException("Ugyldig tilstand for faksak " + fagsak.getSaksnummer()));
    }

    @SuppressWarnings("unchecked")
    public List<Fagsak> hentaktuelleFagsaker() {
        var query = entityManager.createNativeQuery("""
        select f.*
        from fpsak.fagsak f
        join fpsak.fagsak_relasjon fr  on (aktiv = :aktiv and f.id in (fagsak_en_id, fagsak_to_id))
        where fagsak_to_id is not null
        and fagsak_status = :lopende
        and exists (select * from fpsak.fagsak f2 join fpsak.behandling b on b.fagsak_id = f2.id join fpsak.behandling_resultat br on br.behandling_id = b.id where f2.id = f.id and br.behandling_resultat_type = :opphor)
        and not exists (select * from fpsak.behandling b2 where b2.fagsak_id = f.id and behandling_status <> :avsluttet)
        and not exists (select * from fpsak.gr_nestesak ns join fpsak.behandling b3 on b3.id = ns.behandling_id join fpsak.fagsak f3 on f3.id = b3.fagsak_id where f3.id = f.ID and ns.aktiv = :aktiv )
        """, Fagsak.class);
        query.setParameter("aktiv", true);
        query.setParameter("lopende", FagsakStatus.LØPENDE);
        query.setParameter("opphor", BehandlingResultatType.OPPHØR);
        query.setParameter("avsluttet", BehandlingStatus.AVSLUTTET);

        return query.getResultList();
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
