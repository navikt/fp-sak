package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LukkForespørselForMottattIMObserver {
    private static final Logger LOG = LoggerFactory.getLogger(LukkForespørselForMottattIMObserver.class);
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    public LukkForespørselForMottattIMObserver() {
        //CDI
    }

    public LukkForespørselForMottattIMObserver(FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste) {
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
    }

    public void observerLukkForespørselForMotattImEvent(@Observes LukkForespørselForMottattImEvent event) {
        var maskertOrgnr = tilMaskertNummer(event.getOrgNummer().getId());
        LOG.info("Mottatt LukkForespørselForMottattImEvent for behandlingId {} med saksnummer {} og orgnummer {}", event.getBehandlingId(), event.getSaksnummer(), maskertOrgnr);
        fpInntektsmeldingTjeneste.lagLukkForespørselTask(event.behandling(), event.getOrgNummer());
    }
}
