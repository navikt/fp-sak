package no.nav.foreldrepenger.produksjonsstyring.fagsakstatus;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse.HendelseForBehandling;
import no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse.PubliserBehandlingHendelseTask;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class OppdaterFagsakStatusTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterFagsakStatusTjeneste.class);
    private static final boolean IS_LOCAL = Environment.current().isLocal();

    private FagsakRepository fagsakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FptilbakeRestKlient fptilbakeRestKlient;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public OppdaterFagsakStatusTjeneste() {
        //CDI
    }

    @Inject
    public OppdaterFagsakStatusTjeneste(FagsakRepository fagsakRepository,
                                        BehandlingsresultatRepository behandlingsresultatRepository,
                                        BehandlingRepository behandlingRepository,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                        ProsessTaskTjeneste prosessTaskTjeneste,
                                        FptilbakeRestKlient fptilbakeRestKlient) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    public void oppdaterFagsakNårBehandlingOpprettet(Fagsak fagsak, Long behandlingId, BehandlingStatus nyStatus) {
        //Fagsak har status under behandling eller løpende - det opprettes ny behandling
        if (erBehandlingOpprettetEllerUnderBehandling(nyStatus)) {
            oppdaterFagsakStatus(fagsak, behandlingId, FagsakStatus.UNDER_BEHANDLING);
        } else {
            throw new IllegalStateException(String.format("Utviklerfeil: oppdaterFagsakNårBehandlingOpprettet ble trigget for behandlingId %s med status %s. Det skal ikke skje og må følges opp",
                behandlingId , nyStatus));
        }
    }

    public void oppdaterFagsakNårBehandlingAvsluttet(Fagsak fagsak, Long behandlingId) {
        oppdaterFagsakStatusNårAlleBehandlingerErLukket(fagsak, behandlingId, false);
    }

    public void lagBehandlingAvsluttetTask(Fagsak fagsak, Long behandlingId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BehandlingAvsluttetHendelseTask.class);
        if (behandlingId != null) {
            prosessTaskData.setBehandling(fagsak.getSaksnummer().getVerdi(), fagsak.getId(), behandlingId);
        } else {
            prosessTaskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        }
        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    public FagsakStatusOppdateringResultat oppdaterFagsakStatusNårAutomatiskAvslBatch(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            throw new IllegalStateException(String.format("Utviklerfeil: BehandlingId: %s med ytelsestype %s skal ikke trigges av AutomatiskFagsakAvslutningTask. Noe er galt.",  behandling.getId() ,behandling.getStatus()));
        } else {
            if (alleAndreBehandlingerErLukket(fagsak, behandling.getId())) {
                oppdaterFagsakStatus(fagsak, behandling.getId(), FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            }
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    public void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak) {
        oppdaterFagsakStatusNårAlleBehandlingerErLukket(fagsak, null, true);
    }

    public FagsakStatusOppdateringResultat oppdaterFagsakStatusNårAlleBehandlingerErLukket(Fagsak fagsak,
                                                                                           Long behandlingId,
                                                                                           boolean tvingAvsluttSak) {
        if (alleAndreBehandlingerErLukket(fagsak, behandlingId)) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
                oppdaterFagsakStatus(fagsak,behandlingId, FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            } else {
                if ((behandlingId == null && tvingAvsluttSak) || ingenLøpendeYtelseVedtak(fagsak)) {
                    oppdaterFagsakStatus(fagsak, behandlingId, FagsakStatus.AVSLUTTET);
                    return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
                }
                oppdaterFagsakStatus(fagsak, behandlingId, FagsakStatus.LØPENDE);
                return FagsakStatusOppdateringResultat.FAGSAK_LØPENDE;
            }
        } else if (!erFagsakOpprettetEllerUnderBehandling(fagsak.getStatus())) {
            oppdaterFagsakStatus(fagsak, behandlingId, FagsakStatus.UNDER_BEHANDLING);
            return FagsakStatusOppdateringResultat.FAGSAK_UNDER_BEHANDLING;
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    private boolean ingenLøpendeYtelseVedtak(Fagsak fagsak) {
        var sisteYtelsesvedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());

        if (sisteYtelsesvedtak.isPresent()) {
            //avlutter fagsak når avslått da fagsakstatus er under behandling, og vil ikke plukkes opp av AutomatiskFagsakAvslutningBatchTjeneste
            if (erBehandlingResultatAvslått(sisteYtelsesvedtak.get())) return true;

            //Dersom saken har en avslutningsdato vil avslutning av saken hånderes av AutomatiskFagsakAvslutningBatchTjeneste
            //Hvis den ikke har en avslutningsdato skal den derfor avsluttes
            return ingenAvslutningsdato(fagsak);
        }
        return true;
    }

    private boolean erBehandlingOpprettetEllerUnderBehandling(BehandlingStatus status) {
        return BehandlingStatus.OPPRETTET.equals(status) || BehandlingStatus.UTREDES.equals(status) ;
    }

    private boolean erFagsakOpprettetEllerUnderBehandling(FagsakStatus status) {
        return FagsakStatus.OPPRETTET.equals(status) || FagsakStatus.UNDER_BEHANDLING.equals(status) ;
    }

    private void oppdaterFagsakStatus(Fagsak fagsak, Long behandlingId, FagsakStatus nyStatus) {
        var gammelStatus = fagsak.getStatus();
        if (Objects.equals(gammelStatus, nyStatus)) {
            return;
        }
        var fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);


        // TODO: palfi / espenwaaga- trenger vi denne ?
        if (behandlingId != null && FagsakStatus.AVSLUTTET.equals(nyStatus)) {
            try {
                // Bruker AVSLUTTET her for behandling som allerede er avsluttet. Dette er for å trigge ny opphenting av sak i fpoversikt.
                // Det vil i realiteten være to AVSLUTTET behandling hendelser på siste behandling i avsluttet fagsak.
                var task = PubliserBehandlingHendelseTask.prosessTask(fagsak.getSaksnummer(), fagsakId, behandlingId, HendelseForBehandling.AVSLUTTET);
                prosessTaskTjeneste.lagre(task);
            } catch (Exception ex) {
                LOG.warn("Publisering av FagsakAvsluttet feilet ved fagsak avsluttet", ex);
            }
        }

        if (gammelStatus == null) {
            LOG.info("Fagsak status opprettet: id [{}]; type [{}];", fagsak.getId(), fagsak.getYtelseType());
        } else {
            var gammelStatusKode = gammelStatus.getKode();
            var nyStatusKode = Optional.ofNullable(nyStatus).map(FagsakStatus::getKode).orElse("null");

            if (behandlingId != null) {
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}] behandlingId [{}]", gammelStatusKode, nyStatusKode, fagsak.getId(), behandlingId);
            } else {
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}]", gammelStatusKode, nyStatusKode, fagsak.getId());
            }
        }
    }

    private boolean erBehandlingResultatAvslått(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatAvslått)
            .isPresent();
    }

    private boolean alleAndreBehandlingerErLukket(Fagsak fagsak, Long behandlingId) {
        var antallÅpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsak.getId())
            .stream()
            .filter(b -> !b.getId().equals(behandlingId))
            .count();
        if (antallÅpneBehandlinger > 0) {
            return false;
        }
        return harIngenÅpenTilbakeBehandling(fagsak);
    }

    private boolean harIngenÅpenTilbakeBehandling(Fagsak fagsak) {
        return IS_LOCAL || !fptilbakeRestKlient.harÅpenBehandling(fagsak.getSaksnummer());
    }

    private boolean ingenAvslutningsdato(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak).map(FagsakRelasjon::getAvsluttningsdato).isEmpty();
    }
}
