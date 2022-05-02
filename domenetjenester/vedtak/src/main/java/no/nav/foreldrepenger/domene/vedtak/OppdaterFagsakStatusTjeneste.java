package no.nav.foreldrepenger.domene.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class OppdaterFagsakStatusTjeneste {

    protected FagsakRepository fagsakRepository;
    protected FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    protected BehandlingsresultatRepository behandlingsresultatRepository;
    protected BehandlingRepository behandlingRepository;
    protected FagsakRelasjonRepository fagsakRelasjonRepository;

    public OppdaterFagsakStatusTjeneste() {
        //CDI
    }

    @Inject
    public OppdaterFagsakStatusTjeneste(FagsakRepository fagsakRepository,
                                        FagsakStatusEventPubliserer fagsakStatusEventPubliserer,
                                        BehandlingsresultatRepository behandlingsresultatRepository,
                                        BehandlingRepository behandlingRepository,
                                        FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    public void oppdaterFagsakNårBehandlingOpprettet(Behandling behandling) {
        //Fagsak har status under behandling eller løpende - det opprettes ny behandling
        if (erBehandlingOpprettetEllerUnderBehandling(behandling.getStatus())) {
            oppdaterFagsakStatus(behandling.getFagsak(), behandling, FagsakStatus.UNDER_BEHANDLING);
        } else {
            throw new IllegalStateException(String.format("Utviklerfeil: oppdaterFagsakNårBehandlingOpprettet ble trigget for behandlingId %s med status %s. Det skal ikke skje og må følges opp",
                behandling.getId() ,behandling.getStatus()));
        }
    }

    public void oppdaterFagsakNårBehandlingAvsluttet(Behandling behandling) {
        //Fagsakstatus har som oftest under behandling
        if (BehandlingStatus.AVSLUTTET.equals(behandling.getStatus())) {
            oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        } else {
            throw new IllegalStateException(String.format("Utviklerfeil: oppdaterFagsakNårBehandlingAvsluttet ble trigget for behandlingId %s med status %s. Det skal ikke skje og må følges opp",
                behandling.getId() ,behandling.getStatus()));
        }
    }

    public FagsakStatusOppdateringResultat oppdaterFagsakStatusNårAutomatiskAvslBatch(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            throw new IllegalStateException(String.format("Utviklerfeil: BehandlingId: %s med ytelsestype %s skal ikke trigges av AutomatiskFagsakAvslutningTask. Noe er galt.",  behandling.getId() ,behandling.getStatus()));
        } else {
            if (alleAndreBehandlingerErLukket(fagsak, behandling)) {
                oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            }
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    public void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak) {
        oppdaterFagsakStatusNårAlleBehandlingerErLukket(fagsak, null);
    }

    public void settUnderBehandlingNårAktiveBehandlinger(Fagsak fagsak) {
        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsak.getId()) && FagsakStatus.LØPENDE.equals(fagsak.getStatus())) {
            var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
            oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.UNDER_BEHANDLING);
        }
    }

    public FagsakStatusOppdateringResultat oppdaterFagsakStatusNårAlleBehandlingerErLukket(Fagsak fagsak, Behandling behandling) {
        if (alleAndreBehandlingerErLukket(fagsak, behandling)) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
                oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            } else {
                if (behandling == null || ingenLøpendeYtelseVedtak(behandling)) {
                    oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
                    return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
                }
                oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.LØPENDE);
                return FagsakStatusOppdateringResultat.FAGSAK_LØPENDE;
            }
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    private boolean ingenLøpendeYtelseVedtak(Behandling behandling) {
        var sisteYtelsesvedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

        if (sisteYtelsesvedtak.isPresent()) {
            //avlutter fagsak til avsluttet med en gang siden de som oftest har fagsakstatus under behandling, og vil derfor ikke plukkes opp av AutomatiskFagsakAvslutningBatchTjeneste
            if (erBehandlingResultatAvslått(sisteYtelsesvedtak.get())) return true;
            //Dersom saken har en avslutningsdato vil avslutning av saken hånderes av AutomatiskFagsakAvslutningBatchTjeneste
            //Hvis den ikke har en avslutningsdato kan den avsluttes
            return ingenAvslutningsdato(behandling.getFagsakId());
        }
        return true;
    }

    private boolean erBehandlingOpprettetEllerUnderBehandling(BehandlingStatus status) {
        return BehandlingStatus.OPPRETTET.equals(status) || BehandlingStatus.UTREDES.equals(status) ;
    }

    private void oppdaterFagsakStatus(Fagsak fagsak, Behandling behandling, FagsakStatus nyStatus) {
        var gammelStatus = fagsak.getStatus();
        var fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);

        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(fagsak, behandling, gammelStatus, nyStatus);
        }
    }

    private boolean erBehandlingResultatAvslått(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatAvslått)
            .isPresent();
    }

    private boolean alleAndreBehandlingerErLukket(Fagsak fagsak, Behandling behandling) {
        return behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsak.getId())
            .stream()
            .filter(b -> behandling == null || !b.getId().equals(behandling.getId()))
            .count() <= 0;
    }

    private boolean ingenAvslutningsdato(Long fagsakId) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId).map(FagsakRelasjon::getAvsluttningsdato).isEmpty();
    }
}
