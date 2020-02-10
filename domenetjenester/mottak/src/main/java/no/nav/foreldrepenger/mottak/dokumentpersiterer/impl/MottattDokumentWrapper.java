package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v2.MottattDokumentWrapperInntektsmelding;

public abstract class MottattDokumentWrapper<S> {

    private S skjema;
    private String namespace;

    protected MottattDokumentWrapper(S skjema, String namespace) {
        this.skjema = skjema;
        this.namespace = namespace;
    }

    @SuppressWarnings("rawtypes")
    public static MottattDokumentWrapper tilXmlWrapper(Object skjema) {
        if (skjema instanceof no.nav.vedtak.felles.xml.soeknad.v3.Soeknad) {
            return new no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentWrapperSøknad((no.nav.vedtak.felles.xml.soeknad.v3.Soeknad) skjema);
        } else if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM) {
            return new no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1.MottattDokumentWrapperInntektsmelding((no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM) skjema);
        } else if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM) {
            return new MottattDokumentWrapperInntektsmelding((no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM) skjema);
        }
        throw MottattDokumentFeil.FACTORY.ukjentSkjemaType(skjema.getClass().getCanonicalName()).toException();
    }

    public S getSkjema() {
        return this.skjema;
    }

    String getSkjemaType() {
        return this.namespace;
    }
}
