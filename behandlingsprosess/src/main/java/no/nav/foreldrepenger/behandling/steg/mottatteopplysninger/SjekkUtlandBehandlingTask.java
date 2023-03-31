package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask("oppgavebehandling.sjekk.utland")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class SjekkUtlandBehandlingTask implements ProsessTaskHandler {

    public static final String BEHANDLING_ID = "behandlingId";

    private final RegistrerFagsakEgenskaper registrerFagsakEgenskaper;
    private final BehandlingRepository behandlingRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final BehandlendeEnhetTjeneste enhetTjeneste;

    @Inject
    public SjekkUtlandBehandlingTask(RegistrerFagsakEgenskaper registrerFagsakEgenskaper,
                                     BehandlingRepository behandlingRepository,
                                     BehandlendeEnhetTjeneste enhetTjeneste,
                                     YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.registrerFagsakEgenskaper =registrerFagsakEgenskaper;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.behandlingRepository = behandlingRepository;
        this.enhetTjeneste = enhetTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandling = behandlingRepository.hentBehandling(Long.valueOf(prosessTaskData.getPropertyValue(BEHANDLING_ID)));
        var sjekkEØS = FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) && ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .filter(UttakOmsorgUtil::oppgittAnnenForelderTilknytningEØS)
            .isPresent();
        var utland = registrerFagsakEgenskaper.registrerFagsakEgenskaper(behandling, sjekkEØS);
        if (FagsakMarkering.BOSATT_UTLAND.equals(utland) && !BehandlendeEnhetTjeneste.erUtlandsEnhet(behandling)) {
            enhetTjeneste.oppdaterBehandlendeEnhetUtland(behandling, HistorikkAktør.VEDTAKSLØSNINGEN, "Søknadsopplysninger");
        }
    }

}
