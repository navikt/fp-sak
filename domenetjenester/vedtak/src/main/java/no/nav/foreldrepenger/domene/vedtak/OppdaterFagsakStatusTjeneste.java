package no.nav.foreldrepenger.domene.vedtak;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
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
    protected FamilieHendelseRepository familieHendelseRepository;

    public OppdaterFagsakStatusTjeneste() {
        //CDI
    }

    @Inject
    public OppdaterFagsakStatusTjeneste(FagsakRepository fagsakRepository,
                                        FagsakStatusEventPubliserer fagsakStatusEventPubliserer,
                                        BehandlingsresultatRepository behandlingsresultatRepository,
                                        BehandlingRepository behandlingRepository,
                                        FagsakRelasjonRepository fagsakRelasjonRepository,
                                        FamilieHendelseRepository familieHendelseRepository) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
            this.familieHendelseRepository = familieHendelseRepository;
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

    public void oppdaterFagsakNårBehandlingAvsluttet(Behandling behandling, BehandlingStatus nyStatus) {
        //Fagsakstatus har som oftest under behandling
        if (BehandlingStatus.AVSLUTTET.equals(nyStatus)) {
            oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        } else {
            throw new IllegalStateException(String.format("Utviklerfeil: oppdaterFagsakNårBehandlingAvsluttet ble trigget for behandlingId %s med status %s. Det skal ikke skje og må følges opp",
                behandling.getId() ,nyStatus));
        }
    }

    public FagsakStatusOppdateringResultat oppdaterFagsakStatusNårAutomatiskAvslBatch(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            throw new IllegalStateException(String.format("Utviklerfeil: BehandlingId: %s med ytelsestype %s skal ikke trigges av AutomatiskFagsakAvslutningTask. Noe er galt.",  behandling.getId() ,behandling.getStatus()));
        } else {
            if (alleAndreBehandlingerErLukket(fagsak, behandling)) {
                oppdaterFagsakStatus(fagsak, behandling.getId(), FagsakStatus.AVSLUTTET);
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
            oppdaterFagsakStatus(fagsak, behandling.getId(), FagsakStatus.UNDER_BEHANDLING);
        }
    }

    public FagsakStatusOppdateringResultat oppdaterFagsakStatusNårAlleBehandlingerErLukket(Fagsak fagsak, Behandling behandling) {
        var behandlingId = behandling != null ? behandling.getId() : null;
        if (alleAndreBehandlingerErLukket(fagsak, behandling)) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
                oppdaterFagsakStatus(fagsak,behandlingId, FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            } else {
                if (behandling == null || ingenLøpendeYtelseVedtak(behandling)) {
                    oppdaterFagsakStatus(fagsak, behandlingId, FagsakStatus.AVSLUTTET);
                    return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
                }
                oppdaterFagsakStatus(fagsak, behandlingId, FagsakStatus.LØPENDE);
                return FagsakStatusOppdateringResultat.FAGSAK_LØPENDE;
            }
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    private boolean ingenLøpendeYtelseVedtak(Behandling behandling) {
        var sisteYtelsesvedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

        if (sisteYtelsesvedtak.isPresent()) {
            //avlutter fagsak når avslått da fagsakstatus er under behandling, og vil ikke plukkes opp av AutomatiskFagsakAvslutningBatchTjeneste
            if (erBehandlingResultatAvslått(sisteYtelsesvedtak.get())) return true;

            //spesialhåndtering når saken er opphørt og skal avsluttes, men annen part ikke skal avsluttes - avslutningsdato kan da ikke settes. Inntil vi har løst tfp-5062
            if(behandlingHarEnkeltOpphør(behandling) && sakErKobletTIlAnnenPart(behandling)) return true;

            //Dersom saken har en avslutningsdato vil avslutning av saken hånderes av AutomatiskFagsakAvslutningBatchTjeneste
            //Hvis den ikke har en avslutningsdato skal den derfor avsluttes
            return ingenAvslutningsdato(behandling.getFagsak());
        }
        return true;
    }

    private boolean erBehandlingOpprettetEllerUnderBehandling(BehandlingStatus status) {
        return BehandlingStatus.OPPRETTET.equals(status) || BehandlingStatus.UTREDES.equals(status) ;
    }

    private void oppdaterFagsakStatus(Fagsak fagsak, Long behandlingId, FagsakStatus nyStatus) {
        var gammelStatus = fagsak.getStatus();
        var fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);

        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(fagsak, behandlingId, gammelStatus, nyStatus);
        }
    }

    private boolean behandlingHarEnkeltOpphør(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatOpphørt)
            .isPresent() &&
            !behandling.harBehandlingÅrsak(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)  && !alleBarnaErDøde(behandling);
    }

    private boolean sakErKobletTIlAnnenPart(Behandling behandling) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()).filter(fagsakRelasjon -> fagsakRelasjon.getFagsakNrTo().isPresent()).isPresent();
    }

    private boolean alleBarnaErDøde(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList())
            .stream().allMatch(b-> b.getDødsdato().isPresent());
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

    private boolean ingenAvslutningsdato(Fagsak fagsak) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak).map(FagsakRelasjon::getAvsluttningsdato).isEmpty();
    }
}
