package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaMessageHandler;
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
    private OppdaterFagsakStatusTjeneste fagsakStatusTjeneste;

    public BehandlingHendelseHåndterer() {
    }

    @Inject
    public BehandlingHendelseHåndterer(@KonfigVerdi(value = "kafka.behandlinghendelse.topic") String topicName,
                                       FagsakTjeneste fagsakTjeneste,
                                       OppdaterFagsakStatusTjeneste fagsakStatusTjeneste) {
        this.topicName = topicName;
        this.fagsakTjeneste = fagsakTjeneste;
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
            } else if (Hendelse.OPPRETTET.equals(mottattHendelse.getHendelse())) {
                if (!FagsakStatus.UNDER_BEHANDLING.equals(fagsak.getStatus())) {
                    fagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, null, BehandlingStatus.UTREDES);
                }
            } else if (!FagsakStatus.UNDER_BEHANDLING.equals(fagsak.getStatus())){
                fagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, null, BehandlingStatus.UTREDES);
            }
        }
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
