package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface InntektsmeldingFeil extends DeklarerteFeil {

    InntektsmeldingFeil FACTORY = FeilFactory.create(InntektsmeldingFeil.class);

    @TekniskFeil(feilkode = "FP-938211", feilmelding = "Fant ikke informasjon om arbeidsforhold på inntektsmelding", logLevel = WARN)
    Feil manglendeInformasjon();

    @TekniskFeil(feilkode = "FP-183452", feilmelding = "Fant ikke informasjon om arbeidsgiver på inntektsmelding", logLevel = WARN)
    Feil manglendeArbeidsgiver();

    @TekniskFeil(feilkode = "FP-159641", feilmelding = "Fant ikke personident for arbeidsgiver som er privatperson i TPS", logLevel = WARN)
    Feil finnerIkkeArbeidsgiverITPS();
}
