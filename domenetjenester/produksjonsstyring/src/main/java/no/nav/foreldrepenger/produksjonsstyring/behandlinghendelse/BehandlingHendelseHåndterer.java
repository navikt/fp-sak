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
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.hendelser.behandling.BehandlingHendelse;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.Kildesystem;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.log.util.LoggerUtils;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandlingHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseHåndterer.class);

    private FagsakTjeneste fagsakTjeneste;
    private OppdaterFagsakStatusTjeneste fagsakStatusTjeneste;

    public BehandlingHendelseHåndterer() {
    }

    @Inject
    public BehandlingHendelseHåndterer(FagsakTjeneste fagsakTjeneste, OppdaterFagsakStatusTjeneste fagsakStatusTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.fagsakStatusTjeneste = fagsakStatusTjeneste;
    }

    void handleMessage(String key, String payload) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka gir opp og dør på seg.
        try {
            var mottattHendelse = StandardJsonConfig.fromJson(payload, BehandlingHendelse.class);
            if (Kildesystem.FPTILBAKE.equals(mottattHendelse.getKildesystem()) && mottattHendelse instanceof BehandlingHendelseV1 hendelseV1) {
                handleMessageIntern(hendelseV1);
            }
        } catch (VLException e) {
            LOG.warn("FP-328773 Behandling-Hendelse Feil under parsing av vedtak. key={} payload={}", key, payload, e);
        } catch (Exception e) {
            LOG.warn("Behandling-Hendelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(payload), e);
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
            } else if (!FagsakStatus.UNDER_BEHANDLING.equals(fagsak.getStatus())) {
                fagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, null, BehandlingStatus.UTREDES);
            }
        }
    }

}
