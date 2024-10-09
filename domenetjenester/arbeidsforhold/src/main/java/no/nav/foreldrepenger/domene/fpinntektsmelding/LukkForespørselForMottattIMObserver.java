package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@ApplicationScoped
public class LukkForespørselForMottattIMObserver {
    private static final Logger LOG = LoggerFactory.getLogger(LukkForespørselForMottattIMObserver.class);
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    public LukkForespørselForMottattIMObserver() {
        //CDI
    }
    @Inject
    public LukkForespørselForMottattIMObserver(FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste) {
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
    }

    public void observerLukkForespørselForMotattImEvent(@Observes LukkForespørselForMottattImEvent event) {
        var maskertOrgnr = tilMaskertNummer(event.getOrgNummer().getId());
        LOG.info("Mottatt LukkForespørselForMottattImEvent for behandlingId {} med saksnummer {} og orgnummer {}", event.getBehandlingId(), event.getSaksnummer(), maskertOrgnr);
        fpInntektsmeldingTjeneste.lagLukkForespørselTask(event.behandling(), event.getOrgNummer());
    }
}
