package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.feilpraksisutsettelse.forlenget", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisForlengetSaksbehandlingstidTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisForlengetSaksbehandlingstidTask.class);
    private static final String DRY_RUN = "dryRun";

    private final DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private final EntityManager entityManager;
    private final DokumentBehandlingTjeneste dokumentBehandlingTjeneste;


    @Inject
    FeilPraksisForlengetSaksbehandlingstidTask(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                               EntityManager entityManager,
                                               DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.entityManager = entityManager;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var kandidater = finnKandidater();
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();
        kandidater.forEach(k -> opprettBestillingTask(k, dryRun));
    }

    private List<Behandling> finnKandidater() {
        return entityManager.createQuery("""
                                        select b from Behandling b
                            join BehandlingÅrsak ba on ba.behandling = b
                            where ba.behandlingÅrsakType =:årsak
                            and not exists (select 1 from Behandling b2 where b2.avsluttetDato is null and b2.fagsak = b.fagsak and b2.behandlingType in (:ikkeTyper))
                            and b.avsluttetDato is null
                """, Behandling.class)
            .setParameter("årsak", BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE)
            .setParameter("ikkeTyper", Set.of(BehandlingType.KLAGE, BehandlingType.ANKE))
            .getResultList();
    }

    private void opprettBestillingTask(Behandling behandling, boolean dryRun) {
        if (dokumentBehandlingTjeneste.erDokumentBestiltForFagsak(behandling.getFagsakId(),
            DokumentMalType.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID)) {
            LOG.info("Brev allerede bestilt for {}", behandling.getSaksnummer());
            return;
        }

        var saksnummer = behandling.getSaksnummer();
        LOG.info("Bestiller forlenget saksbehandlingstid brev for {} {}", saksnummer, behandling.getId());
        var behandlingUuid = behandling.getUuid();
        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(saksnummer)
            .medDokumentMal(DokumentMalType.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID)
            .build();
        if (!dryRun) {
            dokumentBestillerTjeneste.bestillDokument(bestilling);
        }
    }
}
