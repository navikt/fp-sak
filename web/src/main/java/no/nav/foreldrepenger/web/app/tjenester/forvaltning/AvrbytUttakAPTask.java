package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET;

import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@Dependent
@ProsessTask(value = "migrering.yf.avbrytap", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AvrbytUttakAPTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AvrbytUttakAPTask.class);

    private InformasjonssakRepository informasjonssakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsprosessTjeneste prosesseringTjeneste;

    @Inject
    public AvrbytUttakAPTask(InformasjonssakRepository informasjonssakRepository,
                             ProsessTaskTjeneste taskTjeneste,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingsprosessTjeneste prosesseringTjeneste) {
        this.informasjonssakRepository = informasjonssakRepository;
        this.taskTjeneste = taskTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosesseringTjeneste = prosesseringTjeneste;
    }

    AvrbytUttakAPTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        var gamleAP = Set.of(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
            AksjonspunktDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK, AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING,
            AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO, AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG,
            AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET, AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV);
        var behandlinger = informasjonssakRepository.finnBehandlingerMedGamleUttakAP(gamleAP);
        if (behandlinger.isEmpty()) {
            LOG.info("Migrering uttak - ingen flere kandidater for migrering");
            return;
        }
        behandlinger.forEach(b -> {
            LOG.info("Migrering uttak - avbryter AP for behandling {}", b);
            avbrytOgFortsett(b, gamleAP);
        });

        var task = ProsessTaskDataBuilder.forProsessTask(AvrbytUttakAPTask.class)
            .medCallId(callId)
            .medPrioritet(100)
            .build();
        taskTjeneste.lagre(task);
    }

    private void avbrytOgFortsett(Long behandlingId, Set<AksjonspunktDefinisjon> gamleAP) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = prosesseringTjeneste.hentBehandling(behandlingId);
        var aksjonspunkter = behandling.getÅpneAksjonspunkter(gamleAP);
        LOG.info("Migrering uttak - avbryter {} for behandling {}", aksjonspunkter, behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkter);

        if (behandling.isBehandlingPåVent()) {
            LOG.info("Migrering uttak - behandling på vent {}", behandlingId);
            return;
        }
        if (behandling.erAvsluttet()) {
            LOG.info("Migrering uttak - behandling avsluttet {}", behandlingId);
            return;
        }
        if (behandling.getAktivtBehandlingSteg() != BehandlingStegType.KONTROLLER_AKTIVITETSKRAV) {
            LOG.info("Migrering uttak - behandling står i steg {} {} {}", behandlingId, behandling.getAktivtBehandlingSteg(),
                aksjonspunkter);
        }
        LOG.info("Migrering uttak - fortsetter behandling {}", behandlingId);
        prosesseringTjeneste.asynkKjørProsess(behandling);
    }
}
