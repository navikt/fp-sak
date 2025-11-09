package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public interface Årsak extends Kodeverdi {

    Årsak UKJENT = new Årsak() {

        @Override
        public String getNavn() {
            return "Ikke definert";
        }

        @Override
        public String getKodeverk() {
            return "AARSAK_TYPE";
        }

        @Override
        public String getKode() {
            return Kodeverdi.STANDARDKODE_UDEFINERT;
        }

    };
}
