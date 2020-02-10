package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.util.List;

import javax.xml.bind.JAXBElement;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Vedlegg;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Ytelse;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Spraakkode;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

public class MottattDokumentWrapperSøknad extends MottattDokumentWrapper<Soeknad> {

    public MottattDokumentWrapperSøknad(Soeknad skjema) {
        super(skjema, SøknadConstants.NAMESPACE);
        sjekkNødvendigeFeltEksisterer(getSkjema());
    }

    public static void sjekkNødvendigeFeltEksisterer(Soeknad søknad) {
        if (søknad.getMottattDato() == null || søknad.getOmYtelse() == null || søknad.getSoeker() == null) {
            throw MottattDokumentFeil.FACTORY.ukjentSoeknadXMLFormat(søknad.getClass().getCanonicalName()).toException();
        }
    }

    public String getBegrunnelseForSenSoeknad() {
        return getSkjema().getBegrunnelseForSenSoeknad();
    }

    public String getTilleggsopplysninger() {
        return getSkjema().getTilleggsopplysninger();
    }

    public Spraakkode getSpråkvalg() {
        return getSkjema().getSprakvalg();
    }

    public List<Vedlegg> getPåkrevdVedleggListe() {
        return getSkjema().getPaakrevdeVedlegg();
    }

    public List<Vedlegg> getIkkePåkrevdVedleggListe() {
        return getSkjema().getAndreVedlegg();
    }

    public Bruker getBruker() {
        return getSkjema().getSoeker();
    }

    public Ytelse getOmYtelse() {
        sjekkNødvendigeFeltEksisterer(getSkjema());
        return getSkjema().getOmYtelse().getAny().stream()
            .filter(o -> o instanceof JAXBElement)
            .map(o -> (Ytelse) ((JAXBElement<?>) o).getValue())
            .findFirst().orElse(null);
    }
}
