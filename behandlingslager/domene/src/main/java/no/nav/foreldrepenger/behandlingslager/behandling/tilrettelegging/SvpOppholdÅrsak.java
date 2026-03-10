package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;

public enum SvpOppholdÅrsak implements DatabaseKode {
    // Ikke endre rekkefølgen på disse - de er lagret i databasen som 0,1,2 osv.
    SYKEPENGER,
    FERIE
}
