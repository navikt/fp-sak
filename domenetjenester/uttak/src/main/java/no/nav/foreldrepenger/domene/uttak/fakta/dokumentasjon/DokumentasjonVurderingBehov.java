package no.nav.foreldrepenger.domene.uttak.fakta.dokumentasjon;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

public record DokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode, Behov behov, DokumentasjonVurdering vurdering) {

    public boolean måVurderes() {
        return behov != null && vurdering == null;
    }

    public record Behov(Behov.Type type, Behov.Årsak årsak) {
        public enum Type {
            UTSETTELSE,
            TIDLIG_OPPSTART_FAR,
            OVERFØRING,
            AKTIVITETSKRAV,
            ;
        }

        public enum Årsak {
            INNLEGGELSE_SØKER,
            INNLEGGELSE_BARN,
            HV_OVELSE,
            NAV_TILTAK,
            SYKDOM_SØKER

        }
    }
}
