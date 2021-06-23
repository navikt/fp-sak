package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

public interface UtsettelseDokKontrollerer {

    boolean måSaksbehandlerManueltBekrefte(OppgittPeriodeEntitet søknadsperiode);
}
