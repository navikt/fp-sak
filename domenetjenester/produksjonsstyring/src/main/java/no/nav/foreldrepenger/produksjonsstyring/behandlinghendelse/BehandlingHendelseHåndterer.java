package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.OppdaterPersonoversiktTask;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaMessageHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.hendelser.behandling.BehandlingHendelse;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.Kildesystem;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingHendelseHåndterer implements KafkaMessageHandler.KafkaStringMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseHåndterer.class);
    private static final String GROUP_ID = "fpsak-behandling-hendelse";  // Hold konstant pga offset commit !!

    private String topicName;
    private FagsakTjeneste fagsakTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private OppdaterFagsakStatusTjeneste fagsakStatusTjeneste;

    public BehandlingHendelseHåndterer() {
    }

    @Inject
    public BehandlingHendelseHåndterer(@KonfigVerdi(value = "kafka.behandlinghendelse.topic") String topicName,
                                       FagsakTjeneste fagsakTjeneste,
                                       ProsessTaskTjeneste taskTjeneste,
                                       OppdaterFagsakStatusTjeneste fagsakStatusTjeneste) {
        this.topicName = topicName;
        this.fagsakTjeneste = fagsakTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.fagsakStatusTjeneste = fagsakStatusTjeneste;
    }

    @Override
    public void handleRecord(String key, String value) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka gir opp og dør på seg.
        try {
            var mottattHendelse = StandardJsonConfig.fromJson(value, BehandlingHendelse.class);
            if (Kildesystem.FPTILBAKE.equals(mottattHendelse.getKildesystem()) && mottattHendelse instanceof BehandlingHendelseV1 hendelseV1) {
                setCallIdForHendelse(hendelseV1);
                handleMessageIntern(hendelseV1);
            }
        } catch (VLException e) {
            LOG.warn("FP-328773 Behandling-Hendelse Feil under parsing av vedtak. key={} payload={}", key, value, e);
        } catch (Exception e) {
            LOG.warn("Behandling-Hendelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(value), e);
        }
    }

    // Foreløpig håndteres bare behandling opprettet/avsluttet i fptilbake her.
    private void handleMessageIntern(BehandlingHendelseV1 mottattHendelse) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(mottattHendelse.getSaksnummer()), true).orElse(null);
        if (fagsak != null) {
            if (Hendelse.AVSLUTTET.equals(mottattHendelse.getHendelse())) {
                if (!FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
                    fagsakStatusTjeneste.lagBehandlingAvsluttetTask(fagsak, null);
                }
                lagPersonoversiktTask(fagsak, mottattHendelse);
            } else if (Hendelse.OPPRETTET.equals(mottattHendelse.getHendelse())) {
                if (!FagsakStatus.UNDER_BEHANDLING.equals(fagsak.getStatus())) {
                    fagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, null, BehandlingStatus.UTREDES);
                }
                lagPersonoversiktTask(fagsak, mottattHendelse);
            } else if (!FagsakStatus.UNDER_BEHANDLING.equals(fagsak.getStatus())){
                fagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, null, BehandlingStatus.UTREDES);
            }
        }
    }

    private void lagPersonoversiktTask(Fagsak fagsak, BehandlingHendelseV1 hendelse) {
        var behandlingType = switch (hendelse.getBehandlingstype()) {
            case TILBAKEBETALING -> BehandlingType.TILBAKEKREVING_ORDINÆR;
            case TILBAKEBETALING_REVURDERING -> BehandlingType.TILBAKEKREVING_REVURDERING;
            default -> null;
        };
        var behandlingStatus = switch (hendelse.getHendelse()) {
            case OPPRETTET -> BehandlingStatus.OPPRETTET;
            case AVSLUTTET -> BehandlingStatus.AVSLUTTET;
            default -> null;
        };
        if (behandlingType == null || behandlingStatus == null) {
            return;
        }
        var behandlingRef = String.format("%s_T%s", Fagsystem.FPSAK.getOffisiellKode(), hendelse.getBehandlingUuid().toString());

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdaterPersonoversiktTask.class);
        prosessTaskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_REF_KEY, behandlingRef);
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_STATUS_KEY, behandlingStatus.getKode());
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_TID_KEY, hendelse.getTidspunkt().toString());
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_TYPE_KEY, behandlingType.getKode());
        taskTjeneste.lagre(prosessTaskData);
    }

    private static void setCallIdForHendelse(BehandlingHendelseV1 hendelse) {
        MDCOperations.putCallId(hendelse.getBehandlingUuid().toString());
    }

    @Override
    public String topic() {
        return topicName;
    }

    @Override
    public String groupId() {
        return GROUP_ID;
    }
}
