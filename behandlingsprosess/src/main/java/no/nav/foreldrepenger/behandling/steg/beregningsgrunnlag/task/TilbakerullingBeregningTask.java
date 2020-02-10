package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(TilbakerullingBeregningTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class TilbakerullingBeregningTask implements ProsessTaskHandler {

    public static final String TASKNAME = "beregning.tilbakerullingAvSaker";

    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    TilbakerullingBeregningTask() {
        // for CDI proxy
    }

    @Inject
    public TilbakerullingBeregningTask(BehandlingRepository behandlingRepository,
                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                       HistorikkRepository historikkRepository,
                                       ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                       BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.historikkRepository = historikkRepository;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if(!erBehandlingBerørt(behandling) && !behandling.erSaksbehandlingAvsluttet() && behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)){
            hoppTilbake(behandling, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        }
    }

    private void hoppTilbake(Behandling behandling, BehandlingStegType tilSteg) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        doHoppTilSteg(behandling, kontekst, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        prosesseringAsynkTjeneste.asynkProsesserBehandlingMergeGruppe(behandling);
    }

    private void doHoppTilSteg(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingStegType tilSteg) {
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
    }

    private boolean erBehandlingBerørt(Behandling behandling){
        return behandling.getBehandlingÅrsaker().stream().anyMatch(b -> BehandlingÅrsakType.BERØRT_BEHANDLING.equals(b.getBehandlingÅrsakType()));
    }

    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        Historikkinnslag nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        String fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
            .medBegrunnelse("Behandlingen er flyttet fra " + fraStegNavn + " tilbake til " + tilStegNavn);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

}
