package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v2.InntektsmeldingWrapper;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadWrapper;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;

public abstract class MottattDokumentWrapper<S> {

    private final S skjema;
    private final String namespace;

    protected MottattDokumentWrapper(S skjema, String namespace) {
        this.skjema = skjema;
        this.namespace = namespace;
    }

    @SuppressWarnings("rawtypes")
    public static MottattDokumentWrapper tilXmlWrapper(Object skjema) {
        if (skjema instanceof Soeknad soeknad) {
            return new SøknadWrapper(soeknad);
        }
        if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM im) {
            return new no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1.InntektsmeldingWrapper(im);
        }
        if (skjema instanceof InntektsmeldingM im) {
            return new InntektsmeldingWrapper(im);
        }
        throw MottattDokumentFeil.ukjentSkjemaType(skjema.getClass().getCanonicalName());
    }

    public S getSkjema() {
        return this.skjema;
    }

    String getSkjemaType() {
        return this.namespace;
    }
}
