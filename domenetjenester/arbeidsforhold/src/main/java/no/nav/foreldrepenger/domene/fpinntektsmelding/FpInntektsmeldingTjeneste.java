package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpInntektsmeldingTjeneste.class);
    private FpinntektsmeldingKlient klient;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpinntektsmeldingKlient klient, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.klient = klient;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void lagForespørsel(String ag, BehandlingReferanse ref) {
        // Toggler av for prod og lokalt, ikke støtte lokalt
        if (!Environment.current().isDev()) {
            return;
        }
        if (!OrganisasjonsNummerValidator.erGyldig(ag)) {
            return;
        }
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        ref.medSkjæringstidspunkt(skjæringstidspunkter);
        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()),
            new OpprettForespørselRequest.OrganisasjonsnummerDto(ag), ref.getUtledetSkjæringstidspunkt(), mapYtelsetype(ref.fagsakYtelseType()),
            new OpprettForespørselRequest.SaksnummerDto(ref.saksnummer().getVerdi()));

        try {
            klient.opprettForespørsel(request);
        } catch (Exception e) {
            LOG.warn("Feil ved oversending til fpinntektsmelding med forespørsel: " + request + " Fikk feil: " + e);
        }
    }

    private OpprettForespørselRequest.YtelseType mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> OpprettForespørselRequest.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT, ENGANGSTØNAD -> throw new IllegalArgumentException("Kan ikke opprette forespørsel for ytelsetype " + fagsakYtelseType);
        };
    }
}
