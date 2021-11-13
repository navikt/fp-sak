package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.util.Comparator;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.EndringsSøknadUtsettelseUttak;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;

@NamespaceRef(SøknadConstants.NAMESPACE)
public class EndringUtsettelseUttak {

    public static EndringsSøknadUtsettelseUttak ekstraherUtsettelseUttakFra(SøknadWrapper wrapper, MottattDokument mottattDokument) {
        if (!(wrapper.getOmYtelse() instanceof final Endringssoeknad omYtelse) || !erEndring(mottattDokument)) {
            throw new IllegalArgumentException("Ikke endringssøknad");
        }
        var perioder = omYtelse.getFordeling().getPerioder();

        var utsettelseFom = perioder.stream()
            .filter(p -> p instanceof Utsettelsesperiode || p instanceof Oppholdsperiode)
            .map(LukketPeriodeMedVedlegg::getFom)
            .min(Comparator.naturalOrder()).orElse(null);
        var uttakFom = perioder.stream()
            .filter(p -> p instanceof Uttaksperiode || p instanceof Overfoeringsperiode)
            .map(LukketPeriodeMedVedlegg::getFom)
            .min(Comparator.naturalOrder()).orElse(null);

        return new EndringsSøknadUtsettelseUttak(utsettelseFom, uttakFom);
    }

    private static boolean erEndring(MottattDokument mottattDokument) {
        return mottattDokument.getDokumentType().equals(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
    }

}
