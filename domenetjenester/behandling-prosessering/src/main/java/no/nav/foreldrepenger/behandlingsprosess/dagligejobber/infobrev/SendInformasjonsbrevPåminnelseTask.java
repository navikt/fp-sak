package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("opprettsak.informasjonsbrevPåminnelse")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendInformasjonsbrevPåminnelseTask implements ProsessTaskHandler {

    public static final String FAGSAK_ID_KEY = "fagsakId";

    private static final Logger LOG = LoggerFactory.getLogger(SendInformasjonsbrevPåminnelseTask.class);

    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;

    SendInformasjonsbrevPåminnelseTask() {
        // for CDI proxy
    }

    @Inject
    public SendInformasjonsbrevPåminnelseTask(BehandlingRepositoryProvider repositoryProvider,
                                              BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                              PersoninfoAdapter personinfoAdapter,
                                              BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                              DokumentBestillerTjeneste dokumentBestillerTjeneste) {
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var aktørId = new AktørId(prosessTaskData.getAktørId());
        var bruker = hentPersonInfo(aktørId);
        if (bruker.dødsdato() != null) {
            return; // Unngå brev til død bruker
        }

        var fagsak = fagsakRepository.finnEksaktFagsakReadOnly(Long.parseLong(prosessTaskData.getPropertyValue(FAGSAK_ID_KEY)));
        var brukEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);

        var eksisterendeBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (eksisterendeBehandling.isPresent() && !eksisterendeBehandling.get().erAvsluttet()) {
            var bestillBrevDto = new BestillBrevDto(eksisterendeBehandling.get().getId(), eksisterendeBehandling.get().getUuid(), DokumentMalType.FORELDREPENGER_INFO_TIL_ANNEN_FORELDER);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
        } else {
            var behandling = opprettFørstegangsbehandlingTilInfobrev(fagsak, brukEnhet);
            behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            LOG.info("Opprettet informasjonsbehandling {} på fagsak {}", behandling.getId(), fagsak.getSaksnummer().getVerdi());
        }
    }

    private Behandling opprettFørstegangsbehandlingTilInfobrev(Fagsak fagsak, OrganisasjonsEnhet enhet) {
        return behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, enhet, BehandlingÅrsakType.INFOBREV_PÅMINNELSE);
    }

    private PersoninfoBasis hentPersonInfo(AktørId aktørId) {
        return personinfoAdapter.hentBrukerBasisForAktør(aktørId)
            .orElseThrow(() -> new TekniskException("FP-442142", String.format("Fant ingen ident for aktør %s.", aktørId)));
    }
}
