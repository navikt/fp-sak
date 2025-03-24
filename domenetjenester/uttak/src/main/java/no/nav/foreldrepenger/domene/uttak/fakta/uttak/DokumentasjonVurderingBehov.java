package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

public record DokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode,
                                          Behov behov,
                                          DokumentasjonVurdering vurdering,
                                          RegisterVurdering registerVurdering) {

    public DokumentasjonVurderingBehov {
        Objects.requireNonNull(behov);
    }

    public DokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode, Behov behov, DokumentasjonVurdering vurdering) {
        this(oppgittPeriode, behov, vurdering, null);
    }

    public boolean måVurderes() {
        if (vurdering == null) {
            return registerVurdering != RegisterVurdering.MORS_AKTIVITET_GODKJENT;
        }
        return false;
    }

    public boolean harSaksbehandlerVurdering() {
        return vurdering != null;
    }

    public record Behov(Behov.Type type, Behov.Årsak årsak) {
        public enum Type {
            UTSETTELSE,
            OVERFØRING,
            UTTAK,
        }

        public enum Årsak {
            //Utsettelse
            INNLEGGELSE_SØKER,
            INNLEGGELSE_BARN,
            HV_ØVELSE,
            NAV_TILTAK,
            SYKDOM_SØKER,

            //Overføring
            INNLEGGELSE_ANNEN_FORELDER,
            SYKDOM_ANNEN_FORELDER,
            BARE_SØKER_RETT,
            ALENEOMSORG,

            AKTIVITETSKRAV_ARBEID,
            AKTIVITETSKRAV_UTDANNING,
            AKTIVITETSKRAV_KVALPROG,
            AKTIVITETSKRAV_INTROPROG,
            AKTIVITETSKRAV_TRENGER_HJELP,
            AKTIVITETSKRAV_INNLAGT,
            AKTIVITETSKRAV_ARBEID_OG_UTDANNING,
            AKTIVITETSKRAV_IKKE_OPPGITT,
            TIDLIG_OPPSTART_FAR,
        }
    }
}
