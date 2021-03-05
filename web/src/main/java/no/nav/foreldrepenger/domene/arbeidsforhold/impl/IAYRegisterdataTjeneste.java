package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;

@SuppressWarnings("unused")
@ApplicationScoped
public class IAYRegisterdataTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(IAYRegisterdataTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public IAYRegisterdataTjeneste() {
    }

    @Inject
    public IAYRegisterdataTjeneste(InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
    }

    public void håndterCallback(RegisterdataCallback callback) {
        // TODO (Frode C.) - fortsett prosess
    }
}
