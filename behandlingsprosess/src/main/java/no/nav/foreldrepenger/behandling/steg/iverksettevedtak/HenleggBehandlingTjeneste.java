package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class HenleggBehandlingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private SøknadRepository søknadRepository;
    private HistorikkRepository historikkRepository;


    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            DokumentBestillerTjeneste dokumentBestillerTjeneste,
            ProsessTaskTjeneste taskTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
    }

    public void henleggBehandling(Long behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, false);
    }

    private void doHenleggBehandling(Long behandlingId, BehandlingResultatType årsakKode, String begrunnelse, boolean avbrytVentendeAutopunkt) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (avbrytVentendeAutopunkt && behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        } else {
            håndterHenleggelseUtenOppgitteSøknadsopplysninger(behandling, kontekst);
        }
        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)
                || BehandlingResultatType.HENLAGT_KLAGE_TRUKKET.equals(årsakKode)
                || BehandlingResultatType.HENLAGT_INNSYN_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling);
        }
        lagHistorikkinnslagForHenleggelse(behandlingId, årsakKode, begrunnelse, HistorikkAktør.SAKSBEHANDLER);

        if (behandling.erYtelseBehandling() && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            startTaskForDekøingAvBerørtBehandling(behandling);
        }
    }

    public void henleggBehandlingAvbrytAutopunkter(Long behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, true);
    }

    public void lagHistorikkInnslagForHenleggelseFraSteg(Long behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        lagHistorikkinnslagForHenleggelse(behandlingId, årsakKode, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private void håndterHenleggelseUtenOppgitteSøknadsopplysninger(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var søknad = søknadRepository.hentSøknad(behandling.getId());
        if (søknad == null) {
            // Må ta behandling av vent for å tillate henleggelse (krav i
            // Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        }
    }

    private void startTaskForDekøingAvBerørtBehandling(Behandling behandling) {
        var taskData = ProsessTaskData.forProsessTask(StartBerørtBehandlingTask.class);
        taskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        taskTjeneste.lagre(taskData);
    }

    private void sendHenleggelsesbrev(Behandling behandling) {
        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(DokumentMalType.INFO_OM_HENLEGGELSE)
            .build();
        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private void lagHistorikkinnslagForHenleggelse(Long behandlingsId, BehandlingResultatType aarsak, String begrunnelse, HistorikkAktør aktør) {
        var builder = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
                .medÅrsak(aarsak)
                .medBegrunnelse(begrunnelse);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandlingsId);
        builder.build(historikkinnslag);

        historikkinnslag.setAktør(aktør);
        historikkRepository.lagre(historikkinnslag);
    }
}
