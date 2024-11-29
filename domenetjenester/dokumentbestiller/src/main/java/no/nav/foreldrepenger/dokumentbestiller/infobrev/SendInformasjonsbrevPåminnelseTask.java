package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "opprettsak.informasjonsbrevPåminnelse", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendInformasjonsbrevPåminnelseTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SendInformasjonsbrevPåminnelseTask.class);

    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    SendInformasjonsbrevPåminnelseTask() {
        // for CDI proxy
    }

    @Inject
    public SendInformasjonsbrevPåminnelseTask(BehandlingRepositoryProvider repositoryProvider,
                                              BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                              PersoninfoAdapter personinfoAdapter,
                                              BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                              DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                              UttakInputTjeneste uttakInputTjeneste,
                                              StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                              RelatertBehandlingTjeneste relatertBehandlingTjeneste) {
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakFar = fagsakRepository.finnEksaktFagsakReadOnly(prosessTaskData.getFagsakId());

        var aktørId = fagsakFar.getAktørId();
        var bruker = hentPersonInfo(aktørId);
        if (bruker.dødsdato() != null) {
            return; // Unngå brev til død bruker
        }

        //Sjekk at det finnes fedrekvote igjen
        var behandlingMor = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(fagsakFar.getSaksnummer()).orElseThrow();
        var uttakInput = uttakInputTjeneste.lagInput(behandlingMor);
        var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);

        if (!saldoUtregning.saldoITrekkdager(Stønadskontotype.FEDREKVOTE).merEnn0()) {
            return;
        }

        var brukEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsakFar);
        var eksisterendeBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakFar.getId());
        if (eksisterendeBehandling.isPresent() && !eksisterendeBehandling.get().erAvsluttet()) {
            var dokumentBestilling = DokumentBestilling.builder()
                .medBehandlingUuid(eksisterendeBehandling.get().getUuid())
                .medDokumentMal(DokumentMalType.FORELDREPENGER_INFO_TIL_ANNEN_FORELDER)
                .build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling, HistorikkAktør.VEDTAKSLØSNINGEN);
        } else {
            var behandling = opprettFørstegangsbehandlingTilInfobrev(fagsakFar, brukEnhet);
            behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            LOG.info("Opprettet informasjonsbehandling {} på fagsak {}", behandling.getId(), fagsakFar.getSaksnummer().getVerdi());
        }
    }

    private Behandling opprettFørstegangsbehandlingTilInfobrev(Fagsak fagsak, OrganisasjonsEnhet enhet) {
        return behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, enhet, BehandlingÅrsakType.INFOBREV_PÅMINNELSE);
    }

    private PersoninfoBasis hentPersonInfo(AktørId aktørId) {
        return personinfoAdapter.hentBrukerBasisForAktør(FagsakYtelseType.FORELDREPENGER, aktørId)
            .orElseThrow(() -> new TekniskException("FP-442142", String.format("Fant ingen ident for aktør %s.", aktørId)));
    }
}
